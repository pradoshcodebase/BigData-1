package logic

import org.apache.log4j.{Logger, PropertyConfigurator}
import org.apache.spark.sql.SparkSession

object Bootstrap extends App  {

  // Only applicable if spark program runs on windows system
  System.setProperty("hadoop.home.dir", """C:\Work\winutil\""")

  val log = Logger.getLogger(Bootstrap.getClass)
  PropertyConfigurator.configure("src/main/resources/conf/log4j.properties")

   // SparkSession
  val sparkSession = SparkSession
    .builder()
    .appName("app-nyc-taxi-analyzer")
    .config("spark.master", "local[5]")
    .getOrCreate()

  log.info("Spark session object created successfully.")

  // Call the process method
  JobProcessor.process(sparkSession)
}
