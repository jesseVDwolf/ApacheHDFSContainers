#!/bin/bash

sudo service ssh start

export PATH=$PATH:/home/hadoop/hadoop-$HADOOP_VERSION/bin
export PATH=$PATH:/home/hadoop/hbase-$HBASE_VERSION/bin
export PATH=$PATH:/home/hadoop/spark-$SPARK_VERSION-bin-hadoop3.2/bin

# initialize hadoop cluster
hdfs namenode -format
bash /home/hadoop/hadoop-$HADOOP_VERSION/sbin/start-dfs.sh

hdfs dfs -mkdir /datasets

bash /home/hadoop/hadoop-$HADOOP_VERSION/sbin/start-yarn.sh

# starts up the folowing daemons
# 1. HMaster
# 2. HRegionServer
# 3. ZooKeeper
echo -e "Host *\n\tStrictHostKeyChecking no" > ~/.ssh/config # fix for anoying (yes/no) question
bash hbase-$HBASE_VERSION/bin/start-hbase.sh

spark-submit \
	--class TaskTwoHBase \
	--master yarn \
	--queue dev  \
	--packages ch.cern.hbase.connectors.spark:hbase-spark:1.0.1_spark-3.0.1_4 \
	app/target/scala-2.12/task-two-hbase_2.12-1.0.jar

hbase shell
