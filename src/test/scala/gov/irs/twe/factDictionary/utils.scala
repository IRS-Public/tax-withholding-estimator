package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.*
import gov.irs.factgraph.FactDictionary
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Graph
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper

def setupFactDictionary(): FactDictionary =
  val facts = FileLoaderHelper.getAllFacts()
  try {
    val factDictionary = FactDictionaryForTests.fromXml(facts)
    factDictionary
  } catch {
    case e: Exception =>
      println(s"💥 Exception during XML parsing: ${e.getClass.getSimpleName}")
      println(s"🚨 Error message: ${e.getMessage}")
      e.printStackTrace()
      throw e
  }

def makeGraphWith(factDictionary: FactDictionary, facts: (Path, WritableType)*): Graph = {
  val graph = Graph(factDictionary)
  facts.foreach { case (path, value) =>
    graph.set(path, value)
  }
  graph
}
