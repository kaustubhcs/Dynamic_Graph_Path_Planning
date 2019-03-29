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
    // Declares
    val START_VERTEX = 3
    val END_VERTEX = 4
    val INF_DIST = 10000
    val KTB_info_stamp: String = "[\u001b[0;36;01mKTB\u001b[m] "
    val KTB_warn_stamp: String = "[\u001b[0;33;05;01mKTB\u001b[m] "
    val KTB_error_stamp: String = "[\u001b[0;31;05;01mKTB\u001b[m] "

    val conf = new SparkConf().setAppName("PathFinder")
    val sc = new SparkContext(conf)
    val exit_status = sc.longAccumulator("exit_status")

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

    // ********************************************* Vertex | Adj_list | Distance to Start | Path to start | ACTIVATION_STATUS
    // Int Vertex
    // List[Int] Adj_list
    // Int Distance to Start
    // List[Int] Path to start
    // String ACTIVATION_STATUS
    def metadata_add(node_data:(Int, ListBuffer[Int])) : (Int,(Seq[Int],Int,Set[Int],String)) = {
      var activation_state = "INACTIVE"
      var distance = INF_DIST



//      println(args(START_VERTEX));
//      println(args(END_VERTEX));
      if (node_data._1 == args(START_VERTEX).toInt) {
        activation_state = "ACTIVE"
        distance = 0
      }
      val empty_list : Set[Int] = Set()
      return (node_data._1,(node_data._2.toSeq,distance,empty_list,activation_state))
    }

    def path_map(node:(Int,(Seq[Int],Int,Set[Int],String))) : ListBuffer[(Int,(Seq[Int],Int,Set[Int],String))] = {

      val current_vertex = node._1
      val node_metadata = node._2
      val adj_list = node_metadata._1
      val distance = node_metadata._2
      var path2start = node_metadata._3
      var activation = node_metadata._4
      var next_iteration_list = new ListBuffer[(Int,(Seq[Int],Int,Set[Int],String))]
      if (activation == "ACTIVE") {
        for (neighbour_node <- adj_list) {
          val new_distance = distance + 1
          val new_status = "ACTIVE"
          if (neighbour_node == args(END_VERTEX).toInt) {
            exit_status.add(1)
          }
          path2start.+=(current_vertex)
          val output_node_data = (neighbour_node,(Seq(),new_distance,path2start,new_status))
          next_iteration_list.+=(output_node_data)
        }
        activation = "DONE"
      }
      next_iteration_list.+=((current_vertex,(adj_list,distance,path2start,activation)))
      return next_iteration_list
    }

    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    
    // if (args.length != 2) {
    //   logger.error("Usage:\npr.PageRankMain <k> <output dir>")
    //   System.exit(1)
    // }


    val spark = SparkSession
      .builder
      .appName("SparkPageRank")
      .getOrCreate()

    val textFile = sc.textFile(args(0))
    val blocked_points :RDD[Int] = textFile.map(x => x.toInt)

    val gen_graph = generateGraph(blocked_points.collect().toSet)
    val final_graph = sc.parallelize(gen_graph).map( x => metadata_add(x)).flatMap(path_map)

    final_graph.collect.foreach( x => logger.info(KTB_warn_stamp + x.toString()))


    

  }
}