package gov.irs.twe.factDictionary

import gov.irs.factgraph.types.*
import gov.irs.factgraph.FactDictionary
import gov.irs.factgraph.FactDictionaryForTests
import gov.irs.factgraph.Graph
import gov.irs.factgraph.Path
import gov.irs.twe.FileLoaderHelper

def setupFactDictionary(): FactDictionary =
  val facts = FileLoaderHelper.getAllFacts()
  val factDictionary = FactDictionaryForTests.fromXml(facts)
  factDictionary

def makeGraphWith(factDictionary: FactDictionary, facts: (Path, WritableType)*): Graph = {
  val graph = Graph(factDictionary)
  facts.foreach { case (path, value) =>
    graph.set(path, value)
  }
  graph
}
