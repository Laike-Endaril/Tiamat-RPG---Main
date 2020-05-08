package com.fantasticsource.tiamatrpg.inventory;

import com.fantasticsource.tiamatitems.nbt.MiscTags;
import com.fantasticsource.tools.Tools;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public class TiamatInventoryContainer extends Container
{
    private final EntityPlayer player;

    public TiamatInventoryContainer(EntityPlayer player)
    {
        this.player = player;
        InventoryPlayer playerInventory = player.inventory;
        TiamatPlayerInventory tiamatPlayerInventory = player.world.isRemote ? new TiamatPlayerInventory(player) : TiamatPlayerInventory.tiamatServerInventories.get(player.getPersistentID());
        if (tiamatPlayerInventory == null)
        {
            tiamatPlayerInventory = new TiamatPlayerInventory(player);
        }

        //Active mainhand and offhand
        //Index 0 - 1
        //Internal index 0 and 40 (vanilla)
        addSlotToContainer(new FilteredSlot(playerInventory, 0, 24, 114, 608, 0, true, 1, stack -> getSlot(1).getStack().isEmpty() || (!MiscTags.isTwoHanded(stack) && !MiscTags.isTwoHanded(getSlot(1).getStack()))));
        addSlotToContainer(new FilteredSlot(playerInventory, 40, 42, 114, 624, 0, true, 1, stack -> getSlot(0).getStack().isEmpty() || (!MiscTags.isTwoHanded(stack) && !MiscTags.isTwoHanded(getSlot(0).getStack()))));

        //Inactive mainhand and offhand
        //Index 2 - 3
        //Internal index 0 and 1 (tiamat)
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 0, 78, 114, 608, 0, false, 1, stack -> getSlot(3).getStack().isEmpty() || (!MiscTags.isTwoHanded(stack) && !MiscTags.isTwoHanded(getSlot(3).getStack()))));
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 1, 96, 114, 624, 0, false, 1, stack -> getSlot(2).getStack().isEmpty() || (!MiscTags.isTwoHanded(stack) && !MiscTags.isTwoHanded(getSlot(2).getStack()))));

        //"Cargo" inventory
        //Index 4 - 30
        //Internal index 9 - 35 (vanilla)
        for (int yy = 0; yy < 3; ++yy)
        {
            for (int xx = 0; xx < 9; ++xx)
            {
                addSlotToContainer(new BetterSlot(playerInventory, xx + (yy + 1) * 9, 114 + xx * 18, 42 + yy * 18, -1, -1));
            }
        }

        //Armor slots
        //Index 31 - 36
        //Internal indices...
        //...39 (vanilla head)
        //...2 (tiamat shoulders)
        //...3 (tiamat cape)
        //...38 (vanilla chest)
        //...37 (vanilla legs)
        //...36 (vanilla feet)
        addVanillaEquipmentSlot(playerInventory, EntityEquipmentSlot.HEAD, 39, 6, 6, 512, 0);
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 2, 6, 24, 528, 0, true, 1, stack -> stack.hasTagCompound() && MiscTags.stackFitsSlot(stack, "Tiamat Shoulders")));
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 3, 6, 42, 544, 0, true, 1, stack -> stack.hasTagCompound() && MiscTags.stackFitsSlot(stack, "Tiamat Cape")));
        addVanillaEquipmentSlot(playerInventory, EntityEquipmentSlot.CHEST, 38, 6, 60, 560, 0);
        addVanillaEquipmentSlot(playerInventory, EntityEquipmentSlot.LEGS, 37, 6, 78, 576, 0);
        addVanillaEquipmentSlot(playerInventory, EntityEquipmentSlot.FEET, 36, 6, 96, 592, 0);

        //Quick slots
        //Index 37 - 39
        //Internal index 4 - 6 (tiamat)
        for (int xx = 0; xx < 3; xx++)
        {
            addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 4 + xx, 222 + xx * 18, 96, 784, 0, true, 1, stack -> stack.hasTagCompound() && MiscTags.stackFitsSlot(stack, "Tiamat Quick Item")));
        }

        //Backpack slot
        //Index 40
        //Internal index 7 (tiamat)
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 7, 114, 24, 768, 0, true, 1, stack -> stack.hasTagCompound() && MiscTags.stackFitsSlot(stack, "Tiamat Backpack")));

        //Pet slot
        //Index 41
        //Internal index 8 (tiamat)
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 8, 132, 96, 640, 0, true, 1, stack -> stack.hasTagCompound() && MiscTags.stackFitsSlot(stack, "Tiamat Pet")));

        //Deck slot
        //Index 42
        //Internal index 9 (tiamat)
        addSlotToContainer(new FilteredSlot(tiamatPlayerInventory, 9, 114, 96, 752, 0, true, 1, stack -> stack.hasTagCompound() && MiscTags.stackFitsSlot(stack, "Tiamat Deck")));
    }

    public static boolean canCombine(ItemStack from, ItemStack to)
    {
        if (from.isEmpty()) return false;
        if (to.isEmpty()) return true;
        return from.getItem() == to.getItem() && (!to.getHasSubtypes() || to.getMetadata() == from.getMetadata()) && ItemStack.areItemStackTagsEqual(to, from);
    }

    private void addVanillaEquipmentSlot(IInventory inventory, EntityEquipmentSlot slotEnum, int index, int x, int y, int u, int v)
    {
        addSlotToContainer(new BetterSlot(inventory, index, x, y, u, v)
        {
            public int getSlotStackLimit()
            {
                return 1;
            }

            public boolean isItemValid(ItemStack stack)
            {
                return stack.getItem().isValidArmor(stack, slotEnum, player);
            }

            public boolean canTakeStack(EntityPlayer playerIn)
            {
                ItemStack itemstack = getStack();
                return (itemstack.isEmpty() || playerIn.isCreative() || !EnchantmentHelper.hasBindingCurse(itemstack));
            }

            @Nullable
            @SideOnly(Side.CLIENT)
            public String getSlotTexture()
            {
                return ItemArmor.EMPTY_SLOT_NAMES[slotEnum.getIndex()];
            }
        });
    }

    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return true;
    }

    //Returning ItemStack.EMPTY from this method indicates that we are done with the transfer.  Mine always finishes in one go, so it always returns ItemStack.EMPTY
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index)
    {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;


        ItemStack stack = slot.getStack();

        if (index <= 3)
        {
            //From any weaponset slot
            //To main inventory or hotbar, in that order
            tryMergeItemStackRanges(stack, 12, 38);
            tryMergeItemStackRanges(stack, 4, 11);
        }
        else if (index <= 11)
        {
            //From hotbar
            //To any armor slot, if applicable, or to a weaponset slot or main inventory otherwise

            tryMergeItemStackRanges(stack, 39, 44);
            tryMergeItemStackRanges(stack, 0, 3);
            tryMergeItemStackRanges(stack, 12, 38);
        }
        else if (index <= 38)
        {
            //From main inventory
            //To any equipment slot, if applicable, or to a weaponset slot or hotbar otherwise
            tryMergeItemStackRanges(stack, 39, 44);
            tryMergeItemStackRanges(stack, 0, 3);
            tryMergeItemStackRanges(stack, 4, 11);
        }
        else if (index <= 44)
        {
            //From armor slots
            //To main inventory, hotbar, or weaponset slot, in that order
            tryMergeItemStackRanges(stack, 12, 38);
            tryMergeItemStackRanges(stack, 4, 11);
            tryMergeItemStackRanges(stack, 0, 3);
        }


        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.putStack(stack);

        return ItemStack.EMPTY;
    }

    public void tryMergeItemStackRanges(ItemStack stackFrom, int... ranges)
    {
        if (stackFrom.isEmpty()) return;


        for (int ii = 0; ii < ranges.length; ii += 2)
        {
            int startIndex = ranges[ii];
            int endIndex = ranges[ii + 1];

            if (startIndex <= endIndex)
            {
                for (int i = startIndex; i <= endIndex; i++)
                {
                    tryMergeItemStack(stackFrom, i);
                }
            }
            else
            {
                for (int i = startIndex; i >= endIndex; i--)
                {
                    tryMergeItemStack(stackFrom, i);
                }
            }
        }
    }

    protected void tryMergeItemStack(ItemStack stackFrom, int slotIDTo)
    {
        Slot slotTo = this.inventorySlots.get(slotIDTo);
        if (!slotTo.isItemValid(stackFrom)) return;

        ItemStack stackTo = slotTo.getStack();
        if (!canCombine(stackFrom, stackTo)) return;

        int limit = Tools.min(slotTo.getSlotStackLimit(), !stackTo.isEmpty() ? stackTo.getMaxStackSize() : stackFrom.getMaxStackSize());

        int moveAmount = Tools.min(stackFrom.getCount(), limit - stackTo.getCount());

        if (stackTo.isEmpty())
        {
            stackTo = stackFrom.copy();
            stackTo.setCount(moveAmount);
            slotTo.putStack(stackTo);
        }
        else
        {
            stackTo.grow(moveAmount);
        }

        stackFrom.shrink(moveAmount);

        slotTo.onSlotChanged();
    }
}
