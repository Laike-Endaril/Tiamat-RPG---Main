package com.fantasticsource.tiamatrpg;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.mctools.controlintercept.ControlEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.fantasticsource.tiamatrpg.TiamatRPG.MODID;

public class ClientControlHandler
{
    protected static boolean skillset1Locked = false, skillset2Locked = false;

    @SubscribeEvent
    public static void controls(ControlEvent event)
    {
        //Early exit for creative and spectator modes
        GameType gameType = MCTools.getGameType(Minecraft.getMinecraft().player);
        if (gameType == null || gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR) return;

        if (event.state)
        {
            if (event.name.equals("key.attack"))
            {
                //Mainhand / left click
                if (Keys.SKILLSET_1.isKeyDown() && !skillset1Locked)
                {
                    Network.WRAPPER.sendToServer(new Network.ActionPacket(1));
                    skillset1Locked = true;
                    event.cancelOriginal();
                }
                else if (Keys.SKILLSET_2.isKeyDown() && !skillset2Locked)
                {
                    Network.WRAPPER.sendToServer(new Network.ActionPacket(4));
                    skillset2Locked = true;
                    event.cancelOriginal();
                }
                else if (Minecraft.getMinecraft().player.inventory.currentItem == 0)
                {
                    //TODO reenable this and do the actions for it
//                        Network.WRAPPER.sendToServer(new Network.ActionPacket(Network.ACTION_MAINHAND));
//                        event.cancelOriginal();
                }
            }
            else if (event.name.equals("key.use"))
            {
                //Offhand / right click
                if (Keys.SKILLSET_1.isKeyDown() && !skillset1Locked)
                {
                    Network.WRAPPER.sendToServer(new Network.ActionPacket(2));
                    skillset1Locked = true;
                    event.cancelOriginal();
                }
                else if (Keys.SKILLSET_2.isKeyDown() && !skillset2Locked)
                {
                    Network.WRAPPER.sendToServer(new Network.ActionPacket(5));
                    skillset2Locked = true;
                    event.cancelOriginal();
                }
                else if (Minecraft.getMinecraft().player.inventory.currentItem == 0)
                {
                    //TODO reenable this and do the actions for it
//                        Network.WRAPPER.sendToServer(new Network.ActionPacket(Network.ACTION_OFFHAND));
//                        event.cancelOriginal();
                }
            }
        }
        else
        {
            if (event.name.equals(MODID + ".key.skillset1"))
            {
                if (!skillset1Locked) Network.WRAPPER.sendToServer(new Network.ActionPacket(0));
                skillset1Locked = false;
            }
            else if (event.name.equals(MODID + ".key.skillset2"))
            {
                if (!skillset2Locked) Network.WRAPPER.sendToServer(new Network.ActionPacket(3));
                skillset2Locked = false;
            }
        }
    }
}
