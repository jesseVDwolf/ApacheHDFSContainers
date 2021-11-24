name := "Task Two HBase"

version := "1.0"

scalaVersion := "2.12.14"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-sql" % "3.0.1",
    "org.apache.hadoop" % "hadoop-hdfs" % "3.3.1",
    "ch.cern.hbase.connectors.spark" % "hbase-spark" % "1.0.1_spark-3.0.1_4"
)