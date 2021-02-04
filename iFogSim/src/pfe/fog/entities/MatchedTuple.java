package pfe.fog.entities;

import org.cloudbus.cloudsim.UtilizationModel;
import org.fog.entities.Tuple;

public class MatchedTuple extends Tuple {
	
	protected int destinationFogDeviceId;

	public MatchedTuple(String appId, int cloudletId, int direction, long cloudletLength, int pesNumber,
			long cloudletFileSize, long cloudletOutputSize,
			UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(appId, cloudletId, direction, cloudletLength, pesNumber, cloudletFileSize,
				cloudletOutputSize, utilizationModelCpu, utilizationModelRam,
				utilizationModelBw);
	}
	
	public MatchedTuple(Tuple tuple) {
		this(tuple.getAppId(), tuple.getCloudletId(), tuple.getDirection(), tuple.getCloudletLength(), tuple.getNumberOfPes(),
				tuple.getCloudletFileSize(), tuple.getCloudletOutputSize(),
				tuple.getUtilizationModelCpu(),
				tuple.getUtilizationModelRam(),
				tuple.getUtilizationModelBw());
	}
	
	public void setDestinationFogDeviceId(int id) {
		destinationFogDeviceId = id;
	}
	public  int getDestinationFogDevice() {
	return destinationFogDeviceId;
    }
	
}
