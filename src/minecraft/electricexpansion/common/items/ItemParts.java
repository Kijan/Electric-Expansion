package electricexpansion.common.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import universalelectricity.prefab.UETab;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import electricexpansion.common.CommonProxy;

public class ItemParts extends Item
{
	public ItemParts(int par1, int meta)
	{
		super(par1);
		this.setHasSubtypes(true);
		this.setItemName("Parts");
		this.setCreativeTab(UETab.INSTANCE);
		this.setMaxStackSize(32);
		this.setIconIndex(240);
		this.setTextureFile(CommonProxy.AITEMS);
	}

	@Override
	public int getIconFromDamage(int meta)
	{
		return this.iconIndex + meta;
	}

	@Override
	public int getMetadata(int meta)
	{
		return meta;
	}

	@Override
	public String getItemNameIS(ItemStack i)
	{
		String name = "Unknown";
		int j = i.getItemDamage();
		switch (j)
		{
			case 0:
				name = "DrawPlates";
				break;
		}
		return i.getItem().getItemName() + "." + name;
	}

	@SideOnly(Side.CLIENT)
	public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List par3List)
	{
		for (int var4 = 0; var4 < 1; ++var4)
			par3List.add(new ItemStack(par1, 1, var4));
	}
}