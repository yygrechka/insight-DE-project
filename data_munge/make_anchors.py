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

break_points = [20000 + 3000 * i for i in range(15)]
break_ends = [0] * 15
start_ts = [0] * 15

for i,point in enumerate(break_points):
    start_ts[i] = list_of_ints[point]
    k = list_of_ints[point]
    index = point
    while k < list_of_ints[point] + 600000:
        index+=1
        k = list_of_ints[index]
    break_ends[i] = index


list_of_prices = np.array(list_of_prices)
#std__ = np.std(list_of_prices)

first_part = 'BEGIN BATCH\n'
last_part = 'APPLY BATCH;'
middle_part = 'INSERT INTO anchor_table (anchor, ts, price) VALUES ({0},{1},{2});\n'

full_query = first_part
for i in range(15):
    full_query = first_part
    for j in range(break_points[i],break_ends[i]):
        std__ = np.std(list_of_prices[break_points[i]:break_ends[i]])
        full_query += middle_part.format(i,list_of_ints[j]-list_of_ints[break_points[i]],(list_of_prices[j]- list_of_prices[break_points[i]]) )
    full_query += last_part


    session.execute(full_query)


