import os, sys, json, shutil

def executTest():
  os.system("ant Test")
  os.system("ant Test1")

dirPath = "results/res"

min = max = step = 0
resultFile = ""
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
  min = minV = int(input("Min: "))
  max = maxV = int(input("Max: "))
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
  min = minV = int(input("Min: "))
  max = maxV = int(input("Max: "))
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