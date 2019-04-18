package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import scala.collection.mutable.Set
import scala.collection.mutable.LinkedHashSet

// $example off$
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

    // if (args.length != 2) {
    //   logger.error("Usage:\npr.PageRankMain <k> <output dir>")
    //   System.exit(1)
    // }




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
//
//    // ********************************************* Vertex | Adj_list | Distance to Start | Path to start | ACTIVATION_STATUS
//    // Int Vertex
//    // List[Int] Adj_list
//    // Int Distance to Start
//    // List[Int] Path to start
//    // String ACTIVATION_STATUS
//
//    def metadata_add(node_data:(Int, Set[Int])) : (Int,(Set[Int],Int,LinkedHashSet[Int],String)) = {
//      var activation_state = "INACTIVE"
//      var distance = INF_DIST
//
//      if (node_data._1 == args(START_VERTEX).toInt) {
//        activation_state = "ACTIVE"
//        distance = 0
//      }
//      val empty_list : LinkedHashSet[Int] = new LinkedHashSet()
//      return (node_data._1,(node_data._2,distance,empty_list,activation_state))
//    }

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
    start_point_rdd.collect().foreach(println)
    start_point_rdd.saveAsTextFile(args(1))
//     var final_graph = sc.parallelize(gen_graph).map( x => metadata_add(x))
//     var done_count_before_planning = 0l
//     var done_count_after_planning = 1l
//     var looping_counter = 0
//     for (i <- 1 until longest_path
//       if done_count_before_planning != done_count_after_planning) {

//       if (i % 10 == 0){
//         done_count_before_planning = final_graph.filter(x => x._2._4 == "DONE").count()
//       }


//       final_graph = final_graph.flatMap(path_map)
//       final_graph = final_graph.reduceByKey(path_reduce)
//       if (i % 10 == 0){
//         done_count_after_planning = final_graph.filter(x => x._2._4 == "DONE").count()
//       }
//       looping_counter += 1

//     }
//     logger.info( KTB_warn_stamp + "ran for "+ looping_counter + " iterations")
// //    final_graph.collect.foreach( x => logger.info(KTB_warn_stamp + x.toString()))
// //    final_graph.reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).collect.foreach( x => logger.info(KTB_warn_stamp + x.toString()))
//     final_graph.collect.foreach(x => logger.info(KTB_warn_stamp + x.toString()))
//     final_graph.saveAsTextFile(args(1))

    

  }
}