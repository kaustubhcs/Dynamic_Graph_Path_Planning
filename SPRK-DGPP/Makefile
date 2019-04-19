# Makefile for Spark WordCount project.

# Customize these paths for your environment.
# -----------------------------------------------------------
spark.root=${SPARK_HOME}
hadoop.root=${HADOOP_HOME}
app.name=Dynamic Path Planning
jar.name=spark-dpp-1.0.jar
jar.path=target/${jar.name}
job.name=pr.node_wise_path_plan
local.input=input
# local.input=../../../data/Twitter-dataset/data
local.master=local[4]
local.output=output
local.log=log
# Pseudo-Cluster Execution
hdfs.user.name=visheshhemnani
hdfs.input=input
hdfs.output=output
# AWS EMR Execution
aws.emr.release=emr-5.20.0
aws.region=us-east-1

aws.bucket.name=cs-6240-hemnaniv
aws.subnet.id=subnet-62e8916d
aws.input=input
aws.output=output
aws.log.dir=log
aws.num.nodes=5
aws.instance.type=m4.large
# update job version before running job
aws.project.name=DGPP-project
aws.project.version=v1-nodewise-5-clusters-n-200
local.aws.output=aws

graph.dimension=200
start.vertex =1
end.vertex =40000
# [II]. Basic statistics
# Number of Nodes: 11,316,811
# Number of Edges: 85,331,846
# -----------------------------------------------------------

# Compiles code and builds jar (with dependencies).
jar:
	mvn clean package
	# cp ${jar.path} ${jar.name}

# Removes local output directory.
clean-local-output:
	rm -rf ${local.output}/local
	rm -rf ${local.output}/${aws.project.version}

# Removes local log directory.
clean-local-log:
	rm -rf ${local.log}/${aws.project.version}

# Runs standalone
local: jar clean-local-output
	spark-submit --class ${job.name} --master ${local.master} --name "${app.name}" ${jar.path} ${local.input} ${local.output}/local ${graph.dimension} ${start.vertex} ${end.vertex}

# Start HDFS
start-hdfs:
	${hadoop.root}/sbin/start-dfs.sh

# Stop HDFS
stop-hdfs: 
	${hadoop.root}/sbin/stop-dfs.sh
	
# Start YARN
start-yarn: stop-yarn
	${hadoop.root}/sbin/start-yarn.sh

# Stop YARN
stop-yarn:
	${hadoop.root}/sbin/stop-yarn.sh

# Reformats & initializes HDFS.
format-hdfs: stop-hdfs
	rm -rf /tmp/hadoop*
	${hadoop.root}/bin/hdfs namenode -format

# Initializes user & input directories of HDFS.	
init-hdfs: start-hdfs
	${hadoop.root}/bin/hdfs dfs -rm -r -f /user
	${hadoop.root}/bin/hdfs dfs -mkdir /user
	${hadoop.root}/bin/hdfs dfs -mkdir /user/${hdfs.user.name}
	${hadoop.root}/bin/hdfs dfs -mkdir /user/${hdfs.user.name}/${hdfs.input}

# Load data to HDFS
upload-input-hdfs: start-hdfs
	${hadoop.root}/bin/hdfs dfs -put ${local.input}/* /user/${hdfs.user.name}/${hdfs.input}

# Removes hdfs output directory.
clean-hdfs-output:
	${hadoop.root}/bin/hdfs dfs -rm -r -f ${hdfs.output}*

# Download output from HDFS to local.
download-output-hdfs:
	mkdir ${local.output}
	${hadoop.root}/bin/hdfs dfs -get ${hdfs.output}/* ${local.output}

# Runs pseudo-clustered (ALL). ONLY RUN THIS ONCE, THEN USE: make pseudoq
# Make sure Hadoop  is set up (in /etc/hadoop files) for pseudo-clustered operation (not standalone).
# https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html#Pseudo-Distributed_Operation
pseudo: jar stop-yarn format-hdfs init-hdfs upload-input-hdfs start-yarn clean-local-output 
	spark-submit --class ${job.name} --master yarn --deploy-mode cluster ${jar.path} ${hdfs.input} ${hdfs.output}
	make download-output-hdfs

# Runs pseudo-clustered (quickie).
pseudoq: jar clean-local-output clean-hdfs-output 
	spark-submit --class ${job.name} --master yarn --deploy-mode cluster ${jar.path} ${hdfs.input} ${hdfs.output}
	make download-output-hdfs

# Create S3 bucket.
make-bucket:
	aws s3 mb s3://${aws.bucket.name}

# Upload data to S3 input dir.
upload-input-aws: make-bucket
	aws s3 sync ${local.input} s3://${aws.bucket.name}/${aws.project.name}/${aws.input}
	
# Delete S3 output dir.
delete-output-aws:
	aws s3 rm s3://${aws.bucket.name}/ --recursive --exclude "*" --include "${aws.project.name}/${aws.project.version}/${aws.output}/*"

# Upload application to S3 bucket.
upload-app-aws:
	aws s3 cp ${jar.path} s3://${aws.bucket.name}/${aws.project.name}/${aws.project.version}/

# Main EMR launch.
aws: jar upload-app-aws delete-output-aws
	aws emr create-cluster \
		--name "${app.name} Spark Cluster Version ${aws.project.version}" \
		--release-label ${aws.emr.release} \
		--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
	    --applications Name=Hadoop Name=Spark \
		--steps Type=CUSTOM_JAR,Name="${app.name}",Jar="command-runner.jar",ActionOnFailure=TERMINATE_CLUSTER,Args=["spark-submit","--deploy-mode","cluster","--class","${job.name}","s3://${aws.bucket.name}/${aws.project.name}/${aws.project.version}/${jar.name}","s3://${aws.bucket.name}/${aws.project.name}/${aws.input}","s3://${aws.bucket.name}/${aws.project.name}/${aws.project.version}/${aws.output}","${graph.dimension}","${start.vertex}","${end.vertex}"] \
		--log-uri s3://${aws.bucket.name}/${aws.project.name}/${aws.project.version}/${aws.log.dir} \
		--use-default-roles \
		--enable-debugging \
		--ec2-attributes '{"SubnetId": "${aws.subnet.id}"}'\
		--auto-terminate
		
# Download output from S3.
download-output-aws: clean-local-output
	mkdir -p ${local.aws.output}
	aws s3 sync s3://${aws.bucket.name}/${aws.project.name}/${aws.project.version}/${aws.output} ${local.output}/${aws.project.version}

# Download log from S3.
download-log-aws: clean-local-log
	mkdir -p ${local.log}/${aws.project.version}
	aws s3 sync s3://${aws.bucket.name}/${aws.project.name}/${aws.project.version}/${aws.log.dir} ${local.log}/${aws.project.version}

# Change to standalone mode.
switch-standalone:
	cp config/standalone/*.xml ${hadoop.root}/etc/hadoop

# Change to pseudo-cluster mode.
switch-pseudo:
	cp config/pseudo/*.xml ${hadoop.root}/etc/hadoop

# Package for release.
distro:
	rm -f Spark-Demo.tar.gz
	rm -f Spark-Demo.zip
	rm -rf build
	mkdir -p build/deliv/Spark-Demo
	cp -r src build/deliv/Spark-Demo
	cp -r config build/deliv/Spark-Demo
	cp -r input build/deliv/Spark-Demo
	cp pom.xml build/deliv/Spark-Demo
	cp Makefile build/deliv/Spark-Demo
	cp README.txt build/deliv/Spark-Demo
	tar -czf Spark-Demo.tar.gz -C build/deliv Spark-Demo
	cd build/deliv && zip -rq ../../Spark-Demo.zip Spark-Demo
	
