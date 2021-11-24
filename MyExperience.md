# My experience

Starting this assessment I first wanted to make sure I had a place to write down all the
new things I'd learn, installation steps, places I'd get stuck on and solutions I found out.
For this I created a new NoteBook in [evernote](https://evernote.com/). I can share this NoteBook if need be.

## First task

First I made a summary for myself as to which tools I was supposed to be using for this
specific task. In this case this would be the following:
1. Hadoop
2. Spark
3. Hive

Because I've never worked with these individual tools, and neither as a collection, I thought  
it best to start by spending my first hours researching and looking for good tutorials. This  
meant at least going through the *Getting Started* guides:
* [Hadoop](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html)
* [Spark](https://spark.apache.org/docs/latest/sql-getting-started.html)
* [Hive](https://cwiki.apache.org/confluence/display/Hive/GettingStarted)

### Setting up Spark, Hadoop and Hive in a virtual machine

I figured just downloading a docker-compose.yaml file from some [repository](https://github.com/big-data-europe/docker-hadoop-spark-workbench) on github would not
really help me understand how these applications interact and how they're individualy configured.
So I figured I'd get down and dirty and go through the whole installation and configuration process
myself inside of a virtual machine.

I found a really good list of tutorials installing a Single Node HDFS Cluster on debian with Spark:
[![Hadoop Spark Playlist](https://img.youtube.com/vi/hRtInGQhBxs/0.jpg)](https://www.youtube.com/watch?v=hRtInGQhBxs&list=PLJlKGwy-7Ac6ASmzZPjonzYsV4vPELf0x)

So I downloaded a debian 11 .iso file from the debian project website and set up a VM using Hyper-V
on my Windows 10 workstation. I created a new **hadoop** user and made sure to do all the following
as this user (`su - hadoop`):
1. Get the latest version of hadoop from https://spark.apache.org/downloads.html
2. Get a compatible version of spark from https://spark.apache.org/downloads.html 
3. Get the latest version (`hive-3.1.2`) of hive from https://dlcdn.apache.org/hive/
4. Configure the tools using their provided `*.template` or `*-site.xml` files.

**jdk version for hive**

At first I installed the latest openjdk version (`openjdk-11`) but when trying to start
up `beeline -u jdbc:hive2://` it gave me an exception stating it couldn't create the
`$AppClassLoader` class.

On [stack overflow](https://stackoverflow.com/questions/54037773/hive-exception-class-jdk-internal-loader-classloadersappclassloader-cannot/54837096) I pretty quickly found what was wrong. I installed `openjdk-1.8`, set it as default
using `update-alternatives --config java` and changed my JAVA_HOME environment variable in ~/.bashrc.

**Derby metabase_db java version**

To initialize Hive I used `schematool -dbType "derby" -initSchema`. This created a derby
metadata database on my local filesystem. Problem was that when I tried to use spark to
talk to it, it give me an exception stating `the database was created by or upgraded by version 10.14`.

- [stack overflow](https://stackoverflow.com/questions/31169998/derby-version-mismatch-between-spark-and-hive-unable-to-instantiate-org-apache) gave me part of a solution.
- I had to force both spark and hive to use the same jar file and recreate the metastore_db
	- look in $HIVE_HOME/lib/derby*.jar and $SPARK_HOME/jars/derby*.jar
	- rm the oldest one and place the latest version of the jar in both
	- create a backup of the metastore_db folder so Hive recreates it mv $HOME/metastore_db $HOME/metastore_db_BCKP
	- `$HIVE_HOME/bin/schematool -dbType <db type> -initSchema` to recreate the database

This seemed to have solved the issue... For some time. It seemed like the metadata database
kept ending up corrupted so in the end I installed a previous hive version (`hive-2.3.9`)
which seemed to finally fix the issue with spark.

**Dependencies with sbt**

I'm very used to using configuration files that help you keep track of dependencies. Tools like `make`, `cmake`
or your `setup.py` for your python application. So this .sbt file should not be a problem... Right... Then
why doesn't this find my `hadoop-hdfs` dependency on the corresponding [maven repository](https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-hdfs/3.3.1) !?
```sbt
libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-sql" % "3.0.1",
	"org.apache.hadoop" %% "hadoop-hdfs" % "3.3.1"
)
```

After some (too much) time I figured out that `org.apache.hadoop.hadoop-hdfs.3.3.1` didn't actually have a scala
target which meant that the extra `%` in `%%` was adding a `_{SCALA_VERSION}` where this wasn't needed. Removing
this resolved the dependency and I could continue compiling my spark app.

**Creating a spark app using scala**

I've already written some Java code before so project structure wise it was not too hard. With scala, it was just
getting to know the syntax. When I wanted to do something to a spark dataframe, in my head I would compare it too
one of the `pandas` dataframes I'd use in python. I downloaded a sample [dataset](https://web.stanford.edu/class/archive/cs/cs109/cs109.1166/problem12.html) and tried stuff out on it.

For example, making sure the column names of the dataframe didn't have any SQL unfriendly characters in there.
1. I would use the `spark-shell --master yarn --queue dev --name interactive` command to open a interactive scala
shell. I would then load my sample dataset into a dataframe using:
```scala
val df = spark.read.format("csv").option("header", true).option("seperator", ",").load("{LOCAL_PATH_TO_FILE"})
```
2. I found a [stack overflow](https://stackoverflow.com/questions/35592917/renaming-column-names-of-a-dataframe-in-spark-scala) post explaining how I could change the column names.
```scala
val lookup = Map("_1" -> "foo", "_3" -> "bar")

df.select(df.columns.map(c => col(c).as(lookup.getOrElse(c, c))): _*)
```
3. So I figured I'd need some way to create this `lookUp` variable based on the incoming dataframe since I
wasn't going to write individual code for each specific .csv file that I had to parse. I found another [stack overflow](https://stackoverflow.com/questions/2189784/in-scala-is-there-a-way-to-take-convert-two-lists-into-a-map)
post showing me how I could combine two `Array` types into the `Map` I needed:
```scala
val o = (listA zip listB).toMap
```
4. Based on the above I wrote the code that would replace unacceptable characters from the current columns
and create a new dataframe with valid column names:
```scala
val lookUp = (dfRaw.columns zip dfRaw.columns.map( x => x.replaceAll("[-_]", "_"))).toMap
val df = dfRaw.select(dfRaw.columns.map(c => col(c).as(lookUp.getOrElse(c, c))): _*)
```

**Creating tables in Hive**

The assignment stated that the Spark app should first upload the `.csv` files to HDFS. One of the tutorials already
talked about `.parquet` files and another [tutorial](https://www.youtube.com/watch?v=noFkYVkixPA&t=253s) showed that  you could create external tables on Hive using these files, which was the second requirement for the Spark app. Lastly,
when the tables had been created, I would have to run a SQL query that would build a new dataframe joining some of the
data together and aggregating it.

- I first found out that `spark.read` and `{DF}.write` could cover for reading the `.csv` file from HDFS and creating the `.parquet` file I need to create the table.
- I found some [snippets](https://www.edureka.co/community/52030/copy-file-from-local-to-hdfs-from-the-spark-job-in-yarn-mode) that showed that I could create a `FileSystem` object based on the `hadoopConfiguration` that was part of my `spark.sparkContext` and use a `copyFromLocalFile` method to
copy the `.csv` files to HDFS.
- In the same way I found a [stack overflow](https://stackoverflow.com/questions/38618981/create-hive-external-table-with-schema-in-spark) post that showed how to generate the SQL needed to create
the tables on Hive. The `HiveContext` I could create using my `spark.sparkContext`.

I only had to modify some of the code since my tables would contain other datatypes as well (`df.schema.foreach( x => println(x))`):
```scala
val schema = results.schema.map( x => x.name.concat(" ")
	.concat( x.dataType.toString() match {
		case "StringType" => "STRING"
	})).mkString(",")
```
to
```scala
val schema = df.schema.map( x => x.name.concat(" ")
	.concat( x.dataType.toString() match {
		case "IntegerType" => "INT"
		case "StringType" => "STRING"
		case "DoubleType" => "DOUBLE"
		}
	)).mkString(",")
```
- Lastly, writing the aggregation query wasn't too much work since I'm already quite familiar with SQL and `HiveContext.sql()` already returns a dataframe.

> **A note on some onfusion about where my data was actually going and {df}.saveAsTable()**   
> I kept being confused because spark would keep creating `spark-warehouse` folders all over the place and when
> starting up `beeline` and running `SHOW TABLES;` my tables wouldn't show up. But all my queries worked and I
> didn't get any errors so they're stored somewhere.
> I read this [article](https://towardsdatascience.com/3-ways-to-create-tables-with-apache-spark-32aed0f355ab)
> talking about certain types of Hive tables, which made things somewhat clearer. I'm not quite sure if, for
> this assignment I've actually stored my tables in the correct place so I've just created, as far I understand
> it, a managed table with `{df}.saveAsTable` and an external table using `HiveContext.sql`
> 


**Dockerfile**

I felt like exporting my Hyper-V virtual machine and uploading that to github would not be much appreciated and
also not very practical for presenting if I also wanted to do the other tasks. That's why I thought to put everything needed to showcase each individual task into a docker container. I had already wrote down most of the installation steps I did throughout working in the Debian virtual machine so I could already create and test most of the Dockerfile in the first hour.

I did run into an issue with the HDFS UI which didn't seem to want to show up in my browser on port 9870. I found that [this guy](https://github.com/panovvv/hadoop-hive-spark-docker/blob/master/entrypoint.sh)
used a property `dfs.datanode.http.address`. By default it runs on `localhost`, which is internally. I wanted to reach it from the host machine so `0.0.0.0` would just accept everything from all IP addresses. I changed this to 0.0.0.0:9871 later since 9870 was already taken by UI and datanode wouldn't start (`> hadoop datanode` gave ) `java.   net.BindException: Problem binding to [0.0.0.0:9870] java.net.BindException: Address already in use; For more details see:  http://wiki.apache.org/hadoop/BindException`)`

## Second task

I've never worked with HBase before so I thought I'd read through the [Getting Started Guide](https://hbase.apache.org/book.html) which helped set me up on `Pseudo-Distrubted` mode. I built on top of the Dockerfile.TaskOne I'd already made for TaskOne but removed the `Hive` part. After the Guide I could use the `hbase shell` to starting testing stuff.

**Compatibility issue**

I quickly found out that there where some compatibility issues. I found this [matrix](https://hbase.apache.org/book.html#hadoop) that showed me that I my Hadoop version was not compatible with the HBase version I wanted to use so I downgraded to 3.2.2.

Then I ran into issues with my spark version. I got this [error](https://stackoverflow.com/questions/44387404/noclassdeffounderror-scala-productclass/44387752) and found out that the HBase libraries I wanted to use where compiled with scala 2.11 and that spark 3.1.2 used scala 2.12. This [post](https://stackoverflow.com/questions/56197221/which-scala-version-does-spark-2-4-3-uses) also suggested it.

So I downgraded my spark version to the latest version with scala 2.11. But now I got yarn throwing exceptions after
exception at me. I went down this road for some time but eventually went back to the scala versioning issue I had
with my spark 3.1.2. I found this [post](https://sparkbyexamples.com/hbase/spark-hbase-connectors-which-one-to-use/)
stating that the following dependencies should work:
- https://mvnrepository.com/artifact/org.apache.hbase/hbase-client/2.4.5
- https://mvnrepository.com/artifact/org.apache.hbase.connectors.spark/hbase-spark/1.0.0
- https://community.cloudera.com/t5/Support-Questions/Using-Spark-Hbase-Connector-on-CDH-6-3-2-Spark-2-4-HBase-2-1/td-p/302957

Unfortinately they also where not compiled with scala 2.12, which is where I got stuck ones again. Finally I found this [repository](https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Spark_HBase_Connector.md) where on `Spark 3.x and the Apache Spark HBase connector` he actually had the libraries I needed!

Now I could finally start hacking away in the `spark-shell` and try out the new classes I now had access to. I could use `spark-shell --master yarn --queue dev --name interactive --packages ch.cern.hbase.connectors.spark:hbase-spark:1.0.1_spark-3.0.1_4` to get the packages loaded into the shell and used [this](https://docs.cloudera.com/runtime/7.2.8/managing-hbase/topics/hbase-example-using-hbase-spark-connector.html ) as a reference.

**HBase tables, columns, column family and column qualifiers**

I found this awesome tutorial about CRUD in HBase:   
[![HBase CRUD](https://img.youtube.com/vi/3ZJ-2H6L9Hg/0.jpg)](https://www.youtube.com/watch?v=3ZJ-2H6L9Hg)

I used the `hbase shell` to try stuff out and learn how to use the `create`, `scan`, `get`, `put`, `disable`, `drop` commands. Unfortinately when I tried to use the code I wrote, and worked, from the spark-shell I would get a NullPointerException when I called `org.apache.spark.sql.DataFrame.write` I would get a NullPointerException. After lots
of searching I found the [solution](https://stackoverflow.com/questions/52372245/hbase-spark-load-data-raise-nullpointerexception-error-scala) which required me to create an `HBaseConfiguration` which solved the issue.
```scala
val hbaseConfig = new HBaseConfiguration(spark.sparkContext.hadoopConfiguration)
hbaseConfig.addResource(new Path("/home/hadoop/hbase-2.3.6/conf/hbase-site.xml"))
```

**Create a table `dangerous-driving` on HBase, load dangerous-driver.csv**

Now I had the code to create a table (which I got from [here](https://www.bogotobogo.com/Hadoop/BigData_hadoop_HBase_Table_with_Java_API.php)) and the [code](https://docs.cloudera.com/runtime/7.2.8/managing-hbase/topics/hbase-example-using-hbase-spark-connector.html) that would write the dataframe to the HBase table for me.
```scala
df.write.format("org.apache.hadoop.hbase.spark")
	.option("hbase.columns.mapping", mapping)
	.option("hbase.table", hbaseTableName)
	.save()
```

I reused most code from TaskOne to:
1. Copy the local files to HDFS using `org.apache.hadoop.fs.FileSystem.copyFromLocalFile`
2. read the `.csv` file from HDFS into a spark dataframe
3. The mapping to create the `hbase.columns.mapping`
```scala
val mapping = df.schema.map( x => x.name.concat(" STRING ")
	.concat( x.name match
	{
		case "eventId" => ":key"
		case default => hbaseTableColumnFamily + ":" + x.name
	})
).mkString(",")
```

**add a 4th element to the table from extra-driver.csv**

Why do I not have 4 rows in my table.... I'm not getting any errors.... Wait, let me check with `hbase shell` to check what the data in the table looks like compared to the original .csv files...

![Mister Bean](https://memegenerator.net/img/instances/53621195.jpg)

**update id = 4 to display routeName as Los Angeles to Santa Clara instead of Santa Clara to San Diego**

I found this [post](https://stackoverflow.com/questions/60126227/error-constructor-htable-in-class-htable-cannot-be-applied-to-given-types) pretty quickly explaining how to use `org.apache.hadoop.hbase.client.Put` and `org.apache.hadoop.hbase.client.Table` to update a specific column qualifier in a specific row. Testing it first from the `spark-shell` and then added it to the spark app.

**output to console the name of the driver, the type of event and the event time if the origin or destination is Los Angeles.**

I had already tried some filtering from `hbase shell` from looking at this [post](https://stackoverflow.com/questions/11013197/scan-htable-rows-for-specific-column-value-using-hbase-shell). Then I found out that you could load java class into the [shell](https://sparkbyexamples.com/hbase/hbase-table-filtering-data-like-where-clause/) and I got a bit of an idea as too how I was going to have to do this. I found this [example](https://www.javatips.net/api/org.apache.hadoop.hbase.filter.substringcomparator) showing the use of:
* BinaryComparator
* RegexStringComparator
* SubstringComparator

In the end I choose the `org.apache.hadoop.hbase.filter.RegexStringComparator` since this would best cover the search. The binary data stored in `routeName` would have to either start or end with "Los Angeles". I found this [regex](https://howtodoinjava.com/java/regex/start-end-of-string/) to serve the purpose and [tested](https://regex101.com/) it.

I did get a bit confused at the start because the example used a `RowFilter`, and my scanner gave me back nada. I found that the [example](https://hbase.apache.org/apidocs/org/apache/hadoop/hbase/filter/RegexStringComparator.html ) from the official `api-docs` used a `ValueFilter` instead. This finally gave me back one result. But there where more drivers that matched the searching criteria so there was still something going wrong.

this [example](https://www.programcreek.com/java-api-examples/?api=org.apache.hadoop.hbase.filter.SubstringComparator) also had a SingleColumnValueFilter class and for this they did not add a column using addColumn. Another [example](https://www.javatips.net/api/org.apache.hadoop.hbase.filter.regexstringcomparator) was the same. So I tried this and this did exactly what I needed it to do. The same example also had a loop where they iterated over the ResultScanner and used `getValue` to retrieve the individual values using the column family and qualifier. I modified this a bit and ended up with:
```scala
hbaseScanner.forEach( x => println(
	Bytes.toString(x.getValue(Bytes.toBytes("cf"), Bytes.toBytes("driverName"))) + " | " +
	Bytes.toString(x.getValue(Bytes.toBytes("cf"), Bytes.toBytes("eventType"))) + " | " +
	Bytes.toString(x.getValue(Bytes.toBytes("cf"), Bytes.toBytes("eventTime")))
))
```

## Overal impressions

Starting that monday I really did not know if I'd actually like the assignment and the type of tools / programming I would be doing. I had already made up my mind that I would spend at least one or two whole days and would only keep working on it from there on out if I enjoyed working on the assignment.

Now that I've spend the first monday night, tuesday night, wednesday, thursday, friday, a big part of both saterday and sunday and the monday night hacking away, I must admit that I've actually really enjoyed myself working with these tools.

My only little regret is starting in the virtual machine, knowing I could've saved a lot of time with docker. I might've even had time to work on the Kafka Ingestion task. Other then that, I definitely feel satisfied with the result. I've given it a 110% and learned LOADS of new stuff.
