import numpy as np
import pandas
import matplotlib.pyplot as plt
import sys

if len(sys.argv) == 1:
  print("Usage: python script_plot.py <dirname>")
  exit()

res_sm = pandas.read_csv(sys.argv[1] + "/out_sm.csv", sep = ";", header = 0, dtype = np.float64)
res_ff = pandas.read_csv(sys.argv[1] + "/out_ff.csv", sep = ";", header = 0, dtype = np.float64)

# Energy per Level
energyPerLvl = np.concatenate((res_sm[["NumberOfLayers", "AvgEnergie"]].to_numpy(), res_ff[["AvgEnergie"]].to_numpy()), axis = 1).T
# Normalisation de l'energie
maxEnergy = energyPerLvl[1:, :].max()
print(maxEnergy)
energyPerLvl[1:, :] = energyPerLvl[1:, :] / maxEnergy
print(energyPerLvl)

fig, ax = plt.subplots()
ax.plot(energyPerLvl[0], energyPerLvl[1], label = "Proposé")
ax.plot(energyPerLvl[0], energyPerLvl[2], label = "First Fit")
ax.set_xlabel("Nombre de niveaux")
ax.set_ylabel("Consommation d'énergie moyenne (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(sys.argv[1] + "/energyPerLvl.png")

# Loop Delay per Level
tupleDelayPerLvl = np.concatenate((res_sm[["NumberOfLayers", "AvgTupleCpuExecutionDelay"]].to_numpy(), res_ff[["AvgTupleCpuExecutionDelay"]].to_numpy()), axis = 1).T
manDelay = tupleDelayPerLvl[1:, :].max()
print(manDelay)
tupleDelayPerLvl[1:, :] = tupleDelayPerLvl[1:, :] / manDelay
print(tupleDelayPerLvl)
fig, ax = plt.subplots()
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[1], label = "Proposé")
ax.plot(tupleDelayPerLvl[0], tupleDelayPerLvl[2], label = "First Fit")
ax.set_xlabel("Nombre de niveaux")
ax.set_ylabel("Délai moyen d'exécution d'un tuple (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(sys.argv[1] + "/tupleDelayPerLvl.png")

# Loop Delay per Level
loopDelayPerLvl = np.concatenate((res_sm[["NumberOfLayers", "AvgAppLoopDelay"]].to_numpy(), res_ff[["AvgAppLoopDelay"]].to_numpy()), axis = 1).T
manDelay = loopDelayPerLvl[1:, :].max()
print(manDelay)
loopDelayPerLvl[1:, :] = loopDelayPerLvl[1:, :] / manDelay
print(loopDelayPerLvl)
fig, ax = plt.subplots()
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[1], label = "Proposé")
ax.plot(loopDelayPerLvl[0], loopDelayPerLvl[2], label = "First Fit")
ax.set_xlabel("Nombre de niveaux")
ax.set_ylabel("Délai moyen d'exécution d'une application (échelle de 0 à 1)")
ax.set_ylim(ymin=0)
ax.legend()
plt.savefig(sys.argv[1] + "/loopDelayPerLvl.png")