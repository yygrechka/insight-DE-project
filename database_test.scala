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
import scala.collection.mutable.{HashMap => MMap}
import scala.collection.mutable.HashSet
import scala.collection.mutable.TreeSet
import scala.util.control.Breaks.break

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

            arrayHolder.append((a,indArray,tsArray,priceArray))
        }
        val anchorRDD = sc.parallelize(arrayHolder)
        var count = 0
        
        var hTree = new FXHashTree(3,9)
        while (true){
            var last_ts_rdd : java.lang.Long = cc.sql("SELECT * FROM playground.last_ts").map(a => a(1)).first.asInstanceOf[Long] - count // this is to test while new prices aren't being updated
            println(last_ts_rdd)
            val hour = last_ts_rdd / (1000*3600)
            val mintime = last_ts_rdd - 60000
            val start_stop_rows = cc.sql("SELECT prev_time, time, prev_price, price FROM playground.demo_week3 WHERE hour = " + hour.toString + " and time > " + mintime.toString + " and time <=" + last_ts_rdd ).map(a=> (a(0).asInstanceOf[Long], a(1).asInstanceOf[Long], a(2).asInstanceOf[Double], a(3).asInstanceOf[Double]))
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
/*            arrayHolder(0)._2.foreach(println)
            println("____________")
            arrayHolder(0)._3.foreach(println)
            println("____________")
            arrayHolder(0)._4.foreach(println)
            println("____________")
            */
            val b_ = normalized_rdd.collect()
           /* println(b_(0)._1)
            println(b_(0)._2)
            println(b_(0)._3)
            println(b_(0)._4) */
          /*  for (j <- 0 to 8){

                println(j)
                println(arrayHolder(j)._3.length)
            for ( i <- 0 to b_.length - 1){
            println(b_(i))
            println(get_area(b_(i)._1, b_(i)._2, b_(i)._3, b_(i)._4,arrayHolder(j)._2, arrayHolder(j)._3,arrayHolder(j)._4))
            println("---------------")
}
        }*/
            //normalized_rdd.map(a => get_area(a._1,a._2,a._3,a._4,arrayHolder(0)._2,arrayHolder(0)._3,arrayHolder(0)._4)).collect().foreach(println)
            
            //normalized_rdd.collect.foreach(println)
            
            // the next three lines should represent the core functionality
            val cart_product = anchorRDD.cartesian(normalized_rdd)
            val sum_distance = cart_product.map(a => (a._1._1, get_area(a._2._1,a._2._2,a._2._3,a._2._4,a._1._2,a._1._3,a._1._4))).reduceByKey((a,b) => a + b).sortBy(_._2)
//            sum_distance.collect.foreach(println)
            
//            count += 1000
            //Thread sleep(1000)

            val permutation = sum_distance.map(a => a._1).toArray
            println(permutation.getClass)
            permutation.foreach(println)
            val dp = new DataPoint(first_ts,9)
            dp.permutation = permutation
            if (hTree.size > 1){
                println("nearest ts")
                val nn = hTree.get(dp)
                val dummyArray : Array[(Int,Long)]  = Array((1,nn))
                val dummyRDD = sc.parallelize(dummyArray)
                dummyRDD.saveToCassandra("playground","nn_ts")
                println("___________")
            }
            hTree.put(dp)
            
            hTree.print()
            
        }

               /*while (true){
            Thread sleep(1000)
            println("hi")
        }*/
    }


}


class DataPoint(val timestamp : Long, anchorNum : Int){
    var permutation = new Array[Int](anchorNum)
    // need to call fillPermutation
    //def fillPermulation()  {

    //}
}

class Node(level : Int){
    val hashValues = MMap.empty[Int,Node]
    var pointArray = ArrayBuffer.empty[DataPoint]
}

class AnchorContext(){

}

class FXHashTree(maxelem : Int, maxdepth: Int){
    val root = new Node(0)
    var size = 0
    var existing_ts = HashSet.empty[Long]

    def put(dp : DataPoint){
        if (existing_ts.contains(dp.timestamp)) {
            return
        }
        size += 1
        existing_ts += dp.timestamp
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
            cnode.hashValues(nextValue).pointArray += dp
            cnode.pointArray.foreach((ii : DataPoint) => _put(cnode,ii,level))
            cnode.pointArray = ArrayBuffer.empty[DataPoint]
        }
    }

    def get(dp : DataPoint) : Long = {
        var myTreeSet = TreeSet.empty[(Int,Int)]
        val md = maxdepth - 1
        for ( a <- 0 to md)  {
            myTreeSet += ((a,dp.permutation(a)))
        }
        return _get(root, myTreeSet, (0,0))
    }

    def _get(cnode : Node, currentTree : TreeSet[(Int,Int)],parentComb : (Int,Int)) : Long = {
        val hashSize = cnode.hashValues.size
        println(hashSize)
        if (hashSize > 0)  {
            var minV : (Int,Int) = (-1,-1)
            var found = false;
            for (vv <- currentTree){
                if (!found){
                    if (cnode.hashValues.contains(vv._2)){
                        minV = vv
                        found = true;
                    }
                }
            }
            currentTree -= minV     
            return _get(cnode.hashValues(minV._2), currentTree, minV)
        } else {
            println("****")
            println(parentComb._1)
            println(parentComb._2)
            println(cnode.pointArray.length)
            return cnode.pointArray(0).timestamp
        }
        


    }

    def print(){
        _print(root,0,0)
    }
    
    def _print(cnode: Node, level: Int, parent : Int){
        if (cnode.hashValues.size > 0){
            println(level)
            println(parent)
            println("***")
            cnode.hashValues.foreach( kv => _print(cnode.hashValues(kv._1), level + 1,kv._1))
        }else {
            println(level)
            println(parent)
            println(cnode.pointArray.length)
            println("-----")
        }
    }





}

