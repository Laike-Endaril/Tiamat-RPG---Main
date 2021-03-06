package com.fantasticsource.tiamatinventory.inventory.inventoryhacks;

import com.fantasticsource.mctools.GlobalInventory;
import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.mctools.Slottings;
import com.fantasticsource.mctools.event.InventoryChangedEvent;
import com.fantasticsource.mctools.inventory.slot.FilteredSlot;
import com.fantasticsource.mctools.items.ItemMatcher;
import com.fantasticsource.tiamatinventory.Network;
import com.fantasticsource.tiamatinventory.TiamatInventory;
import com.fantasticsource.tiamatinventory.config.TiamatConfig;
import com.fantasticsource.tiamatinventory.inventory.TiamatInventoryContainer;
import com.fantasticsource.tiamatinventory.inventory.TiamatPlayerInventory;
import com.fantasticsource.tiamatinventory.nbt.SlotDataTags;
import com.fantasticsource.tools.ReflectionTool;
import com.fantasticsource.tools.Tools;
import com.fantasticsource.tools.datastructures.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.fantasticsource.tiamatinventory.inventory.TiamatInventoryContainer.*;

public class InventoryHacks
{
    protected static final Field
            CONTAINER_LISTENERS_FIELD = ReflectionTool.getField(Container.class, "field_75149_d", "listeners"),
            ITEMSTACK_AGE_FIELD = ReflectionTool.getField(EntityItem.class, "field_70292_b", "age");


    public static int getCurrentInventorySize(EntityPlayerMP player)
    {
        int slotCount = TiamatConfig.serverSettings.defaultInventorySize;
        for (ItemStack stack : GlobalInventory.getValidEquippedItems(player))
        {
            slotCount += SlotDataTags.getInvSlotCount(stack);
        }
        return slotCount;
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event)
    {
        EntityPlayer player = event.player;

        GameType gameType = MCTools.getGameType(player);
        if (gameType != GameType.CREATIVE && gameType != GameType.SPECTATOR)
        {
            if (!TiamatInventory.playerHasHotbar(player)) player.inventory.currentItem = 0;

            if (player instanceof EntityPlayerMP) dropItemsInBlockedSlots((EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public static void playerContainer(PlayerContainerEvent.Open event)
    {
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        GameType gameType = MCTools.getGameType(player);
        if (gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR) return;


        Container container = event.getContainer();
        TiamatPlayerInventory tiamatInventory = TiamatPlayerInventory.tiamatServerInventories.get(player.getPersistentID());
        if (tiamatInventory != null && !tiamatInventory.forceEmptyHands())
        {
            player.closeScreen();
            return;
        }


        HashSet<Integer> allowedCraftingSlots = new HashSet<>();
        for (int x = 0; x < TiamatConfig.serverSettings.craftW; x++)
        {
            for (int y = 0; y < TiamatConfig.serverSettings.craftH; y++)
            {
                //Expand from bottom-left
                allowedCraftingSlots.add(6 + x - y * 3);
            }
        }


        //Limit slots
        int invSize = TiamatInventory.inventorySize(player);
        for (int i = 0; i < container.inventorySlots.size(); i++)
        {
            Slot slot = container.inventorySlots.get(i);
            if (slot == null) continue;


            int slotIndex = slot.getSlotIndex();
            if (container instanceof TiamatInventoryContainer)
            {
                //For TiamatInventoryContainer
                if (slot.inventory instanceof InventoryCraftResult)
                {
                    if (TiamatConfig.serverSettings.craftW == 0 || TiamatConfig.serverSettings.craftH == 0)
                    {
                        container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                    }
                }
                else if (slot.inventory instanceof InventoryCrafting)
                {
                    if (!allowedCraftingSlots.contains(slotIndex))
                    {
                        container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                    }
                }
                else if (slot.inventory instanceof InventoryPlayer)
                {
                    if (!TiamatInventory.playerHasHotbar(player) && (slotIndex < 9 || slotIndex == 40))
                    {
                        container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                    }
                    else if (slotIndex >= 9 + invSize && slotIndex < 36)
                    {
                        container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                    }
                }
            }
            else
            {
                //For Non-TiamatInventoryContainer
                if (slot.inventory instanceof InventoryPlayer)
                {
                    if (!TiamatInventory.playerHasHotbar(player) && (slotIndex < 9 || slotIndex == 40))
                    {
                        if (tiamatInventory != null)
                        {
                            if (slotIndex < 4)
                            {
                                //Replace first 4 (unavailable) hotbar slots with weaponsets
                                int pairedIndex = slotIndex % 2 == 0 ? slotIndex + 1 : slotIndex - 1;

                                Slot oldSlot = container.inventorySlots.get(i);
                                //First 4 slot indices just happen to line up here; first four hotbar in vanilla inv -> weaponsets in Tiamat inv
                                Slot newSlot = new FilteredSlot(tiamatInventory, pairedIndex, oldSlot.xPos, oldSlot.yPos, TEXTURE, TEXTURE_W, TEXTURE_H, 608, 0, false, WEAPON_SLOT_STACK_LIMIT, stack ->
                                {
                                    ItemStack other = tiamatInventory.getStackInSlot(slotIndex);
                                    return other.isEmpty() || (!Slottings.isTwoHanded(stack) && !Slottings.isTwoHanded(other));
                                });
                                newSlot.slotNumber = oldSlot.slotNumber;
                                container.inventorySlots.set(i, newSlot);
                            }
                            else if (slotIndex < 7)
                            {
                                //Replace 5th - 7th (unavailable) hotbar slots with quickslots
                                Slot oldSlot = container.inventorySlots.get(i);
                                Slot newSlot = new FilteredSlot(tiamatInventory, slotIndex + 2, oldSlot.xPos, oldSlot.yPos, TEXTURE, TEXTURE_W, TEXTURE_H, 784, 0, true, 1, stack -> stack.hasTagCompound() && Slottings.itemIsValidForSlot(stack, "Tiamat Quick Item"));
                                newSlot.slotNumber = oldSlot.slotNumber;
                                container.inventorySlots.set(i, newSlot);
                            }
                            else if (slotIndex == 7)
                            {
                                //Replace 8th (unavailable) hotbar slot with pet
                                Slot oldSlot = container.inventorySlots.get(i);
                                Slot newSlot = new FilteredSlot(tiamatInventory, 10, oldSlot.xPos, oldSlot.yPos, TEXTURE, TEXTURE_W, TEXTURE_H, 640, 0, true, 1, stack -> stack.hasTagCompound() && Slottings.itemIsValidForSlot(stack, "Tiamat Pet"));
                                newSlot.slotNumber = oldSlot.slotNumber;
                                container.inventorySlots.set(i, newSlot);
                            }
                            else if (slotIndex == 8)
                            {
                                //Replace 9th (unavailable) hotbar slot with deck
                                Slot oldSlot = container.inventorySlots.get(i);
                                Slot newSlot = new FilteredSlot(tiamatInventory, 11, oldSlot.xPos, oldSlot.yPos, TEXTURE, TEXTURE_W, TEXTURE_H, 752, 0, true, 1, stack -> stack.hasTagCompound() && Slottings.itemIsValidForSlot(stack, "Tiamat Deck"));
                                newSlot.slotNumber = oldSlot.slotNumber;
                                container.inventorySlots.set(i, newSlot);
                            }
                            else container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                        }
                        else container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                    }
                    else if (slotIndex >= 9 + invSize && slotIndex < 36)
                    {
                        container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void itemPickup(EntityItemPickupEvent event)
    {
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();

        EntityItem entityItem = event.getItem();
        if (entityItem.cannotPickup()) return;
        String owner = entityItem.getOwner();
        if (owner != null && !owner.equals(player.getName()) && entityItem.lifespan - (int) ReflectionTool.get(ITEMSTACK_AGE_FIELD, entityItem) > 200) return;


        ItemStack stack = event.getItem().getItem();
        int oldCount = stack.getCount();


        autoPickup(player, stack);
        if (!stack.isEmpty()) event.setCanceled(true);

        int pickedUpCount = oldCount - stack.getCount();
        if (pickedUpCount > 0)
        {
            player.addStat(StatList.getObjectsPickedUpStats(stack.getItem()), pickedUpCount);

            for (EntityPlayer trackingPlayer : ((WorldServer) player.world).getEntityTracker().getTrackingPlayers(event.getItem()))
            {
                Network.WRAPPER.sendTo(new Network.PickupSoundPacket(player.posX, player.posY, player.posZ), (EntityPlayerMP) trackingPlayer);
            }

            event.getItem().setItem(event.getItem().getItem());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void inventoryChanged(InventoryChangedEvent event)
    {
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) entity;
        GameType gameType = MCTools.getGameType(player);
        if (gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR) return;

        Network.WRAPPER.sendTo(new Network.InventoryDataPacket(InventoryHacks.getCurrentInventorySize(player), TiamatConfig.serverSettings.craftW, TiamatConfig.serverSettings.craftH, TiamatConfig.serverSettings.allowHotbar), player);

        dropItemsInBlockedSlots(player);
    }

    public static void dropItemsInBlockedSlots(EntityPlayerMP player)
    {
        InventoryPlayer playerInventory = player.inventory;
        TiamatPlayerInventory tiamatInv = TiamatPlayerInventory.tiamatServerInventories.get(player.getPersistentID());

        boolean changed = false, haveHotbar = TiamatInventory.playerHasHotbar(player);

        if (!haveHotbar)
        {
            //Blocked mainhand / offhand (if a container is opened)
            if (player.openContainer != player.inventoryContainer || (tiamatInv != null && tiamatInv.playerMPHasAnyContainerOpen))
            {
                for (int i : new int[]{0, 40})
                {
                    ItemStack stack = playerInventory.getStackInSlot(i);
                    if (!stack.isEmpty())
                    {
                        autoPickup(player, stack);
                        if (!stack.isEmpty())
                        {
                            ItemStack copy = MCTools.cloneItemStack(stack);
                            stack.setCount(0);
                            player.entityDropItem(copy, 0);
                        }
                        changed = true;
                    }
                }
            }

            //Blocked hotbar slots (all except first)
            for (int i = 1; i < 9; i++)
            {
                ItemStack stack = playerInventory.getStackInSlot(i);
                if (!stack.isEmpty())
                {
                    autoPickup(player, stack);
                    if (!stack.isEmpty())
                    {
                        ItemStack copy = MCTools.cloneItemStack(stack);
                        stack.setCount(0);
                        player.entityDropItem(copy, 0);
                    }
                    changed = true;
                }
            }
        }

        //Blocked "cargo" slots
        for (int i = 9 + TiamatInventory.inventorySize(player); i < 36; i++)
        {
            ItemStack stack = playerInventory.getStackInSlot(i);
            if (!stack.isEmpty())
            {
                autoPickup(player, stack);
                if (!stack.isEmpty())
                {
                    ItemStack copy = MCTools.cloneItemStack(stack);
                    stack.setCount(0);
                    player.entityDropItem(copy, 0);
                }
                changed = true;
            }
        }

        //If neither hand is empty and one of them is 2H, drop offhand
        ItemStack mainhand = player.getHeldItemMainhand(), offhand = player.getHeldItemOffhand();
        if (!mainhand.isEmpty() && !offhand.isEmpty() && (Slottings.isTwoHanded(mainhand) || Slottings.isTwoHanded(offhand)))
        {
            ItemStack copy = MCTools.cloneItemStack(offhand);
            offhand.setCount(0);
            player.entityDropItem(copy, 0);

            changed = true;
        }

        //If no hotbar and both weaponsets are non-empty, drop vanilla hands
        if (!haveHotbar)
        {
            if (tiamatInv != null)
            {
                if (!tiamatInv.weaponset1Empty() && !tiamatInv.weaponset2Empty())
                {
                    mainhand = player.getHeldItemMainhand();
                    if (!mainhand.isEmpty())
                    {
                        ItemStack copy = MCTools.cloneItemStack(mainhand);
                        mainhand.setCount(0);
                        player.entityDropItem(copy, 0);

                        changed = true;
                    }

                    offhand = player.getHeldItemOffhand();
                    if (!offhand.isEmpty())
                    {
                        ItemStack copy = MCTools.cloneItemStack(offhand);
                        offhand.setCount(0);
                        player.entityDropItem(copy, 0);

                        changed = true;
                    }
                }
            }
        }


        //Update
        if (changed)
        {
            for (IContainerListener listener : (List<IContainerListener>) ReflectionTool.get(CONTAINER_LISTENERS_FIELD, player.inventoryContainer))
            {
                listener.sendAllContents(player.inventoryContainer, player.inventoryContainer.inventoryItemStacks);
            }
        }
    }

    protected static void autoPickup(EntityPlayerMP player, ItemStack stack)
    {
        InventoryPlayer vanillaInv = player.inventory;
        TiamatPlayerInventory tiamatInv = TiamatPlayerInventory.tiamatServerInventories.get(player.getPersistentID());

        ArrayList<Pair<IInventory, Integer>> slotOrder = new ArrayList<>();
        ItemStack mainhand = player.getHeldItemMainhand(), offhand = player.getHeldItemOffhand();
        boolean stackIs2H = Slottings.isTwoHanded(stack);

        if (player.isCreative() || player.isSpectator())
        {
            //Pet
            if (Slottings.itemIsValidForSlot(stack, "Tiamat Pet")) slotOrder.add(new Pair<>(tiamatInv, 10));

            //Deck
            if (Slottings.itemIsValidForSlot(stack, "Tiamat Deck")) slotOrder.add(new Pair<>(tiamatInv, 11));

            //Armor
            if (Slottings.itemIsValidForSlot(stack, "Head")) slotOrder.add(new Pair<>(vanillaInv, 39));
            if (Slottings.itemIsValidForSlot(stack, "Tiamat Shoulders")) slotOrder.add(new Pair<>(tiamatInv, 4));
            if (Slottings.itemIsValidForSlot(stack, "Tiamat Cape")) slotOrder.add(new Pair<>(tiamatInv, 5));
            if (Slottings.itemIsValidForSlot(stack, "Chest")) slotOrder.add(new Pair<>(vanillaInv, 38));
            if (Slottings.itemIsValidForSlot(stack, "Legs")) slotOrder.add(new Pair<>(vanillaInv, 37));
            if (Slottings.itemIsValidForSlot(stack, "Feet")) slotOrder.add(new Pair<>(vanillaInv, 36));

            //Quickslots
            if (Slottings.itemIsValidForSlot(stack, "Tiamat Quick Item"))
            {
                slotOrder.add(new Pair<>(tiamatInv, 6));
                slotOrder.add(new Pair<>(tiamatInv, 7));
                slotOrder.add(new Pair<>(tiamatInv, 8));
            }

            //Vanilla mainhand
            slotOrder.add(new Pair<>(vanillaInv, player.inventory.currentItem));

            //Hotbar
            for (int i = 1; i <= 8; i++)
            {
                slotOrder.add(new Pair<>(vanillaInv, Tools.posMod(i + player.inventory.currentItem, 9)));
            }

            //Vanilla offhand
            slotOrder.add(new Pair<>(vanillaInv, 40));

            //Weaponsets
            for (int i = 0; i < 4; i++) slotOrder.add(new Pair<>(tiamatInv, i));

            //Cargo
            int last = TiamatInventory.inventorySize(player) + 8;
            for (int i = 9; i <= last; i++) slotOrder.add(new Pair<>(vanillaInv, i));
        }
        else
        {
            boolean targetVanillaHands, targetWeaponset1, targetWeaponset2;
            if (TiamatInventory.playerHasHotbar(player))
            {
                targetVanillaHands = true;
                targetWeaponset1 = true;
                targetWeaponset2 = true;
            }
            else
            {
                //No hotbar
                if (player.openContainer == player.inventoryContainer && !tiamatInv.playerMPHasAnyContainerOpen && (TiamatConfig.serverSettings.autopickupSettings.allowPickupMainHand || TiamatConfig.serverSettings.autopickupSettings.allowPickupOffhand) && (tiamatInv.weaponset1Empty() || tiamatInv.weaponset2Empty()))
                {
                    targetVanillaHands = true;
                    if (TiamatConfig.serverSettings.autopickupSettings.allowPickupWeaponset)
                    {
                        if (tiamatInv.weaponset1Empty())
                        {
                            targetWeaponset1 = false;
                            targetWeaponset2 = true;
                        }
                        else
                        {
                            targetWeaponset1 = true;
                            targetWeaponset2 = false;
                        }
                    }
                    else
                    {
                        targetWeaponset1 = false;
                        targetWeaponset2 = false;
                    }
                }
                else
                {
                    targetVanillaHands = false;
                    if (TiamatConfig.serverSettings.autopickupSettings.allowPickupWeaponset)
                    {
                        targetWeaponset1 = true;
                        targetWeaponset2 = true;
                    }
                    else
                    {
                        targetWeaponset1 = false;
                        targetWeaponset2 = false;
                    }
                }
            }


            if (TiamatConfig.serverSettings.autopickupSettings.allowPickupPet)
            {
                if (Slottings.itemIsValidForSlot(stack, "Tiamat Pet")) slotOrder.add(new Pair<>(tiamatInv, 10));
            }
            if (TiamatConfig.serverSettings.autopickupSettings.allowPickupDeck)
            {
                if (Slottings.itemIsValidForSlot(stack, "Tiamat Deck")) slotOrder.add(new Pair<>(tiamatInv, 11));
            }
            if (TiamatConfig.serverSettings.autopickupSettings.allowPickupArmor)
            {
                if (Slottings.itemIsValidForSlot(stack, "Head")) slotOrder.add(new Pair<>(vanillaInv, 39));
                if (Slottings.itemIsValidForSlot(stack, "Tiamat Shoulders")) slotOrder.add(new Pair<>(tiamatInv, 4));
                if (Slottings.itemIsValidForSlot(stack, "Tiamat Cape")) slotOrder.add(new Pair<>(tiamatInv, 5));
                if (Slottings.itemIsValidForSlot(stack, "Chest")) slotOrder.add(new Pair<>(vanillaInv, 38));
                if (Slottings.itemIsValidForSlot(stack, "Legs")) slotOrder.add(new Pair<>(vanillaInv, 37));
                if (Slottings.itemIsValidForSlot(stack, "Feet")) slotOrder.add(new Pair<>(vanillaInv, 36));
            }
            if (TiamatConfig.serverSettings.autopickupSettings.allowPickupQuickslots)
            {
                if (Slottings.itemIsValidForSlot(stack, "Tiamat Quick Item"))
                {
                    slotOrder.add(new Pair<>(tiamatInv, 6));
                    slotOrder.add(new Pair<>(tiamatInv, 7));
                    slotOrder.add(new Pair<>(tiamatInv, 8));
                }
            }
            if (targetVanillaHands && TiamatConfig.serverSettings.autopickupSettings.allowPickupMainHand && (offhand.isEmpty() || (!stackIs2H && !Slottings.isTwoHanded(offhand))))
            {
                slotOrder.add(new Pair<>(vanillaInv, player.inventory.currentItem));
            }
            if (TiamatConfig.serverSettings.allowHotbar && TiamatConfig.serverSettings.autopickupSettings.allowPickupHotbar)
            {
                for (int i = 1; i <= 8; i++)
                {
                    slotOrder.add(new Pair<>(vanillaInv, Tools.posMod(i + player.inventory.currentItem, 9)));
                }
            }
            if (targetVanillaHands && TiamatConfig.serverSettings.autopickupSettings.allowPickupOffhand && (mainhand.isEmpty() || (!stackIs2H && !Slottings.isTwoHanded(mainhand))))
            {
                slotOrder.add(new Pair<>(vanillaInv, 40));
            }
            if (TiamatConfig.serverSettings.autopickupSettings.allowPickupWeaponset)
            {
                if (targetWeaponset1)
                {
                    if (tiamatInv.getSheathedOffhand1().isEmpty() || (!stackIs2H && !Slottings.isTwoHanded(tiamatInv.getSheathedOffhand1()))) slotOrder.add(new Pair<>(tiamatInv, 0));
                    if (tiamatInv.getSheathedMainhand1().isEmpty() || (!stackIs2H && !Slottings.isTwoHanded(tiamatInv.getSheathedMainhand1()))) slotOrder.add(new Pair<>(tiamatInv, 1));
                }
                if (targetWeaponset2)
                {
                    if (tiamatInv.getSheathedOffhand2().isEmpty() || (!stackIs2H && !Slottings.isTwoHanded(tiamatInv.getSheathedOffhand2()))) slotOrder.add(new Pair<>(tiamatInv, 2));
                    if (tiamatInv.getSheathedMainhand2().isEmpty() || (!stackIs2H && !Slottings.isTwoHanded(tiamatInv.getSheathedMainhand2()))) slotOrder.add(new Pair<>(tiamatInv, 3));
                }
            }
            if (TiamatConfig.serverSettings.autopickupSettings.allowPickupCargo)
            {
                int last = TiamatInventory.inventorySize(player) + 8;
                for (int i = 9; i <= last; i++) slotOrder.add(new Pair<>(vanillaInv, i));
            }
        }


        //Fill slots that already have the same type of item in forward order, track whether any did (even if they were full), and remove them from the list if they're not empty
        Boolean[] found = new Boolean[]{false};
        slotOrder.removeIf(pair ->
        {
            ItemStack stack2 = pair.getKey().getStackInSlot(pair.getValue());
            if (stack2.isEmpty()) return false;

            if (ItemMatcher.stacksMatch(stack, stack2, false))
            {
                found[0] = true;

                int moveAmount = Tools.min(stack2.getMaxStackSize() - stack2.getCount(), stack.getCount());
                if (moveAmount > 0)
                {
                    stack.shrink(moveAmount);
                    stack2.grow(moveAmount);
                }
            }
            return true;
        });
        if (stack.isEmpty() || slotOrder.isEmpty()) return;


        //If none were already in inventory, fill up to one empty slot in forward order and remove it from the list
        int max = stack.getMaxStackSize();
        if (!found[0])
        {
            for (Pair<IInventory, Integer> pair : slotOrder)
            {
                IInventory inventory = pair.getKey();
                int slot = pair.getValue();

                if (!inventory.getStackInSlot(slot).isEmpty()) continue;

                ItemStack copy = MCTools.cloneItemStack(stack);
                int moveAmount = Tools.min(max, stack.getCount());
                stack.shrink(moveAmount);
                copy.setCount(moveAmount);
                inventory.setInventorySlotContents(slot, copy);

                //Only filling one slot max!
                slotOrder.remove(pair);
                break;
            }
            if (stack.isEmpty() || slotOrder.isEmpty()) return;
        }


        //Fill empty slots in reverse order
        for (int i = slotOrder.size() - 1; i >= 0; i--)
        {
            Pair<IInventory, Integer> pair = slotOrder.get(i);
            IInventory inventory = pair.getKey();
            int slot = pair.getValue();

            if (!inventory.getStackInSlot(slot).isEmpty()) continue;

            ItemStack copy = MCTools.cloneItemStack(stack);
            int moveAmount = Tools.min(max, stack.getCount());
            stack.shrink(moveAmount);
            copy.setCount(moveAmount);
            inventory.setInventorySlotContents(slot, copy);

            if (stack.isEmpty()) break;
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerContainerState(PlayerContainerEvent.Open event)
    {
        TiamatPlayerInventory inventory = TiamatPlayerInventory.tiamatServerInventories.get(event.getEntityPlayer().getPersistentID());
        if (inventory == null) return;

        inventory.playerMPHasAnyContainerOpen = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerContainerState(PlayerContainerEvent.Close event)
    {
        TiamatPlayerInventory inventory = TiamatPlayerInventory.tiamatServerInventories.get(event.getEntityPlayer().getPersistentID());
        if (inventory == null) return;

        inventory.playerMPHasAnyContainerOpen = false;
    }
}
