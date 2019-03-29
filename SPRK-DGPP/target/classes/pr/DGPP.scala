package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.rdd.RDD

// $example off$
import org.apache.spark.sql.SparkSession


import scala.collection.mutable.ListBuffer

object path_plan {
  

  def main(args: Array[String]) {

    def generateGraph(blocked_points: Set[Int]): ListBuffer[(Int, ListBuffer[Int])] = {

      val n = args(2).toInt
      val graphList = new ListBuffer[(Int, ListBuffer[Int])]
      for (i <- 1 to n * n;
           if !blocked_points.contains(i)) {

        val edgeList = new ListBuffer[Int]

        if ((i - n) > 0  && !blocked_points.contains(i-n)){
          edgeList.+=(i-n)
        }

        if ((i - 1) % n != 0 && !blocked_points.contains(i-1)){
          edgeList.+=(i-1)
        }

        if ((i + 1) % n != 1 && !blocked_points.contains(i+1)){
          edgeList.+=(i+1)
        }

        if ((i + n)  <= n * n && !blocked_points.contains(i+ n)){
          edgeList.+=(i+n)
        }

        graphList.+=((i, edgeList))
      }
      return graphList
    }

    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    
    // if (args.length != 2) {
    //   logger.error("Usage:\npr.PageRankMain <k> <output dir>")
    //   System.exit(1)
    // }

    val conf = new SparkConf().setAppName("PathFinder")
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder
      .appName("SparkPageRank")
      .getOrCreate()

    val textFile = sc.textFile(args(0))
    val blocked_points :RDD[Int] = textFile.map(x => x.toInt)

    val gen_graph = generateGraph(blocked_points.collect().toSet)
    gen_graph.foreach(println)


    

  }
}