import java.net.URI
import org.apache.spark.sql.hive._
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{SparkSession, SaveMode}
import org.apache.hadoop.fs.{FileSystem, Path}

object TaskOneBasicHDFS {

  def main(args: Array[String]) {

      val hdfsURI = "hdfs://localhost:9000/"
      val spark = SparkSession
        .builder()
        .appName("TaskOneBasicHDFS")
        .config("spark.sql.warehouse.dir", hdfsURI + "user/hive/warehouse") 
        .enableHiveSupport()
        .getOrCreate()
      val hiveContext = new HiveContext(spark.sparkContext)
      
      val fs = FileSystem.get(new URI(hdfsURI), spark.sparkContext.hadoopConfiguration)
      var files = Array(
        "drivers.csv",
        "timesheet.csv",
        "truck_event_text_partition.csv"
      )
      val dataPathLocal = "/home/hadoop/datasets/"
      val dataPathHDFS = hdfsURI + "datasets/"
      for ( fileName <- files ) {
        val pathRemote = new Path(dataPathHDFS + fileName)

        /* keep the src file but overwrite destination if exists */
        fs.copyFromLocalFile(false, true, new Path(dataPathLocal + fileName), pathRemote)

        val dfRaw = spark.read.format("csv")
          .option("header", true)
          .option("seperator", ",")
          .option("inferSchema", true)
          .load(pathRemote.toString())
      
        /* make sure the columns contain SQL friendly characters removing - or _ */
        val lookUp = (dfRaw.columns zip dfRaw.columns.map( x => x.replaceAll("[-_]", "_"))).toMap
        val df = dfRaw.select(dfRaw.columns.map(c => col(c).as(lookUp.getOrElse(c, c))): _*)

        /* create a managed table stored on HDFS */
        df.write.partitionBy("driverId")
            .mode(SaveMode.Overwrite)
            .saveAsTable("default.managed_" + fileName.dropRight(4))

        val pathRemoteParquet = dataPathHDFS + fileName.dropRight(4) + ".parquet" 
        df.write.mode(SaveMode.Overwrite).parquet(pathRemoteParquet)

        /* build the schema for the SQL statement to create the table */
        val schema = df.schema.map( x => x.name.concat(" ")
          .concat( x.dataType.toString() match {
            case "IntegerType" => "INT"
            case "StringType" => "STRING"
            case "DoubleType" => "DOUBLE"
          }
        )).mkString(",")

        /* create Hive table */
        hiveContext.sql("""
          CREATE EXTERNAL TABLE IF NOT EXISTS """ + fileName.dropRight(4) + """ (""" + schema + """)
          STORED AS PARQUET
          LOCATION '""" + pathRemoteParquet + """';"""
        )
      
        hiveContext.sql("SELECT * FROM " + fileName.dropRight(4) + " LIMIT 10;").show(10, false)
      }

      /* output a dataframe on spark that contains DRIVERID, NAME, HOURS_LOGGED, MILES_LOGGED */
      hiveContext.sql("""
        SELECT dr.driverId          AS DRIVERID
             , dr.name              AS NAME
             , SUM(ts.hours_logged) AS HOURS_LOGGED
             , SUM(ts.miles_logged) AS MILES_LOGGED
        FROM drivers dr
          INNER JOIN timesheet ts
            ON dr.driverId = ts.driverId
        GROUP BY dr.driverId, dr.name;
      """).show(false)

      spark.stop()
  }
}