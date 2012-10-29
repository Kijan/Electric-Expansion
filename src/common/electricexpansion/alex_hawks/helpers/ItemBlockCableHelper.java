package electricexpansion.alex_hawks.helpers;

import net.minecraft.src.Block;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;

public class ItemBlockCableHelper extends ItemBlock 
{
	public ItemBlockCableHelper(int par1, Block mainBlock) 
	{
		super(par1);
		this.setHasSubtypes(true);
	}
	public int getMetadata(int par1)
    {
        return par1;
    }
	public String getItemNameIS(ItemStack i) 
	{
		String name = null;
		int j = i.getItemDamage();
		switch(j)
		{
			case 0: 	name = "Copper";
						break;
			case 1:		name = "Tin"; 
						break;
			case 2:		name = "Silver";
						break;
			case 3:		name = "HV";
						break;
			case 4:		name = "Endium";
						break;
			case 5:		name = "Connector";
						break;
			default:	name = "Unknown";
						break;
		}
		return i.getItem().getItemName() + "." + name;
	}
}