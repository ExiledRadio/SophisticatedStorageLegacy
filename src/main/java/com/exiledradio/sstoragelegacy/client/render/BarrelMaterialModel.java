/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.client.render;

import com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy;
import com.exiledradio.sstoragelegacy.block.BarrelBlock;
import com.exiledradio.sstoragelegacy.block.BarrelMaterial;
import com.exiledradio.sstoragelegacy.block.LimitedBarrelBlock;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.StorageTier;
import com.exiledradio.sstoragelegacy.block.TintMode;
import com.exiledradio.sstoragelegacy.block.VerticalFacing;
import com.exiledradio.sstoragelegacy.block.WoodType;
import com.exiledradio.sstoragelegacy.common.gui.BlockSide;
import com.exiledradio.sstoragelegacy.init.ModContent;
import com.exiledradio.sstoragelegacy.item.BarrelMaterials;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.property.IExtendedBlockState;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Barrels and limited barrels with materials (the original's BarrelBakedModelBase, materials part):
 * each part takes the look of the block named for it. Barrels without materials keep their
 * multipart blockstate models; this model wraps those and only takes over when the extended state
 * carries materials. Layers, in the original's order: the tints that are set, then the material
 * models (core and trim separately when a material names only one of them, else the whole
 * barrel, with the wood's textures where no material is set), then the tier corners, then packing
 * tape or the lock. A material's texture on each world side is its block's own texture on the
 * face that side stands for (the front shows the block's north face, the top its up face...).
 * Material quads keep no tint, as in the original.
 */
public final class BarrelMaterialModel {
    /** The item model barrels with materials use (see ModelRegistration). */
    public static final ModelResourceLocation ITEM_MODEL = new ModelResourceLocation(SophisticatedStorageLegacy.MODID + ":barrel/materials", "inventory");
    private static final String PART_PATH = SophisticatedStorageLegacy.MODID + ":block/barrel/";
    private static final BarrelMaterial[] PARTICLE_MATERIALS = {BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL, BarrelMaterial.TOP};
    /** Blockstate rotations of regular barrels, by facing (tools/gen_barrel_models.py). */
    private static final Map<EnumFacing, int[]> BARREL_ROTATIONS = new EnumMap<>(EnumFacing.class);

    static {
        BARREL_ROTATIONS.put(EnumFacing.DOWN, new int[] {180, 180});
        BARREL_ROTATIONS.put(EnumFacing.UP, new int[] {0, 180});
        BARREL_ROTATIONS.put(EnumFacing.NORTH, new int[] {270, 180});
        BARREL_ROTATIONS.put(EnumFacing.SOUTH, new int[] {270, 0});
        BARREL_ROTATIONS.put(EnumFacing.WEST, new int[] {270, 90});
        BARREL_ROTATIONS.put(EnumFacing.EAST, new int[] {270, 270});
    }

    private static JsonObject descriptor = new JsonObject();
    private static final Map<String, IModel> MODELS = new HashMap<>();
    private static final Cache<String, List<BakedQuad>> QUADS = CacheBuilder.newBuilder().maximumSize(8192).expireAfterAccess(5, TimeUnit.MINUTES).build();
    private static final Cache<String, String> SPRITES = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    private BarrelMaterialModel() {}

    // ---- Setup -----------------------------------------------------------------------------

    /** Wraps every barrel and limited barrel block model, and puts the item model in place. */
    public static void onModelBake(ModelBakeEvent event) {
        QUADS.invalidateAll();
        SPRITES.invalidateAll();
        MODELS.clear();
        try (Reader reader = new InputStreamReader(Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation(SophisticatedStorageLegacy.MODID, "barrel_materials.json")).getInputStream(), StandardCharsets.UTF_8)) {
            descriptor = new JsonParser().parse(reader).getAsJsonObject();
        } catch (Exception e) {
            SophisticatedStorageLegacy.LOGGER.error("Couldn't read barrel_materials.json; barrel materials won't show", e);
            return;
        }
        // Only our barrels' own model keys, from their state mappers: going through every key in the
        // registry and fetching each model forces mods like VintageFix, which bake models only when
        // first asked for, to bake every model in the game here (a load that looks frozen).
        net.minecraft.client.renderer.block.statemap.BlockStateMapper mapper = event.getModelManager().getBlockModelShapes().getBlockStateMapper();
        int wrapped = 0;
        java.util.Set<String> modelTypes = new java.util.TreeSet<>();
        for (StorageBlockBase block : ModContent.getAllStorages()) {
            Kind kind = Kind.of(block);
            if (kind == null) {
                continue;
            }
            for (ModelResourceLocation key : new java.util.HashSet<>(mapper.getVariants(block).values())) {
                IBakedModel original = event.getModelRegistry().getObject(key);
                if (original != null && !(original instanceof BlockModel)) {
                    event.getModelRegistry().putObject(key, new BlockModel(original, kind));
                    wrapped++;
                    modelTypes.add(original.getClass().getSimpleName());
                }
            }
        }
        SophisticatedStorageLegacy.LOGGER.info("Barrel models: {} wrapped for materials ({})", wrapped, String.join(", ", modelTypes));
        IBakedModel itemReference = event.getModelRegistry().getObject(new ModelResourceLocation(SophisticatedStorageLegacy.MODID + ":barrel/acacia", "inventory"));
        if (itemReference != null) {
            event.getModelRegistry().putObject(ITEM_MODEL, new ItemModel(itemReference));
        }
    }

    private static IModel getModel(String location) {
        return MODELS.computeIfAbsent(location, l -> ModelLoaderRegistry.getModelOrMissing(new ResourceLocation(l)));
    }

    /** A barrel kind: regular (any tier) or a limited barrel of 1 to 4 slots. */
    private static final class Kind {
        final String key;
        final String partPrefix;
        final boolean limited;
        final StorageTier tier;

        Kind(String key, String partPrefix, boolean limited, StorageTier tier) {
            this.key = key;
            this.partPrefix = partPrefix;
            this.limited = limited;
            this.tier = tier;
        }

        @Nullable
        static Kind of(Block block) {
            if (block instanceof BarrelBlock) {
                return new Kind("barrel", "", false, ((BarrelBlock) block).getTier());
            }
            if (block instanceof LimitedBarrelBlock) {
                int slots = ((LimitedBarrelBlock) block).getSlotCount();
                return new Kind("limited_barrel_" + slots, "limited_" + slots + "_", true, ((LimitedBarrelBlock) block).getTier());
            }
            return null;
        }
    }

    /** Everything about a barrel's look that its quads depend on. */
    private static final class Look {
        final Kind kind;
        final Map<BarrelMaterial, String> materials;
        final String wood;
        final boolean flat;
        final boolean open;
        final boolean hasMain;
        final boolean hasAccent;
        final boolean showTier;
        final boolean packed;
        final boolean locked;
        final EnumFacing horizontal;
        final VerticalFacing vertical;
        /** Model x and y rotation; {0, 0} for the item. */
        final int[] rotation;

        Look(Kind kind, Map<BarrelMaterial, String> materials, String wood, boolean flat, boolean open, boolean hasMain, boolean hasAccent,
             boolean showTier, boolean packed, boolean locked, EnumFacing horizontal, VerticalFacing vertical, int[] rotation) {
            this.kind = kind;
            this.materials = materials;
            this.wood = wood;
            this.flat = flat;
            this.open = open;
            this.hasMain = hasMain;
            this.hasAccent = hasAccent;
            this.showTier = showTier;
            this.packed = packed;
            this.locked = locked;
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.rotation = rotation;
        }

        String cacheKey() {
            return kind.key + kind.tier + materials + wood + flat + open + hasMain + hasAccent + showTier + packed + locked + horizontal + vertical
                    + rotation[0] + "," + rotation[1];
        }
    }

    // ---- Quads -----------------------------------------------------------------------------

    private static List<BakedQuad> getQuads(Look look, @Nullable EnumFacing side) {
        String key = look.cacheKey() + side;
        try {
            return QUADS.get(key, () -> buildQuads(look, side));
        } catch (ExecutionException e) {
            SophisticatedStorageLegacy.LOGGER.error("Couldn't build barrel material quads", e);
            return Collections.emptyList();
        }
    }

    private static List<BakedQuad> buildQuads(Look look, @Nullable EnumFacing side) {
        ModelRotation rotation = ModelRotation.getModelRotation(look.rotation[0], look.rotation[1]);
        String flat = look.flat ? "_flat" : "";
        String prefix = PART_PATH + look.kind.partPrefix;
        List<BakedQuad> quads = new ArrayList<>();
        if (look.hasAccent) {
            quads.addAll(partQuads(prefix + "tintable_accent" + flat, rotation, side));
        }
        if (look.hasMain) {
            String open = look.open && !look.kind.limited ? "_open" : "";
            quads.addAll(partQuads(prefix + "tintable_main" + open + flat, rotation, side));
        }
        JsonObject entry = descriptor.getAsJsonObject(look.kind.key).getAsJsonObject(look.flat ? "flat" : "normal");
        boolean coreKey = false;
        boolean trimKey = false;
        for (BarrelMaterial material : look.materials.keySet()) {
            coreKey |= material.getPart() == BarrelMaterial.Part.CORE;
            trimKey |= material.getPart() == BarrelMaterial.Part.TRIM;
        }
        Map<String, String> textures = getTextures(look, entry, side);
        if (coreKey || trimKey) {
            if (!look.hasMain || coreKey) {
                quads.addAll(materialQuads(entry.get("core").getAsString(), textures, rotation, side));
            }
            if (!look.hasAccent || trimKey) {
                quads.addAll(materialQuads(entry.get("trim").getAsString(), textures, rotation, side));
            }
        } else {
            quads.addAll(materialQuads(entry.get("whole").getAsString(), textures, rotation, side));
        }
        if (look.showTier && look.kind.tier != StorageTier.WOOD) {
            String tierName = look.kind.tier.getModelPrefix().substring(0, look.kind.tier.getModelPrefix().length() - 1);
            quads.addAll(partQuads(prefix + "tier_" + tierName + flat, rotation, side));
        }
        if (look.packed) {
            quads.addAll(partQuads(prefix + "packed" + flat, rotation, side));
        } else if (look.locked) {
            quads.addAll(partQuads(prefix + "locked" + flat, rotation, side));
        }
        return ImmutableList.copyOf(quads);
    }

    private static List<BakedQuad> partQuads(String model, ModelRotation rotation, @Nullable EnumFacing side) {
        IBakedModel baked = getModel(model).bake(rotation, DefaultVertexFormats.ITEM, BarrelMaterialModel::getSprite);
        return baked.getQuads(null, side, 0);
    }

    private static List<BakedQuad> materialQuads(String model, Map<String, String> textures, ModelRotation rotation, @Nullable EnumFacing side) {
        IBakedModel baked = getModel(model).retexture(ImmutableMap.copyOf(textures)).bake(rotation, DefaultVertexFormats.ITEM, BarrelMaterialModel::getSprite);
        return baked.getQuads(null, side, 0);
    }

    private static TextureAtlasSprite getSprite(ResourceLocation location) {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
    }

    /** The part texture variables: each material's sprite for this side, the wood's textures for the rest. */
    private static Map<String, String> getTextures(Look look, JsonObject entry, @Nullable EnumFacing side) {
        Map<String, String> textures = new HashMap<>();
        EnumFacing face = getMaterialFace(side, look.horizontal, look.vertical);
        BarrelMaterials.uncompact(look.materials).forEach((material, name) -> textures.put(material.getName(), getMaterialSprite(name, face)));
        JsonObject woods = entry.getAsJsonObject("woods");
        JsonObject woodTextures = woods.has(look.wood) ? woods.getAsJsonObject(look.wood) : woods.getAsJsonObject(WoodType.DEFAULT.getName());
        woodTextures.entrySet().forEach(e -> textures.putIfAbsent(e.getKey(), e.getValue().getAsString()));
        return textures;
    }

    /** The face of a material's block that a world side of the barrel shows (the original's BlockSide mapping). */
    private static EnumFacing getMaterialFace(@Nullable EnumFacing side, EnumFacing horizontal, VerticalFacing vertical) {
        if (side == null) {
            return EnumFacing.NORTH;
        }
        for (BlockSide blockSide : BlockSide.values()) {
            if (blockSide.toDirection(horizontal, vertical) == side) {
                return blockSide.toDirection(EnumFacing.NORTH, VerticalFacing.NO);
            }
        }
        return side;
    }

    /** A material block's texture on a face: from its model's quads on that face, else its particle texture. */
    private static String getMaterialSprite(String materialName, EnumFacing face) {
        try {
            return SPRITES.get(materialName + "|" + face, () -> {
                IBlockState state = BarrelMaterials.getState(materialName);
                if (state == null) {
                    return "missingno";
                }
                IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
                List<BakedQuad> culled = model.getQuads(state, face, 0);
                if (!culled.isEmpty()) {
                    return culled.get(0).getSprite().getIconName();
                }
                List<BakedQuad> general = model.getQuads(state, null, 0);
                for (BakedQuad quad : general) {
                    if (quad.getFace() == face) {
                        return quad.getSprite().getIconName();
                    }
                }
                return general.isEmpty() ? model.getParticleTexture().getIconName() : general.get(0).getSprite().getIconName();
            });
        } catch (ExecutionException | RuntimeException e) {
            return "missingno";
        }
    }

    @Nullable
    private static TextureAtlasSprite getMaterialParticle(Map<BarrelMaterial, String> materials) {
        for (BarrelMaterial material : PARTICLE_MATERIALS) {
            if (materials.containsKey(material)) {
                IBlockState state = BarrelMaterials.getState(materials.get(material));
                if (state != null) {
                    return Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state).getParticleTexture();
                }
            }
        }
        return null;
    }

    // ---- The block model -------------------------------------------------------------------

    /** A barrel's blockstate model, taken over while it has materials. */
    private static final class BlockModel implements IBakedModel {
        private final IBakedModel original;
        private final Kind kind;

        BlockModel(IBakedModel original, Kind kind) {
            this.original = original;
            this.kind = kind;
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            Map<BarrelMaterial, String> materials = state instanceof IExtendedBlockState ? ((IExtendedBlockState) state).getValue(BarrelBlock.MATERIALS) : null;
            if (materials == null || materials.isEmpty() || !(state.getBlock() instanceof StorageBlockBase)) {
                return original.getQuads(state, side, rand);
            }
            StorageBlockBase block = (StorageBlockBase) state.getBlock();
            EnumFacing horizontal = block.getHorizontalDirection(state);
            VerticalFacing vertical = block.getVerticalFacing(state);
            int[] rotation = kind.limited ? limitedRotation(horizontal, vertical) : BARREL_ROTATIONS.get(block.getFacing(state));
            TintMode tint = state.getValue(BarrelBlock.TINT);
            Look look = new Look(kind, materials, state.getValue(BarrelBlock.WOOD).getName(), state.getValue(BarrelBlock.FLAT_TOP),
                    !kind.limited && state.getValue(BarrelBlock.OPEN), tint == TintMode.MAIN || tint == TintMode.BOTH,
                    tint == TintMode.ACCENT || tint == TintMode.BOTH, state.getValue(BarrelBlock.SHOW_TIER), state.getValue(BarrelBlock.PACKED),
                    state.getValue(BarrelBlock.LOCKED), horizontal, vertical, rotation);
            return BarrelMaterialModel.getQuads(look, side);
        }

        /** Limited barrels' blockstate rotations (tools/gen_limited_barrel_models.py). */
        private static int[] limitedRotation(EnumFacing horizontal, VerticalFacing vertical) {
            int y = horizontal == EnumFacing.EAST ? 270 : horizontal == EnumFacing.NORTH ? 180 : horizontal == EnumFacing.WEST ? 90 : 0;
            int x = vertical == VerticalFacing.NO ? 270 : vertical == VerticalFacing.DOWN ? 180 : 0;
            return new int[] {x, y};
        }

        @Override
        public boolean isAmbientOcclusion() {
            return original.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return original.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Nonnull
        @Override
        public TextureAtlasSprite getParticleTexture() {
            return original.getParticleTexture();
        }

        @Nonnull
        @Override
        @SuppressWarnings("deprecation")
        public ItemCameraTransforms getItemCameraTransforms() {
            return original.getItemCameraTransforms();
        }

        @Nonnull
        @Override
        public ItemOverrideList getOverrides() {
            return original.getOverrides();
        }
    }

    // ---- The item model --------------------------------------------------------------------

    /** Barrel items with materials: a model built per stack, drawn like the plain barrel item. */
    private static final class ItemModel implements IBakedModel {
        private final IBakedModel reference;
        private final Cache<String, IBakedModel> perStack = CacheBuilder.newBuilder().maximumSize(256).expireAfterAccess(5, TimeUnit.MINUTES).build();
        private final ItemOverrideList overrides = new ItemOverrideList(Collections.emptyList()) {
            @Nonnull
            @Override
            public IBakedModel handleItemState(@Nonnull IBakedModel model, @Nonnull ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                Kind kind = Kind.of(Block.getBlockFromItem(stack.getItem()));
                Map<BarrelMaterial, String> materials = BarrelMaterials.get(stack);
                if (kind == null || materials.isEmpty()) {
                    return reference;
                }
                try {
                    return perStack.get(stack.getItem().getRegistryName() + "" + stack.getTagCompound(), () -> new StackModel(reference, stackLook(kind, stack, materials)));
                } catch (ExecutionException e) {
                    return reference;
                }
            }
        };

        ItemModel(IBakedModel reference) {
            this.reference = reference;
        }

        private static Look stackLook(Kind kind, ItemStack stack, Map<BarrelMaterial, String> materials) {
            WoodType wood = StorageItemData.getWoodType(stack);
            NBTFlags flags = new NBTFlags(stack);
            return new Look(kind, materials, (wood != null ? wood : WoodType.DEFAULT).getName(), StorageItemData.isFlatTop(stack), false,
                    StorageItemData.getMainColor(stack) != StorageItemData.NO_COLOR, StorageItemData.getAccentColor(stack) != StorageItemData.NO_COLOR,
                    flags.showTier, flags.packed, false, EnumFacing.NORTH, VerticalFacing.UP, new int[] {0, 0});
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return reference.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return reference.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return reference.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Nonnull
        @Override
        public TextureAtlasSprite getParticleTexture() {
            return reference.getParticleTexture();
        }

        @Nonnull
        @Override
        @SuppressWarnings("deprecation")
        public ItemCameraTransforms getItemCameraTransforms() {
            return reference.getItemCameraTransforms();
        }

        @Nonnull
        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }
    }

    /** What the tier and tape flags of a stack say (tier shown unless hidden by the tool, packed if taped). */
    private static final class NBTFlags {
        final boolean showTier;
        final boolean packed;

        NBTFlags(ItemStack stack) {
            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            packed = tag != null && tag.getBoolean(StorageItemData.PACKED_TAG);
            showTier = tag == null || !tag.hasKey("showTier") || tag.getBoolean("showTier");
        }
    }

    /** One stack's model: every side's quads, with the plain barrel item's transforms. */
    private static final class StackModel implements IBakedModel {
        private final IBakedModel reference;
        private final Look look;

        StackModel(IBakedModel reference, Look look) {
            this.reference = reference;
            this.look = look;
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return BarrelMaterialModel.getQuads(look, side);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Nonnull
        @Override
        public TextureAtlasSprite getParticleTexture() {
            TextureAtlasSprite particle = getMaterialParticle(look.materials);
            return particle != null ? particle : reference.getParticleTexture();
        }

        @Nonnull
        @Override
        @SuppressWarnings("deprecation")
        public ItemCameraTransforms getItemCameraTransforms() {
            return reference.getItemCameraTransforms();
        }

        @Nonnull
        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }

        @Nonnull
        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(@Nonnull ItemCameraTransforms.TransformType type) {
            return Pair.of(this, reference.handlePerspective(type).getRight());
        }
    }
}
