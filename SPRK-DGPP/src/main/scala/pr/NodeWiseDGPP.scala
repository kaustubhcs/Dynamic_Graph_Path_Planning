package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import scala.collection.mutable.Set
import scala.collection.mutable.LinkedHashSet

import org.apache.spark.sql.SparkSession


import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Queue

object node_wise_path_plan {


  def main(args: Array[String]) {
    // Declares
    val MATRIX_DIMENSION= 2
    val START_VERTEX = 3
    val END_VERTEX = 4
    val INF_DIST = 10000
    val KTB_info_stamp: String = "[\u001b[0;36;01mKTB\u001b[m] "
    val KTB_warn_stamp: String = "[\u001b[0;33;05;01mKTB\u001b[m] "
    val KTB_error_stamp: String = "[\u001b[0;31;05;01mKTB\u001b[m] "
    val longest_path = args(MATRIX_DIMENSION).toInt * args(MATRIX_DIMENSION).toInt / 2
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger



    val conf = new SparkConf().setAppName("NodeWisePathFinder")
    val sc = new SparkContext(conf)
    val exit_status = sc.longAccumulator("exit_status")
    val spark = SparkSession
      .builder
      .appName("NodeWisePathFinder")
      .getOrCreate()

    def getStartPoints(blocked_points: Set[Int]): ListBuffer[Int] = {
      val n = args(MATRIX_DIMENSION).toInt
      val pointList = new ListBuffer[Int]
      for (i <- 1 to n * n;
           if !blocked_points.contains(i)){
        pointList.+=(i)
      }
      return pointList
    }
    def generateGraph(blocked_points: Set[Int]): ListBuffer[(Int, Set[Int])] = {
      val n = args(MATRIX_DIMENSION).toInt
      val graphList = new ListBuffer[(Int, Set[Int])]
      for (i <- 1 to n * n;
           if !blocked_points.contains(i)) {

        var edgeList: Set[Int] =  Set()

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


    def sssp(x:Int, adj_list: Broadcast[Map[Int, Set[Int]]]): ListBuffer[(Int, Int, LinkedHashSet[Int], Int)] ={
      val visited: Set[Int] = Set()
      var current = x
      val queue: Queue[Int] = Queue()
      var paths: mutable.Map[Int, LinkedHashSet[Int]] = mutable.Map()
      queue.enqueue(current)
      while (queue.size > 0){
        current = queue.dequeue()
        visited.add(current)
        val curr_path : LinkedHashSet[Int] = paths.getOrElse(current, LinkedHashSet[Int](x))

        adj_list.value.get(current).get.map(
          a => if (!visited.contains(a))
          {
            paths.+=((a, curr_path.+(a)))
            queue.enqueue(a)

          })
      }

      val return_paths : ListBuffer[(Int, Int, LinkedHashSet[Int], Int)] = ListBuffer()
      for (path <- paths){
        return_paths.+=((x, path._1, path._2, path._2.size - 1))
      }
      return return_paths
    }

  

    val textFile = sc.textFile(args(0))
    val blocked_points :RDD[Int] = textFile.map(x => x.toInt)
    val block_point_set : Set[Int] = Set().++(blocked_points.collect())
    val gen_graph : ListBuffer[(Int, Set[Int])] = generateGraph(block_point_set)
    val broadcast_adj_list = sc.broadcast(gen_graph.toMap)
    val start_points : ListBuffer[Int] = getStartPoints(block_point_set)
    val start_point_rdd = sc.parallelize(start_points).flatMap(x => sssp(x, broadcast_adj_list))
    start_point_rdd.saveAsTextFile(args(1))

    

  }
}