package pfe.fog.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.fog.utils.Config;

public class ResultToCSV {
	private static PrintWriter writer;
	
	public static void init() {
		try {
			if ((new File(Config.outputFileName)).exists()) {
				writer = new PrintWriter(new FileWriter(Config.outputFileName, true), true);
			} else {
				writer = new PrintWriter(new FileWriter(Config.outputFileName), true);
				writer.println("NumberOfLayers;NumberOfNodePerLayer;TokenDelay;TransmitRate;NumberOfSensorTypes;AvgEnergie;AvgAppLoopDelay;AvgTupleCpuExecutionDelay;TotalExecutedTuples;");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void addLine(double avgEnergie, double avgAppLoopDelay, double avgTupleCpuExecutionDelay, int totalExecutedTuples) {
		writer.printf("%d;%d;%f;%f;%d;%f;%f;%f;%d\n", Config.nbOfLayers,
														Config.nbOfNodePerLayer,
														Config.tokenDelay,
														Config.transmitRate,
														Config.numberOfSensorTypes,
														avgEnergie,
														avgAppLoopDelay,
														avgTupleCpuExecutionDelay,
														totalExecutedTuples);
	}
}
