# Data Engineering test

The goal of this test is to evaluate your technical knowledge and open a path to a clear and concise technical interview. It is not going to be a whiteboard-let's-talk-about-binary-tree-data-sorting-task. It is all about the steps you took, why you did so and what was your line of thinking.

Remember, you already went through our first interview; it is a preparation for a mutual technical conversation.

---
## Where to run

I expect that you are capable of setting up a localdev with the tools of your choice. It is up to you to work with Hadoop ecosystem or replace it with cloud stack. Yep, we work with cloud a lot.

---
## Task 1. Basic HDFS & Hive (or their cloud substitutions) with Spark
> It is up to you to use a cloud object storage or create a Hadoop cluster with HDFS.

> Replace Hive with something similiar in the cloud, if you want. Hint: it should allow you to easily create tables from HDFS or object storage.

Build a Scala application using Spark and execute against Hive to do the following:
- upload the `.csv` files on <a href="data-spark/">`data-spark`</a> to HDFS
- create tables on Hive for each `.csv` file
- output a dataframe on Spark that contains `DRIVERID, NAME, HOURS_LOGGED, MILES_LOGGED` so you can have aggregated information about the driver.

Besides the code on a repo, explain you steps and impressions in [MyExperience.md](https://github.com/LINKIT-Group/de-test/blob/master/MyExperience.md).

## Task 2. HBase (or its cloud substitution)

> You are not limited to using HBase... change it to another column storage.

Build a Scala application using Spark to do the following:
- create a table `dangerous_driving` on HBase
- load <a href="data-hbase/dangerous-driver.csv">`dangerous-driver.csv`</a>
- add a 4th element to the table from `extra-driver.csv`
- update `id = 4` to display `routeName` as `Los Angeles to Santa Clara` instead of `Santa Clara to San Diego`
- output to console the name of the driver, the type of event and the event time if the origin or destination is `Los Angeles`.

Same thing here, besides the code on a repo, explain you steps and impression in [MyExperience.md](https://github.com/LINKIT-Group/de-test/blob/master/MyExperience.md).

## Task 3. Kafka Ingestion

Setup a Kafka cluster and create a third application that ingests any raw streams from Kafka with the following:
- Ingest `raw` stream into HDFS. How?
> Choose your preferred tool - Kafka (Streaming or Connect), Spark (Regular or Streaming), Flink, Storm, NiFi... it is up to you.
- Choose if you want to do batch or real-time streaming

Explain your steps in [MyExperience.md](https://github.com/LINKIT-Group/de-test/blob/master/MyExperience.md).

---
## Time limit
It is up to you on how much effort you want to spend on it. The deadline will be communicated via e-mail. Anything commited/requested/pushed after that date will not be considered.

----

## How do I submit it
* Create your branch and keep pushing your changes to this repo
* Describe your decisions in [MyExperience.md](https://github.com/LINKIT-Group/de-test/blob/master/MyExperience.md) so that anyone with a unix machine (Mac or Linux) could download the repo and run it.
* When it is done, let your LINKIT contact know about it via Pull Request

## Who do I talk to?
For any task related questions - your LINKIT contact [Alyona Galyeva](mailto:alyona.galyeva@linkit.nl).
