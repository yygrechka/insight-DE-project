from cassandra.cluster import Cluster
import sys

cluster = Cluster(['52.41.153.121'])
session = cluster.connect('fx')


try:
    session.execute('DROP TABLE anchor_table;')
except:
    pass


anchor_table = 'CREATE TABLE anchor_table (anchor int, ts int, price double, PRIMARY KEY ((anchor), ts),) WITH CLUSTERING ORDER BY (ts ASC);'


session.execute(anchor_table)
