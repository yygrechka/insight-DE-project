import sys

n_args = sys.argv[1]


hour = 'hour'
ts = 'last_ts_rdd'

line1 = 'val collection = sc.parallelize(Array((hour, last_ts_rdd, '
for i in range(int(n_args)):
    line1 += 'permutation(' + str(i) + '), '

line1 = line1[:-2]
line1 = line1 + ')))'

line2 = 'collection.saveToCassandra("playground","ts_permutation",SomeColumns("day", "ts", '
for i in range(1,int(n_args)+1):
    line2 += '"p' + str(i) + '", '

line2 = line2[:-2]
line2 += '))'

print(line1)
print(line2)
