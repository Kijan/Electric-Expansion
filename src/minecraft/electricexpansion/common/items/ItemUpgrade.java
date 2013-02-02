package electricexpansion.common.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import universalelectricity.prefab.modifier.IModifier;
import electricexpansion.common.ElectricExpansion;
import electricexpansion.common.misc.EETab;

public class ItemUpgrade extends Item implements IModifier
{
	private String[] names = new String[] { "Storage1", "Storage2", "Storage3", "Storage4", "HalfVoltage", "HVUpgrade", "HVInputUpgrade" };

	public ItemUpgrade(int id, int texture)
	{
		super(id);
		this.setMaxDamage(0);
		this.setMaxStackSize(16);
		this.setHasSubtypes(true);
		this.setCreativeTab(EETab.INSTANCE);
		this.setIconIndex(32);
	}

	@Override
	public int getMetadata(int damage)
	{
		return damage;
	}

	@Override
	public String getItemNameIS(ItemStack itemstack)
	{
		return getItemName() + "." + names[itemstack.getItemDamage()];
	}

	@Override
	public int getIconFromDamage(int i)
	{
		if (i == 0) { return this.iconIndex + 0; }
		if (i == 1) { return this.iconIndex + 1; }
		if (i == 2) { return this.iconIndex + 2; }
		if (i == 3) { return this.iconIndex + 3; }
		if (i == 4) { return this.iconIndex + 4; }
		if (i == 5) { return this.iconIndex + 5; }
		if (i == 6) { return this.iconIndex + 6; }
		return 6;

	}

	@Override
	public String getTextureFile()
	{
		return ElectricExpansion.ITEM_FILE;
	}

	@Override
	public void getSubItems(int par1, CreativeTabs par2CreativeTabs, List par3List)
	{
		for (int i = 0; i < names.length; i++)
		{
			par3List.add(new ItemStack(this, 1, i));
		}
	}

	@Override
	public String getName(ItemStack itemstack)
	{
		if (itemstack.getItemDamage() == 0) { return "Capacity"; }
		if (itemstack.getItemDamage() == 1) { return "Capacity"; }
		if (itemstack.getItemDamage() == 2) { return "Capacity"; }
		if (itemstack.getItemDamage() == 3) { return "Capacity"; }
		if (itemstack.getItemDamage() == 4) { return "VoltageModifier"; }
		if (itemstack.getItemDamage() == 5) { return "VoltageModifier"; }
		if (itemstack.getItemDamage() == 6) { return "InputVoltageModifier"; }

		return null;
	}

	@Override
	public int getEffectiveness(ItemStack itemstack)
	{
		if (itemstack.getItemDamage() == 0) { return 1000000; }
		if (itemstack.getItemDamage() == 1) { return 2000000; }
		if (itemstack.getItemDamage() == 2) { return 3000000; }
		if (itemstack.getItemDamage() == 3) { return 5000000; } // Tier 4 storage upgrade(
																// "...unbeatable end game..." )
		if (itemstack.getItemDamage() == 4) { return -2; }
		if (itemstack.getItemDamage() == 5) { return 20; }
		if (itemstack.getItemDamage() == 6) { return 20; }

		return 0;
	}

	/**
	 * Allows items to add custom lines of information to the mouseover description. If you want to
	 * add more information to your item, you can super.addInformation() to keep the electiricty
	 * info in the item info bar.
	 */
	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List par3List, boolean par4)
	{
		switch (itemstack.getItemDamage())
		{
			case 0:
				par3List.add("\u00a72Increases Capacity by 1 Mj");
				break;
			case 1:
				par3List.add("\u00a72Increases Capacity by 2 Mj");
				break;
			case 2:
				par3List.add("\u00a72Increases Capacity by 3 Mj");
				break;
			case 3:
				par3List.add("\u00a72Increases Capacity by 5 Mj");
				break;
			case 4:
				par3List.add("\u00a72Decreases Voltage by Half");
				break;
			case 5:
				par3List.add("\u00a72Multiplies Voltage by 20");
				break;
			case 6:
				par3List.add("\u00a72Multiplies Input Voltage Acceptance by 20");
				break;
		}
	}
}
