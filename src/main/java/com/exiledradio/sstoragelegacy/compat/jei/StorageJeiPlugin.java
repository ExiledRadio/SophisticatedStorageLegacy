/*
 * Modified from Sophisticated Storage (c) P3pp3rF1y, GPLv3. Ported to 1.12.2 by ExiledRadio.
 */
package com.exiledradio.sstoragelegacy.compat.jei;

import com.exiledradio.sstoragelegacy.block.ShulkerBoxBlock;
import com.exiledradio.sstoragelegacy.block.StorageBlockBase;
import com.exiledradio.sstoragelegacy.block.WoodType;
import com.exiledradio.sstoragelegacy.crafting.StorageTierUpgradeRecipe;
import com.exiledradio.sstoragelegacy.init.ModContent;
import com.exiledradio.sstoragelegacy.item.BarrelItem;
import com.exiledradio.sstoragelegacy.item.StorageBlockItem;
import com.exiledradio.sstoragelegacy.item.StorageItemData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreIngredient;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * As in the original's StoragePlugin: each tier, wood, colour pair and flat top is its own JEI entry,
 * and the dynamic dye and flat-top recipes get display-only examples in the crafting category.
 */
@JEIPlugin
public class StorageJeiPlugin implements IModPlugin {
    // By dye metadata; 1.12's names differ ("silver"), so they can't be built from the enum.
    private static final String[] DYE_ORE_NAMES = {
            "dyeWhite", "dyeOrange", "dyeMagenta", "dyeLightBlue", "dyeYellow", "dyeLime", "dyePink", "dyeGray",
            "dyeLightGray", "dyeCyan", "dyePurple", "dyeBlue", "dyeBrown", "dyeGreen", "dyeRed", "dyeBlack"
    };

    @Override
    public void registerItemSubtypes(@Nonnull ISubtypeRegistry subtypeRegistry) {
        // Contents and upgrades of a picked-up storage don't make a different entry.
        ISubtypeRegistry.ISubtypeInterpreter barrelInterpreter = stack -> {
            StringJoiner result = new StringJoiner(",");
            WoodType woodType = StorageItemData.getWoodType(stack);
            if (woodType != null) {
                result.add("woodName:" + woodType.getName());
            }
            int mainColor = StorageItemData.getMainColor(stack);
            if (mainColor != StorageItemData.NO_COLOR) {
                result.add("mainColor:" + mainColor);
            }
            int accentColor = StorageItemData.getAccentColor(stack);
            if (accentColor != StorageItemData.NO_COLOR) {
                result.add("accentColor:" + accentColor);
            }
            result.add("flatTop:" + StorageItemData.isFlatTop(stack));
            return "{" + result + "}";
        };
        for (StorageBlockBase storage : ModContent.getAllStorages()) {
            subtypeRegistry.registerSubtypeInterpreter(Item.getItemFromBlock(storage), barrelInterpreter);
        }
    }

    @Override
    public void register(@Nonnull IModRegistry registry) {
        List<IRecipe> recipes = new ArrayList<>();
        // Dyeing works on every tier; the original shows the examples for each.
        for (StorageBlockBase storage : ModContent.getAllStorages()) {
            addDyeRecipes(recipes, Item.getItemFromBlock(storage), !(storage instanceof ShulkerBoxBlock));
        }
        addFlatTopRecipes(recipes, Item.getItemFromBlock(ModContent.BARREL));
        for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (recipe instanceof StorageTierUpgradeRecipe) {
                addTierUpgradeExamples(recipes, (StorageTierUpgradeRecipe) recipe);
            }
        }
        addControllerExample(recipes);
        registry.addRecipes(recipes, VanillaRecipeCategoryUid.CRAFTING);
        // The crafting upgrade crafts like a table (the original's catalyst).
        Item craftingUpgrade = Item.getByNameOrId(com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy.MODID + ":crafting_upgrade");
        if (craftingUpgrade != null) {
            registry.addRecipeCatalyst(new ItemStack(craftingUpgrade), VanillaRecipeCategoryUid.CRAFTING);
        }
    }

    /** The controller recipe with every listed variant of its storages (the original's ControllerRecipesMaker). */
    private static void addControllerExample(List<IRecipe> recipes) {
        IRecipe controller = ForgeRegistries.RECIPES.getValue(new net.minecraft.util.ResourceLocation(com.exiledradio.sstoragelegacy.SophisticatedStorageLegacy.MODID, "controller"));
        if (!(controller instanceof net.minecraftforge.common.crafting.IShapedRecipe)) {
            return;
        }
        NonNullList<Ingredient> expanded = NonNullList.create();
        for (Ingredient ingredient : controller.getIngredients()) {
            NonNullList<ItemStack> variants = NonNullList.create();
            for (ItemStack matching : ingredient.getMatchingStacks()) {
                if (matching.getItem() instanceof StorageBlockItem) {
                    matching.getItem().getSubItems(CreativeTabs.SEARCH, variants);
                }
            }
            expanded.add(variants.isEmpty() ? ingredient : Ingredient.fromStacks(variants.toArray(new ItemStack[0])));
        }
        net.minecraftforge.common.crafting.IShapedRecipe shaped = (net.minecraftforge.common.crafting.IShapedRecipe) controller;
        recipes.add(new ShapedRecipes("", shaped.getRecipeWidth(), shaped.getRecipeHeight(), expanded, controller.getRecipeOutput()));
    }

    /** The storage (any wood) over one dye for each colour, and yellow-storage-lime for two colours. */
    private static void addDyeRecipes(List<IRecipe> recipes, Item barrel, boolean wooden) {
        List<ItemStack> woodBarrels = new ArrayList<>();
        if (wooden) {
            for (WoodType woodType : WoodType.values()) {
                if (woodType.isAvailable()) {
                    woodBarrels.add(StorageItemData.setWoodType(new ItemStack(barrel), woodType));
                }
            }
        } else {
            woodBarrels.add(new ItemStack(barrel));
        }
        Ingredient anyWood = Ingredient.fromStacks(woodBarrels.toArray(new ItemStack[0]));
        for (EnumDyeColor color : EnumDyeColor.values()) {
            int rgb = BarrelItem.colorOf(color);
            ItemStack result = StorageItemData.setAccentColor(StorageItemData.setMainColor(new ItemStack(barrel), rgb), rgb);
            recipes.add(new ShapedRecipes("", 1, 2, NonNullList.from(Ingredient.EMPTY, anyWood, dye(color)), result));
        }
        ItemStack twoColors = StorageItemData.setAccentColor(StorageItemData.setMainColor(new ItemStack(barrel),
                BarrelItem.colorOf(EnumDyeColor.YELLOW)), BarrelItem.colorOf(EnumDyeColor.LIME));
        recipes.add(new ShapedRecipes("", 3, 1, NonNullList.from(Ingredient.EMPTY,
                dye(EnumDyeColor.YELLOW), anyWood, dye(EnumDyeColor.LIME)), twoColors));
    }

    /** A barrel on its own flattens its top, and a flat one goes back. */
    private static void addFlatTopRecipes(List<IRecipe> recipes, Item barrel) {
        ItemStack normal = StorageItemData.setWoodType(new ItemStack(barrel), WoodType.ACACIA);
        ItemStack flat = StorageItemData.setFlatTop(normal.copy(), true);
        recipes.add(new ShapelessRecipes("", flat, NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(normal))));
        recipes.add(new ShapelessRecipes("", normal, NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(flat))));
    }

    /**
     * The tier recipes keep the barrel's NBT, so their JSON output (no wood, no colour) matches no
     * listed barrel. Like the original's TierUpgradeRecipesMaker: one copy per listed variant of
     * the input barrel, with that variant in the middle and the barrel it really becomes as output.
     */
    private static void addTierUpgradeExamples(List<IRecipe> recipes, StorageTierUpgradeRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int index = 0; index < ingredients.size(); index++) {
            ItemStack[] matching = ingredients.get(index).getMatchingStacks();
            if (matching.length != 1 || !(matching[0].getItem() instanceof StorageBlockItem)) {
                continue;
            }
            NonNullList<ItemStack> variants = NonNullList.create();
            matching[0].getItem().getSubItems(CreativeTabs.SEARCH, variants);
            for (ItemStack variant : variants) {
                NonNullList<Ingredient> copy = NonNullList.create();
                copy.addAll(ingredients);
                copy.set(index, Ingredient.fromStacks(variant));
                ItemStack result = recipe.getRecipeOutput().copy();
                NBTTagCompound tag = variant.getTagCompound();
                if (tag != null) {
                    result.setTagCompound(tag.copy());
                }
                recipes.add(new ShapedRecipes("", recipe.getRecipeWidth(), recipe.getRecipeHeight(), copy, result));
            }
        }
    }

    private static Ingredient dye(EnumDyeColor color) {
        return new OreIngredient(DYE_ORE_NAMES[color.getMetadata()]);
    }
}
