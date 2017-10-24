/*
 * Copyright 2017 Barclays Africa Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.sample

import org.apache.spark.sql.SparkSession

object MareksJob {
  def main(args: Array[String]) {
    val spark = SparkSession.builder()
      .appName("Marek's Job")
      .config("spark.sql.shuffle.partitions", "4")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Initializing library to hook up to Apache Spark
    import za.co.absa.spline.core.SparkLineageInitializer._
    spark.enableLineageTracking()

    // A business logic of a spark job ...
    val input = spark.read.option("header", "true").csv("data/input/devIndicators.csv")

    val cleaned = input.select(
      $"Country Name" as "country_name",
      $"Country Code" as "country_code",
      $"Series Name" as "metric",
      $"2011 [YR2011]" as "amount"
    )

    val gdpPerCapital = cleaned.filter($"metric" === "GDP per capita (current US$)")

    val beerConsumtion = spark.read.parquet("data/results/beerConsCtl")

    val result = beerConsumtion
      .join(gdpPerCapital, $"country_code" === $"Code", "inner")
      .select($"country_name", $"Year2011" as "beer_consumption", $"amount" as "gdp_per_capital")
      .sort($"beer_consumption" desc)


    result.write.mode("overwrite").parquet("data/results/beerAndgdpPerCapital")
  }
}
