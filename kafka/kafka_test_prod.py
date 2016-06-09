from kafka import KafkaProducer
from kafka.errors import KafkaError
import time
import requests

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
while True:
        fx_response = requests.get(website + '?id=' + id_string)
        producer.send('FX_test', bytes(fx_response.text,'utf-8'))
        time.sleep(5)
