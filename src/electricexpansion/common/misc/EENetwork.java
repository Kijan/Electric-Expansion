package electricexpansion.common.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import universalelectricity.core.block.IConductor;
import universalelectricity.core.block.IConnectionProvider;
import universalelectricity.core.block.INetworkProvider;
import universalelectricity.core.electricity.ElectricityPack;
import universalelectricity.core.electricity.IElectricityNetwork;
import universalelectricity.core.path.Pathfinder;
import universalelectricity.core.path.PathfinderChecker;
import universalelectricity.core.vector.Vector3;
import cpw.mods.fml.common.FMLLog;
import electricexpansion.api.IRedstoneNetAccessor;
import electricexpansion.common.tile.TileEntityRedstoneNetworkCore;
/**
 *  Originally created by Calclavia as {@link universalelectricity.core.electricity.ElectricityNetwork} and modified
 *  by me (Alex_hawks) to be better suited to the Redstone Signal handling that I required...
 * 
 * @author Calclavia
 * @author Alex_hawks
 */
public class EENetwork implements IElectricityNetwork
{
    private final HashMap<TileEntity, ElectricityPack> producers = new HashMap<TileEntity, ElectricityPack>();
    private final HashMap<TileEntity, ElectricityPack> consumers = new HashMap<TileEntity, ElectricityPack>();

    private final Set<IConductor> conductors = new HashSet<IConductor>();
    private final List<IRedstoneNetAccessor> redstoneInterfacers = new ArrayList<IRedstoneNetAccessor>();
    
    public byte rsLevel = 0;
    
    public TileEntityRedstoneNetworkCore coreProcessor;
    
    public EENetwork(IConductor... conductors)
    {
        this.conductors.addAll(Arrays.asList(conductors));
        
        this.cleanUpConductors();
    }
    
    public EENetwork(IElectricityNetwork oldNetwork) 
    {
        this.conductors.addAll(oldNetwork.getConductors());
        this.consumers.putAll(oldNetwork.getConsumers());
        this.producers.putAll(oldNetwork.getProducers());
        
        this.cleanUpConductors();
    }
    
    @Override
    public void cleanUpConductors()
    {
        Iterator<IConductor> it = this.conductors.iterator();
        this.redstoneInterfacers.clear();

        while (it.hasNext())
        {
            IConductor conductor = (IConductor) it.next();

            if (conductor == null)
            {
                it.remove();
            }
            else if (((TileEntity) conductor).isInvalid())
            {
                it.remove();
            }
            else if (conductor instanceof IRedstoneNetAccessor)
            {
                conductor.setNetwork(this);
                this.redstoneInterfacers.add((IRedstoneNetAccessor) conductor);
            }
            else
            {
                conductor.setNetwork(this);
            }
        }
    }
    
    @Override
    public void mergeConnection(IElectricityNetwork network)
    {
        if (network != null && network != this && network instanceof EENetwork)
        {
            EENetwork newNetwork = new EENetwork();
            newNetwork.getConductors().addAll(this.getConductors());
            newNetwork.getConductors().addAll(network.getConductors());
            newNetwork.cleanUpConductors();
            
            if (this.coreProcessor != null && ((EENetwork) network).coreProcessor == null)
                newNetwork.coreProcessor = this.coreProcessor;
            else if (this.coreProcessor == null && ((EENetwork) network).coreProcessor != null)
                newNetwork.coreProcessor = ((EENetwork) network).coreProcessor;
        }
    }

    @Override
    public void splitNetwork(IConnectionProvider splitPoint)
    {
        if (splitPoint instanceof TileEntity)
        {
            this.getConductors().remove(splitPoint);

            /**
             * Loop through the connected blocks and attempt to see if there are connections between
             * the two points elsewhere.
             */
            TileEntity[] connectedBlocks = splitPoint.getAdjacentConnections();

            for (int i = 0; i < connectedBlocks.length; i++)
            {
                TileEntity connectedBlockA = connectedBlocks[i];

                if (connectedBlockA instanceof IConnectionProvider)
                {
                    for (int ii = 0; ii < connectedBlocks.length; ii++)
                    {
                        final TileEntity connectedBlockB = connectedBlocks[ii];

                        if (connectedBlockA != connectedBlockB && connectedBlockB instanceof IConnectionProvider)
                        {
                            Pathfinder finder = new PathfinderChecker(((TileEntity) splitPoint).worldObj, (IConnectionProvider) connectedBlockB, splitPoint);
                            finder.init(new Vector3(connectedBlockA));

                            if (finder.results.size() > 0)
                            {
                                /**
                                 * The connections A and B are still intact elsewhere. Set all
                                 * references of wire connection into one network.
                                 */

                                for (Vector3 node : finder.closedSet)
                                {
                                    TileEntity nodeTile = node.getTileEntity(((TileEntity) splitPoint).worldObj);

                                    if (nodeTile instanceof INetworkProvider)
                                    {
                                        if (nodeTile != splitPoint)
                                        {
                                            ((INetworkProvider) nodeTile).setNetwork(this);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                /**
                                 * The connections A and B are not connected anymore. Give both of
                                 * them a new network.
                                 */
                                IElectricityNetwork newNetwork = new EENetwork();

                                for (Vector3 node : finder.closedSet)
                                {
                                    TileEntity nodeTile = node.getTileEntity(((TileEntity) splitPoint).worldObj);

                                    if (nodeTile instanceof INetworkProvider)
                                    {
                                        if (nodeTile != splitPoint)
                                        {
                                            newNetwork.getConductors().add((IConductor) nodeTile);
                                        }
                                    }
                                }

                                newNetwork.cleanUpConductors();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return "RedstoneNetwork[" + this.hashCode() + "|Wires:" + this.getConductors().size() + "]";
    }

    @Override
    public void startProducing(TileEntity tileEntity, ElectricityPack electricityPack)
    {
        if (tileEntity != null && electricityPack.getWatts() > 0)
        {
            this.producers.put(tileEntity, electricityPack);
        }
    }

    @Override
    public void startProducing(TileEntity tileEntity, double amperes, double voltage)
    {
        this.startProducing(tileEntity, new ElectricityPack(amperes, voltage));
    }

    @Override
    public boolean isProducing(TileEntity tileEntity)
    {
        return this.producers.containsKey(tileEntity);
    }

    /**
     * Sets this tile entity to stop producing energy in this network.
     */
    @Override
    public void stopProducing(TileEntity tileEntity)
    {
        this.producers.remove(tileEntity);
    }

    /**
     * Sets this tile entity to start producing energy in this network.
     */
    @Override
    public void startRequesting(TileEntity tileEntity, ElectricityPack electricityPack)
    {
        if (tileEntity != null && electricityPack.getWatts() > 0)
        {
            this.consumers.put(tileEntity, electricityPack);
        }
    }

    @Override
    public void startRequesting(TileEntity tileEntity, double amperes, double voltage)
    {
        this.startRequesting(tileEntity, new ElectricityPack(amperes, voltage));
    }

    @Override
    public boolean isRequesting(TileEntity tileEntity)
    {
        return this.consumers.containsKey(tileEntity);
    }

    /**
     * Sets this tile entity to stop producing energy in this network.
     */
    @Override
    public void stopRequesting(TileEntity tileEntity)
    {
        this.consumers.remove(tileEntity);
    }

    /**
     * @param ignoreTiles The TileEntities to ignore during this calculation. Null will make it not
     * ignore any.
     * @return The electricity produced in this electricity network
     */
    @Override
    @SuppressWarnings("rawtypes")
    public ElectricityPack getProduced(TileEntity... ignoreTiles)
    {
        ElectricityPack totalElectricity = new ElectricityPack(0, 0);

        Iterator it = this.producers.entrySet().iterator();

        loop:
        while (it.hasNext())
        {
            Map.Entry pairs = (Map.Entry) it.next();

            if (pairs != null)
            {
                TileEntity tileEntity = (TileEntity) pairs.getKey();

                if (tileEntity == null)
                {
                    it.remove();
                    continue;
                }

                if (tileEntity.isInvalid())
                {
                    it.remove();
                    continue;
                }

                if (tileEntity.worldObj.getBlockTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord) != tileEntity)
                {
                    it.remove();
                    continue;
                }

                if (ignoreTiles != null)
                {
                    for (TileEntity ignoreTile : ignoreTiles)
                    {
                        if (tileEntity == ignoreTile)
                        {
                            continue loop;
                        }
                    }
                }

                ElectricityPack pack = (ElectricityPack) pairs.getValue();

                if (pairs.getKey() != null && pairs.getValue() != null && pack != null)
                {
                    double newWatts = totalElectricity.getWatts() + pack.getWatts();
                    double newVoltage = Math.max(totalElectricity.voltage, pack.voltage);

                    totalElectricity.amperes = newWatts / newVoltage;
                    totalElectricity.voltage = newVoltage;
                }
            }
        }

        return totalElectricity;
    }

    /**
     * @return How much electricity this network needs.
     */
    @Override
    public ElectricityPack getRequest(TileEntity... ignoreTiles)
    {
        ElectricityPack totalElectricity = this.getRequestWithoutReduction();
        totalElectricity.amperes = Math.max(totalElectricity.amperes - this.getProduced(ignoreTiles).amperes, 0);
        return totalElectricity;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ElectricityPack getRequestWithoutReduction()
    {
        ElectricityPack totalElectricity = new ElectricityPack(0, 0);

        Iterator it = this.consumers.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry pairs = (Map.Entry) it.next();

            if (pairs != null)
            {
                TileEntity tileEntity = (TileEntity) pairs.getKey();

                if (tileEntity == null)
                {
                    it.remove();
                    continue;
                }

                if (tileEntity.isInvalid())
                {
                    it.remove();
                    continue;
                }

                if (tileEntity.worldObj.getBlockTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord) != tileEntity)
                {
                    it.remove();
                    continue;
                }

                ElectricityPack pack = (ElectricityPack) pairs.getValue();

                if (pack != null)
                {
                    totalElectricity.amperes += pack.amperes;
                    totalElectricity.voltage = Math.max(totalElectricity.voltage, pack.voltage);
                }
            }
        }

        return totalElectricity;
    }

    /**
     * @param tileEntity
     * @return The electricity being input into this tile entity.
     */
    @Override
    public ElectricityPack consumeElectricity(TileEntity tileEntity)
    {
        ElectricityPack totalElectricity = new ElectricityPack(0, 0);

        try
        {
            ElectricityPack tileRequest = this.consumers.get(tileEntity);

            if (this.consumers.containsKey(tileEntity) && tileRequest != null)
            {
                // Calculate the electricity this TileEntity is receiving in percentage.
                totalElectricity = this.getProduced();

                if (totalElectricity.getWatts() > 0)
                {
                    ElectricityPack totalRequest = this.getRequestWithoutReduction();
                    totalElectricity.amperes *= (tileRequest.amperes / totalRequest.amperes);

                    double ampsReceived = totalElectricity.amperes - (totalElectricity.amperes * totalElectricity.amperes * this.getTotalResistance()) / totalElectricity.voltage;
                    double voltsReceived = totalElectricity.voltage - (totalElectricity.amperes * this.getTotalResistance());

                    totalElectricity.amperes = ampsReceived;
                    totalElectricity.voltage = voltsReceived;

                    return totalElectricity;
                }
            }
        }
        catch (Exception e)
        {
            FMLLog.severe("Failed to consume electricity!");
            e.printStackTrace();
        }

        return totalElectricity;
    }

    /**
     * @return Returns all producers in this electricity network.
     */
    @Override
    public HashMap<TileEntity, ElectricityPack> getProducers()
    {
        return this.producers;
    }

    /**
     * Gets all the electricity providers.
     */
    @Override
    public List<TileEntity> getProviders()
    {
        List<TileEntity> providers = new ArrayList<TileEntity>();
        providers.addAll(this.producers.keySet());
        return providers;
    }

    /**
     * @return Returns all consumers in this electricity network.
     */
    @Override
    public HashMap<TileEntity, ElectricityPack> getConsumers()
    {
        return this.consumers;
    }

    /**
     * Gets all the electricity receivers.
     */
    @Override
    public List<TileEntity> getReceivers()
    {
        List<TileEntity> receivers = new ArrayList<TileEntity>();
        receivers.addAll(this.consumers.keySet());
        return receivers;
    }

    /**
     * This function is called to refresh all conductors in this network
     */
    @Override
    public void refreshConductors()
    {
        this.cleanUpConductors();

        try
        {
            Iterator<IConductor> it = this.conductors.iterator();

            while (it.hasNext())
            {
                IConductor conductor = it.next();
                conductor.updateAdjacentConnections();
            }
        }
        catch (Exception e)
        {
            FMLLog.severe("Universal Electricity: Failed to refresh conductor.");
            e.printStackTrace();
        }
    }

    @Override
    public double getTotalResistance()
    {
        double resistance = 0;

        for (IConductor conductor : this.conductors)
        {
            resistance += conductor.getResistance();
        }

        return resistance;
    }

    @Override
    public double getLowestCurrentCapacity()
    {
        double lowestAmp = 0;

        for (IConductor conductor : this.conductors)
        {
            if (lowestAmp == 0 || conductor.getCurrentCapcity() < lowestAmp)
            {
                lowestAmp = conductor.getCurrentCapcity();
            }
        }

        return lowestAmp;
    }

    @Override
    public Set<IConductor> getConductors()
    {
        return this.conductors;
    }

    public List<IRedstoneNetAccessor> getRedstoneInterfacers()
    {
        return redstoneInterfacers;
    }
}
