from cassandra.cluster import Cluster

cluster = Cluster(['52.26.195.153'])
session = cluster.connect()

session.execute("CREATE KEYSPACE FX WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 3};")


