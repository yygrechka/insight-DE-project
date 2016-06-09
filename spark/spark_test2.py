from pyspark.streaming.kafka import KafkaUtils
from pyspark import SparkContext
from pyspark.streaming import StreamingContext
import sys

print(0)
sc = SparkContext(appName="PythonStreamingTest")
print(1)
ssc = StreamingContext(sc,2)
print(2)
brokers, topic = sys.argv[1:]
print(3)

#directKafkaStream = KafkaUtils.createDirectStream(ssc, [topic], {"metadata.broker.list": brokers})
directKafkaStream = KafkaUtils.createStream(ssc, brokers,"spark-streaming-consumer", {topic:1})
line = directKafkaStream.map(lambda x: x[1])
line.pprint()

print(4)
print(directKafkaStream)
#lines = directKafkaStream.map(lambda x : x[1])
#lines.pprint()

ssc.start()
ssc.awaitTermination()
