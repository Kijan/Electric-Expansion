package electricexpansion.common.helpers;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.electricity.ElectricityNetwork;
import universalelectricity.prefab.network.PacketManager;
import universalelectricity.prefab.tile.TileEntityConductor;

import com.google.common.io.ByteArrayDataInput;

import electricexpansion.api.CableInterfaces.ISelectiveConnector;
import electricexpansion.common.ElectricExpansion;

/**
 * 
 * @author Alex_hawks Helper Class used by me to make adding methods to all cables easily...
 */

public abstract class TileEntityCableHelper extends TileEntityConductor implements ISelectiveConnector
{
	private ElectricityNetwork network;

	public TileEntityCableHelper()
	{
		this.reset();
		this.channel = ElectricExpansion.CHANNEL;
	}

	@Override
	public double getResistance()
	{
		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);

		switch (meta)
		{
			case 0:
				return 0.05;
			case 1:
				return 0.04;
			case 2:
				return 0.02;
			case 3:
				return 0.2;
			case 4:
				return 0.005;
			default:
				return 0.05;
		}
	}

	@Override
	public double getMaxAmps()
	{
		int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		switch (meta)
		{
			case 0:
				return 500;
			case 1:
				return 60; // Bit less than a basic Coal-Generator. #Cruel
			case 2:
				return 200;
			case 3:
				return 2500; // HV
			case 4:
				return 1000;
			default:
				return 500;
		}
	}

	@Override
	public void handlePacketData(INetworkManager network, int type, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		if (this.worldObj.isRemote)
		{
			this.refreshConnectedBlocks();
		}
	}

	public boolean canConnect(ForgeDirection side)
	{
		/*
		 * boolean returnValue = false; int x = side.offsetX; int y = side.offsetY; int z =
		 * side.offsetZ;
		 * 
		 * int thisID = this.worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord); int
		 * thismeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
		 * TileEntity thisTE = this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord,
		 * this.zCoord); int ID = this.worldObj.getBlockId(this.xCoord + x, this.yCoord + y,
		 * this.zCoord + z); int meta = this.worldObj.getBlockMetadata(this.xCoord + x, this.yCoord
		 * + y, this.zCoord + z); TileEntity TE = this.worldObj.getBlockTileEntity(this.xCoord + x,
		 * this.yCoord + y, this.zCoord + z);
		 * 
		 * if(TE instanceof ISelectiveConnector) { if(this.cableType(thisID, thismeta) ==
		 * ((ISelectiveConnector)TE).cableType(ID, meta)) returnValue = true; else
		 * if(this.cableType(ID, meta) == "Connector") returnValue = true; else
		 * if(((ISelectiveConnector)TE).cableType(ID, meta) == "Connector") returnValue = true; }
		 * else if(Loader.isModLoaded("BasicComponents")) { if(TE instanceof
		 * basiccomponents.tile.TileEntityCopperWire && this.cableType(thisID, thismeta) ==
		 * "Copper") returnValue = true; } else if((TE instanceof IConnector) && !(TE instanceof
		 * IConductor)) returnValue = true; return returnValue;
		 */return true;
	}

	public String cableType(int ID, int meta)
	{
		String type = "Unknown";
		switch (meta)
		{
			case 0:
				type = "Copper";
				break;
			case 1:
				type = "Tin";
				break;
			case 2:
				type = "Silver";
				break;
			case 3:
				type = "Aluminium";
				break;
			case 4:
				type = "Endium";
				break;
			case 5:
				type = "Connector";
				break;
		}
		return type;
	}

	@Override
	public void onOverCharge()
	{
		if (!this.worldObj.isRemote)
		{
			int ID = this.getBlockType().blockID;
			int setToID = 0;
			if (ID == ElectricExpansion.rawWire)
				setToID = 0;
			if (ID == ElectricExpansion.insulatedWire)
				setToID = 0;
			if (ID == ElectricExpansion.wireBlock)
				setToID = Block.stone.blockID;
			if (ID == ElectricExpansion.SwitchWire)
				setToID = 0;
			if (ID == ElectricExpansion.SwitchWireBlock)
				setToID = Block.stone.blockID;

			this.worldObj.setBlockWithNotify(this.xCoord, this.yCoord, this.zCoord, setToID);
		}
	}
}
