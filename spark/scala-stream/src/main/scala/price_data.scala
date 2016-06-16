import kafka.serializer.StringDecoder

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.sql._
import scala.collection.mutable.{HashMap => MMap}
import scala.collection.mutable.{ArrayBuffer => MBuffer}
import scala.util.control.Breaks.break


object PriceDataStreaming {
  def main(args: Array[String]) {
    val test = MMap.empty[Int,Int]
    val brokers = "localhost:9092"
    val topics = "FX_test"
    val topicsSet = topics.split(",").toSet

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("price_data")
    val ssc = new StreamingContext(sparkConf, Duration(200))

    // Create direct kafka stream with brokers and topics
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    // Get the lines and show results
    messages.foreachRDD{
	rdd => 
	val sqlContext = SQLContextSingleton.getInstance(rdd.sparkContext)
	val priceJson = rdd.map(_._2)	// why is it _2 and not _1
	import sqlContext.implicits._
	val df = sqlContext.jsonRDD(priceJson)
	df.show()
	//lines.collect().foreach(println)  
}
    //messages.show()
    /* messages.foreachRDD { rdd =>

        val sqlContext = SQLContextSingleton.getInstance(rdd.sparkContext)
        import sqlContext.implicits._

        val lines = rdd.map(_._2)
        val ticksDF = lines.map( x => {
                                  val tokens = x.split(";")
                                  Tick(tokens(0), tokens(2).toDouble, tokens(3).toInt)}).toDF()
        val ticks_per_source_DF = ticksDF.groupBy("source")
                                .agg("price" -> "avg", "volume" -> "sum")
                                .orderBy("source")

        ticks_per_source_DF.show()
    }*/

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}


class DataPoint(timestamp : Long, duration : Int, anchorNum : Int){
	var permutation = new Array[Int](anchorNum)
	// need to call fillPermutation
    def fillPermulation()  {

	}
}

class Node(level : Int){
    val hashValues = MMap.empty[Int,Node]
    var pointArray = MBuffer.empty[DataPoint]
}

class AnchorContext(){
    
}

class FXHashTree(maxelem : Int, maxdepth: Int){
    val root = new Node(0)
    
    def put(dp : DataPoint){
        this._put(root,dp,0)
    }

    def _put(cnode : Node, dp : DataPoint, level : Int) {
        if (level == maxdepth){
            cnode.pointArray += dp
            return
        }
        var nextValue = dp.permutation(level)
        val hashSize = cnode.hashValues.size
        if (hashSize > 0){
            if (cnode.hashValues.contains(nextValue)){
                _put(cnode.hashValues(nextValue), dp, level+1)
            } else {
                cnode.hashValues(nextValue) = new Node(level)
                cnode.hashValues(nextValue).pointArray += dp
            }
        } else if (cnode.pointArray.length < maxelem) {
            cnode.pointArray += dp
        } else {
            cnode.hashValues(nextValue) = new Node(level)
            cnode.pointArray.foreach((ii : DataPoint) => _put(cnode,ii,level))
            cnode.pointArray = MBuffer.empty[DataPoint]
        }
    }

}




case class Tick(source: String, price: Double, volume: Int)




/** Lazily instantiated singleton instance of SQLContext */


object SQLContextSingleton {

  @transient  private var instance: SQLContext = _

  def getInstance(sparkContext: SparkContext): SQLContext = {
    if (instance == null) {
      instance = new SQLContext(sparkContext)
    }
    instance
  }
}
