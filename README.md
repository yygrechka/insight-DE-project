# Similar Series

### Real time Approximate Nearest Neighbor on EUR/USD data.
www.yygrechka.website

http://www.slideshare.net/YevgeniyGrechka/similarity-series

Similar Series is a proof of concept of a technology that allows traders and financial researchers to find points in time in the past when the price pattern was similar to what it is at this moment. The following technologies were used in the project:

* Kafka
* Spark and Spark Streaming
* Cassandra
* Flask with Javascript

### High Level Overview

Similar series allows the user to issue real time nearest neighbor queries. The response is calculated in less than a second even though the historical data has over 200,000,000 events. This is possible due to a version of Locality Sensitive Hashing for general metric spaces. After performing some transformations, an interval of the time series data is hashed; the hash is subsequently used to locate similar time series in the past in near-constant time.

### Website Layout

The website demonstrates the real-time approximate nearest neighbor technology. The top grath represents the query interval, or the latest 10 minutes of the EUR-USD prices. The folowing graph shows the nearest neighbor that was computed with my algorithm. The third graph shows the difference between the two above graphs. It highlights the area between them, which I use for my measure of distance.

<p align="center">
<img src="/images/website_layout.png" width="450"/>
</p>

### Relavent Metrics

I used the area between the curves as my distance metric. Although I considered some more esoteric metrics, I decided that the area-distance would work just fine to demonstrate the technology. Its other advantage is that it is very easy to visualize. 

The Second metric I used was a comparison of how the found distance compared to a distribution of distances between random 10-minute series intervals. This is a crude measure of how good the LSH method is at finding an approximate nearest neighbor, but it is sufficient for a proof of concept.

### LSH Idea

The idea for this specific flavor of LSH comes from the paper "On Locality-sensitive Indexing in Generic Metric Spaces" (Novak et al 2010). The idea is as follows: a set of 10-minute intervals is chosen from the data set; these will be called pivot points. In my case this set was chosen randomly, but there are better ways to make this selection. Then for a given 10-minute series interval I compute the distance to each one of the pivot points and order these distances in increasing order. This will define a permutation of the pivot points; and we subsequently search our collection of permutations which we have found for each 10-minute series interval in our data set and find the closest match.

<p align="center">
<img src="/images/LSH.png" width="450"/>
</p>

### Spark Batch Processing and Cassandra Tables Schema 

We will search for a similar permutation using a key-value store, going depth first into the nested key value store until we have found the most similar permutation. One of the main challenges that I faced is how to properly implement this key-value store. My first pass was to use an in-memory data structure which I implemented as a Scala class. This however has severe limitations, as I can't access the data structure from a web API query, and the structure will dissapear as soon as the process is terminated. 

My solution to both issues was to use Cassandra as a nested key-value store with the following schema:

I have a number of tables that is equal to the number of pivot points. Each table has the same columns, namely the timestamp of the data point which corresponds to the 10 minute price interval, as well as columns for each permutation integer of the pivot permutation. The difference between the tables is the following: the first table's primary key is the first number of the permutation, and each subsequent table adds an additional permutation number of the primary key. The effect of this is that each table defines a granularity level at which I can search in a depth first fassion to see if the desired permutation exists, and if it doesn't select the closest one that was already traversed. 

This design allowed me to do spark batch processing of the data very efficiently, as I did not have to worry about altering the cassandra rows in any way. I would simply compute the permutation for each data point and insert it into every single one of the N tables.

<p align="center">
<img src="/images/NestedTree.png" width="450"/>
</p>

### Data Pipeline

My pipeline uses Kafka to ingest data from www.truefx.com. This website provides both historical and real time foreign exchange bid/offer prices. The data is then put into Cassandra, and Spark is used to process the data as described above. The approximate nearest neighbor is then served up for the current price series using flask.

<p align="center">
<img src="/images/pipeline.png" width="450"/>
</p>
