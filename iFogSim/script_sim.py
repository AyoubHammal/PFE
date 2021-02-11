import os, json, shutil
import numpy as np
import pandas
import matplotlib.pyplot as plt

def executTest():
  os.system("ant Test")
  os.system("ant Test1")

dirPath = "results/res"

min = max = step = 0
resultFile = ""
x_label = ""
index = ""
parameter = 0
while parameter > 5 or parameter < 1:
  print("Select parameter to vary:")
  print("1. Number of layers")
  print("2. Number of nodes per layers")
  print("3. Token delay")
  print("4. Tuple transmission rate")
  print("5. Number of sensors types")

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

dirPath += "_var" + str(parameter) + "from" + str(min) + "to" + str(max) + "step" + str(step) + "/"
os.mkdir(dirPath)
shutil.move("output/" + resultFile + "_sm.csv", dirPath + resultFile + "_sm.csv")
shutil.move("output/" + resultFile + "_ff.csv", dirPath + resultFile + "_ff.csv")
shutil.copy("topologies/param.json", dirPath + "param.json")

# PARTIE PLOT
res_sm = pandas.read_csv(dirPath + "out_sm.csv", sep = ";", header = 0, dtype = np.float64)
res_ff = pandas.read_csv(dirPath + "out_ff.csv", sep = ";", header = 0, dtype = np.float64)

# Energy per Level
energyPerLvl = np.concatenate((res_sm[[index, "AvgEnergie"]].to_numpy(), res_ff[["AvgEnergie"]].to_numpy()), axis = 1).T
# Normalisation de l'energie
maxEnergy = energyPerLvl[1:, :].max()
print(maxEnergy)
energyPerLvl[1:, :] = energyPerLvl[1:, :] / maxEnergy
print(energyPerLvl)

fig, ax = plt.subplots()
ax.plot(energyPerLvl[0], energyPerLvl[1], label = "Proposé")
ax.plot(energyPerLvl[0], energyPerLvl[2], label = "First Fit")
ax.set_xlabel(x_label)
ax.set_ylabel("Consommation d'énergie moyenne (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/energyPerLvl.png")

# Loop Delay per Level
tupleDelayPerLvl = np.concatenate((res_sm[[index, "AvgTupleCpuExecutionDelay"]].to_numpy(), res_ff[["AvgTupleCpuExecutionDelay"]].to_numpy()), axis = 1).T
manDelay = tupleDelayPerLvl[1:, :].max()
print(manDelay)
tupleDelayPerLvl[1:, :] = tupleDelayPerLvl[1:, :] / manDelay
print(tupleDelayPerLvl)
fig, ax = plt.subplots()
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[1], label = "Proposé")
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[2], label = "First Fit")
ax.set_xlabel(x_label)
ax.set_ylabel("Délai moyen d'exécution d'un tuple (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/tupleDelayPerLvl.png")

# Loop Delay per Level
loopDelayPerLvl = np.concatenate((res_sm[[index, "AvgAppLoopDelay"]].to_numpy(), res_ff[["AvgAppLoopDelay"]].to_numpy()), axis = 1).T
manDelay = loopDelayPerLvl[1:, :].max()
print(manDelay)
loopDelayPerLvl[1:, :] = loopDelayPerLvl[1:, :] / manDelay
print(loopDelayPerLvl)
fig, ax = plt.subplots()
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[1], label = "Proposé")
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[2], label = "First Fit")
ax.set_xlabel(x_label)
ax.set_ylabel("Délai moyen d'exécution d'une application (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(dirPath + "/loopDelayPerLvl.png")