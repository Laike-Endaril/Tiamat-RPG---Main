package com.fantasticsource.tiamatinventory;

import com.fantasticsource.mctools.GlobalInventory;
import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.mctools.aw.RenderModes;
import com.fantasticsource.mctools.event.InventoryChangedEvent;
import com.fantasticsource.tiamatinventory.config.TiamatConfig;
import com.fantasticsource.tiamatinventory.inventory.ClientInventoryData;
import com.fantasticsource.tiamatinventory.inventory.TiamatInventoryGUI;
import com.fantasticsource.tiamatinventory.inventory.TiamatPlayerInventory;
import com.fantasticsource.tiamatinventory.inventory.inventoryhacks.ClientInventoryHacks;
import com.fantasticsource.tiamatinventory.inventory.inventoryhacks.InventoryHacks;
import com.fantasticsource.tools.Tools;
import moe.plushie.rpg_framework.api.currency.ICurrencyCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.BaseAttribute;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = TiamatInventory.MODID, name = TiamatInventory.NAME, version = TiamatInventory.VERSION, dependencies = "required-after:fantasticlib@[1.12.2.044zm,)")
public class TiamatInventory
{
    public static final String MODID = "tiamatinventory";
    public static final String NAME = "Tiamat Inventory";
    public static final String VERSION = "1.12.2.000zx";


    @CapabilityInject(ICurrencyCapability.class)
    public static Capability<ICurrencyCapability> CURRENCY_CAPABILITY = null;


    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {
        Network.init();
        MinecraftForge.EVENT_BUS.register(TiamatInventory.class);
        MinecraftForge.EVENT_BUS.register(InventoryHacks.class);
        InventoryChangedEvent.watchedClasses.add(EntityPlayerMP.class);

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            //Physical client
            AttributeDisplayData.updateDisplayList();
            Keys.init(event);
            MinecraftForge.EVENT_BUS.register(TiamatInventoryGUI.class);
            MinecraftForge.EVENT_BUS.register(ClientInventoryHacks.class);
            MinecraftForge.EVENT_BUS.register(TooltipAlterer.class);
        }
    }

    @SubscribeEvent
    public static void saveConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MODID)) ConfigManager.sync(MODID, Config.Type.INSTANCE);
    }

    @SubscribeEvent
    public static void syncConfig(ConfigChangedEvent.PostConfigChangedEvent event)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null)
        {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers())
            {
                Network.WRAPPER.sendTo(new Network.InventoryDataPacket(InventoryHacks.getCurrentInventorySize(player), TiamatConfig.serverSettings.craftW, TiamatConfig.serverSettings.craftH, TiamatConfig.serverSettings.allowHotbar), player);
            }
        }
        AttributeDisplayData.updateDisplayList();
    }


    @Mod.EventHandler
    public static void serverStarting(FMLServerStartingEvent event)
    {
        TiamatPlayerInventory.init(event);

        event.registerServerCommand(new Commands());
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        TiamatPlayerInventory.load(event);
        Network.WRAPPER.sendTo(new Network.InventoryDataPacket(InventoryHacks.getCurrentInventorySize(player), TiamatConfig.serverSettings.craftW, TiamatConfig.serverSettings.craftH, TiamatConfig.serverSettings.allowHotbar), player);
    }

    @SubscribeEvent
    public static void entityJoinWorld(EntityJoinWorldEvent event)
    {
        Entity entity = event.getEntity();


        if (!entity.world.isRemote && entity instanceof EntityLivingBase)
        {
            AbstractAttributeMap attributeMap = ((EntityLivingBase) entity).getAttributeMap();
            for (IAttributeInstance attributeInstance : attributeMap.getAllAttributes())
            {
                IAttribute attribute = attributeInstance.getAttribute();
                if (attribute instanceof BaseAttribute && Tools.contains(TiamatConfig.serverSettings.attributesToSync, attribute.getName()))
                {
                    ((BaseAttribute) attribute).setShouldWatch(true);
                }
            }
        }


        if (entity instanceof EntityPlayer)
        {
            TiamatPlayerInventory inv = entity.world.isRemote ? TiamatPlayerInventory.tiamatClientInventory : TiamatPlayerInventory.tiamatServerInventories.get(entity.getPersistentID());
            if (inv != null) inv.player = (EntityPlayer) entity;
        }

        //Render modes
        if (!entity.world.isRemote && entity instanceof EntityLivingBase && Loader.isModLoaded("armourers_workshop"))
        {
            //Headpiece Default
            if (RenderModes.getRenderMode(entity, "HeadControl") == null) RenderModes.setRenderMode(entity, "HeadControl", "On");

            //Cape Default
            if (RenderModes.getRenderMode(entity, "CapeInvControl") == null) RenderModes.setRenderMode(entity, "CapeInvControl", "On");

            //Shoulders Default
            if (RenderModes.getRenderMode(entity, "ShoulderLControl") == null) RenderModes.setRenderMode(entity, "ShoulderLControl", "On");
            if (RenderModes.getRenderMode(entity, "ShoulderRControl") == null) RenderModes.setRenderMode(entity, "ShoulderRControl", "On");


            //Headpiece
            ItemStack headpiece = GlobalInventory.getVanillaHeadItem(entity);
            if (headpiece == null || headpiece.isEmpty() || !headpiece.hasTagCompound())
            {
                RenderModes.setRenderMode(entity, "Hat", "Off");
                RenderModes.setRenderMode(entity, "Helmet", "Off");
                RenderModes.setRenderMode(entity, "Mask", "Off");
            }
            else
            {
                String nbtString = headpiece.getTagCompound().toString();
                if (nbtString.contains("@Hat"))
                {
                    RenderModes.setRenderMode(entity, "Hat", RenderModes.getRenderMode(entity, "HeadControl"));
                    RenderModes.setRenderMode(entity, "Helmet", "Off");
                    RenderModes.setRenderMode(entity, "Mask", "Off");
                }
                else if (nbtString.contains("@Helmet"))
                {
                    RenderModes.setRenderMode(entity, "Hat", "Off");
                    RenderModes.setRenderMode(entity, "Helmet", RenderModes.getRenderMode(entity, "HeadControl"));
                    RenderModes.setRenderMode(entity, "Mask", "Off");
                }
                else if (nbtString.contains("@Mask"))
                {
                    RenderModes.setRenderMode(entity, "Hat", "Off");
                    RenderModes.setRenderMode(entity, "Helmet", "Off");
                    RenderModes.setRenderMode(entity, "Mask", RenderModes.getRenderMode(entity, "HeadControl"));
                }
                else
                {
                    RenderModes.setRenderMode(entity, "Hat", "Off");
                    RenderModes.setRenderMode(entity, "Helmet", "Off");
                    RenderModes.setRenderMode(entity, "Mask", "Off");
                }
            }

            //Cape
            ItemStack cape = GlobalInventory.getTiamatCapeItem(entity);
            if (cape == null || cape.isEmpty()) RenderModes.setRenderMode(entity, "CapeInv", "Off");
            else RenderModes.setRenderMode(entity, "CapeInv", RenderModes.getRenderMode(entity, "CapeInvControl"));

            //Shoulders
            ItemStack shoulder = GlobalInventory.getTiamatShoulderItem(entity);
            if (shoulder == null || shoulder.isEmpty())
            {
                RenderModes.setRenderMode(entity, "ShoulderL", "Off");
                RenderModes.setRenderMode(entity, "ShoulderR", "Off");
            }
            else
            {
                RenderModes.setRenderMode(entity, "ShoulderL", RenderModes.getRenderMode(entity, "ShoulderLControl"));
                RenderModes.setRenderMode(entity, "ShoulderR", RenderModes.getRenderMode(entity, "ShoulderRControl"));
            }
        }
    }

    @SubscribeEvent
    public static void inventoryChanged(InventoryChangedEvent event)
    {
        Entity entity = event.getEntity();


        //Update render modes
        if (entity instanceof EntityLivingBase && Loader.isModLoaded("armourers_workshop"))
        {
            //Headpiece
            ItemStack headpiece = GlobalInventory.getVanillaHeadItem(entity);
            if (headpiece == null || headpiece.isEmpty() || !headpiece.hasTagCompound())
            {
                RenderModes.setRenderMode(entity, "Hat", "Off");
                RenderModes.setRenderMode(entity, "Helmet", "Off");
                RenderModes.setRenderMode(entity, "Mask", "Off");
            }
            else
            {
                String nbtString = headpiece.getTagCompound().toString();
                if (nbtString.contains("@Hat"))
                {
                    RenderModes.setRenderMode(entity, "Hat", RenderModes.getRenderMode(entity, "HeadControl"));
                    RenderModes.setRenderMode(entity, "Helmet", "Off");
                    RenderModes.setRenderMode(entity, "Mask", "Off");
                }
                else if (nbtString.contains("@Helmet"))
                {
                    RenderModes.setRenderMode(entity, "Hat", "Off");
                    RenderModes.setRenderMode(entity, "Helmet", RenderModes.getRenderMode(entity, "HeadControl"));
                    RenderModes.setRenderMode(entity, "Mask", "Off");
                }
                else if (nbtString.contains("@Mask"))
                {
                    RenderModes.setRenderMode(entity, "Hat", "Off");
                    RenderModes.setRenderMode(entity, "Helmet", "Off");
                    RenderModes.setRenderMode(entity, "Mask", RenderModes.getRenderMode(entity, "HeadControl"));
                }
                else
                {
                    RenderModes.setRenderMode(entity, "Hat", "Off");
                    RenderModes.setRenderMode(entity, "Helmet", "Off");
                    RenderModes.setRenderMode(entity, "Mask", "Off");
                }
            }

            //Cape
            ItemStack cape = GlobalInventory.getTiamatCapeItem(entity);
            if (cape == null || cape.isEmpty()) RenderModes.setRenderMode(entity, "CapeInv", "Off");
            else RenderModes.setRenderMode(entity, "CapeInv", RenderModes.getRenderMode(entity, "CapeInvControl"));

            //Shoulders
            ItemStack shoulder = GlobalInventory.getTiamatShoulderItem(entity);
            if (shoulder == null || shoulder.isEmpty())
            {
                RenderModes.setRenderMode(entity, "ShoulderL", "Off");
                RenderModes.setRenderMode(entity, "ShoulderR", "Off");
            }
            else
            {
                RenderModes.setRenderMode(entity, "ShoulderL", RenderModes.getRenderMode(entity, "ShoulderLControl"));
                RenderModes.setRenderMode(entity, "ShoulderR", RenderModes.getRenderMode(entity, "ShoulderRControl"));
            }
        }


        //Sync changed tiamat inventory items to client

        if (entity instanceof EntityPlayerMP)
        {
            Network.WRAPPER.sendTo(new Network.TiamatItemSyncPacket(event.newInventory.tiamatInventory), (EntityPlayerMP) entity);
        }
    }

    @SubscribeEvent
    public static void playerSave(net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile event)
    {
        TiamatPlayerInventory.save(event);
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        TiamatPlayerInventory.saveUnload(event);
    }

    @Mod.EventHandler
    public static void serverStop(FMLServerStoppedEvent event)
    {
        TiamatPlayerInventory.saveUnloadAll(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerDrops(PlayerDropsEvent event)
    {
        EntityPlayer player = event.getEntityPlayer();
        TiamatPlayerInventory inventory = TiamatPlayerInventory.tiamatServerInventories.get(player.getPersistentID());
        if (inventory != null)
        {
            player.captureDrops = true;
            inventory.dropAllItems();
            player.captureDrops = false;
        }
    }


    public static boolean playerHasHotbar(EntityPlayer player)
    {
        if (player instanceof EntityPlayerMP)
        {
            if (TiamatConfig.serverSettings.allowHotbar) return true;
        }
        else
        {
            if (ClientInventoryData.allowHotbar) return true;
        }

        GameType gameType = MCTools.getGameType(player);
        return gameType == GameType.CREATIVE || gameType == GameType.SPECTATOR;
    }


    public static int inventorySize(EntityPlayer player)
    {
        if (player.isCreative()) return 27;

        if (player instanceof EntityPlayerMP) return InventoryHacks.getCurrentInventorySize((EntityPlayerMP) player);
        else return ClientInventoryData.inventorySize;
    }
}
