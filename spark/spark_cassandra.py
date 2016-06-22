from pyspark.streaming.kafka import KafkaUtils
from pyspark import SparkContext
from pyspark.streaming import StreamingContext
import pyspark_cassandra
import sys
import json
from pyspark_cassandra import streaming

sc = SparkContext(appName="PythonStreamingTest")
ssc = StreamingContext(sc,2)
brokers, topic = sys.argv[1:]



##directKafkaStream = KafkaUtils.createDirectStream(ssc, [topic], {"metadata.broker.list": brokers})
directKafkaStream = KafkaUtils.createStream(ssc, brokers,"spark-streaming-consumer", {topic:1})
line = directKafkaStream.map(lambda x: json.loads(x[1]))
#print line
line.saveToCassandra('playground','demo_week3')
line.saveToCassandra('playground','last_ts')

#line.pprint()

#lines = directKafkaStream.map(lambda x : x[1])
#lines.pprint()

ssc.start()
ssc.awaitTermination()
