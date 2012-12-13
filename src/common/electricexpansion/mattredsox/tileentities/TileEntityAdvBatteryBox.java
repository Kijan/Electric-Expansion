package electricexpansion.mattredsox.tileentities;

import java.util.EnumSet;

import net.minecraft.src.Direction;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet250CustomPayload;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;
import universalelectricity.core.UniversalElectricity;
import universalelectricity.core.electricity.ElectricInfo;
import universalelectricity.core.electricity.ElectricityConnections;
import universalelectricity.core.implement.IConductor;
import universalelectricity.core.implement.IItemElectric;
import universalelectricity.core.implement.IJouleStorage;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.implement.IRedstoneProvider;
import universalelectricity.prefab.modifier.IModifier;
import universalelectricity.prefab.network.IPacketReceiver;
import universalelectricity.prefab.network.PacketManager;
import universalelectricity.prefab.tile.TileEntityElectricityReceiver;
import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerFramework;
import buildcraft.api.power.PowerProvider;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import electricexpansion.mattredsox.blocks.BlockAdvBatteryBox;

public class TileEntityAdvBatteryBox extends TileEntityElectricityReceiver implements IPowerReceptor, IJouleStorage, IPacketReceiver, IRedstoneProvider, IInventory, ISidedInventory, IPeripheral
{
	private double joules = 0;

	public double maxJoules;
	
	private ItemStack[] containingItems = new ItemStack[5];

	private boolean isFull = false;

	private int playersUsing = 0;

	public IPowerProvider powerProvider;

	public TileEntityAdvBatteryBox()
	{
		super();
		this.setPowerProvider(null);
	}


	@Override
	public void initiate()
	{
		ElectricityConnections.registerConnector(this, EnumSet.of(ForgeDirection.getOrientation(this.getBlockMetadata() - BlockAdvBatteryBox.BATTERY_BOX_METADATA + 2), ForgeDirection.getOrientation(this.getBlockMetadata() - BlockAdvBatteryBox.BATTERY_BOX_METADATA + 2).getOpposite()));
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();

		if (!this.isDisabled())
		{
			if (!this.worldObj.isRemote)
			{
				ForgeDirection inputDirection = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockAdvBatteryBox.BATTERY_BOX_METADATA + 2).getOpposite();
				TileEntity inputTile = Vector3.getTileEntityFromSide(this.worldObj, Vector3.get(this), inputDirection);

				if (inputTile != null)
				{
					if (inputTile instanceof IConductor)
					{
						if (this.joules >= this.getMaxJoules())
						{
							((IConductor) inputTile).getNetwork().stopRequesting(this);
						}
						else
						{
							((IConductor) inputTile).getNetwork().startRequesting(this, this.getMaxJoules() - this.getJoules(), this.getVoltage());
							this.setJoules(this.joules + ((IConductor) inputTile).getNetwork().consumeElectricity(this).getWatts());
						}
					}
				}
			}

			/**
			 * Receive Electricity
			 */
			if (this.powerProvider != null)
			{
				double receivedElectricity = this.powerProvider.useEnergy(50, 50, true) * UniversalElectricity.BC3_RATIO;
				this.setJoules(this.joules + receivedElectricity);
			}

			/*
			 * The top slot is for recharging items. Check if the item is a electric item. If so,
			 * recharge it.
			 */
			if (this.containingItems[0] != null && this.joules > 0)
			{
				if (this.containingItems[0].getItem() instanceof IItemElectric)
				{
					IItemElectric electricItem = (IItemElectric) this.containingItems[0].getItem();
					double ampsToGive = Math.min(ElectricInfo.getAmps(electricItem.getMaxJoules(this.containingItems[0]) * 0.005, this.getVoltage()), this.joules);
					double joules = electricItem.onReceive(ampsToGive, this.getVoltage(), this.containingItems[0]);
					this.setJoules(this.joules - (ElectricInfo.getJoules(ampsToGive, this.getVoltage(), 1) - joules));
				}

			}

			// Power redstone if the battery box is full
			boolean isFullThisCheck = false;

			if (this.joules >= this.getMaxJoules())
			{
				isFullThisCheck = true;
			}

			if (this.isFull != isFullThisCheck)
			{
				this.isFull = isFullThisCheck;
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID);
			}

			/**
			 * Output Electricity
			 */

			/**
			 * The bottom slot is for decharging. Check if the item is a electric item. If so,
			 * decharge it.
			 */
			if (this.containingItems[1] != null && this.joules < this.getMaxJoules())
			{
				if (this.containingItems[1].getItem() instanceof IItemElectric)
				{
					IItemElectric electricItem = (IItemElectric) this.containingItems[1].getItem();

					if (electricItem.canProduceElectricity())
					{
						double joulesReceived = electricItem.onUse(electricItem.getMaxJoules(this.containingItems[1]) * 0.005, this.containingItems[1]);
						this.setJoules(this.joules + joulesReceived);
					}
				}
				
			}

			if (this.joules > 0)
			{
				ForgeDirection outputDirection = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockAdvBatteryBox.BATTERY_BOX_METADATA + 2);
				TileEntity tileEntity = Vector3.getTileEntityFromSide(this.worldObj, Vector3.get(this), outputDirection);

				if (tileEntity != null)
				{
					TileEntity connector = Vector3.getConnectorFromSide(this.worldObj, Vector3.get(this), outputDirection);

					// Output UE electricity
					if (connector instanceof IConductor)
					{
						double joulesNeeded = ((IConductor) connector).getNetwork().getRequest().getWatts();
						double transferAmps = Math.max(Math.min(Math.min(ElectricInfo.getAmps(joulesNeeded, this.getVoltage()), ElectricInfo.getAmps(this.joules, this.getVoltage())), 80), 0);

						if (!this.worldObj.isRemote && transferAmps > 0)
						{
							((IConductor) connector).getNetwork().startProducing(this, transferAmps, this.getVoltage());
							this.setJoules(this.joules - ElectricInfo.getWatts(transferAmps, this.getVoltage()));
						}
						else
						{
							((IConductor) connector).getNetwork().stopProducing(this);
						}

					}
					else
					{
						// Output IC2/Buildcraft energy
						
						 if (this.isPoweredTile(tileEntity))
						{
							IPowerReceptor receptor = (IPowerReceptor) tileEntity;
							double BCjoulesNeeded = Math.min(receptor.getPowerProvider().getMinEnergyReceived(), receptor.getPowerProvider().getMaxEnergyReceived()) * UniversalElectricity.BC3_RATIO;
							float transferJoules = (float) Math.max(Math.min(Math.min(BCjoulesNeeded, this.joules), 80000), 0);
							receptor.getPowerProvider().receiveEnergy((float) (transferJoules * UniversalElectricity.TO_BC_RATIO), ForgeDirection.getOrientation(this.getBlockMetadata() - BlockAdvBatteryBox.BATTERY_BOX_METADATA + 2).getOpposite());
							this.setJoules(this.joules - transferJoules);
						}
					}
				}
			}
		}

		// Energy Loss
		this.setJoules(this.joules - 0.00005);

		if (!this.worldObj.isRemote)
		{
			if (this.ticks % 3 == 0 && this.playersUsing > 0)
			{
				PacketManager.sendPacketToClients(getDescriptionPacket(), this.worldObj, Vector3.get(this), 12);
			}
		}
	}

	@Override
	public Packet getDescriptionPacket()
	{
		return PacketManager.getPacket("ElecEx", this, this.joules, this.disabledTicks);
	}

	@Override
	public void handlePacketData(INetworkManager network, int type, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		try
		{
			this.joules = dataStream.readDouble();
			this.disabledTicks = dataStream.readInt();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void openChest()
	{
		this.playersUsing++;
	}

	@Override
	public void closeChest()
	{
		this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		this.playersUsing--;
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readFromNBT(par1NBTTagCompound);
		this.joules = par1NBTTagCompound.getDouble("electricityStored");

		NBTTagList var2 = par1NBTTagCompound.getTagList("Items");
		this.containingItems = new ItemStack[this.getSizeInventory()];

		for (int var3 = 0; var3 < var2.tagCount(); ++var3)
		{
			NBTTagCompound var4 = (NBTTagCompound) var2.tagAt(var3);
			byte var5 = var4.getByte("Slot");

			if (var5 >= 0 && var5 < this.containingItems.length)
			{
				this.containingItems[var5] = ItemStack.loadItemStackFromNBT(var4);
			}
		}
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeToNBT(par1NBTTagCompound);
		par1NBTTagCompound.setDouble("electricityStored", this.joules);
		NBTTagList var2 = new NBTTagList();

		for (int var3 = 0; var3 < this.containingItems.length; ++var3)
		{
			if (this.containingItems[var3] != null)
			{
				NBTTagCompound var4 = new NBTTagCompound();
				var4.setByte("Slot", (byte) var3);
				this.containingItems[var3].writeToNBT(var4);
				var2.appendTag(var4);
			}
		}

		par1NBTTagCompound.setTag("Items", var2);
	}

	@Override
	public int getStartInventorySide(ForgeDirection side)
	{
		if (side == side.DOWN) { return 1; }

		if (side == side.UP) { return 1; }

		return 0;
	}

	@Override
	public int getSizeInventorySide(ForgeDirection side)
	{
		return 1;
	}

	@Override
	public int getSizeInventory()
	{
		return this.containingItems.length;
	}

	@Override
	public ItemStack getStackInSlot(int par1)
	{
		return this.containingItems[par1];
	}

	@Override
	public ItemStack decrStackSize(int par1, int par2)
	{
		if (this.containingItems[par1] != null)
		{
			ItemStack var3;

			if (this.containingItems[par1].stackSize <= par2)
			{
				var3 = this.containingItems[par1];
				this.containingItems[par1] = null;
				return var3;
			}
			else
			{
				var3 = this.containingItems[par1].splitStack(par2);

				if (this.containingItems[par1].stackSize == 0)
				{
					this.containingItems[par1] = null;
				}

				return var3;
			}
		}
		else
		{
			return null;
		}
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int par1)
	{
		if (this.containingItems[par1] != null)
		{
			ItemStack var2 = this.containingItems[par1];
			this.containingItems[par1] = null;
			return var2;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
	{
		this.containingItems[par1] = par2ItemStack;

		if (par2ItemStack != null && par2ItemStack.stackSize > this.getInventoryStackLimit())
		{
			par2ItemStack.stackSize = this.getInventoryStackLimit();
		}
	}

	@Override
	public String getInvName()
	{
		return "   Advanced Battery Box";	
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 1;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer par1EntityPlayer)
	{
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) != this ? false : par1EntityPlayer.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;
	}

	@Override
	public boolean isPoweringTo(ForgeDirection side)
	{
		return this.isFull;
	}

	@Override
	public boolean isIndirectlyPoweringTo(ForgeDirection side)
	{
		return isPoweringTo(side);
	}

	@Override
	public double getJoules(Object... data)
	{
		return this.joules;
	}

	@Override
	public void setJoules(double joules, Object... data)
	{
		this.joules = Math.max(Math.min(joules, this.getMaxJoules()), 0);
	}

	@Override
	public double getMaxJoules(Object... data) 
	{	
		int slot1 = 0, slot2 = 0, slot3 = 0;
		
		if(this.containingItems[2] != null && this.containingItems[2].getItem() instanceof IModifier && ((IModifier)this.containingItems[2].getItem()).getName(this.containingItems[2]) == "Capacity")
			slot1 = ((IModifier)this.containingItems[2].getItem()).getEffectiveness(this.containingItems[2]);
		if(this.containingItems[3] != null && this.containingItems[3].getItem() instanceof IModifier && ((IModifier)this.containingItems[3].getItem()).getName(this.containingItems[3]) == "Capacity")
			slot2 = ((IModifier)this.containingItems[3].getItem()).getEffectiveness(this.containingItems[3]);
		if(this.containingItems[4] != null && this.containingItems[4].getItem() instanceof IModifier && ((IModifier)this.containingItems[4].getItem()).getName(this.containingItems[4]) == "Capacity")
			slot3 = ((IModifier)this.containingItems[4].getItem()).getEffectiveness(this.containingItems[4]);
	
		return this.maxJoules = 3000000 + slot1 + slot2 + slot3;
	}

	/**
	 * BUILDCRAFT FUNCTIONS
	 */

	/**
	 * Is this tile entity a BC tile?
	 */
	public boolean isPoweredTile(TileEntity tileEntity)
	{
		if (tileEntity instanceof IPowerReceptor)
		{
			IPowerReceptor receptor = (IPowerReceptor) tileEntity;
			IPowerProvider provider = receptor.getPowerProvider();
			return provider != null && provider.getClass().getSuperclass().equals(PowerProvider.class);
		}

		return false;
	}

	@Override
	public void setPowerProvider(IPowerProvider provider)
	{
		if (provider == null)
		{
			if (PowerFramework.currentFramework != null)
			{
				powerProvider = PowerFramework.currentFramework.createPowerProvider();
				powerProvider.configure(20, 25, 25, 25, (int) (this.getMaxJoules() * UniversalElectricity.BC3_RATIO));
			}
		}
		else
		{
			this.powerProvider = provider;
		}
	}

	@Override
	public IPowerProvider getPowerProvider()
	{
		return this.powerProvider;
	}

	@Override
	public void doWork()
	{
	}

	@Override
	public int powerRequest()
	{
		return (int) Math.ceil((this.getMaxJoules() - this.joules) * UniversalElectricity.BC3_RATIO);
	}

	/**
	 * COMPUTERCRAFT FUNCTIONS
	 */

	@Override
	public String getType()
	{
		return "BatteryBox";
	}

	@Override
	public String[] getMethodNames()
	{
		return new String[]
		{ "getVoltage", "getWattage", "isFull" };
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws Exception
	{

		final int getVoltage = 0;
		final int getWattage = 1;
		final int isFull = 2;

		switch (method)
		{
			case getVoltage:
				return new Object[]
				{ getVoltage() };
			case getWattage:
				return new Object[]
				{ ElectricInfo.getWatts(joules) };
			case isFull:
				return new Object[]
				{ isFull };
			default:
				throw new Exception("Function unimplemented");
		}
	}

	@Override
	public boolean canAttachToSide(int side)
	{
		return true;
	}

	@Override
	public void attach(IComputerAccess computer, String computerSide)
	{
	}

	@Override
	public void detach(IComputerAccess computer)
	{
	}

}
