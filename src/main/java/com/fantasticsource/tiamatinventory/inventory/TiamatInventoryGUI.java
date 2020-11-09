package com.fantasticsource.tiamatinventory.inventory;

import com.fantasticsource.mctools.inventory.gui.BetterContainerGUI;
import com.fantasticsource.tiamatinventory.AttributeDisplayData;
import com.fantasticsource.tiamatinventory.Keys;
import com.fantasticsource.tools.Collision;
import com.fantasticsource.tools.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.fantasticsource.tiamatinventory.inventory.TiamatInventoryContainer.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;

@SideOnly(Side.CLIENT)
public class TiamatInventoryGUI extends BetterContainerGUI
{
    public static final int MODEL_WINDOW_X = 43, MODEL_WINDOW_Y = 6, MODEL_WINDOW_W = 88, MODEL_WINDOW_H = 106;
    public static final int STAT_WINDOW_X = 25, STAT_WINDOW_Y = 6, STAT_WINDOW_W = 261, STAT_WINDOW_H = 124;
    public static final int STAT_SCROLLBAR_X = 288, STAT_SCROLLBAR_Y = 6, STAT_SCROLLBAR_W = 5, STAT_SCROLLBAR_H = 124;
    public static final int STAT_SCROLLKNOB_SIZE = 5;
    public static final double U_PIXEL = 1d / TEXTURE_W, V_PIXEL = 1d / TEXTURE_H;

    protected static double statsScroll = 0;
    protected static int statLineHeight;
    protected static int statHeightDif;
    protected int tab = 0;
    protected String[] rawStats, stats, statTooltips;
    protected boolean statsScrollGrabbed = false, modelGrabbed = false;
    protected int uOffset, vOffset, modelGrabX, modelGrabY;
    protected double modelYaw = 0, modelPitch = 0, modelScale = 1;
    protected Container inventorySlotsSaved;

    public TiamatInventoryGUI()
    {
        super(new TiamatInventoryContainer(Minecraft.getMinecraft().player));
        inventorySlotsSaved = inventorySlots;

        rawStats = new String[AttributeDisplayData.displayAttributes.length];
        stats = new String[rawStats.length];
        statTooltips = new String[rawStats.length];
        for (int i = 0; i < stats.length; i++)
        {
            String name = AttributeDisplayData.displayAttributes[i];
            rawStats[i] = name;
            if (name.isEmpty()) stats[i] = "";
            else stats[i] = name.replace(TextFormatting.getTextWithoutFormattingCodes(name), I18n.translateToLocal("attribute.name." + name));
            statTooltips[i] = I18n.translateToLocal(AttributeDisplayData.displayAttributeDescriptions[i]);
        }
        statLineHeight = fontRenderer.FONT_HEIGHT + 1;
        statHeightDif = Tools.max(0, statLineHeight * stats.length - STAT_WINDOW_H);
    }

    public static void drawEntityOnScreen(int posX, int posY, double scale, double yaw, double pitch, EntityLivingBase ent)
    {
        GlStateManager.enableColorMaterial();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50);
        GlStateManager.scale((float) (-30), (float) 30, (float) 30);
        GlStateManager.rotate(180, 0, 0, 1);

        GlStateManager.rotate((float) pitch, 1, 0, 0);
        GlStateManager.rotate((float) yaw + ent.renderYawOffset, 0, 1, 0);
        GlStateManager.scale((float) (scale), (float) scale, (float) scale);

        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntity(ent, 0, -ent.height / 2, 0, 0, 1, false);
        rendermanager.setRenderShadow(true);

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public void initGui()
    {
        setTab(tab);

        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
    }

    @Override
    public void onGuiClosed()
    {
        if (mc.player != null && inventorySlots != null) inventorySlots.onContainerClosed(mc.player);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        if (tab == 1)
        {
            //Render scrollknob
            GlStateManager.color(1, 1, 1, 1);
            mc.getTextureManager().bindTexture(TEXTURE);

            double scrollKnobY = STAT_SCROLLBAR_Y + (STAT_SCROLLBAR_H - STAT_SCROLLKNOB_SIZE) * statsScroll;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(STAT_SCROLLBAR_X, scrollKnobY + STAT_SCROLLKNOB_SIZE, zLevel).tex(544 * U_PIXEL, (64 + STAT_SCROLLKNOB_SIZE) * V_PIXEL).endVertex();
            bufferbuilder.pos(STAT_SCROLLBAR_X + STAT_SCROLLBAR_W, scrollKnobY + STAT_SCROLLKNOB_SIZE, zLevel).tex((544 + STAT_SCROLLBAR_W) * U_PIXEL, (64 + STAT_SCROLLKNOB_SIZE) * V_PIXEL).endVertex();
            bufferbuilder.pos(STAT_SCROLLBAR_X + STAT_SCROLLBAR_W, scrollKnobY, zLevel).tex((544 + STAT_SCROLLBAR_W) * U_PIXEL, 64 * V_PIXEL).endVertex();
            bufferbuilder.pos(STAT_SCROLLBAR_X, scrollKnobY, zLevel).tex(544 * U_PIXEL, 64 * V_PIXEL).endVertex();
            tessellator.draw();


            //Render stats
            int hoveredIndex = -1;
            if (Collision.pointRectangle(mouseX - guiLeft, mouseY - guiTop, STAT_WINDOW_X, STAT_WINDOW_Y, STAT_WINDOW_X + STAT_WINDOW_W, STAT_WINDOW_Y + STAT_WINDOW_H))
            {
                hoveredIndex = (int) ((mouseY - guiTop - STAT_WINDOW_Y + statHeightDif * statsScroll) / statLineHeight);
            }

            scissor(STAT_WINDOW_X, STAT_WINDOW_Y, STAT_WINDOW_W, STAT_WINDOW_H);

            int yy = STAT_WINDOW_Y;

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -statsScroll * statHeightDif, 0);
            int spacing = fontRenderer.getCharWidth('0');
            for (int i = 0; i < stats.length; i++)
            {
                String rawStat = rawStats[i];
                if (rawStat.isEmpty())
                {
                    yy += statLineHeight;
                    continue;
                }

                IAttributeInstance attributeInstance = mc.player.getAttributeMap().getAttributeInstanceByName(TextFormatting.getTextWithoutFormattingCodes(rawStat));
                if (attributeInstance == null)
                {
                    drawString(fontRenderer, TextFormatting.RED + "ERROR: <" + rawStat + ">", STAT_WINDOW_X, yy, 0xffffffff);
                    yy += statLineHeight;
                    continue;
                }

                String stat = stats[i];
                int color = hoveredIndex == i ? 0xffffffff : 0xff777777;
                drawString(fontRenderer, stat, STAT_WINDOW_X, yy, color);
                String amount = String.format("%.2f", attributeInstance.getAttributeValue());
                int xx = STAT_WINDOW_X + STAT_WINDOW_W - fontRenderer.getStringWidth(amount);
                drawString(fontRenderer, amount, xx, yy, color);

                drawHorizontalLine(STAT_WINDOW_X + fontRenderer.getStringWidth(stat) + spacing, xx - spacing, yy + (statLineHeight >>> 1) - 1, color);
                yy += statLineHeight;
            }
            GlStateManager.popMatrix();

            unScissor();


            //Render stat tooltips
            if (hoveredIndex >= 0 && hoveredIndex < statTooltips.length)
            {
                String tooltip = statTooltips[hoveredIndex];
                if (!tooltip.trim().isEmpty())
                {
                    List<String> list = Arrays.asList(Tools.fixedSplit(tooltip, "\n"));
                    for (int i = 0; i < list.size(); i++) list.set(i, TextFormatting.DARK_PURPLE + list.get(i));
                    drawHoveringText(list, mouseX - guiLeft, mouseY - guiTop);
                }
            }
        }
    }

    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(guiLeft, guiTop + ySize, zLevel).tex(uOffset * U_PIXEL, (vOffset + ySize) * V_PIXEL).endVertex();
        bufferbuilder.pos(guiLeft + xSize, guiTop + ySize, zLevel).tex((uOffset + xSize) * U_PIXEL, (vOffset + ySize) * V_PIXEL).endVertex();
        bufferbuilder.pos(guiLeft + xSize, guiTop, zLevel).tex((uOffset + xSize) * U_PIXEL, vOffset * V_PIXEL).endVertex();
        bufferbuilder.pos(guiLeft, guiTop, zLevel).tex(uOffset * U_PIXEL, vOffset * V_PIXEL).endVertex();
        tessellator.draw();

        if (tab == 0)
        {
            scissor(MODEL_WINDOW_X, MODEL_WINDOW_Y, MODEL_WINDOW_W, MODEL_WINDOW_H);
            drawEntityOnScreen(guiLeft + MODEL_WINDOW_X + (MODEL_WINDOW_W >> 1), guiTop + MODEL_WINDOW_Y + (MODEL_WINDOW_H >> 1), modelScale, modelYaw, modelPitch, mc.player);
            unScissor();
        }
    }

    private void setTab(int tab)
    {
        inventorySlots = tab == 0 ? inventorySlotsSaved : null;
        mc.player.openContainer = inventorySlots;

        this.tab = tab;

        buttonList.clear();

        xSize = 318;
        ySize = 136;
        uOffset = 0;
        vOffset = 136 * tab;

        guiTop = (height - ySize) >> 1;
        guiLeft = (width - xSize) >> 1;

        //Tab buttons
        buttonList.add(new GuiButtonImage(0, guiLeft + 299, guiTop + 7, 19, 23, TEXTURE_W - 18, TEXTURE_H - 21, 0, TEXTURE));
        buttonList.add(new GuiButtonImage(1, guiLeft + 299, guiTop + 32, 19, 23, TEXTURE_W - 18, TEXTURE_H - 21, 0, TEXTURE));
        buttonList.add(new GuiButtonImage(2, guiLeft + 299, guiTop + 57, 19, 23, TEXTURE_W - 18, TEXTURE_H - 21, 0, TEXTURE));
        buttonList.add(new GuiButtonImage(3, guiLeft + 299, guiTop + 82, 19, 23, TEXTURE_W - 18, TEXTURE_H - 21, 0, TEXTURE));
        buttonList.add(new GuiButtonImage(4, guiLeft + 299, guiTop + 107, 19, 23, TEXTURE_W - 18, TEXTURE_H - 21, 0, TEXTURE));

        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, buttonList));
    }

    protected void actionPerformed(GuiButton button)
    {
        buttonClicked_ = true;

        if (button.id <= 5) setTab(button.id);
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();

        int scroll = Mouse.getDWheel();
        if (scroll != 0)
        {
            if (tab == 1)
            {
                ScaledResolution sr = new ScaledResolution(mc);

                int mouseX = Mouse.getX() / sr.getScaleFactor(), mouseY = sr.getScaledHeight() - Mouse.getY() / sr.getScaleFactor();
                if (Collision.pointRectangle(mouseX, mouseY, guiLeft + STAT_WINDOW_X, guiTop + STAT_WINDOW_Y, guiLeft + STAT_WINDOW_X + STAT_WINDOW_W, guiTop + STAT_WINDOW_Y + STAT_WINDOW_H)
                        || Collision.pointRectangle(mouseX, mouseY, guiLeft + STAT_SCROLLBAR_X, guiTop + STAT_SCROLLBAR_Y, guiLeft + STAT_SCROLLBAR_X + STAT_SCROLLBAR_W, guiTop + STAT_SCROLLBAR_Y + STAT_SCROLLBAR_H))
                {
                    if (scroll > 0) statsScroll = Tools.max(0, statsScroll - (double) statLineHeight / statHeightDif);
                    else statsScroll = Tools.min(1, statsScroll + (double) statLineHeight / statHeightDif);
                }
                else if (Collision.pointRectangle(mouseX, mouseY, guiLeft + MODEL_WINDOW_X, guiTop + MODEL_WINDOW_Y, guiLeft + MODEL_WINDOW_X + MODEL_WINDOW_W, guiTop + MODEL_WINDOW_Y + MODEL_WINDOW_H))
                {
                    if (scroll > 0) modelScale *= 1.1;
                    else modelScale /= 1.1;
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (tab == 0)
        {
            if (Collision.pointRectangle(mouseX - guiLeft, mouseY - guiTop, MODEL_WINDOW_X, MODEL_WINDOW_Y, MODEL_WINDOW_X + MODEL_WINDOW_W, MODEL_WINDOW_Y + MODEL_WINDOW_H))
            {
                modelGrabbed = true;
                modelGrabX = mouseX;
                modelGrabY = mouseY;
            }
        }
        if (tab == 1)
        {
            if (mouseButton == 0 && Collision.pointRectangle(mouseX - guiLeft, mouseY - guiTop, STAT_SCROLLBAR_X, STAT_SCROLLBAR_Y, STAT_SCROLLBAR_X + STAT_SCROLLBAR_W, STAT_SCROLLBAR_Y + STAT_SCROLLBAR_H))
            {
                statsScroll = Tools.min(Tools.max((mouseY - guiTop - STAT_SCROLLBAR_Y - (double) (STAT_SCROLLKNOB_SIZE >> 1)) / (STAT_SCROLLBAR_H - STAT_SCROLLKNOB_SIZE), 0), 1);
                statsScrollGrabbed = true;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if (statsScrollGrabbed)
        {
            statsScroll = Tools.min(Tools.max((mouseY - guiTop - STAT_SCROLLBAR_Y - (double) (STAT_SCROLLKNOB_SIZE >> 1)) / (STAT_SCROLLBAR_H - STAT_SCROLLKNOB_SIZE), 0), 1);
        }
        else if (modelGrabbed)
        {
            modelYaw += mouseX - modelGrabX;
            modelPitch += mouseY - modelGrabY;

            modelGrabX = mouseX;
            modelGrabY = mouseY;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);

        statsScrollGrabbed = false;
        modelGrabbed = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
    {
        if (keyCode == 1 || Keys.INVENTORY.isActiveAndMatches(keyCode))
        {
            if (tab == 0) mc.player.connection.sendPacket(new CPacketCloseWindow(mc.player.openContainer.windowId));
            mc.player.closeScreenAndDropStack();
            return;
        }

        checkHotbarKeys(keyCode);

        if (hoveredSlot_ != null && hoveredSlot_.getHasStack())
        {
            if (mc.gameSettings.keyBindPickBlock.isActiveAndMatches(keyCode))
            {
                handleMouseClick(hoveredSlot_, hoveredSlot_.slotNumber, 0, ClickType.CLONE);
            }
            else if (mc.gameSettings.keyBindDrop.isActiveAndMatches(keyCode))
            {
                handleMouseClick(hoveredSlot_, hoveredSlot_.slotNumber, isCtrlKeyDown() ? 1 : 0, ClickType.THROW);
            }
        }
    }


    private void scissor(int x, int y, int w, int h)
    {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL_SCISSOR_TEST);
        GL11.glScissor((guiLeft + x) * scale, (sr.getScaledHeight() - (guiTop + y + h)) * scale, w * scale, h * scale);
    }

    private void unScissor()
    {
        GL11.glDisable(GL_SCISSOR_TEST);
    }
}
