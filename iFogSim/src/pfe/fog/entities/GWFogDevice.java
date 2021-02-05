package pfe.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

public class GWFogDevice extends FogDevice {
	protected Queue<Tuple> waitingQueue = new LinkedList<Tuple>();
	protected Map<Tuple, Integer> tupleToMatchedDevice = new HashMap<Tuple, Integer>();
	protected List<GWFogDevice> gwDevices;
	protected boolean token;
	protected List<Integer> parentsIds = new ArrayList<Integer>();
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
		northTupleQueues.add(new LinkedList<Tuple>()); // Queue est une interface !!
	}
	
	public List<Integer> getParentsId() {
		return parentsIds;
	}
	
	protected void sendUp(Tuple tuple, int linkId) {
		if (!isNorthLinkBusy()) {
			sendUpFreeLink(tuple, linkId);
		} else {
			northTupleQueues.get(linkId).add(tuple);
		}
	}
	
	protected void sendUpFreeLink(Tuple tuple, int linkId) {
		double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
		
		setNorthLinkBusy(true);
		send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
		send(parentsIds.get(linkId), networkDelay + getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
	}
	
	@Override
	public void startEntity() {
		super.startEntity();
		if (token) {
			token = false;
			Tuple t = new Tuple(null, 0, 0, 0, 0, 0, 0, null, null, null);
			t.setTupleType("TOKEN");
			sendToSelf(t);
		}
	}
	
	protected void processTupleArrival(SimEvent ev){
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
			// Match
			mapTupleToDevice();
			// Envoi
			for (MatchedTuple mt : matchedTupleList) {
				int link = -1;
				if (parentsIds.contains(mt.getDestinationFogDevice()))
					link = parentsIds.indexOf(mt.getDestinationFogDevice());
				
				sendUp(mt, link == -1 ? 0 : link);
			}
			
			for (MatchedTuple mt : toCloudTupleList)
				sendUp(mt, 0);
			
			// Envoi a la prochaine gateway
			tuple.setSourceDeviceId(getId());
			sendUp(tuple, 0);
		} else
			// Ajout a la queue
			waitingQueue.add(tuple);	
	}
	
	private void mapTupleToDevice() {
		MatchedTuple m;
		matchedTupleList = new ArrayList<MatchedTuple>();
		toCloudTupleList = new ArrayList<MatchedTuple>();

		int n = clusterFogDevicesIds.size();
		
		HashMap<MatchedTuple,List<Integer>> tuple_prepositionsList = new HashMap<MatchedTuple,List<Integer>>();
		
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
		
		
		
		for(int i = 0; (i < waitingQueue.size()) && (i < n); i++) {
			Tuple t = waitingQueue.poll();
			MatchedTuple mt = new MatchedTuple(t);
			toBeMatchedTupleList.add(mt);
			tuple_prepositionsList.put(mt, new ArrayList<Integer>(clusterFogDevicesIds));
			// initailement chaque tuple peut se proposé à tout les noeuds.
		}
		
		while (!toBeMatchedTupleList.isEmpty()) {
			for (MatchedTuple mt : toBeMatchedTupleList) {
				int id = selectBestDeviceForTuple(mt,tuple_prepositionsList.get(mt));
				if (id == -1) {
					toBeMatchedTupleList.remove(mt);
					toCloudTupleList.add(mt);
				} else {
					tuplesRequestingDevice.get(id).add(mt); 
			//Chaque tuple se propose au noeud qu'il préfère parmi ceux à qui il ne s'est pas déja présenté.
				}
			}
			for (int id : clusterFogDevicesIds) {
				if (tuplesRequestingDevice.get(id).size() > 0) {
					MatchedTuple mt = selectBestTupleForDevice(id , tuplesRequestingDevice.get(id));
					// On choise le meilleur tuple pour le noeud parmi les proposition.
					for(MatchedTuple mt2 : tuplesRequestingDevice.get(id))
					{
						if(!mt2.equals(mt)) tuple_prepositionsList.get(mt2).remove(Integer.valueOf(id));
					}
					/* Si id préfère mt à tout les autres tuple qui se sont proposé à lui, alors ces tuples
				  	ne peuvent plus se proposé à lui. 
					 */
					if (selectedTupleForDevice.get(id) != null) 
						// si le noeud est déja pris, alors on éclate le couple.
					{
						m = selectedTupleForDevice.get(id);
						toBeMatchedTupleList.add(m);
						// on ajoute l'ancien tuple à l'ensemble des tuple à matcher.
						tuple_prepositionsList.get(m).remove(Integer.valueOf(id));
						// on supprime le neuds de la liste des neuds que le tuple peut se proposer.
						matchedTupleList.remove(m);
						// et on le supprime de la liste des tuples matcher.
					}
					toBeMatchedTupleList.remove(mt);
					// on supprime le nouveau tuple préféré de la liste des tuples à matcher.
					matchedTupleList.add(mt);// on ajoute le nouveau tuple préféré de la liste des tuples matchés. 
					selectedTupleForDevice.put(id, mt);// et on place le nouveau couple.
					tuplesRequestingDevice.get(id).clear();
				}
			}
		}

		for (Map.Entry<Integer, MatchedTuple> e : selectedTupleForDevice.entrySet())
			if (e.getValue() != null)
				matchedTupleList.get(matchedTupleList.indexOf(e.getValue())).setDestinationFogDeviceId(e.getKey());
		for (MatchedTuple mt : toCloudTupleList)
			mt.setDestinationFogDeviceId(CloudSim.getEntityId("cloud"));
	}
	
	private int selectBestDeviceForTuple(MatchedTuple mt, List<Integer> prepositionsList) {
		double minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(prepositionsList.get(0)), mt);
		int bestId = prepositionsList.get(0);
		for (int id : prepositionsList) {
			if (minDist > calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt)) {
				minDist = calculateDistance((ClusterFogDevice)CloudSim.getEntity(id), mt);
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
	
	public List<GWFogDevice> getGwDevices() {
		return gwDevices;
	}

	public void setGwDevices(List<GWFogDevice> gwDevices) {
		this.gwDevices = gwDevices;
	}

	public boolean isToken() {
		return token;
	}

	public void setToken(boolean token) {
		this.token = token;
	}
}

