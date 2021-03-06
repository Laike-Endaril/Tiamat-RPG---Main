package moe.plushie.rpg_framework.api.loot;

import moe.plushie.rpg_framework.api.core.IIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.ArrayList;
import java.util.Random;

public interface ILootTablePool {

    public IIdentifier getIdentifier();

    public String getName();

    public String getCategory();

    public ArrayList<ILootTableItem> getPoolItems();

    public void getLoot(NonNullList<ItemStack> items, Random random);
}
