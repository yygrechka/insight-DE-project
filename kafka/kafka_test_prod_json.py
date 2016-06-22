from kafka import KafkaProducer
from kafka.errors import KafkaError
import time
import requests
import json

user_ = ''
pass_ = ''



with open('../.gitignore/userpass.txt','r') as input:
	user_ = input.readline().strip()
	pass_ = input.readline().strip()

#9092
producer = KafkaProducer(bootstrap_servers=['172.31.0.199'])

auth_response = requests.get('http://webrates.truefx.com/rates/connect.html?u=' + user_ + '&p=' + pass_ + '&q=eurates&c=EUR/USD&f=csv&s=n')

website = 'http://webrates.truefx.com/rates/connect.html'
id_string = auth_response.text.strip()

c=0
last_ts = 0
last_price = 0
	
while True:
        fx_response = requests.get(website + '?id=' + id_string)
        if fx_response.text.strip() == '':
                continue
        #producer.send('FX_test', bytes(fx_response.text,'utf-8'))
        to_send = fx_response.text.strip().split(',')
        v1 = int(to_send[1])
        hr = v1 // 3600 // 1000
        v2 = (to_send[2])
        v3 = to_send[3]
        v4 = float(v2 + v3)
	
        json_dict = {'hour': hr, 'time':v1, 'prev_time':last_ts, 'price':v4, 'prev_price': last_price, 'const':1}
        json_dump = json.dumps(json_dict)
        if v1 != last_ts :
                if last_ts != 0:
                    producer.send('FX_test', bytes(json_dump, 'utf-8'))
        #        print(last_ts)
                last_ts = v1
                last_price = v4
        time.sleep(.1)
