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
import scala.util.Sorting
import scala.math.sqrt

object price_data {

// defining the area

    def get_area(timestamps1 : Array[Int], timestamps2 : Array[Int], prices1 : Array[Double], prices2 : Array[Double]) : Double = {
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
        println(args(0))
        val conf = new SparkConf().setAppName("PriceDataExercise").set("spark.cassandra.connection.host", "52.39.13.76")
        val sc = new SparkContext(conf)
        val cc = new CassandraSQLContext(sc)
   	    val prices = sc.cassandraTable("fx","batch_table").select("price").where("batch_id = " + args(0)).map(s => s.get[Double]("price")).toArray
	    val timestamps = sc.cassandraTable("fx","batch_table").select("ts").where("batch_id = " + args(0)).map(s => s.get[Long]("ts")).toArray

        val num_anchors = 15

        val anchorTS = new Array[Array[Int]](num_anchors)
        val anchorP = new Array[Array[Double]](num_anchors)
        for (i <- 0 to num_anchors - 1){
            anchorTS(i) = sc.cassandraTable("fx","anchor_table").select("ts").where("anchor = " + i.toString).map(s => s.get[Int]("ts")).toArray
            anchorP(i) = sc.cassandraTable("fx","anchor_table").select("price").where("anchor = " + i.toString).map(s => s.get[Double]("price")).toArray
        }
       

	

        //val bpriceArray = sc.broadcast(arrayHolder)
        val bprice = sc.broadcast(prices)
        val btime = sc.broadcast(timestamps)
        val banchorTS = sc.broadcast(anchorTS)
        val banchorP = sc.broadcast(anchorP)
        val bnanchors = sc.broadcast(num_anchors)
        
        def ff(aa : Int) : Array[(Double,Int)] ={
            val final_ = Array.fill[(Double,Int)](bnanchors.value)((0.0,0))
            val lastVal = btime.value(aa)
            var startInd = 0
            var iii = aa
            while (iii >= 0 && btime.value(iii) > lastVal - 600000){
                iii -= 1
            }

            if (iii > -1) {
                startInd = iii + 1
            } else {
                return final_
            }
            val slicedArrayTS = btime.value.slice(startInd, aa + 1)
            val slicedArrayP = bprice.value.slice(startInd, aa + 1)           

//            val mean_ = slicedArrayP.sum/slicedArrayP.length
//            val std_ = sqrt((slicedArrayP.map( _ - mean_).map(t => t*t).sum)/slicedArrayP.length)

            var slicedIntArrayTS = new Array[Int](slicedArrayTS.length)
            var start_value = slicedArrayP(0)

            for (ii <- 0 to slicedArrayTS.length -1){
                
                slicedIntArrayTS(ii) = (slicedArrayTS(ii) - slicedArrayTS(0)).toInt
                slicedArrayP(ii) = (slicedArrayP(ii) - start_value) // std_
            }
            for (ii <- 0 to bnanchors.value - 1){
                val a = get_area(slicedIntArrayTS, banchorTS.value(ii), slicedArrayP, banchorP.value(ii))
                final_(ii) = (a,ii)
            }
            Sorting.quickSort(final_)
            return final_
        }


       val rddTest = sc.parallelize(Range(0,btime.value.length-1))
       val permutationResults = rddTest.map( 
                    a =>{ val x = ff(a) 
                            (btime.value(a),x(0)._2,x(1)._2,x(2)._2,x(3)._2,x(4)._2,x(5)._2,x(6)._2,x(7)._2,x(8)._2,x(9)._2,x(10)._2,x(11)._2,x(12)._2,x(13)._2,x(14)._2)}

                ) 
        val distResults = rddTest.map( 
                    a =>{ val x = ff(a) 
                            (btime.value(a),x(0)._1,x(1)._1,x(2)._1,x(3)._1,x(4)._1,x(5)._1,x(6)._1,x(7)._1,x(8)._1,x(9)._1,x(10)._1,x(11)._1,x(12)._1,x(13)._1,x(14)._1)}

                ) 

        val debugResults =  rddTest.map(
            a => { val x = ff(a)
                    var y : Long = 0;
                    if (x(0)._1 > 10){
                        y = btime.value(a)
                    }
                    (y,x(0)._1) }

   )

       for (a <- 0 to bnanchors.value -1){
        permutationResults.saveToCassandra("fx","permutation_granularity_" + a.toString, SomeColumns("ts", "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10", "v11", "v12", "v13", "v14"))
       }

       permutationResults.saveToCassandra("fx","ts_to_permutation", SomeColumns("ts", "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10", "v11", "v12", "v13", "v14"))

       distResults.saveToCassandra("fx","ts_to_distance", SomeColumns("ts", "v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9", "v10", "v11", "v12", "v13", "v14"))

        debugResults.saveToCassandra("fx","debugger",SomeColumns("ts","max_d"))
            
/*
            for (ii <- 1 to btime.value.length-1){
                println(ii)
                val x = ff(ii)
                println((timestamps(ii), x(0)._2,x(1)._2,x(2)._2))
            } */
        }

}


