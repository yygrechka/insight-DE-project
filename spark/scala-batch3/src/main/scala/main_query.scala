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
import scala.math.sqrt
import org.apache.spark.sql.cassandra.CassandraSQLContext
import scala.collection.JavaConversions._
import scala.collection.mutable.{HashMap => MMap}
import scala.collection.mutable.HashSet
import scala.collection.mutable.TreeSet
import scala.util.control.Breaks.break
import scala.util.Sorting

object price_data {
        def get_area(timestamps1 : Array[Int], timestamps2 : Array[Int], prices1 : Array[Double], prices2 : Array[Double]) : Double = {
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

        var shorterTS = Array.empty[Int]
        var longerTS = Array.empty[Int]
        var shorterP = Array.empty[Double]
        var longerP = Array.empty[Double]

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

        var cts : Int = 0
        var cts_temp : Int = 0

        var nts1 = shorterTS(1)
        var nts2 = longerTS(1)
        var p1_temp = shorterP(1)
        var p2_temp = longerP(1)

        var cslope1 = p1_temp / nts1
        var cslope2 = p2_temp / nts2
        var prev_slope1 = 0.0
        var prev_slope2 = 0.0
        
        println(p1_temp)
        println(nts1)

        var indexLonger = 1
        val ll = shorterTS.length
        for (ii <- 0 to ll -2){
            prev_slope1 = cslope1
            cslope1 = (shorterP(ii+1) - shorterP(ii)) / (shorterTS(ii+1) - shorterTS(ii))
            while (longerTS(indexLonger) < shorterTS(ii + 1)){
                prev_slope2 = cslope2
                cslope2 = (longerP(indexLonger+1) - longerP(indexLonger)) / (longerTS(indexLonger+1) - longerTS(indexLonger))
                cts_temp = longerTS(indexLonger)
                p1_temp = p1 + cslope1 * (cts_temp - cts)
                p2_temp = longerP(indexLonger)
                if (signum(p1_temp - p2_temp) == signum(p1 - p2)){
                    area += .5 * (abs(p2_temp - p1_temp) + abs(p2 - p1)) * (cts_temp - cts)
                }else{
                    val midpoint = cts + abs(p1 - p2) / abs(cslope1 + prev_slope2)
                    area += .5 * (midpoint - cts) * abs(p1 - p2)
                    area += .5 * (cts_temp - midpoint) * abs(p1_temp - p2_temp)
                }
                p1 = p1_temp
                p2 = p2_temp
                cts = cts_temp
                indexLonger += 1
            }
            cts_temp = shorterTS(ii+1)
            p2_temp = p2 + cslope2 * (cts_temp - cts )
            p1_temp = shorterP(ii+1)
            if (signum(p1_temp - p2_temp) == signum(p1 - p2)){
                area += .5 * (abs(p2_temp - p1_temp) + abs(p2 - p1)) * (cts_temp - cts)
            }else{
                val midpoint = cts + abs(p1 - p2) / abs(cslope1 + cslope2)
                area += .5 * (midpoint - cts) * abs(p1 - p2)
                area += .5 * (cts_temp - midpoint) * abs(p1_temp - p2_temp)
            }
            cts = cts_temp;
            p1 = p1_temp
            p2 = p2_temp

        }

       return area / ( totalTime.toDouble ) * 1000

    }



    def main(args: Array[String]){
        val conf = new SparkConf().setAppName("PriceDataExercise").set("spark.cassandra.connection.host", "52.39.13.76")
        val sc = new SparkContext(conf)

        val num_anchors = 15
        val anchorTS = new Array[Array[Int]](num_anchors)
        val anchorP = new Array[Array[Double]](num_anchors)
        for (i <- 0 to num_anchors - 1){
            anchorTS(i) = sc.cassandraTable("fx","anchor_table").select("ts").where("anchor = " + i.toString).map(s => s.get[Int]("ts")).toArray
            anchorP(i) = sc.cassandraTable("fx","anchor_table").select("price").where("anchor = " + i.toString).map(s => s.get[Double]("price")).toArray
        }

//        val banchorTS = sc.broadcast(anchorTS)
//        val banchorP = sc.broadcast(anchorP)
//        val bnanchors = sc.broadcast(num_anchors)

        def ff(ts : Array[Int], prices: Array[Double]) : Array[(Double,Int)] ={
            val final_ = Array.fill[(Double,Int)](num_anchors)((0.0,0))

            for (ii <- 0 to num_anchors - 1){
                val a = get_area(ts, anchorTS(ii), prices, anchorP(ii))
                final_(ii) = (a,ii)
            }
            Sorting.quickSort(final_)
            return final_
        }



        while (true){
            val lastTS_ = sc.cassandraTable("fx","last_ts").select("ts").map(s => s.get[Long]("ts")).toArray
            val lastTS = lastTS_(0)
            val lastTS__ = lastTS - 600000
            val hour_ = lastTS / 3600000
//            println(hour_)
            val hour__ = hour_ - 1
            val RDD_last_1 = sc.cassandraTable("fx","source_table").select("ts","price").where("hour = " + hour_.toString).where("ts <= " + lastTS.toString).where("ts > " + lastTS__.toString) 
            val RDD_last_2 = sc.cassandraTable("fx","source_table").select("ts","price").where("hour = " + hour__.toString).where("ts > " + lastTS__.toString).where("ts <= " + lastTS.toString)
            val ts1 = RDD_last_1.map(s => s.get[Long]("ts")).toArray
            val ts2 = RDD_last_2.map(s => s.get[Long]("ts")).toArray
            val p1 = RDD_last_1.map(s => s.get[Double]("price")).toArray
            val p2 = RDD_last_2.map(s => s.get[Double]("price")).toArray
            val ts_combined = ts1 ++ ts2
            val ts_combined_int = new Array[Int](ts_combined.length)
            val p_combined = p1 ++ p2
            
      //      val mean_ = p_combined.sum/p_combined.length
      //      val std_ = sqrt((p_combined.map( _ - mean_).map(t => t*t).sum)/p_combined.length)
            val start_value = p_combined(0)

            for (ii <- 0 to ts_combined.length -1){
                val x = ts_combined(ii) - ts_combined(0)
                p_combined(ii) = (p_combined(ii) - start_value) // std_

                ts_combined_int(ii) = x.toInt 
            }
            
            val xx = ff(ts_combined_int, p_combined)
//            xx.foreach(println)
//            Thread.sleep(100000)

            val permutation = sc.parallelize(Array(ff(ts_combined_int, p_combined))).map(x => (1,lastTS,x(0)._2,x(1)._2,x(2)._2,x(3)._2,x(4)._2,x(5)._2,x(6)._2,x(7)._2,x(8)._2,x(9)._2,x(10)._2,x(11)._2,x(12)._2,x(13)._2,x(14)._2))
            permutation.saveToCassandra("fx","last_ts_permutation", SomeColumns("const", "ts", "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10", "v11", "v12", "v13", "v14"))


            val pp2 = permutation.map( x => (x._2,x._3,x._4,x._5,x._6,x._7,x._8,x._9,x._10,x._11,x._12,x._13,x._14,x._15,x._16,x._17))
            pp2.saveToCassandra("fx","ts_to_permutation", SomeColumns("ts", "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10", "v11", "v12", "v13", "v14"))


            Thread.sleep(1000)


        }
        
        
        
        
        
       

               /*while (true){
            Thread sleep(1000)
            println("hi")
        }*/
    }


}







