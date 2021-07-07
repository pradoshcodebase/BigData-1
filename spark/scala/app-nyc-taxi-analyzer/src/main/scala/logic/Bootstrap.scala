package logic

import commons.SparkConfigs
import org.apache.log4j.{Logger, PropertyConfigurator}
import org.apache.spark.sql.SparkSession

object Bootstrap extends App  {

  val log = Logger.getLogger(Bootstrap.getClass)
  PropertyConfigurator.configure("src/main/resources/conf/log4j.properties")

  // SparkSession
  val sparkSession = SparkSession
    .builder()
    .appName("app-nyc-taxi-analyzer")
    .config("spark.master", "local[5]")
    .getOrCreate()

  // Spark Context
  val sparkContext = sparkSession.sparkContext

  log.info("Spark session object created successfully.")


  JobProcessor.process(sparkSession)
}
