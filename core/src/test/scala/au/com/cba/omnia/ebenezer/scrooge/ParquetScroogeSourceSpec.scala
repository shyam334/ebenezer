package au.com.cba.omnia.ebenezer
package scrooge

import cascading.flow.FlowDef
import cascading.tuple.Fields
import com.twitter.scalding._
import com.twitter.scalding.TDsl._
import com.twitter.scrooge._

import au.com.cba.omnia.ebenezer.test._
import au.com.cba.omnia.thermometer.core._, Thermometer._
import au.com.cba.omnia.thermometer.tools._
import au.com.cba.omnia.thermometer.fact._, PathFactoids._

import org.apache.hadoop.mapred.JobConf

import scalaz.effect.IO

object ParquetScroogeSourceSpec extends ThermometerSpec { def is = s2"""

ParquetSource usage
===================

  Write to parquet                          $write
  Read from parquet                         $read
  Rebuild parquet with more data            $rebuild

"""
  val data = List(
    Customer("CUSTOMER-1", "Fred", "Bedrock", 40),
    Customer("CUSTOMER-2", "Wilma", "Bedrock", 40),
    Customer("CUSTOMER-3", "Barney", "Bedrock", 39),
    Customer("CUSTOMER-4", "BamBam", "Bedrock", 2)
  )

  val moar = List(
    Customer("CUSTOMER-5", "Homer", "Springfield", 40),
    Customer("CUSTOMER-6", "Marge", "Springfield", 40),
    Customer("CUSTOMER-7", "Flanders", "Springfield", 55)
  )

  def write =
    ThermometerSource(data)
      .write(ParquetScroogeSource[Customer]("customers"))
      .withFacts(
        "customers" </> "_SUCCESS"   ==> exists
      , "customers" </> "*.parquet"  ==> records(ParquetThermometerRecordReader[Customer], data)
      )

  def read = withDependency(write) {
    ParquetScroogeSource[Customer]("customers")
      .map(customer => (customer.id, customer.name, customer.address, customer.age))
      .write(TypedPsv("customers.psv"))
      .withFacts(
        "customers.psv" </> "_SUCCESS"   ==> exists
      , "customers.psv" </> "part-*"     ==> lines(data.map(customer =>
        List(customer.id, customer.name, customer.address, customer.age).mkString("|")))
      )
  }

  def rebuild = withDependency(write) {
    ThermometerSource(data ++ moar)
      .write(ParquetScroogeSource[Customer]("customers"))
      .withFacts(
        "customers" </> "_SUCCESS"   ==> exists
      , "customers" </> "*.parquet"  ==> records(ParquetThermometerRecordReader[Customer], data ++ moar)
      )
  }
}