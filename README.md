# Similar Series

### Real time Approximate Nearest Neighbor on EUR/USD data.
www.yygrechka.website

Similar Series is a proof of concept of a technology that allows traders and financial researchers to find points in time in the past when the price pattern was similar to what it is at this moment. The following technologies were used in the project:

* Kafka
* Spark and Spark Streaming
* Cassandra
* Flask with Javascript and Ajax

### High Level Overview

Similar series allows the user to issue real time nearest neighbor queries. The response is calculated in less than a second even though the historical data has over 200,000,000 events. This is possible due to a version of Locality Sensitive Hashing for general metric spaces. After performing some transformations, an interval of the time series data is hashed; the hash is subsequently used to locate similar time series in the past in near-constant time.

