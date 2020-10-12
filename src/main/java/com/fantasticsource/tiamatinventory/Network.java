package com.fantasticsource.tiamatinventory;

import com.fantasticsource.mctools.component.CItemStack;
import com.fantasticsource.tiamatinventory.inventory.InterfaceTiamatInventory;
import com.fantasticsource.tiamatinventory.inventory.TiamatInventoryContainer;
import com.fantasticsource.tiamatinventory.inventory.TiamatPlayerInventory;
import com.fantasticsource.tiamatinventory.inventory.inventoryhacks.InventoryHacks;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

public class Network
{
    public static final SimpleNetworkWrapper WRAPPER = new SimpleNetworkWrapper(TiamatInventory.MODID);
    private static int discriminator = 0;

    public static void init()
    {
        WRAPPER.registerMessage(OpenTiamatInventoryPacketHandler.class, OpenTiamatInventoryPacket.class, discriminator++, Side.SERVER);
        WRAPPER.registerMessage(InventorySizePacketHandler.class, InventorySizePacket.class, discriminator++, Side.CLIENT);
        WRAPPER.registerMessage(PickupSoundPacketHandler.class, PickupSoundPacket.class, discriminator++, Side.CLIENT);
        WRAPPER.registerMessage(TiamatItemSyncPacketHandler.class, TiamatItemSyncPacket.class, discriminator++, Side.CLIENT);
    }


    public static class PickupSoundPacket implements IMessage
    {
        double x, y, z;

        public PickupSoundPacket()
        {
            //Required
        }

        public PickupSoundPacket(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            x = buf.readDouble();
            y = buf.readDouble();
            z = buf.readDouble();
        }
    }

    public static class PickupSoundPacketHandler implements IMessageHandler<PickupSoundPacket, IMessage>
    {
        @Override
        public IMessage onMessage(PickupSoundPacket packet, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().world.playSound(packet.x, packet.y, packet.z, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, (float) ((Math.random() - Math.random()) * 1.4 + 2), false));
            }

            return null;
        }
    }


    public static class InventorySizePacket implements IMessage
    {
        public int currentInventorySize;

        public InventorySizePacket()
        {
            //Required
        }

        public InventorySizePacket(int currentInventorySize)
        {
            this.currentInventorySize = currentInventorySize;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(currentInventorySize);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            currentInventorySize = buf.readInt();
        }
    }

    public static class InventorySizePacketHandler implements IMessageHandler<InventorySizePacket, IMessage>
    {
        @Override
        public IMessage onMessage(InventorySizePacket packet, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                Minecraft.getMinecraft().addScheduledTask(() ->
                {
                    InventoryHacks.clientInventorySize = packet.currentInventorySize;
                });
            }

            return null;
        }
    }


    public static class OpenTiamatInventoryPacket implements IMessage
    {
        @Override
        public void toBytes(ByteBuf buf)
        {
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
        }
    }

    public static class OpenTiamatInventoryPacketHandler implements IMessageHandler<OpenTiamatInventoryPacket, IMessage>
    {
        @Override
        public IMessage onMessage(OpenTiamatInventoryPacket packet, MessageContext ctx)
        {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            server.addScheduledTask(() ->
            {
                InterfaceTiamatInventory iface = new InterfaceTiamatInventory();

                EntityPlayerMP player = ctx.getServerHandler().player;
                player.getNextWindowId();
                player.connection.sendPacket(new SPacketOpenWindow(player.currentWindowId, iface.getGuiID(), iface.getDisplayName()));

                player.openContainer = iface.createContainer(player.inventory, player);
                player.openContainer.windowId = player.currentWindowId;
                player.openContainer.addListener(player);
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, new TiamatInventoryContainer(player)));
            });
            return null;
        }
    }


    public static class TiamatItemSyncPacket implements IMessage
    {
        public HashMap<Integer, ItemStack> newTiamatItems;

        public TiamatItemSyncPacket()
        {
            //Required
        }

        public TiamatItemSyncPacket(HashMap<Integer, ItemStack> newTiamatItems)
        {
            this.newTiamatItems = newTiamatItems;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(newTiamatItems.size());
            for (Map.Entry<Integer, ItemStack> entry : newTiamatItems.entrySet())
            {
                buf.writeInt(entry.getKey());
                new CItemStack().set(entry.getValue()).write(buf);
            }
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            CItemStack cstack = new CItemStack();
            newTiamatItems = new HashMap<>();
            for (int i = buf.readInt(); i > 0; i--)
            {
                newTiamatItems.put(buf.readInt(), cstack.read(buf).value);
            }
        }
    }

    public static class TiamatItemSyncPacketHandler implements IMessageHandler<TiamatItemSyncPacket, IMessage>
    {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(TiamatItemSyncPacket packet, MessageContext ctx)
        {
            if (ctx.side == Side.CLIENT)
            {
                Minecraft mc = Minecraft.getMinecraft();
                mc.addScheduledTask(() ->
                {
                    TiamatPlayerInventory inventory = TiamatPlayerInventory.tiamatClientInventory;
                    if (inventory == null)
                    {
                        inventory = new TiamatPlayerInventory(mc.player);
                        TiamatPlayerInventory.tiamatClientInventory = inventory;
                    }

                    for (Map.Entry<Integer, ItemStack> entry : packet.newTiamatItems.entrySet())
                    {
                        inventory.setInventorySlotContents(entry.getKey(), entry.getValue());
                    }
                });
            }

            return null;
        }
    }
}