import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.collection.mutable.ArrayBuffer
import com.datastax.spark.connector._
import scala.math.abs
import scala.math.signum
import scala.math.max

object price_data {

    def get_area(t1 : Int, t2 : Int, p1 : Float, p2 : Float, indexes : ArrayBuffer[Int], timestamps : ArrayBuffer[Int], prices : ArrayBuffer[Float]) : Float =  {
        // takes arrays of indexes, timestamps and prices
        var p1_ = p1
        var p2_ = p2
        var t1_ = t1
        var t2_ = t2
        // need to get the relValeus
        val relValues = ArrayBuffer.empty[Int]
        val relPrices = ArrayBuffer.empty[Float]
        var firstInd = 0;
        var lastInd = 0;
        val ll = timestamps.length -1
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
        val slope = (p2_ - p1_).toFloat / (t2_ - t1_).toFloat // need to convert to float
        var _p1 = p1_.toFloat
        var _p2 = p1_.toFloat
        var area = 0.0 // need to make to float
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
        return area.toFloat


    }
    def main(args: Array[String]){
        val conf = new SparkConf().setAppName("PriceDataExercise").set("spark.cassandra.connection.host", "52.26.195.153")
        val sc = new SparkContext(conf)
        val rdd = sc.cassandraTable("playground","anchor1")
        //println(rdd.first.get[BigInt]("ts"))
        val rdd2 = rdd.map(s => s.get[BigInt]("ts"))
        rdd2.toArray.foreach(println)
    }


}



