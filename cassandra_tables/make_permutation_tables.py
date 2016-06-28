from cassandra.cluster import Cluster
import sys

cluster = Cluster(['52.41.153.121'])
session = cluster.connect('fx')

n_vars = int(sys.argv[1])

permutation_tables = [None] * n_vars

try:
    session.execute('DROP TABLE ts_to_permutation')
except:
    pass

for i in range(n_vars):
    try:
        session.execute('DROP TABLE permutation_granularity_{}'.format(i))
    except:
        pass


ts_to_permutation = 'CREATE TABLE ts_to_permutation (ts bigint, '
for i in range(n_vars):
    ts_to_permutation += 'v' + str(i) + ' int, '




ts_to_permutation += 'PRIMARY KEY ((ts)),);'


for i in range(n_vars):
    stmt = 'CREATE TABLE permutation_granularity_{} (ts bigint, '.format(i)
    for j in range(n_vars):
        stmt += 'v' + str(j) + ' int, '
    stmt += 'PRIMARY KEY (('
    for j in range(i+1):
        stmt += 'v' + str(j) + ', '
    stmt = stmt[:-2]
    stmt += ')),)'
    print(stmt)
    permutation_tables[i] = stmt
    while True:
        try:
            session.execute(stmt)
        except:
            continue
        break


session.execute(ts_to_permutation)

print(ts_to_permutation)






