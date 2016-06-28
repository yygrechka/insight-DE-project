from datetime import datetime
from cassandra.cluster import Cluster
import sys
import numpy as np

cluster = Cluster(['52.41.153.121'])
session = cluster.connect('fx')


file1 = sys.argv[1]

pattern = '%Y%m%d %H:%M:%S.%f'
list_of_vals = []
list_of_nums = []
list_of_prices_ = []
list_of_prices = []

with open(file1,'r') as input_:
  for line in input_:
    list_of_vals.append(line.strip().split(',')[1] + '000')
    list_of_prices_.append(line.strip().split(',')[2])

list_of_nums = [float(datetime.strptime(i, pattern).timestamp()) for i in list_of_vals]
list_of_ints = [int(i*1000) for i in list_of_nums]
list_of_prices = [float(i) for i in list_of_prices_]

v0 = list_of_ints[0]

break_points = [0]

for i,j in enumerate(list_of_ints):
  if j > v0 + 3600000:
    break_points.append(i)
    v0 = j
  v0=j

print(break_points)
s_ = len(break_points)

with open(file1,'r') as input_:
  with open("remainder.csv",'w') as output_:
    for c,line in enumerate(input_):
      if c >= break_points[-1]:
        output_.write(line)

