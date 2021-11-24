name := "Task One Basic HDFS"

version := "1.0"

scalaVersion := "2.12.14"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-sql" % "3.0.1",
    "org.apache.spark" %% "spark-hive" % "3.0.1",
    "org.apache.hadoop" % "hadoop-hdfs" % "3.3.1"
)