import java.net.URI
import org.apache.spark.sql.SparkSession
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.functions.typedLit
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.TableExistsException
import org.apache.hadoop.hbase.client.{Table, Put, Scan}
import org.apache.hadoop.hbase.client.{HBaseAdmin, ConnectionFactory}
import org.apache.hadoop.hbase.filter.{SingleColumnValueFilter, RegexStringComparator, CompareFilter}
import org.apache.hadoop.hbase.{HBaseConfiguration, HColumnDescriptor, HTableDescriptor, TableName}

object TaskTwoHBase {

  def main(args: Array[String]) {

    val hdfsURI = "hdfs://localhost:9000/"
    val spark = SparkSession
        .builder()
        .appName("TaskOneBasicHDFS")
        .config("spark.hadoop.validateOutputSpecs", false)
        .getOrCreate()
    val hbaseConfig = new HBaseConfiguration(spark.sparkContext.hadoopConfiguration)
    hbaseConfig.addResource(new Path("/home/hadoop/hbase-2.3.6/conf/hbase-site.xml"))
    new HBaseContext(spark.sparkContext, hbaseConfig)
    val hbaseConn = ConnectionFactory.createConnection(hbaseConfig)
    val hbAdmin = hbaseConn.getAdmin

    /* create new HBase table with one column family adding all DF columns under it as qualifiers */
    val hbaseTableName = "dangerous-driver"
    val hbaseTableColumnFamily = "cf"
    val hbaseTableDescriptor = new HTableDescriptor(TableName.valueOf(hbaseTableName))
    hbaseTableDescriptor.addFamily( new HColumnDescriptor(hbaseTableColumnFamily));
    try {
        hbAdmin.createTable(hbaseTableDescriptor)
    }
    catch {
        case e: TableExistsException => println("Table " + hbaseTableName + " already exists, skipping creation")
    }

    val fs = FileSystem.get(new URI(hdfsURI), spark.sparkContext.hadoopConfiguration)
    var files = Array(
        "dangerous-driver.csv",
        "extra-driver.csv"
    )
    val dataPathLocal = "/home/hadoop/datasets/"
    val dataPathHDFS = hdfsURI + "datasets/"
    for ( fileName <- files ) {
        val pathRemote = new Path(dataPathHDFS + fileName)

        /* keep the src file but overwrite destination if exists */
        fs.copyFromLocalFile(false, true, new Path(dataPathLocal + fileName), pathRemote)

        var df = spark.read.format("csv")
          .option("header", true)
          .option("seperator", ",")
          .option("inferSchema", true)
          .load(pathRemote.toString())

        if ( fileName.dropRight(4) == "extra-driver") {
          df = df.withColumn("eventId", typedLit(4))
        }

        df.show(3, false)

        /* create column mapping and write DF data to HBase table */
        val mapping = df.schema.map( x => x.name.concat(" STRING ")
            .concat( x.name match
            {
                case "eventId" => ":key"
                case default => hbaseTableColumnFamily + ":" + x.name
            })
        ).mkString(",")
        df.write.format("org.apache.hadoop.hbase.spark")
            .option("hbase.columns.mapping", mapping)
            .option("hbase.table", hbaseTableName)
            .save()
    }

	/* update id = 4 to display routeName as Los Angeles to Santa Clara instead of Santa Clara to San Diego */
	val hbaseTable = hbaseConn.getTable(TableName.valueOf(hbaseTableName))
	val put = new Put(Bytes.toBytes("4"))
	put.addColumn(
		Bytes.toBytes(hbaseTableColumnFamily),		// column family
		Bytes.toBytes("routeName"),					// column family qualifier
		Bytes.toBytes("Los Angeles to Santa Clara")	// value to add / replace
	)
	hbaseTable.put(put)

	/*
	output to console the name of the driver, the type of event and
	the event time if the origin or destination is Los Angeles.
	*/
	val scan = new Scan()
	val filter = new SingleColumnValueFilter(
		Bytes.toBytes("cf"),						// column family
		Bytes.toBytes("routeName"),					// column family qualifier
		CompareFilter.CompareOp.EQUAL,
		// regex that matches if "Los Angeles" is at start or at end of string
		new RegexStringComparator("^Los Angeles|Los Angeles$")
	)
	scan.setFilter(filter)
	val hbaseScanner = hbaseTable.getScanner(scan)
	hbaseScanner.forEach( x => println(
		Bytes.toString(x.getValue(Bytes.toBytes("cf"), Bytes.toBytes("driverName"))) + " | " +
		Bytes.toString(x.getValue(Bytes.toBytes("cf"), Bytes.toBytes("eventType"))) + " | " +
		Bytes.toString(x.getValue(Bytes.toBytes("cf"), Bytes.toBytes("eventTime")))
	))
	
	hbaseTable.close()
	hbaseConn.close()
    spark.stop()
  }
}