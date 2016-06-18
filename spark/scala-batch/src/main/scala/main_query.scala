import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
//import org.apache.spark.sql.SQLContext
import scala.collection.mutable.ArrayBuffer
import com.datastax.spark.connector._
import scala.math.abs
import scala.math.signum
import scala.math.max
import org.apache.spark.sql.cassandra.CassandraSQLContext
import scala.collection.JavaConversions._

object price_data {

    def get_area(t1 : Int, t2 : Int, p1 : Double, p2 : Double, indexes : Array[Int], timestamps : Array[Int], prices : Array[Double]) : Double =  {
        // takes arrays of indexes, timestamps and prices
        if (t1 == t2){
            return 0.0
        }
        var p1_ = p1
        var p2_ = p2
        var t1_ = t1
        var t2_ = t2
        // need to get the relValeus
        val relValues = ArrayBuffer.empty[Int]
        val relPrices = ArrayBuffer.empty[Double]
        var firstInd = 0;
        var lastInd = 0;
        val ll = timestamps.length -1
        if (timestamps(ll) <= t1){
            return 0.0 //hacky fix to a problem where the function has an out of bounds exception if the interval is completely outside
        }

        if (timestamps(ll) < t2_){
            p2_ = p2_ - (p2_ - p1_) / (t2_ - t1_) * (t2_ - timestamps(ll))
            t2_ = timestamps(ll)
        }
        val tt1 = t1_ / 1000
        val tt2 = t2_ / 1000
        if (timestamps(indexes(tt1)) > t1_){
            firstInd = indexes(tt1) - 1
        } else {
            var tempInd = indexes(tt1)
            while (timestamps(tempInd) <= t1_){
                tempInd += 1
            }
            firstInd = tempInd -1
        }
        if (timestamps(indexes(tt2)) > t2_){
            lastInd = indexes(tt2)
        }else {
            var tempInd = indexes(tt2)
            while (timestamps(tempInd) < t2_){
                tempInd += 1
            }
            lastInd = tempInd
        }
        for ( ii <- firstInd to lastInd){
            relValues.append(timestamps(ii))
            relPrices.append(prices(ii))
        }
        val lll = lastInd - firstInd
        relPrices(lll) = relPrices(lll) - (relPrices(lll) - relPrices(lll-1)) / (relValues(lll) - relValues(lll-1)) * (relValues(lll) - t2_)
        relValues(lll) = t2_
        relPrices(0) = relPrices(0) + (relPrices(1) - relPrices(0)) / (relValues(1) - relValues(0)) * (t1_ - relValues(0))
        relValues(0) = t1_
        val slope = (p2_ - p1_).toDouble / (t2_ - t1_).toDouble // need to convert to double
        var _p1 = p1_.toDouble
        var _p2 = p1_.toDouble
        var area = 0.0 // need to make to double
        for (ii <- 0 to lll-1){
            val pp1 = relPrices(ii)
            val pp2 = relPrices(ii+1)
            _p1 = _p2
            _p2 = _p1 + (relValues(ii+1) - relValues(ii)) * slope
            if (signum(pp1 - _p1) == signum(pp2 - _p2)){
                area += .5 * (abs(pp1 - _p1) + abs(pp2 - _p2)) * (relValues(ii+1) - relValues(ii))
            } else{
                val slope2 = (pp2 - pp1) / (relValues(ii+1) - relValues(ii))
                val ts_intersect = abs(pp1 - _p1) / (abs(slope) + abs(slope2)) + relValues(ii)
                area += .5 * abs(pp1 - _p1) * (ts_intersect - relValues(ii))
                area += .5 * abs(pp2 - _p2) * (relValues(ii+1) - ts_intersect)
            }

        }
        return area


    }
    def main(args: Array[String]){
        val conf = new SparkConf().setAppName("PriceDataExercise").set("spark.cassandra.connection.host", "52.26.195.153")
        val sc = new SparkContext(conf)
        val cc = new CassandraSQLContext(sc)
//        val sqlContext = SQLContext(sc)
        var arrayHolder = ArrayBuffer.empty[(Int,Array[Int], Array[Int], Array[Double])]
        for (a <- 1 to 9) {
            
        
            val rdd = sc.cassandraTable("playground","anchor" + a.toString)
            
            //println(rdd.first.get[BigInt]("ts"))
            

            val tsArray = rdd.map(s => s.get[Int]("ts")).toArray
            val indArray = rdd.map(s => s.get[Int]("index_")).toArray
            val priceArray = rdd.map(s => s.get[Double]("price")).toArray
        //    priceArray.foreach(println)
        //    println(get_area(0,1000,0,.0001,indArray,tsArray,priceArray))

            arrayHolder.append((a,tsArray,indArray,priceArray))
        }
        val anchorRDD = sc.parallelize(arrayHolder)
        while (true){
            var last_ts_rdd : java.lang.Long = cc.sql("SELECT * FROM playground.last_ts").map(a => a(1)).first.asInstanceOf[Long]
            println(last_ts_rdd)
            val hour = last_ts_rdd / (1000*3600)
            val mintime = last_ts_rdd - 60000
            val start_stop_rows = cc.sql("SELECT prev_time, time, prev_price, price FROM playground.demo_week3 WHERE hour = " + hour.toString + " and time > " + mintime.toString ).map(a=> (a(0).asInstanceOf[Long], a(1).asInstanceOf[Long], a(2).asInstanceOf[Double], a(3).asInstanceOf[Double]))
            val first_ts = start_stop_rows.map(a => a._1).first
            val first_price = start_stop_rows.map(a => a._3).first
            println(first_price)
            val normalized_rdd = start_stop_rows.map(a => ((a._1 - first_ts).asInstanceOf[Int], (a._2 - first_ts).asInstanceOf[Int], a._3 - first_price, a._4 - first_price))
            //val k = normalized_rdd.collect
            //for (aa <- 0 to k.length-1){
             //   println(k.length-1)
                 //println(aa)
                 //println(k(aa))
                 //arrayHolder(0)._2.foreach(println)
                 //println(arrayHolder(0)._3)
                 //println(arrayHolder(0)._4)
             //    println(get_area(k(aa)._1,k(aa)._2,k(aa)._3,k(aa)._4,arrayHolder(0)._3,arrayHolder(0)._2,arrayHolder(0)._4))
            //}
            //normalized_rdd.map(a => get_area(a._1,a._2,a._3,a._4,arrayHolder(0)._2,arrayHolder(0)._3,arrayHolder(0)._4)).collect().foreach(println)
            val cart_product = anchorRDD.cartesian(normalized_rdd)
//            cart_product.collect.foreach(println)
            val sum_distance = cart_product.map(a => (a._1._1, get_area(a._2._1,a._2._2,a._2._3,a._2._4,a._1._2,a._1._3,a._1._4))).reduceByKey((a,b) => a + b)
            sum_distance.collect.foreach(println)

           // start_stop_rows.toArray.foreach(println)
            Thread sleep(1000)

            
        }

               /*while (true){
            Thread sleep(1000)
            println("hi")
        }*/
    }


}



