package com.exiledradio.sstoragelegacy.common;

import com.exiledradio.sstoragelegacy.block.StorageTileEntity;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/**
 * Dev runs only (registered in a deobfuscated environment): server-side actions that otherwise
 * need a click in a GUI or a ticking hopper, so tests can run from the server console:
 * `sstoragedev sort x y z`, `sstoragedev insert x y z item count [meta]` (through the item
 * capability, as a hopper or pipe would).
 */
public class DevCommand extends CommandBase {
    @Override
    public String getName() {
        return "sstoragedev";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/sstoragedev sort <x> <y> <z> | insert <x> <y> <z> <item> <count> [meta] | tier <x> <y> <z> <tier upgrade item> | canadd <x> <y> <z> <upgrade> | break <x> <y> <z> [sneak] | shiftclick <x> <y> <z> <slot>"
                + " | place <x> <y> <z> <clicked face> <item> <yaw> <sneak|stand> [nbt] | use <x> <y> <z> <item[*n]> [face] | craft <9 items or -> | info <x> <y> <z> | drops <x> <y> <z>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 1 && "stashtest".equals(args[0])) {
            stashTest(sender);
            return;
        }
        if (args.length >= 2 && "craft".equals(args[0])) {
            craft(sender, args);
            return;
        }
        if (args.length < 4) {
            throw new WrongUsageException(getUsage(sender));
        }
        BlockPos pos = parseBlockPos(sender, args, 1, false);
        if ("place".equals(args[0]) && args.length >= 8) {
            place(sender, pos, args);
            return;
        }
        TileEntity te = sender.getEntityWorld().getTileEntity(pos);
        if ("extract".equals(args[0]) && args.length >= 5 && te != null) {
            // Through the item capability, as a pipe would: from the first slots that have anything.
            net.minecraftforge.items.IItemHandler handler = te.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null) {
                throw new CommandException("No item handler at " + pos);
            }
            int wanted = parseInt(args[4], 1);
            StringBuilder got = new StringBuilder("Extracted:");
            for (int slot = 0; slot < handler.getSlots() && wanted > 0; slot++) {
                ItemStack taken = handler.extractItem(slot, wanted, false);
                if (!taken.isEmpty()) {
                    wanted -= taken.getCount();
                    got.append(' ').append(taken.getCount()).append('x').append(taken.getItem().getRegistryName());
                }
            }
            sender.sendMessage(new TextComponentString(got.toString()));
            return;
        }
        if ("insert".equals(args[0]) && args.length >= 6 && te != null && !(te instanceof StorageTileEntity)) {
            net.minecraftforge.items.IItemHandler handler = te.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null) {
                throw new CommandException("No item handler at " + pos);
            }
            ItemStack stack = new ItemStack(getItemByText(sender, args[4]), parseInt(args[5], 1), args.length > 6 ? parseInt(args[6]) : 0);
            ItemStack remaining = net.minecraftforge.items.ItemHandlerHelper.insertItem(handler, stack, false);
            sender.sendMessage(new TextComponentString("Inserted " + (stack.getCount() - remaining.getCount()) + " of " + stack.getCount() + " (slots " + handler.getSlots() + ")"));
            return;
        }
        if (!(te instanceof StorageTileEntity)) {
            throw new CommandException("No storage at " + pos);
        }
        if ("sort".equals(args[0])) {
            ((StorageTileEntity) te).getStorageWrapper().sort();
            sender.sendMessage(new TextComponentString("Sorted " + pos));
        } else if ("insert".equals(args[0]) && args.length >= 6) {
            net.minecraft.item.Item item = getItemByText(sender, args[4]);
            ItemStack stack = new ItemStack(item, parseInt(args[5], 1), args.length > 6 ? parseInt(args[6]) : 0);
            net.minecraftforge.items.IItemHandler handler = te.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler == null) {
                throw new CommandException("No item handler at " + pos);
            }
            ItemStack remaining = net.minecraftforge.items.ItemHandlerHelper.insertItem(handler, stack, false);
            sender.sendMessage(new TextComponentString("Inserted " + (stack.getCount() - remaining.getCount()) + " of " + stack.getCount()));
        } else if ("tier".equals(args[0]) && args.length >= 5) {
            net.minecraft.item.Item item = getItemByText(sender, args[4]);
            if (!(item instanceof com.exiledradio.sstoragelegacy.item.StorageTierUpgradeItem)) {
                throw new CommandException(args[4] + " is not a tier upgrade");
            }
            com.exiledradio.sstoragelegacy.item.StorageTierUpgradeItem upgrade = (com.exiledradio.sstoragelegacy.item.StorageTierUpgradeItem) item;
            net.minecraft.world.World world = sender.getEntityWorld();
            if (!upgrade.canUpgrade(world, pos, world.getBlockState(pos))) {
                throw new CommandException("Can't upgrade " + pos);
            }
            upgrade.upgradeStorage(net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) world), world, pos,
                    world.getBlockState(pos), new ItemStack(item, 64));
            sender.sendMessage(new TextComponentString("Upgraded " + pos));
        } else if ("break".equals(args[0])) {
            net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) sender.getEntityWorld());
            fake.setSneaking(args.length > 4 && "sneak".equals(args[4]));
            boolean broken;
            try {
                broken = fake.interactionManager.tryHarvestBlock(pos);
            } catch (NullPointerException e) {
                // A cancelled break resends the block to the player, and a fake player has no connection.
                broken = false;
            }
            fake.setSneaking(false);
            sender.sendMessage(new TextComponentString("Break " + pos + ": " + (broken ? "broken" : "refused")));
        } else if ("shiftclick".equals(args[0]) && args.length >= 5) {
            // A fake player shift-clicks a storage slot; reports the stacks that land in its inventory.
            net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) sender.getEntityWorld());
            fake.inventory.clear();
            com.exiledradio.sstoragelegacy.common.gui.StorageBlockContainer container = new com.exiledradio.sstoragelegacy.common.gui.StorageBlockContainer(fake, (StorageTileEntity) te);
            container.slotClick(parseInt(args[4]), 0, net.minecraft.inventory.ClickType.QUICK_MOVE, fake);
            StringBuilder result = new StringBuilder("Player got:");
            for (ItemStack stack : fake.inventory.mainInventory) {
                if (!stack.isEmpty()) {
                    result.append(' ').append(stack.getCount()).append('x').append(stack.getItem().getRegistryName());
                }
            }
            fake.inventory.clear();
            sender.sendMessage(new TextComponentString(result.toString()));
        } else if ("use".equals(args[0]) && args.length >= 5) {
            // A fake player right-clicks the storage holding an item (packing tape).
            net.minecraft.world.World world = sender.getEntityWorld();
            net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) world);
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            net.minecraft.util.EnumFacing face = args.length > 5 ? net.minecraft.util.EnumFacing.byName(args[5]) : net.minecraft.util.EnumFacing.UP;
            String[] itemAndCount = args[4].split("[*]");
            fake.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(getItemByText(sender, itemAndCount[0]), itemAndCount.length > 1 ? parseInt(itemAndCount[1], 1) : 1));
            boolean used = state.getBlock().onBlockActivated(world, pos, state, fake, net.minecraft.util.EnumHand.MAIN_HAND, face, 0.5F, 0.5F, 0.5F);
            int left = fake.getHeldItemMainhand().getCount();
            fake.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, ItemStack.EMPTY);
            sender.sendMessage(new TextComponentString("Use " + pos + " " + face + ": " + used + ", held after " + left));
        } else if ("info".equals(args[0])) {
            sender.sendMessage(new TextComponentString(describe((StorageTileEntity) te)));
        } else if ("drops".equals(args[0])) {
            net.minecraft.world.World world = sender.getEntityWorld();
            net.minecraft.util.NonNullList<ItemStack> drops = net.minecraft.util.NonNullList.create();
            world.getBlockState(pos).getBlock().getDrops(drops, world, pos, world.getBlockState(pos), 0);
            for (ItemStack drop : drops) {
                sender.sendMessage(new TextComponentString("Drop " + drop.getItem().getRegistryName() + " " + drop.getTagCompound()));
            }
        } else if ("canadd".equals(args[0]) && args.length >= 5) {
            net.minecraft.item.Item item = getItemByText(sender, args[4]);
            if (!(item instanceof com.exiledradio.scorelegacy.api.IUpgradeItem)) {
                throw new CommandException(args[4] + " is not an upgrade");
            }
            com.exiledradio.scorelegacy.api.UpgradeSlotChangeResult result = ((com.exiledradio.scorelegacy.api.IUpgradeItem<?>) item)
                    .canAddUpgradeTo(((StorageTileEntity) te).getStorageWrapper(), new ItemStack(item), true);
            sender.sendMessage(new TextComponentString("canAdd " + args[4] + ": " + (result.isSuccessful() ? "yes"
                    : "no, " + result.getErrorKey() + " " + java.util.Arrays.toString(result.getErrorParams()))));
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    /** A fake player facing yaw uses an item on a face of the clicked block, sneaking or not; nbt is the stack's tag. */
    private void place(ICommandSender sender, BlockPos clicked, String[] args) throws CommandException {
        net.minecraft.world.World world = sender.getEntityWorld();
        net.minecraft.util.EnumFacing face = net.minecraft.util.EnumFacing.byName(args[4]);
        if (face == null) {
            throw new CommandException("Unknown face " + args[4]);
        }
        // item or item*count
        String[] itemAndCount = args[5].split("[*]");
        ItemStack stack = new ItemStack(getItemByText(sender, itemAndCount[0]), itemAndCount.length > 1 ? parseInt(itemAndCount[1], 1) : 1);
        if (args.length > 8) {
            try {
                stack.setTagCompound(net.minecraft.nbt.JsonToNBT.getTagFromJson(buildString(args, 8)));
            } catch (net.minecraft.nbt.NBTException e) {
                throw new CommandException("Bad nbt: " + e.getMessage());
            }
        }
        net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) world);
        fake.rotationYaw = (float) parseDouble(args[6]);
        fake.setSneaking("sneak".equals(args[7]));
        fake.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, stack);
        // As the game does: onItemUseFirst (tier upgrades), then the block, then onItemUse.
        net.minecraft.util.EnumActionResult result = stack.getItem().onItemUseFirst(fake, world, clicked, face, 0.5F, 0.5F, 0.5F, net.minecraft.util.EnumHand.MAIN_HAND);
        if (result == net.minecraft.util.EnumActionResult.PASS) {
            result = stack.onItemUse(fake, world, clicked, net.minecraft.util.EnumHand.MAIN_HAND, face, 0.5F, 0.5F, 0.5F);
        }
        sender.sendMessage(new TextComponentString("Held after: " + fake.getHeldItemMainhand().getCount()));
        fake.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, ItemStack.EMPTY);
        fake.setSneaking(false);
        sender.sendMessage(new TextComponentString("Place on " + clicked + " " + face + ": " + result));
    }

    /**
     * Fills a 3x3 crafting grid row by row (each argument "modid:item[@meta]" or "-" for empty;
     * fewer than nine leaves the rest empty) and reports the matching recipe and its result.
     */
    private void craft(ICommandSender sender, String[] args) throws CommandException {
        net.minecraft.inventory.Container dummy = new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return false;
            }
        };
        net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(dummy, 3, 3);
        for (int i = 1; i < args.length && i <= 9; i++) {
            if (!"-".equals(args[i])) {
                String[] itemAndMeta = args[i].split("@");
                grid.setInventorySlotContents(i - 1, new ItemStack(getItemByText(sender, itemAndMeta[0]), 1, itemAndMeta.length > 1 ? parseInt(itemAndMeta[1]) : 0));
            }
        }
        net.minecraft.item.crafting.IRecipe recipe = net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, sender.getEntityWorld());
        if (recipe == null) {
            sender.sendMessage(new TextComponentString("Craft: no recipe"));
            return;
        }
        ItemStack result = recipe.getCraftingResult(grid);
        sender.sendMessage(new TextComponentString("Craft: " + recipe.getRegistryName() + " -> " + result.getCount() + "x" + result.getItem().getRegistryName() + " " + result.getTagCompound()));
    }

    /** Both stash directions with a fake player's inventory: carried diamonds onto a box, then a box onto a slot of dirt and of a shulker box. */
    private void stashTest(ICommandSender sender) {
        net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer) sender.getEntityWorld());
        fake.inventory.clear();
        net.minecraft.inventory.Container container = fake.inventoryContainer;
        net.minecraft.item.Item boxItem = net.minecraft.item.Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("sstoragelegacy:shulker_box"));
        container.getSlot(9).putStack(new ItemStack(boxItem));
        fake.inventory.setItemStack(new ItemStack(net.minecraft.init.Items.DIAMOND, 20));
        boolean first = com.exiledradio.sstoragelegacy.item.StorageItemContents.tryStash(container.getSlot(9), fake, false);
        sender.sendMessage(new TextComponentString("Carried diamonds onto box: " + first + ", carried after " + fake.inventory.getItemStack().getCount()
                + ", box " + container.getSlot(9).getStack().getTagCompound()));
        fake.inventory.setItemStack(container.getSlot(9).getStack());
        container.getSlot(9).putStack(ItemStack.EMPTY);
        container.getSlot(10).putStack(new ItemStack(net.minecraft.init.Blocks.DIRT, 30));
        boolean second = com.exiledradio.sstoragelegacy.item.StorageItemContents.tryStash(container.getSlot(10), fake, false);
        container.getSlot(11).putStack(new ItemStack(boxItem));
        boolean third = com.exiledradio.sstoragelegacy.item.StorageItemContents.tryStash(container.getSlot(11), fake, true);
        sender.sendMessage(new TextComponentString("Box onto dirt: " + second + ", slot after " + container.getSlot(10).getStack()
                + "; box onto a box: " + third + "; box " + fake.inventory.getItemStack().getTagCompound()));
        fake.inventory.setItemStack(ItemStack.EMPTY);
        fake.inventory.clear();
    }

    /** Type, halves, slot counts and where the items are. */
    private static String describe(StorageTileEntity te) {
        net.minecraft.block.state.IBlockState state = te.getWorld().getBlockState(te.getPos());
        StringBuilder out = new StringBuilder("Info " + te.getPos() + " " + state.getBlock().getRegistryName() + " " + state.getProperties().values());
        if (te instanceof com.exiledradio.sstoragelegacy.block.ChestTileEntity) {
            out.append(" mainPos=").append(((com.exiledradio.sstoragelegacy.block.ChestTileEntity) te).getDoubleMainPos());
        }
        out.append(" packed=").append(te.isPacked()).append(" locked=").append(te.isLocked()).append(" controller=").append(te.getControllerPos());
        com.exiledradio.sstoragelegacy.block.StorageBlockWrapper wrapper = te.getStorageWrapper();
        net.minecraftforge.items.IItemHandler inventory = wrapper.getInventoryHandler();
        out.append(" slots=").append(inventory.getSlots()).append(" upgrades=").append(!wrapper.hasNoUpgrades()).append(" items:");
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                out.append(' ').append(slot).append('=').append(stack.getCount()).append(stack.getItem().getRegistryName().getPath());
            }
        }
        out.append(" display=").append(te.getDisplayItems().size());
        return out.toString();
    }
}
