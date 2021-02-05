package pfe.fog.entities;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.NetworkUsageMonitor;

public class ClusterFogDevice extends FogDevice {
	protected List<Integer> parentsIds = new ArrayList<Integer>();
	protected List<Boolean> isNorthLinkBusyById = new ArrayList<Boolean>();
	protected List<Queue<Tuple>> northTupleQueues = new ArrayList<Queue<Tuple>>();
	
	protected long availableMips;
	protected int availableRam;
	
	public ClusterFogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
			double uplinkLatency, double ratePerMips) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
		availableMips = characteristics.getMips();
		availableRam = characteristics.getHostList().get(0).getRam();
	}
	
	public void addParent(int patendId) {
		parentsIds.add(parentId);
		isNorthLinkBusyById.add(false);
		northTupleQueues.add(new LinkedList<Tuple>());
	}
	
	public List<Integer> getParentsId() {
		return parentsIds;
	}
	
	protected void sendUp(Tuple tuple, int linkId) {
		if (!isNorthLinkBusyById.get(linkId)) {
			sendUpFreeLink(tuple, linkId);
		} else {
			northTupleQueues.get(linkId).add(tuple);
		}
	}
	
	protected void sendUpFreeLink(Tuple tuple, int linkId) {
		double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
		
		isNorthLinkBusyById.set(linkId, true);
		send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
		send(parentsIds.get(linkId), networkDelay + getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
	}
	
	protected void processTupleArrival(SimEvent ev) {
		Tuple tuple = (Tuple)ev.getData();
		
		if(getName().equals("cloud")){
			updateCloudTraffic();
		}
		
		Logger.debug(getName(),
				"Received tuple " + tuple.getCloudletId() + " with tupleType = " + tuple.getTupleType() + "\t| Source : "
						+ CloudSim.getEntityName(ev.getSource()) + "|Dest : "
						+ CloudSim.getEntityName(ev.getDestination()));
		
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		
		if (tuple.getDirection() == Tuple.ACTUATOR) {
			sendTupleToActuator(tuple);
			return;
		}
		
		if (tuple.getTupleType() == "TOKEN") {
			int srcId = tuple.getSourceDeviceId();
			System.out.println(srcId);
			int index = getChildrenIds().indexOf(srcId);
			index = (index + 1) % getChildrenIds().size();
			sendDown(tuple, getChildrenIds().get(index));
			return;
		}
	}
	
	public List<Integer> getParentsIds() {
		return parentsIds;
	}

	public void setParentsIds(List<Integer> parentsIds) {
		this.parentsIds = parentsIds;
	}

	public List<Boolean> getIsNorthLinkBusyById() {
		return isNorthLinkBusyById;
	}

	public void setIsNorthLinkBusyById(List<Boolean> isNorthLinkBusyById) {
		this.isNorthLinkBusyById = isNorthLinkBusyById;
	}

	public List<Queue<Tuple>> getNorthTupleQueues() {
		return northTupleQueues;
	}

	public void setNorthTupleQueues(List<Queue<Tuple>> northTupleQueues) {
		this.northTupleQueues = northTupleQueues;
	}

	public double getAvailableMips() {
		return getHostList().get(0).getAvailableMips();
		// return availableMips;
	}

	public void setAvailableMips(long availableMips) {
		this.availableMips = availableMips;
	}

	public int getAvailableRam() {
		return getHostList().get(0).getRam();
		// return availableRam;
	}

	public void setAvailableRam(int availableRam) {
		this.availableRam = availableRam;
	}

}
