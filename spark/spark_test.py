from pyspark import SparkContext
from pyspark.streaming import StreamingContext

sc = SparkContext("local[2]", "NetworkWordCount")
ssc = StreamingContext(sc,1)

lines = ssc.socketTextStream("172.31.0.199",9092)

words = lines.flatMap(lambda line: line.split(" "))

pairs = words.map(lambda word: (word, 1))
wordCounts = pairs.reduceByKey(lambda x,y: x+y)

wordCounts.pprint()
ssc.start()
ssc.awaitTermination()
