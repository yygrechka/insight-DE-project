from cassandra.cluster import Cluster

cluster = Cluster(['52.41.153.121'])
session = cluster.connect()

session.execute("CREATE KEYSPACE FX WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 3};")


