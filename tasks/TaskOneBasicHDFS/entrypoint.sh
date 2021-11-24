#!/bin/bash

sudo service ssh start

export PATH=$PATH:/home/hadoop/hadoop-3.3.1/bin
export PATH=$PATH:/home/hadoop/spark-3.1.2-bin-hadoop3.2/bin
export PATH=$PATH:/home/hadoop/apache-hive-2.3.9-bin/bin

hdfs namenode -format
bash /home/hadoop/hadoop-3.3.1/sbin/start-dfs.sh

hdfs dfs -mkdir /datasets

bash /home/hadoop/hadoop-3.3.1/sbin/start-yarn.sh

# initialize Hive
hadoop fs -mkdir /tmp
hadoop fs -mkdir -p /user/hive/warehouse
hadoop fs -chmod g+w /tmp
hadoop fs -chmod g+w /user/hive/warehouse
/home/hadoop/apache-hive-2.3.9-bin/bin/schematool -dbType "derby" -initSchema

# submit Spark app
spark-submit \
	--class TaskOneBasicHDFS \
	--master yarn \
	--queue dev \
	app/target/scala-2.12/task-one-basic-hdfs_2.12-1.0.jar

hive