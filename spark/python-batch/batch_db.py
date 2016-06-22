import sys
from cassandra.cluster import Cluster
import time


cluster = Cluster(['52.26.195.153'])
session = cluster.connect('playground')


def f(x):
    k = session.execute('select * from ts_permutation;')
    kk = list(k)
    kkk = kk[0]
    return x



#rdd = sc.cassandraTable("playground", "ts_permutation").map( f).collect()

print time.time()
aa = set()
for i in range(10000000):
    aa.add(i)
print time.time()

