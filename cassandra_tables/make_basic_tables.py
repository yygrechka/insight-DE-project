from cassandra.cluster import Cluster

cluster = Cluster(['52.41.153.121'])
session = cluster.connect('fx')

#Creating basic tables

try:
	session.execute('DROP TABLE batch_table')
except:
	pass

try:
	session.execute('DROP TABLE source_table')
except:
	pass


basic_table1 = 'CREATE TABLE batch_table (batch_id int, ts bigint, price double, PRIMARY KEY ((batch_id), ts),) WITH CLUSTERING ORDER BY (ts ASC);'

basic_table2 = 'CREATE TABLE source_table (hour int, ts bigint, price double, PRIMARY KEY ((hour), ts),) WITH CLUSTERING ORDER BY (ts ASC);'



session.execute(basic_table1)
session.execute(basic_table2)
