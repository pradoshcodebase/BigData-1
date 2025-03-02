package commons

import logic.JobConfiguration._
import org.apache.log4j.Logger
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, when}

class WeatherDataReusableFunctions  (val sparkSession: SparkSession){

  val log = Logger.getLogger("WeatherDataReusableFunctions")
  val reusableFunctions = new ReusableFunctions(sparkSession)

  /**
   *
   * @param inputDF
   * @return
   */
  def performDQandAddColumns(inputDF: DataFrame): (DataFrame, DataFrame) = {

    // Replace T with negligible value (0.0001)
    val dfwithReplacedTvalues = replaceTwithNegligibleValues(inputDF, weatherDataColumns)

    // Typecast the dataframe columns
    val dfWithColumnsTypeCast = reusableFunctions.typecastColumns(dfwithReplacedTvalues, weatherDataColumns)

    // Data Quality check for trip data (Columns should not have negative value)
    val (successDFNegativeValueCheck, errorDFNegativeValueCheck) = reusableFunctions.filterRecordsHavingNegativeValue(dfWithColumnsTypeCast, weatherDataDQnegativeValueCheckColumns)

    // Data Quality check for columns to be compared with certain value or any column in dataframe
    val (successDFwithColumnsOrValueCompare, errorDFwithColumnsOrValueCompare) = reusableFunctions.dataframeColumnsCompare(successDFNegativeValueCheck, weatherDataDQcolumnsOrValueCompare)

    // Add additional columns
    val dfWithAdditionalColumns = addAdditionalColumns(successDFwithColumnsOrValueCompare)

    // Error dataframe
    val errorDf = errorDFNegativeValueCheck.union(errorDFwithColumnsOrValueCompare)

    (dfWithAdditionalColumns, errorDf)

  }

  /**
   *
   * @param inputDf
   * @param columnDetails
   * @return DataFrame
   */
  private def replaceTwithNegligibleValues(inputDf: DataFrame, columnDetails: List[(String, String, String)]): DataFrame = {
    columnDetails.foldLeft(inputDf) ((df, colDetail) => {

      // Get the column name and data type
      val colName = colDetail._1
      val colDataType = colDetail._2

      // Check if column exists in dataframe
      reusableFunctions.checkIfColumnsExistInDataFrame(inputDf, List(colName))

      // If decimal then only replace
      colDataType match {
        case "decimal" => df.withColumn(colName, when(col(colName).equalTo("T"), "0.0001").otherwise(col(colName)))
        case _ => df
      }
    })
  }


  /**
   *
   * @param inputDf
   * @return DataFrame
   */

  private def addAdditionalColumns(inputDf: DataFrame): DataFrame = addRainConditionColumn(addSnowDepthConditionColumn(addSnowFallConditionColumn(addTemperatureConditionColumn(inputDf))))

  /**
   *
   * @param inputDf
   * @return DataFrame
   */

  private def addTemperatureConditionColumn (inputDf: DataFrame): DataFrame = {

    // Check if COL_NAME_AVERAGE_TEMPARATURE exists in dataframe
    reusableFunctions.checkIfColumnsExistInDataFrame(inputDf, List(COL_NAME_AVERAGE_TEMPARATURE))

    inputDf.withColumn(COL_NAME_TEMPARATURE_CONDITION, when(col(COL_NAME_AVERAGE_TEMPARATURE) < 32, "verycold")
      .when(col(COL_NAME_AVERAGE_TEMPARATURE) >= 32 && col(COL_NAME_AVERAGE_TEMPARATURE) < 59, "cold")
      .when(col(COL_NAME_AVERAGE_TEMPARATURE) >= 59 && col(COL_NAME_AVERAGE_TEMPARATURE) < 77, "normal")
      .when(col(COL_NAME_AVERAGE_TEMPARATURE) >= 77 && col(COL_NAME_AVERAGE_TEMPARATURE) < 95, "hot")
      .otherwise("veryhot")
    )
  }

  /**
   *
   * @param inputDf
   * @return DataFrame
   */

  private def addSnowFallConditionColumn (inputDf: DataFrame): DataFrame = {

    // Check if COL_NAME_SNOWFALL exists in dataframe
    reusableFunctions.checkIfColumnsExistInDataFrame(inputDf, List(COL_NAME_SNOWFALL))

    inputDf.withColumn(COL_NAME_SNOWFALL_CONDITION, when(col(COL_NAME_SNOWFALL) <= 0.0001 , "nosnow")
      .when(col(COL_NAME_SNOWFALL) >= 0.0001 && col(COL_NAME_SNOWFALL) < 4, "moderate")
      .when(col(COL_NAME_SNOWFALL) >= 4 && col(COL_NAME_SNOWFALL) < 15, "heavy")
      .otherwise("violent")
    )
  }


  /**
   *
   * @param inputDf
   * @return DataFrame
   */

  private def addSnowDepthConditionColumn (inputDf: DataFrame): DataFrame = {

    // Check if COL_NAME_SNOWDEPTH exists in dataframe
    reusableFunctions.checkIfColumnsExistInDataFrame(inputDf, List(COL_NAME_SNOWDEPTH))

    inputDf.withColumn(COL_NAME_SNOWDEPTH_CONDITION, when(col(COL_NAME_SNOWDEPTH) <= 0.0001 , "nosnow")
      .when(col(COL_NAME_SNOWDEPTH) >= 0.0001 && col(COL_NAME_SNOWDEPTH) < 4, "moderate")
      .when(col(COL_NAME_SNOWDEPTH) >= 4 && col(COL_NAME_SNOWDEPTH) < 15, "heavy")
      .otherwise("violent")
    )
  }


  /**
   *
   * @param inputDf
   * @return DataFrame
   */

  private def addRainConditionColumn (inputDf: DataFrame): DataFrame = {

    // Check if COL_NAME_PRECIPITATION exists in dataframe
    reusableFunctions.checkIfColumnsExistInDataFrame(inputDf, List(COL_NAME_PRECIPITATION))

    inputDf.withColumn(COL_NAME_RAIN_CONDITION, when(col(COL_NAME_PRECIPITATION) <= 0 , "norain")
      .when(col(COL_NAME_PRECIPITATION) > 0 && col(COL_NAME_PRECIPITATION) < 0.3, "moderate")
      .when(col(COL_NAME_PRECIPITATION) >= 0.3 && col(COL_NAME_PRECIPITATION) < 2, "heavy")
      .otherwise("violent")
    )
  }


}
