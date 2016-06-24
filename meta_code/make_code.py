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


line3 = '(btime.value(a),'
for i in range(int(n_args)):
    line3 += 'x(' + str(i) + ')._2,'

line3 = line3[:-1]
line3 += ')'

line4 = 'collection.saveToCassandra("fx","permutation_granularity_" + a.toString, SomeColumns("ts", '
for i in range(0,int(n_args)):
    line4 += '"v' + str(i) + '", '

line4 = line4[:-2]
line4 += '))'





print(line1)
print(line2)
print(line3)
print(line4)
