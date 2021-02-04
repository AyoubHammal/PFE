package pfe.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
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

import com.sun.tools.javac.code.Type.ForAll;

public class GWFogDevice extends FogDevice {
	protected Queue<Tuple> waitingQueue;
	protected Map<Tuple, Integer> tupleToMatchedDevice = new HashMap<Tuple, Integer>();
	protected boolean token;
	protected List<Integer> parentsIds = new ArrayList<Integer>();
	protected List<Boolean> isNorthLinkBusyById = new ArrayList<Boolean>();
	protected List<Queue<Tuple>> northTupleQueues = new ArrayList<Queue<Tuple>>();
	protected List<Integer> clusterFogDevicesIds = new ArrayList<Integer>();
	
	public GWFogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
			double uplinkLatency, double ratePerMips, List<Integer> clusterFogDevicesIds) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
		this.clusterFogDevicesIds = clusterFogDevicesIds;
	}
	
	public void addParent(int patendId) {
		parentsIds.add(parentId);
		isNorthLinkBusyById.add(false);
		northTupleQueues.add(new Queue<Tuple>());
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
		ArrayList<MatchedTuple> matchedTupleList = new ArrayList<MatchedTuple>();
		int n = clusterFogDevicesIds.size();
		for(int i = 0; (i < waitingQueue.size()) && (i < n); i++) {
			matchedTupleList.add(new MatchedTuple(waitingQueue.poll()));
		}
		
		while (!matchedTupleList.isEmpty()) {
			
		}
	}
}
