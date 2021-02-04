package pfe.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.NetworkUsageMonitor;

public class GWFogDevice extends FogDevice {
	protected Queue<Tuple> waitingQueue;
	protected Map<Tuple, Integer> tupleToMatchedDevice = new HashMap<Tuple, Integer>();
	protected boolean token;
	protected List<Integer> parentsIds = new ArrayList<Integer>();
	protected List<Boolean> isNorthLinkBusyById = new ArrayList<Boolean>();
	protected List<Queue<Tuple>> northTupleQueues = new ArrayList<Queue<Tuple>>();
	protected List<Integer> clusterFogDevicesIds = new ArrayList<Integer>();

	protected long availableMips;
	protected int availableRam;
	
	// La liste des tuples matches
	ArrayList<MatchedTuple> matchedTupleList = new ArrayList<MatchedTuple>();
	// La liste des tuples delegues au cloud
	ArrayList<MatchedTuple> toCloudTupleList = new ArrayList<MatchedTuple>();
	
	public GWFogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
			double uplinkLatency, double ratePerMips, List<Integer> clusterFogDevicesIds) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
		this.clusterFogDevicesIds = clusterFogDevicesIds;
		
		availableMips = characteristics.getMips();
		availableRam = characteristics.getHostList().get(0).getRam();

	}
	
	public void addParent(int patendId) {
		parentsIds.add(parentId);
		isNorthLinkBusyById.add(false);
		northTupleQueues.add(new LinkedList<Tuple>()); // Queue est une interface !!
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
	
	
	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
		if(getName().equals("cloud")){
			updateCloudTraffic();
		}
		
		Logger.debug(getName(),
				"Received tuple " + tuple.getCloudletId() + "with tupleType = " + tuple.getTupleType() + "\t| Source : "
						+ CloudSim.getEntityName(ev.getSource()) + "|Dest : "
						+ CloudSim.getEntityName(ev.getDestination()));
		System.out.println(getName() + 
				" Received tuple " + tuple.getCloudletId() + "with tupleType = " + tuple.getTupleType() + "\t| Source : "
						+ CloudSim.getEntityName(ev.getSource()) + "|Dest : "
						+ CloudSim.getEntityName(ev.getDestination()));
		
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		
		if (tuple.getDirection() == Tuple.ACTUATOR) {
			sendTupleToActuator(tuple);
			return;
		}

		if (appToModulesMap.containsKey(tuple.getAppId())) {
			if (appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())) {
				
				if (tuple.getTupleType() == "TOKEN") {
					// Match
					mapTupleToDevice();
					// Envoi
					for (MatchedTuple mt : matchedTupleList) {
						int link = -1;
						if (parentsIds.contains(mt.getDestinationFogDevice()))
							link = parentsIds.indexOf(mt.getDestinationFogDevice());
						
						sendUp(tuple, link == -1 ? 0 : link);
					}
					
					for (MatchedTuple mt : toCloudTupleList)
						sendUp(tuple, 0);
					
				} else {
					int vmId = -1;
					for (Vm vm : getHost().getVmList()) {
						if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
							vmId = vm.getId();
					}
					if (vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName())
							&& tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
						return;
					}
					tuple.setVmId(vmId);
					updateTimingsOnReceipt(tuple);
					
					// Ajout a la queue
					waitingQueue.add(tuple);
				}
				
			} else if (tuple.getDestModuleName() != null) {
				if (tuple.getDirection() == Tuple.UP)
					sendUp(tuple);
				else if (tuple.getDirection() == Tuple.DOWN) {
					for (int childId : getChildrenIds())
						sendDown(tuple, childId);
				}
			} else {
				sendUp(tuple);
			}
		} else {
			if (tuple.getDirection() == Tuple.UP)
				sendUp(tuple);
			else if (tuple.getDirection() == Tuple.DOWN) {
				for (int childId : getChildrenIds())
					sendDown(tuple, childId);
			}
		}
		
	}
	
	private void mapTupleToDevice() {
		matchedTupleList = new ArrayList<MatchedTuple>();
		toCloudTupleList = new ArrayList<MatchedTuple>();
		
		int n = clusterFogDevicesIds.size();
		
		// La liste des tuples a matcher 
		ArrayList<MatchedTuple> toBeMatchedTupleList = new ArrayList<MatchedTuple>();
		
		// La liste des tuples qui ont choisi un noeud pendant un tour
		Map<Integer, List<MatchedTuple>> tuplesRequestingDevice = new HashMap<Integer, List<MatchedTuple>>();
		// La liste du tuple selectionne pour un noeud pendant un tour
		Map<Integer, MatchedTuple> selectedTupleForDevice = new HashMap<Integer, MatchedTuple>();
		
		for (int i = 0; i < clusterFogDevicesIds.size(); i++) {
			tuplesRequestingDevice.put(clusterFogDevicesIds.get(i), new ArrayList<MatchedTuple>());
			selectedTupleForDevice.put(clusterFogDevicesIds.get(i), null);
		}
		
		for(int i = 0; (i < waitingQueue.size()) && (i < n); i++)
			toBeMatchedTupleList.add(new MatchedTuple(waitingQueue.poll()));
		
		while (!toBeMatchedTupleList.isEmpty()) {
			for (MatchedTuple mt : toBeMatchedTupleList) {
				int id = selectBestDeviceForTuple(mt);
				if (id == -1) {
					toBeMatchedTupleList.remove(mt);
					toCloudTupleList.add(mt);
				} else {
					tuplesRequestingDevice.get(id).add(mt);
				}
			}
			for (int id : clusterFogDevicesIds) {
				MatchedTuple mt = selectBestTupleForDevice(id , tuplesRequestingDevice.get(id));
				if (selectedTupleForDevice.get(id) != null) {
					toBeMatchedTupleList.add(selectedTupleForDevice.get(id));
					matchedTupleList.remove(selectedTupleForDevice.get(id));
				}
				toBeMatchedTupleList.remove(mt);
				matchedTupleList.add(mt);
				selectedTupleForDevice.put(id, mt);
				
				tuplesRequestingDevice.clear();
			}
		}
		
		for (Map.Entry<Integer, MatchedTuple> e : selectedTupleForDevice.entrySet())
			matchedTupleList.get(matchedTupleList.indexOf(e.getValue())).setDestinationFogDeviceId(e.getKey());
		for (MatchedTuple mt : toCloudTupleList)
			mt.setDestinationFogDeviceId(CloudSim.getEntityId("cloud"));
	}
	
	private int selectBestDeviceForTuple(MatchedTuple mt) {
		double minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(clusterFogDevicesIds.get(0)), mt);
		int bestId = 0;
		for (int id : clusterFogDevicesIds) {
			if (minDist > calculateDistance((ClusterFogDevice)CloudSim.getEntity(clusterFogDevicesIds.get(id)), mt)) {
				minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(clusterFogDevicesIds.get(id)), mt);
				bestId = id;
			}
		}
		return bestId;
	}
	
	private MatchedTuple selectBestTupleForDevice(int id, List<MatchedTuple> tuplesRequestingDevice) {
		double minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), tuplesRequestingDevice.get(0));
		MatchedTuple bestTuple = tuplesRequestingDevice.get(0);
		for (MatchedTuple mt : tuplesRequestingDevice) {
			if (minDist > calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt)) {
				minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt);
				bestTuple = mt;
			}
		}
		return bestTuple;
	}
	
	private double calculateDistance(ClusterFogDevice d, Tuple t) {
		return Math.sqrt(t.getUtilizationModelCpu().getUtilization(d.getAvailableMips()) + t.getUtilizationModelRam().getUtilization(d.getAvailableRam()));
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

	public long getAvailableMips() {
		return availableMips;
	}

	public void setAvailableMips(long availableMips) {
		this.availableMips = availableMips;
	}

	public int getAvailableRam() {
		return availableRam;
	}

	public void setAvailableRam(int availableRam) {
		this.availableRam = availableRam;
	}
	
	public List<Integer> getClusterFogDevicesIds() {
		return clusterFogDevicesIds;
	}

	public void setClusterFogDevicesIds(List<Integer> clusterFogDevicesIds) {
		this.clusterFogDevicesIds = clusterFogDevicesIds;
	}
}
