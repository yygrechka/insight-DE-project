from datetime import datetime
from cassandra.cluster import Cluster
import sys
import numpy as np

cluster = Cluster(['52.41.153.121'])
session = cluster.connect('fx')

#Script to input historical data into the cassandra database
#as a preprocessing step for spark.


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
    v0=j
  v0 = j



first_part = 'BEGIN BATCH\n'
last_part = 'APPLY BATCH;'
middle_part = 'INSERT INTO batch_table (batch_id, ts, price) VALUES ({0},{1},{2});\n'
middle_part2 = 'INSERT INTO source_table (hour, ts, price) VALUES ({0}, {1}, {2});\n'

#for ii,jj in [(0,15000),(15000,30000),(30000,45000),(45000,60000),(60000,86169)]:
for c in range(1,len(break_points)):
    delta = break_points[c] - break_points[c-1]
    n_batch = delta // 100

    x = np.linspace(break_points[c-1],break_points[c],n_batch)
    xx = [int(i) for i in x]
    xxx = list(zip(xx[:-1], xx[1:]))

    for ii,jj in xxx:
        print(ii)
        full_query = first_part

        for i in range(ii,jj):
          full_query += middle_part.format(c,list_of_ints[i],list_of_prices[i])
          full_query += middle_part2.format(list_of_ints[i] // 3600000, list_of_ints[i], list_of_prices[i])
        full_query += last_part

        session.execute(full_query)


