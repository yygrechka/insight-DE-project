import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import scala.collection.mutable.ArrayBuffer
import scala.math.abs
import scala.math.signum
import scala.math.max

object price_data {
    def main(args: Array[String]){
        val conf = new SparkConf().setAppName("PriceDataExercise")
        val sc = new SparkContext(conf)
        
    }

}



def get_area(t1 : Int, t2 : Int, p1 : Float, p2 : Float, indexes : ArrayBuffer[Int], timestamps : ArrayBuffer[Int], prices : ArrayBuffer[Float]) {
    
    // need to get the relValeus
    val relValues = ArrayBuffer.empty[Int]
    val relPrices = ArrayBuffer.empty[Float]
    val firstInd = 0;
    val lastInd = 0;
    val ll = relValues.length -1
    if (timestamps(ll) < t2){
        p2 = p2 - (p2 - p1) / (t2 - t1) * (t2 - timestamps(ll))
        t2 = timestamps(ll)
    }
    val tt1 = t1 / 1000
    val tt2 = t2 / 1000
    if (timestamps(indexes(tt1)) > t1){
        firstInd = indexes(tt1) - 1
    } else {
        val tempInd = indexes(tt1)
        while (timestamps(tempInd) < t1){
            tempInd += 1
        }
        firstInd = tempInd -1
    }
    if (timestamps(indexes(tt2)) > t2){
        lastInd = indexes(tt2)
    }else {
        val tempInd = indexes(tt2)
        while (timestamps(tempInd) < t2){
            tempInd += 1
        }
        lastInd = tempInd
    }
    for ( ii <- firstInd to lastInd){
        relValues += timestamps(ii)
        relPrices += prices(ii)
    }
    val lll = lastInd - firstInd 
    relPrices(lll) = relPrices(lll) - (relPrices(lll) - relPrices(lll-1)) / (relValues(lll) - relValues(lll-1)) * (relValues(lll) - t2)
    relValues(lll) = t2
    relPrices(0) = relPrices(0) + (relPrices(1) - relPrices(0)) / (relValues(1) - relValues(0)) * (t1 - relValues(0))
    relValues(0) = t1

    val slope = (p2 - p1) / (t2 - t1) // need to convert to float
    var _p1 = p1
    var _p2 = p1
    var area = 0 // need to make to float
    for (ii <- 0 to lll-1){
        val pp1 = relPrices(ii)
        val pp2 = relprices(ii+1)
        _p1 = _p2
        _p2 = _p1 + (relValues(ii+1) - relValues(ii)) * slope
        if (signum(pp1 - _p1) == signum(pp2 - _p2)){
            area += .5 * (abs(pp1 - _p1) + abs(pp2 - _p2)) * (relValues(ii+1) - relValues(ii))                         
        } else{
            val slope2 = (pp2 - pp1) / (relValues(ii+1) - relValues(ii))
            val ts_intersect = abs(pp1 - _p1) / (abs(slope) + abs(slope2)) + relValues(ii)
            area += .5 * abs(pp1 - _p1) * (ts_intersect - relValues(ii)
            area += .5 * abs(pp2 - _p2) * (relValues(ii+1) - ts_intersect)
        }

    }
    return area


}
