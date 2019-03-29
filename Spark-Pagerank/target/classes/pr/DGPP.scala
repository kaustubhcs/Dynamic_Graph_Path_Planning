package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.rdd.RDD

// $example off$
import org.apache.spark.sql.SparkSession


import scala.collection.mutable.ListBuffer

object DGPP {
  
  def getGraphs(k: Int): ListBuffer[(Int, Int)] = {

      val graphList = new ListBuffer[(Int, Int)]
      for (i <- 1 until k * k) {

        if (i % k != 0)
        // graphList. += ((i, 0))
        //  else 
        graphList. += ((i, i + 1))
        // graphList.+=((0,i))
      }
      return graphList
    }

  def main(args: Array[String]) {
    
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
    val blocked_points :RDD[(Int, Int)] = textFile.map{x => val y = x.split(',')
                                                      (y(0).toInt , y(1).toInt)}

    
    blocked_points.collect().foreach(println)

    

  }
}