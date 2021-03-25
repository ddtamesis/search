package search.sol

import tester.Tester

import scala.collection.mutable.{HashMap, Set}

object IndexTestSuite {
  val testIndex = new Index("sol/search/sol/TestWiki.xml")

  /**
    * Tests buildTitleIdMaps by comparing to expected hashmaps
    *
    * @param t - tester
    */
  def testBuildTitleIdMaps(t: Tester): Unit = {
    val expectedIdsToTitles = new HashMap[Int, String]
    expectedIdsToTitles.put(0, "Category:Page0")
    expectedIdsToTitles.put(1, "Page1")
    expectedIdsToTitles.put(2, "Page2")
    expectedIdsToTitles.put(3, "Page3")
    t.checkExpect(testIndex.getIdsToTitles, expectedIdsToTitles)

    val expectedTitlesToId = new HashMap[String, Int]
    expectedTitlesToId.put("Category:Page0", 0)
    expectedTitlesToId.put("Page1", 1)
    expectedTitlesToId.put("Page2", 2)
    expectedTitlesToId.put("Page3", 3)
    t.checkExpect(testIndex.getTitlesToId, expectedTitlesToId)
  }

  def testBuildWordsLinksMaps(t: Tester): Unit = {
    val expectedWdToDocFreq = new HashMap[String, HashMap[Int,
      Double]]

    val expectedIdsToMaxCounts = new HashMap[Int, Double]
    expectedIdsToMaxCounts.put(0, 1.0)
    expectedIdsToMaxCounts.put(1, 3.0)
    expectedIdsToMaxCounts.put(2, 2.0)
    expectedIdsToMaxCounts.put(3, 3.0)
    t.checkExpect(testIndex.getIdsToMaxCounts, expectedIdsToMaxCounts)

    val expectedIdsToLinks = new HashMap[Int, Set[Int]]
    val aLinks: Set[Int] = Set()
    aLinks += 1
    aLinks += 2
    aLinks += 3
    val bLinks: Set[Int] = Set()
    val cLinks: Set[Int] = Set()
    cLinks += 0
    cLinks += 3
    val dLinks: Set[Int] = Set()
    dLinks += 0

    expectedIdsToLinks.put(0, aLinks)
    expectedIdsToLinks.put(1, bLinks)
    expectedIdsToLinks.put(2, cLinks)
    expectedIdsToLinks.put(3, dLinks)
    t.checkExpect(testIndex.getIdsToLinks, expectedIdsToLinks)
  }

  // test PageRank
//  System.out.println(testIndex.getIdsToPageRanks)

  /*
  val pageDocFreq = new HashMap[Int, Double]
  pageDocFreq.put(0, 1.0)
  val bDocFreq = new HashMap[Int, Double]
  bDocFreq.put(0, 1.0)
  val bodyDocFreq = new HashMap[Int, Double]
  bodyDocFreq.put(1, 1.0)
  bodyDocFreq.put(3, 1.0)
  val textDocFreq = new HashMap[Int, Double]
  bodyDocFreq.put(1, 1.0)
  bodyDocFreq.put(3, 1.0)
  val iDocFreq = new HashMap[Int, Double]
  bodyDocFreq.put(1, 3.0)
  val linkDocFreq = new HashMap[Int, Double]
  linkDocFreq.put(1, 4.0)
  val anythingDocFreq = new HashMap[Int, Double]
  anythingDocFreq.put(1, 1.0)

  expectedWdToDocFreq.put("body", bodyDocFreq)
  expectedWdToDocFreq.put("text!", textDocFreq)
   */
}

object Main extends App {
  val testIndex = new Index("sol/search/sol/TestWiki.xml")
  Tester.run(IndexTestSuite)
}