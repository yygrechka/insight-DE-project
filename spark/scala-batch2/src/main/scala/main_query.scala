import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
//import org.apache.spark.sql.SQLContext
import scala.collection.mutable.ArrayBuffer
import com.datastax.spark.connector._
import scala.math.abs
import scala.math.signum
import scala.math.max
import scala.math.min
import org.apache.spark.sql.cassandra.CassandraSQLContext
import scala.collection.JavaConversions._
import scala.collection.mutable.{HashMap => MMap}
import scala.collection.mutable.HashSet
import scala.collection.mutable.TreeSet
import scala.util.control.Breaks.break

object price_data {

    def get_area(timestamps1 : ArrayBuffer[Long], timestamps2 : ArrayBuffer[Long], prices1 : ArrayBuffer[Double], prices2 : ArrayBuffer[Double]) : Double = {
        // the first element is assumed to be 0
        val l1 = timestamps1.length
        val l2 = timestamps2.length
        if (l1 < 2 ){
            return 0.0
        }
        if (l2 < 2){
            return 0.0
        }
        val lastts1 = timestamps1(l1-1)
        val lastts2 = timestamps2(l2-1)
        val totalTime = min(lastts1, lastts2)
        
        var isFirstLonger = true
        if (lastts2 > lastts1){
            isFirstLonger = false
        }

        var area = 0.0
        
        var shorterTS = ArrayBuffer.empty[Long]
        var longerTS = ArrayBuffer.empty[Long]
        var shorterP = ArrayBuffer.empty[Double]
        var longerP = ArrayBuffer.empty[Double]

        if (isFirstLonger) {
            shorterTS = timestamps2
            longerTS = timestamps1
            shorterP = prices2
            longerP = prices1
        } else{
            shorterTS = timestamps1
            longerTS = timestamps2
            shorterP = prices1
            longerP = prices2
        }
        
        var ts1 = 0
        var ts2 = 0
        var p1 = 0.0
        var p2 = 0.0

        var cts : Long = 0
        var cts_temp : Long = 0

        var nts1 = shorterTS(1)
        var nts2 = longerTS(1)
        var p1_temp = shorterP(1)
        var p2_temp = longerP(1)

        var cslope1 = p1_temp / nts1
        var cslope2 = p2_temp / nts2

        var indexLonger = 1

        val ll = shorterTS.length
        for (ii <- 0 to ll -2){
            while (longerTS(indexLonger) < shorterTS(ii + 1)){
                // going from index 1 up
                cts_temp = longerTS(indexLonger)
                p1_temp = p1 + cslope1 * (cts_temp - cts)
                p2_temp = longerP(indexLonger)
                cslope2 = ( p2_temp - p2) / ( cts_temp - cts   )
                //println(cslope1)
                if (signum(p1_temp - p2_temp) == signum(p1 - p2)){
                    area += .5 * (abs(p1 - p1_temp) + abs(p2 - p2_temp)) * (cts_temp - cts)
                }else{
                    val midpoint = cts + abs(p1 - p2) / abs(cslope1 + cslope2)
                    area += .5 * (midpoint - cts) * abs(p1 - p2)
                    area += .5 * (cts_temp - midpoint) * abs(p1_temp - p2_temp)
                }
                p1 = p1_temp
                p2 = p2_temp
                cts = cts_temp
                indexLonger += 1
            }
            cslope2 = (longerP(indexLonger) - p2) / (longerTS(indexLonger) - cts)
            cts_temp = shorterTS(ii+1)
            p2_temp = p2 + cslope2 * (cts_temp - cts )
            p1_temp = shorterP(ii+1)
            cslope1 = (p1_temp - p1) / (cts_temp - cts)
            if (signum(p1_temp - p2_temp) == signum(p1 - p2)){
                area += .5 * (abs(p1 - p1_temp) + abs(p2 - p2_temp)) * (cts_temp - cts)
            }else{
                val midpoint = cts + abs(p1 - p2) / abs(cslope1 + cslope2)
                area += .5 * (midpoint - cts) * abs(p1 - p2)
                area += .5 * (cts_temp - midpoint) * abs(p1_temp - p2_temp)
            }

        }

       return area / ( totalTime.toDouble ) * 1000

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

            arrayHolder.append((a,indArray,tsArray,priceArray))
        }
        val anchorRDD = sc.parallelize(arrayHolder)
        var count = 0
        
        val test_rows = cc.sql("SELECT time, price FROM playground.demo_week3 WHERE hour = 407354")
        val prices = test_rows.map(a => a(1).asInstanceOf[Double]).toArray
        val timestamps = test_rows.map(a => a(0).asInstanceOf[Long]).toArray
        var arrayHolder = ArrayBuffer.empty[(Int,Array[Int], Array[Int], Array[Double])]
        
        for (a <- 1 to 9) {


            val rdd = sc.cassandraTable("playground","anchor" + a.toString)

            //println(rdd.first.get[BigInt]("ts"))


            val tsArray = rdd.map(s => s.get[Int]("ts")).toArray
            val indArray = rdd.map(s => s.get[Int]("index_")).toArray
            val priceArray = rdd.map(s => s.get[Double]("price")).toArray
        //    priceArray.foreach(println)
        //    println(get_area(0,1000,0,.0001,indArray,tsArray,priceArray))

            arrayHolder.append((a,indArray,tsArray,priceArray))
        }
        



        val bpriceArray = sc.broadcast(arrayHolder)
        val bprice = sc.broadcast(prices)
        val btime = sc.broadcast(timestamps)

        val mtestArray = Range(150,1800)
        val rddTest = sc.parallelize(mtestArray)
        
        def ff(aa : Int) : ArrayBuffer[(Double,Int)] ={
            val final_ = ArrayBuffer.empty[(Double,Int)]

            for (aaa <- range 1 to 9){

            val k = bprice.value.slice(aa-100,aa)
            var k_ =  ArrayBuffer.fill(100)(0.0)
            val k_0 = k(0)
            for (ii <- 0 to k_.length -1){
                k_(ii) = k(ii) - k_0
            }
            val kk = bprice.value.slice(51,151)
            var kk_ = ArrayBuffer.fill(100)(0.0)
            val kk_0 = kk(0)
            
            for (ii <- 0 to kk_.length - 1){
                kk_(ii) = kk(ii) - kk_0
            }


            val t = btime.value.slice(aa-100,aa)
            var t_ = ArrayBuffer.fill[Long](100)(0)
            val t_0 = t(0)
            for (ii <- 0 to kk_.length - 1){
                t_(ii) = t(ii) - t_0
            }
            val tt = btime.value.slice(51,151)

            var tt_ = ArrayBuffer.fill[Long](100)(0)
            val tt_0 = tt(0)
            for (ii <- 0 to tt_.length - 1){
                tt_(ii) = tt(ii) - tt_0
            }
            

            }
            return get_area(t_,tt_,k_,kk_)
        }


//        println(ff(150))

       rddTest.map( 
                    a => (a,ff(a))

                ).collect().foreach(println) 

            }


}


