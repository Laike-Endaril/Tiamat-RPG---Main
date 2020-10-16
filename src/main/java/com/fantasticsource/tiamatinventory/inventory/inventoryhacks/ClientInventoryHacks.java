package com.fantasticsource.tiamatinventory.inventory.inventoryhacks;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.mctools.inventory.slot.FilteredSlot;
import com.fantasticsource.tiamatinventory.inventory.TiamatInventoryContainer;
import com.fantasticsource.tiamatinventory.inventory.TiamatInventoryGUI;
import com.fantasticsource.tiamatinventory.inventory.TiamatPlayerInventory;
import com.fantasticsource.tiamatitems.nbt.MiscTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fantasticsource.tiamatinventory.inventory.TiamatInventoryContainer.WEAPON_SLOT_STACK_LIMIT;

public class ClientInventoryHacks extends GuiButton
{
    protected GuiContainer gui;
    protected boolean isTiamat;

    public ClientInventoryHacks(GuiContainer gui)
    {
        super(Integer.MIN_VALUE + 777, -10000, -10000, 0, 0, "");
        this.gui = gui;
        isTiamat = gui instanceof TiamatInventoryGUI;
    }

    @Override
    protected int getHoverState(boolean mouseOver)
    {
        return 0;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        mc.renderEngine.bindTexture(TiamatInventoryGUI.TEXTURE);

        TiamatPlayerInventory inventory = TiamatPlayerInventory.tiamatClientInventory;

        Container container = gui.inventorySlots;
        if (container == null) return;

        for (int i = 0; i < container.inventorySlots.size(); i++)
        {
            Slot slot = container.inventorySlots.get(i);
            if (slot == null) continue;

            int slotIndex = slot.getSlotIndex();
            if (isTiamat)
            {
                if (slot.inventory instanceof InventoryPlayer && slotIndex < 36 && !InventoryHacks.getAvailableClientInventorySlots().contains(slotIndex))
                {
                    renderTextureAt(gui.getGuiLeft() + slot.xPos - 1, gui.getGuiTop() + slot.yPos - 1, TiamatInventoryGUI.U_PIXEL * 576, TiamatInventoryGUI.V_PIXEL * 16, 18);
                }
            }
            else
            {
                if (slot.inventory == inventory && slotIndex < 4)
                {
                    if (inventory.getStackInSlot(slotIndex).isEmpty())
                    {
                        renderTextureAt(gui.getGuiLeft() + slot.xPos, gui.getGuiTop() + slot.yPos, TiamatInventoryGUI.U_PIXEL * (slotIndex % 2 == 0 ? 608 : 624), 0, 16);
                    }
                }
                else if (slot.inventory instanceof InventoryPlayer && (slotIndex < 9 || slotIndex >= 36 || !InventoryHacks.getAvailableClientInventorySlots().contains(slotIndex)))
                {
                    if (slotIndex < 9) renderTextureAt(gui.getGuiLeft() + slot.xPos - 1, gui.getGuiTop() + slot.yPos - 1, TiamatInventoryGUI.U_PIXEL * 544, TiamatInventoryGUI.V_PIXEL * 16, 18);
                    else renderTextureAt(gui.getGuiLeft() + slot.xPos - 1, gui.getGuiTop() + slot.yPos - 1, TiamatInventoryGUI.U_PIXEL * 576, TiamatInventoryGUI.V_PIXEL * 16, 18);
                }
            }
        }

        GlStateManager.disableBlend();
    }

    protected void renderTextureAt(int x, int y, double u, double v, int size)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);

        bufferbuilder.pos(x, y + size, zLevel).tex(u, v + TiamatInventoryGUI.V_PIXEL * size).endVertex();
        bufferbuilder.pos(x + size, y + size, zLevel).tex(u + TiamatInventoryGUI.U_PIXEL * size, v + TiamatInventoryGUI.V_PIXEL * size).endVertex();
        bufferbuilder.pos(x + size, y, zLevel).tex(u + TiamatInventoryGUI.U_PIXEL * size, v).endVertex();
        bufferbuilder.pos(x, y, zLevel).tex(u, v).endVertex();

        tessellator.draw();
    }

    @SubscribeEvent
    public static void guiPostInit(GuiScreenEvent.InitGuiEvent.Post event)
    {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        GameType gameType = MCTools.getGameType(player);
        if (gameType == null || gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR) return;

        Gui gui = event.getGui();
        if (!(gui instanceof GuiContainer)) return;

        Container container = ((GuiContainer) gui).inventorySlots;
        if (container == null) return;


        List<GuiButton> buttonList = event.getButtonList();
        boolean found = false;
        for (GuiButton button : buttonList)
        {
            if (button instanceof ClientInventoryHacks)
            {
                found = true;
                break;
            }
        }
        if (!found) buttonList.add(new ClientInventoryHacks((GuiContainer) gui));

        ArrayList<Integer> availableSlots = InventoryHacks.getAvailableClientInventorySlots();

        TiamatPlayerInventory inventory = TiamatPlayerInventory.tiamatClientInventory;
        HashMap<Integer, Integer> tiamatSlotToCurrentSlot = new HashMap<>();
        for (int i = 0; i < container.inventorySlots.size(); i++)
        {
            Slot slot = container.inventorySlots.get(i);
            if (slot == null || !(slot.inventory instanceof InventoryPlayer)) continue;

            int slotIndex = slot.getSlotIndex();
            if (container instanceof TiamatInventoryContainer)
            {
                if (slotIndex < 36 && !availableSlots.contains(slotIndex))
                {
                    container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                }
            }
            else
            {
                if (inventory != null && slotIndex < 4)
                {
                    tiamatSlotToCurrentSlot.put(slotIndex, i);
                }
                else if (slotIndex < 9 || slotIndex >= 36 || !availableSlots.contains(slotIndex))
                {
                    container.inventorySlots.set(i, new FakeSlot(slot.inventory, slotIndex, slot.xPos, slot.yPos));
                }
            }
        }

        if (inventory != null)
        {
            for (Map.Entry<Integer, Integer> entry : tiamatSlotToCurrentSlot.entrySet())
            {
                int tiamatIndex = entry.getKey(), currentIndex = entry.getValue();
                int pairedIndex = tiamatIndex % 2 == 0 ? tiamatIndex + 1 : tiamatIndex - 1;

                Slot oldSlot = container.inventorySlots.get(currentIndex);
                Slot newSlot = new FilteredSlot(inventory, tiamatIndex, oldSlot.xPos, oldSlot.yPos, 608, 0, false, WEAPON_SLOT_STACK_LIMIT, stack ->
                {
                    ItemStack other = inventory.getStackInSlot(pairedIndex);
                    return other.isEmpty() || (!MiscTags.isTwoHanded(stack) && !MiscTags.isTwoHanded(other));
                });
                newSlot.slotNumber = oldSlot.slotNumber;
                container.inventorySlots.set(currentIndex, newSlot);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderHotbar(RenderGameOverlayEvent.Pre event)
    {
        GameType gameType = MCTools.getGameType(Minecraft.getMinecraft().player);
        if (gameType == null || gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR) return;

        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) event.setCanceled(true);
    }
}
