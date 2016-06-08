from kafka import KafkaConsumer

consumer = KafkaConsumer('FX_test',bootstrap_servers=['172.31.0.199:9092'])

while True:
        for message in consumer:
                print(message.key, message.value)
