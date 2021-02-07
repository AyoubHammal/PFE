package pfe.fog.cmp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.PhysicalTopology;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;


public class Test {
	private static String topologyFile = "topologies/topologie2x2";
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static List<Integer> clusterFogDevicesIds = new ArrayList<Integer>();
	static List<ClusterFogDevice> clusterFogDevices = new ArrayList<ClusterFogDevice>();
	
	static int nbOfLayers = 7;
	static int nbOfNodePerLayer = 3;
	
	static double transmitRate = 1;
	
	public static void main(String[] args) {
		try {
			Log.disable();
			Log.printLine("Initialisation");
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = true;
			
			CloudSim.init(num_user, calendar, trace_flag);
			
			String appId = "test_app";
			
			// ?????
			FogBroker broker = new FogBroker("broker");
			
			/* PhysicalTopology physicalTopology = JsonToTopology.getPhysicalTopology(broker.getId(), 
					appId, 
					topologyFile); */
			PhysicalTopology physicalTopology = createPhysicalTopology(broker.getId(), appId);
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			System.out.println(broker.getId());
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
			
			for (ClusterFogDevice d : clusterFogDevices) {
				moduleMapping.addModuleToDevice("m1", d.getName());
				//moduleMapping.addModuleToDevice("m2", d.getName());
			}
			moduleMapping.addModuleToDevice("m1", "cloud");
			//moduleMapping.addModuleToDevice("m2", "cloud");
			
			/*
			 * moduleMapping.addModuleToDevice("m2", "Node2/1");
			 * moduleMapping.addModuleToDevice("m2", "Node2/2");
			 * moduleMapping.addModuleToDevice("m1", "Node1/1");
			 * moduleMapping.addModuleToDevice("m1", "Node1/2");
			 */
			
			Controller controller = new Controller("master-controller", 
					physicalTopology.getFogDevices(), 
					physicalTopology.getSensors(), 
					physicalTopology.getActuators());
			
			controller.submitApplication(application, 0,
					new ModulePlacementMapping(physicalTopology.getFogDevices(),
							application,
							moduleMapping
					));
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static PhysicalTopology createPhysicalTopology(int userId, String appId) {
		ClusterFogDevice cloud = createClusterFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		// creation cluster
		List<FogDevice> previousLayer = new ArrayList<FogDevice>();
		List<FogDevice> currentLayer = new ArrayList<FogDevice>();
		for (int i = 0; i < nbOfLayers; i++) {
			for (int j = 0; j < nbOfNodePerLayer; j++) {
				ClusterFogDevice d = createClusterFogDevice("n" + i + "/" + j, 2200, 4000, 10000, 10000, i + 1, 0.0, 100, 50);
				currentLayer.add(d);
				fogDevices.add(d);
				clusterFogDevices.add(d);
				clusterFogDevicesIds.add(d.getId());
				if (previousLayer.size() == 0) {
					d.setParentId(cloud.getId());
					d.addParent(cloud.getId());
				} else {
					for (FogDevice cd : previousLayer) {
						d.setParentId(cd.getId());
						d.addParent(cd.getId());
					}
				}
			}
			
			previousLayer.clear();
			previousLayer = new ArrayList<FogDevice>(currentLayer);
			currentLayer.clear();
		}
		// creation gw
		GWFogDevice lastGw = null;
		for (int j = 0; j < nbOfNodePerLayer; j++) {
			GWFogDevice gwd = createGWFogDevice("GW" + j, 2200, 4000, 10000, 10000,  nbOfLayers, 0.0, 100, 50, clusterFogDevicesIds);
			currentLayer.add(gwd);
			fogDevices.add(gwd);
			Sensor s = new Sensor("s" + j, "T1", userId, appId, new DeterministicDistribution(transmitRate));
			sensors.add(s);
			Actuator a = new Actuator("a" + j, userId, appId, "A1");
			actuators.add(a);
			s.setGatewayDeviceId(gwd.getId());
			s.setLatency(1.0);
			a.setGatewayDeviceId(gwd.getId());
			a.setLatency(1.0);
			s = new Sensor("s" + j, "T2", userId, appId, new DeterministicDistribution(transmitRate));
			sensors.add(s);
			s.setGatewayDeviceId(gwd.getId());
			s.setLatency(1.0);
			for (FogDevice cd : previousLayer) {
				gwd.setParentId(cd.getId());
				gwd.addParent(cd.getId());
			}
			lastGw = gwd;
		}
		
		
		PhysicalTopology pt = new PhysicalTopology();
		pt.setFogDevices(fogDevices);
		pt.setSensors(sensors);
		pt.setActuators(actuators);
		
		return pt;
	}
	
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);
		//new FogLinearPowerModel(busyPower, idlePower)

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	private static ClusterFogDevice createClusterFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		ClusterFogDevice fogdevice = null;
		try {
			fogdevice = new ClusterFogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}

	private static GWFogDevice createGWFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower, List<Integer> clusterFogDevicesIds) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		GWFogDevice fogdevice = null;
		try {
			fogdevice = new GWFogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips, clusterFogDevicesIds);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	private static Application createApplication(String appId, int userId) {
		Application application = Application.createApplication(appId, userId);
		
		application.addAppModule("m1", 100, 500, 1000, 100);
		application.addAppModule("m2", 100, 1500, 1000, 100);
		
		application.addAppEdge("T1", "m1", 3000, 50, "T1", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("T2", "m2", 9000, 50, "T2", Tuple.UP, AppEdge.SENSOR);
		//application.addAppEdge("m1", "m2", 3000, 500, "e1", Tuple.UP, AppEdge.MODULE);
		//application.addAppEdge("m2", "m1", 3000, 500, "e2", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("m1", "A1", 3000, 50, "A1", Tuple.DOWN, AppEdge.ACTUATOR);
		application.addAppEdge("m2", "A1", 3000, 50, "A1", Tuple.DOWN, AppEdge.ACTUATOR);
		
		application.addTupleMapping("m1", "T1", "A1", new FractionalSelectivity(1.0));
		application.addTupleMapping("m2", "T2", "A1", new FractionalSelectivity(1.0));
		//application.addTupleMapping("m1", "T1", "e1", new FractionalSelectivity(1.0));
		//application.addTupleMapping("m2", "e1", "e2", new FractionalSelectivity(1.0));
		//application.addTupleMapping("m1", "e2", "A1", new FractionalSelectivity(1.0));
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
				{	
					add("T1");
					add("m1");
					add("A1");
				}
			});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>() {
			{	
				add("T2");
				add("m2");
				add("A1");
			}
		});
		List<AppLoop> loops = new ArrayList<AppLoop>() {
			{add(loop1);add(loop2);}
		};
		application.setLoops(loops);
		
		return application;
	}
}
