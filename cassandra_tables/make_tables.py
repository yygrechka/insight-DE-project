from cassandra.cluster import Cluster
import sys


cluster = Cluster(['52.26.195.153'])
session = cluster.connect('playground')

permute_length = sys.argv[1]

try:
    session.execute('DROP TABLE ts_permutation;')
except:
    pass

try:
    session.execute('DROP TABLE processed_ts;')
except:
    pass

try:
    session.execute('DROP TABLE fx_tree;')
except:
    pass

try:
    session.execute('DROP TABLE fx_final_leaves;')
except:
    pass


#t0_stmt = 'CREATE TABLE ts_permutation (day int, ts bigint, permutation list<int>, PRIMARY KEY ((day), ts),);'
#session.execute(t0_stmt)
t0_stmt = 'CREATE TABLE ts_permutation (day int, ts bigint, '
for i in range(1,int(permute_length)+1):
    t0_stmt += 'p' + str(i) + ' int, '

t0_stmt += 'PRIMARY KEY ((day), ts),) WITH CLUSTERING ORDER BY (ts ASC);'
print(t0_stmt)
session.execute(t0_stmt)


t1_stmt = 'CREATE TABLE processed_ts (ts bigint, PRIMARY KEY ((ts)),);'
print(t1_stmt)
session.execute(t1_stmt)


t2_stmt = ''



t2_stmt += 'CREATE TABLE fx_tree (ts bigint, isleaf boolean, '

for i in range(1,int(permute_length)+1):
    t2_stmt += 'pk' + str(i) + ' int, '
    t2_stmt += 'p' + str(i) + ' int, '

t2_stmt += 'PRIMARY KEY (('

for i in range(1,int(permute_length)+1):
    t2_stmt += 'pk' + str(i) + ', '

t2_stmt = t2_stmt[:-2]

t2_stmt += ')), );'

print(t2_stmt)
session.execute(t2_stmt)


t3_stmt = 'CREATE TABLE fx_final_leaves (ts bigint, '
for i in range(1, int(permute_length) + 1):
    t3_stmt += 'p' + str(i) + ' int, '

t3_stmt += 'PRIMARY KEY (('

for i in range(1, int(permute_length) + 1):
    t3_stmt += 'p' + str(i) + ', '

t3_stmt = t3_stmt[:-2]
t3_stmt += ')), );'

print(t3_stmt)
session.execute(t3_stmt)



