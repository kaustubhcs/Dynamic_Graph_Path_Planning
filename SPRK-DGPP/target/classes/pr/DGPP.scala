package pr

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.spark.rdd.RDD

import scala.collection.mutable.Set
import scala.collection.mutable.LinkedHashSet

// $example off$
import org.apache.spark.sql.SparkSession


import scala.collection.mutable.ListBuffer

object path_plan {
  

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




    val conf = new SparkConf().setAppName("PathFinder")
    val sc = new SparkContext(conf)
    val exit_status = sc.longAccumulator("exit_status")
    val spark = SparkSession
      .builder
      .appName("SparkPageRank")
      .getOrCreate()

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

    // ********************************************* Vertex | Adj_list | Distance to Start | Path to start | ACTIVATION_STATUS
    // Int Vertex
    // List[Int] Adj_list
    // Int Distance to Start
    // List[Int] Path to start
    // String ACTIVATION_STATUS
    def metadata_add(node_data:(Int, Set[Int])) : (Int,(Set[Int],Int,LinkedHashSet[Int],String)) = {
      var activation_state = "INACTIVE"
      var distance = INF_DIST



//      println(args(START_VERTEX));
//      println(args(END_VERTEX));
      if (node_data._1 == args(START_VERTEX).toInt) {
        activation_state = "ACTIVE"
        distance = 0
      }
      val empty_list : LinkedHashSet[Int] = new LinkedHashSet()
      return (node_data._1,(node_data._2,distance,empty_list,activation_state))
    }

    def path_map(node:(Int,(Set[Int],Int,LinkedHashSet[Int],String))) : ListBuffer[(Int,(Set[Int],Int,LinkedHashSet[Int],String))] = {

      val current_vertex = node._1
      val node_metadata = node._2
      val adj_list = node_metadata._1
      val distance = node_metadata._2
      val path2start = node_metadata._3
      var path2start_new : LinkedHashSet[Int]=  new LinkedHashSet()
      path2start_new = path2start_new.++(path2start)
      path2start_new = path2start_new.+(current_vertex)
      //path2start_new.+=(current_vertex)//.+= (current_vertex)
      var activation = node_metadata._4
      var next_iteration_list = new ListBuffer[(Int,(Set[Int],Int,LinkedHashSet[Int],String))]
      if (activation == "ACTIVE") {
        for (neighbour_node <- adj_list) {
          val new_distance = distance + 1
          val new_status = "ACTIVE"
          if (neighbour_node == args(END_VERTEX).toInt) {
            exit_status.add(1)
          }
          val output_node_data = (neighbour_node,(Set(): Set[Int],new_distance,path2start_new,new_status))
          next_iteration_list.+=(output_node_data)
        }
        activation = "DONE"
      }
      next_iteration_list.+=((current_vertex,(adj_list,distance,path2start,activation)))
      return next_iteration_list
    }

    def path_reduce(node_metadata_1: (Set[Int],Int,LinkedHashSet[Int],String), node_metadata_2: (Set[Int],Int,LinkedHashSet[Int],String)) : (Set[Int],Int,LinkedHashSet[Int],String) = {


      val adj_list_1 = node_metadata_1._1
      val adj_list_2 = node_metadata_2._1

      val distance_1 = node_metadata_1._2
      val distance_2 = node_metadata_2._2

      val path2start_1 = node_metadata_1._3
      val path2start_2 = node_metadata_2._3

      val activation_1 = node_metadata_1._4
      val activation_2 = node_metadata_2._4
      var adj_list_final: Set[Int] = Set()
      var path2start_final:LinkedHashSet[Int] = new LinkedHashSet()
      var distance_final = INF_DIST
      var activation_final = "INACTIVE"

      if (adj_list_1.size > 0){
        adj_list_final = adj_list_final.++(adj_list_1)
      }

      if (adj_list_2.size > 0){
        adj_list_final = adj_list_final.++(adj_list_2)
      }
//      println(KTB_error_stamp + "OUT distance loop" + " " + distance_1 + " " +distance_2 + " " +distance_final)
      if (distance_final > distance_1){
//        println(KTB_error_stamp + "in distance loop")
        distance_final = distance_1
        path2start_final = LinkedHashSet()
        path2start_final = path2start_final.++(path2start_1)

      }

      if (distance_final > distance_2){
        distance_final = distance_2
        path2start_final = LinkedHashSet()
        path2start_final = path2start_final.++(path2start_2)
      }

      if (activation_1  == "DONE"|| activation_2 == "DONE"){
        activation_final = "DONE"
      } else if(activation_1  == "ACTIVE"|| activation_2 == "ACTIVE"){
        activation_final = "ACTIVE"
      }else if (activation_1  == "INACTIVE"|| activation_2 == "INACTIVE"){
        activation_final = "INACTIVE"
      }

  return (adj_list_final, distance_final, path2start_final, activation_final)
    }



    val textFile = sc.textFile(args(0))
    val blocked_points :RDD[Int] = textFile.map(x => x.toInt)
    val block_point_set : Set[Int] = Set().++(blocked_points.collect())
    val gen_graph = generateGraph(block_point_set)
    var final_graph = sc.parallelize(gen_graph).map( x => metadata_add(x))
    var done_count_before_planning = 0l
    var done_count_after_planning = 1l
    var looping_counter = 0
    for (i <- 1 until longest_path
      if done_count_before_planning != done_count_after_planning) {

      if (i % 10 == 0){
        done_count_before_planning = final_graph.filter(x => x._2._4 == "DONE").count()
      }


      final_graph = final_graph.flatMap(path_map)
      final_graph = final_graph.reduceByKey(path_reduce)
      if (i % 10 == 0){
        done_count_after_planning = final_graph.filter(x => x._2._4 == "DONE").count()
      }
      looping_counter += 1

    }
    logger.info( KTB_warn_stamp + "ran for "+ looping_counter + " iterations")
//    final_graph.collect.foreach( x => logger.info(KTB_warn_stamp + x.toString()))
//    final_graph.reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).reduceByKey(path_reduce).flatMap(path_map).collect.foreach( x => logger.info(KTB_warn_stamp + x.toString()))
    final_graph.collect.foreach(x => logger.info(KTB_warn_stamp + x.toString()))
    final_graph.saveAsTextFile(args(1))

    

  }
}