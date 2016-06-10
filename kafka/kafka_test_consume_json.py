from kafka import KafkaConsumer
import json

consumer = KafkaConsumer('FX_test',bootstrap_servers=['172.31.0.199:9092'])


while True:
        for message in consumer:
                print(json.loads(message.value.decode('utf-8')))
