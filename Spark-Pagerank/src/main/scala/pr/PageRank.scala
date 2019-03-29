package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager

// $example off$
import org.apache.spark.sql.SparkSession


import scala.collection.mutable.ListBuffer

object PageRankMain {

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
    if (args.length != 2) {
      logger.error("Usage:\npr.PageRankMain <k> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Page Rank")
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder
      .appName("SparkPageRank")
      .getOrCreate()

    val graphList = getGraphs(args(0).toInt)
    var initial_graph = sc.parallelize(graphList)


    initial_graph = initial_graph.rightOuterJoin(initial_graph.map(_.swap))
      .flatMap(x=> if (x._2._1 == None) Seq((x._1, 0)) else Seq((x._2._2, x._1),(x._1, x._2._1.get)))

    val dummy_to_node = initial_graph.distinct().map(x => (0, x._1))

    initial_graph = initial_graph.union(dummy_to_node)
    val graph = initial_graph.groupByKey().cache()

    
    // var ranks = graph.map(x => if (x._1 != 0) (x._1,1.0) else (x._1,0.0))
    var ranks = graph.mapValues(x => 1.0)
    val ranks_count = ranks.count()-1

    val iters = 10

    for (i <- 1 to iters) {

      ranks = graph.join(ranks).values.flatMap{
        case (urls, rank) => urls.map(url => (url, rank / urls.size))
      }.reduceByKey(_ + _)

      // val dangling_mass = ranks.lookup(0).head/ranks_count  
      // ranks = ranks.map(x => if (x._1!=0) (x._1,0.15 + 0.85 * (x._2 + dangling_mass)) else (x._1,0.0))
      ranks = ranks.mapValues(x => 0.15 + 0.85 * x)
      // logger.info("Ranks at iterations: "+ s"$i")
      // logger.info(ranks.foreach(tup => println(s"${tup._1} has rank:  ${tup._2} .")))
    }
    
    val dangling_mass = ranks.lookup(0).head/ranks_count  
    ranks = ranks.map(x => if (x._1!=0) (x._1,x._2 + dangling_mass) else (x._1,0.0))
    ranks.saveAsTextFile(args(1))
    logger.info(ranks.toDebugString)
    logger.info(ranks.sortByKey().take(101).foreach(tup => println(s"${tup._1} has rank:  ${tup._2} ")))

  }
}