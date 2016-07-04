from cassandra.cluster import Cluster

cluster = Cluster(['52.41.153.121'])
session = cluster.connect('fx')

#These tables store the latest information.

try:
	session.execute('DROP TABLE last_ts')
except:
	pass

try:
	session.execute('DROP TABLE last_ts_permutation')
except:
	pass


singleton_table1 = 'CREATE TABLE last_ts (const int, ts bigint, PRIMARY KEY ((const)),);'

singleton_table2 = 'CREATE TABLE last_ts_permutation (const int, ts bigint, '

for i in range(15):
    singleton_table2 += 'v' + str(i) + ' int, '

singleton_table2 += 'PRIMARY KEY ((const)),);'


print(singleton_table1)
print(singleton_table2)

session.execute(singleton_table1)
session.execute(singleton_table2)
