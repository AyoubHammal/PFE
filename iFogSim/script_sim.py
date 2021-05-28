#!/bin/python3

import os, json, shutil
import numpy as np
import pandas
import matplotlib.pyplot as plt

def executTest():
  os.system("ant TestSM")
  os.system("ant TestBF")
  os.system("ant TestFF")
  os.system("ant TestWF")

dirPath = "results/res"

min = max = step = 0
resultFile = ""
x_label = ""
index = ""
parameter = 0
while parameter > 6 or parameter < 1:
  print("Select parameter to vary:")
  print("1. Number of layers")
  print("2. Number of nodes per layers")
  print("3. Token delay")
  print("4. Tuple transmission rate")
  print("5. Number of sensors types")
  print("6. Number of layers and nodes per layers at the same time");

  parameter = int(input("? > "))

  os.system("ant cleanall")
  os.system("ant build")

if parameter == 1:
  x_label = "Nombre de niveaux"
  index = "NumberOfLayers"
  min = minV = int(input("Min: "))
  max = maxV = int(input("Max: "))
  step = int(input("Step: "))
  while minV <= maxV:
    paramFile = open("topologies/param.json", "r")
    paramJson = json.load(paramFile)
    paramFile.close()
    
    paramJson["NumberOfLayers"] = minV
    minV += step

    paramFile = open("topologies/param.json", "w")
    paramFile.write(json.dumps(paramJson))
    paramFile.close()

    executTest()
    resultFile = paramJson["OutputFileName"]
elif parameter ==2:
  x_label = "Nombre de noeuds par niveau"
  index = "NumberOfNodePerLayer"
  min = minV = int(input("Min: "))
  max = maxV = int(input("Max: "))
  step = int(input("Step: "))
  while minV <= maxV:
    paramFile = open("topologies/param.json", "r")
    paramJson = json.load(paramFile)
    paramFile.close()

    paramJson["NumberOfNodePerLayer"] = minV
    minV += step

    paramFile = open("topologies/param.json", "w")
    paramFile.write(json.dumps(paramJson))
    paramFile.close()

    executTest()
    resultFile = paramJson["OutputFileName"]
elif parameter == 3:
  x_label = "Délai du Token"
  index = "TokenDelay"
  min = minV = float(input("Min: "))
  max = maxV = float(input("Max: "))
  step = int(input("Step: "))
  while minV <= maxV:
    paramFile = open("topologies/param.json", "r")
    paramJson = json.load(paramFile)
    paramFile.close()

    paramJson["TokenDelay"] = minV
    minV += step

    paramFile = open("topologies/param.json", "w")
    paramFile.write(json.dumps(paramJson))
    paramFile.close()

    executTest()
    resultFile = paramJson["OutputFileName"]
elif parameter == 4:
  x_label = "Délai de transmission des tuples"
  index = "TransmitRate"
  min = minV = float(input("Min: "))
  max = maxV = float(input("Max: "))
  step = int(input("Step: "))
  while minV <= maxV:
    paramFile = open("topologies/param.json", "r")
    paramJson = json.load(paramFile)
    paramFile.close()

    paramJson["TransmitRate"] = minV
    minV += step

    paramFile = open("topologies/param.json", "w")
    paramFile.write(json.dumps(paramJson))
    paramFile.close()

    executTest()
    resultFile = paramJson["OutputFileName"]
elif parameter == 5:
  x_label = "Nombre de type de tuples"
  index = "NumberOfSensorTypes"
  min = minV = int(input("Min: "))
  max = maxV = int(input("Max: "))
  step = int(input("Step: "))
  while minV <= maxV:
    paramFile = open("topologies/param.json", "r")
    paramJson = json.load(paramFile)
    paramFile.close()

    paramJson["NumberOfSensorTypes"] = minV
    minV += step

    paramFile = open("topologies/param.json", "w")
    paramFile.write(json.dumps(paramJson))
    paramFile.close()

    executTest()
    resultFile = paramJson["OutputFileName"]
elif parameter == 6:
  x_label = "Nombre de niveaux et de noeuds par niveau (nxn)"
  index = "NumberOfLayers"
  min = minV = int(input("Min: "))
  max = maxV = int(input("Max: "))
  step = int(input("Step: "))
  while minV <= maxV:
    paramFile = open("topologies/param.json", "r")
    paramJson = json.load(paramFile)
    paramFile.close()

    paramJson["NumberOfLayers"] = minV
    paramJson["NumberOfNodePerLayer"] = minV
    minV += step

    paramFile = open("topologies/param.json", "w")
    paramFile.write(json.dumps(paramJson))
    paramFile.close()

    executTest()
    resultFile = paramJson["OutputFileName"]

dirPath += "_var" + str(parameter) + "from" + str(min) + "to" + str(max) + "step" + str(step) + "/"
os.mkdir(dirPath)
shutil.move("output/" + resultFile + "_sm.csv", dirPath + resultFile + "_sm.csv")
shutil.move("output/" + resultFile + "_ff.csv", dirPath + resultFile + "_ff.csv")
shutil.move("output/" + resultFile + "_bf.csv", dirPath + resultFile + "_bf.csv")
shutil.move("output/" + resultFile + "_wf.csv", dirPath + resultFile + "_wf.csv")
shutil.copy("topologies/param.json", dirPath + "param.json")


############## PARTIE PLOT ##############
plt.style.use('ggplot')
res_sm = pandas.read_csv(dirPath + "out_sm.csv", sep = ";", header = 0, dtype = np.float64)
res_ff = pandas.read_csv(dirPath + "out_ff.csv", sep = ";", header = 0, dtype = np.float64)
res_bf = pandas.read_csv(dirPath + "out_bf.csv", sep = ";", header = 0, dtype = np.float64)
res_wf = pandas.read_csv(dirPath + "out_wf.csv", sep = ";", header = 0, dtype = np.float64)
#-----------------------------------
# Energy per Level
energyPerLvl = np.concatenate((res_sm[[index, "AvgEnergie"]].to_numpy(), res_ff[["AvgEnergie"]].to_numpy(), res_bf[["AvgEnergie"]].to_numpy(),res_wf[["AvgEnergie"]].to_numpy()), axis = 1).T
# Normalisation de l'energie
maxEnergy = energyPerLvl[1:, :].max()
energyPerLvl[1:, :] = energyPerLvl[1:, :] / maxEnergy

fig, ax = plt.subplots()
ax.plot(energyPerLvl[0], energyPerLvl[1], label = "SMRA")
ax.plot(energyPerLvl[0], energyPerLvl[2], label = "First Fit")
ax.plot(energyPerLvl[0], energyPerLvl[3], label = "Best Fit")
ax.plot(energyPerLvl[0], energyPerLvl[4], label = "Worst Fit")
ax.set_xlabel(x_label)
ax.set_ylabel("Consommation d'énergie moyenne (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/energyPerLvl.pdf")
#-----------------------------------
# Tuple Execution Delay per Level
tupleDelayPerLvl = np.concatenate((res_sm[[index, "AvgTupleCpuExecutionDelay"]].to_numpy(), res_ff[["AvgTupleCpuExecutionDelay"]].to_numpy(), res_bf[["AvgTupleCpuExecutionDelay"]].to_numpy(), res_wf[["AvgTupleCpuExecutionDelay"]].to_numpy()), axis = 1).T
manDelay = tupleDelayPerLvl[1:, :].max()
tupleDelayPerLvl[1:, :] = tupleDelayPerLvl[1:, :] / manDelay
fig, ax = plt.subplots()
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[1], label = "SMRA")
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[2], label = "First Fit")
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[3], label = "Best Fit")
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[4], label = "Worst Fit")
ax.set_xlabel(x_label)
ax.set_ylabel("Délai moyen d'exécution d'un tuple (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/tupleDelayPerLvl.pdf")
#-----------------------------------
# Loop Delay per Level
loopDelayPerLvl = np.concatenate((res_sm[[index, "AvgAppLoopDelay"]].to_numpy(), res_ff[["AvgAppLoopDelay"]].to_numpy(), res_bf[["AvgAppLoopDelay"]].to_numpy(), res_wf[["AvgAppLoopDelay"]].to_numpy()), axis = 1).T
manDelay = loopDelayPerLvl[1:, :].max()
loopDelayPerLvl[1:, :] = loopDelayPerLvl[1:, :] / manDelay
fig, ax = plt.subplots()
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[1], label = "SMRA")
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[2], label = "First Fit")
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[3], label = "Best Fit")
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[4], label = "Worst Fit")
ax.set_xlabel(x_label)
ax.set_ylabel("Délai moyen d'exécution d'une application (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/loopDelayPerLvl.pdf")
#-----------------------------------
# Number Of Nodes With High Cpu Usage
nodesHighCpuUsage = np.concatenate((res_sm[[index, "NbOfNodesHighCpuUsage"]].to_numpy(), res_ff[["NbOfNodesHighCpuUsage"]].to_numpy(), res_bf[["NbOfNodesHighCpuUsage"]].to_numpy(), res_wf[["NbOfNodesHighCpuUsage"]].to_numpy()), axis = 1).T
fig, ax = plt.subplots()
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[1], label = "SMRA")
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[2], label = "First Fit")
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[3], label = "Best Fit")
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[4], label = "Worst Fit")
ax.set_xlabel(x_label)
#-----------------------------------
paramFile = open("topologies/param.json", "r")
paramJson = json.load(paramFile)
paramFile.close()
highUsage = paramJson["HighUsage"]

ax.set_ylabel("Nombre de noeuds avec plus " + str(highUsage) + "\% d'utilisation CPU")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/nodesHighCpuUsagePerLvl.pdf")
#-----------------------------------
# Variance of Cpu Usage
nodesHighCpuUsage = np.concatenate((res_sm[[index, "VarianceCpuUsage"]].to_numpy(), res_ff[["VarianceCpuUsage"]].to_numpy(), res_bf[["VarianceCpuUsage"]].to_numpy(), res_wf[["VarianceCpuUsage"]].to_numpy()), axis = 1).T
fig, ax = plt.subplots()
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[1], label = "SMRA")
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[2], label = "First Fit")
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[3], label = "Best Fit")
ax.plot(nodesHighCpuUsage[0], nodesHighCpuUsage[4], label = "Worst Fit")
ax.set_xlabel(x_label)

ax.set_ylabel("La vriance de l'utilisation CPU entre les noeuds")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/varCpuUsagePerLvl.pdf")