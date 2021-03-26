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

    val bananaDocFreq = HashMap(1 -> 1.0, 3 -> 3.0)

    val orangDocFreq = HashMap(1 -> 3.0, 2 -> 2.0)

    val linkDocFreq =  HashMap(1 -> 1.0)

    val categoriDocFreq = HashMap(0 -> 1.0, 2 -> 1.0, 3 -> 1.0)

    val cerealDocFreq = HashMap(1 -> 1.0)

    val linktopage3DocFreq = HashMap(2 -> 1.0)

    val zeroDocFreq = HashMap(0 -> 1.0)

    val oneDocFreq = HashMap(1 -> 1.0)

    val twoDocFreq = HashMap(2 -> 1.0)
    val threeDocFreq = HashMap(3 -> 1.0)

    val page0DocFreq = HashMap(0 -> 1.0, 2 -> 1.0, 3 -> 1.0)

    val page1DocFreq = HashMap(0 -> 1.0, 1 -> 1.0)

    val page2DocFreq = HashMap(0 -> 1.0, 12-> 1.0)

    val page3DocFreq = HashMap(0 -> 2.0, 3 -> 1.0)

    expectedWdToDocFreq.put("banana", bananaDocFreq)
    expectedWdToDocFreq.put("orang", orangDocFreq)
    expectedWdToDocFreq.put("link", linkDocFreq)
    expectedWdToDocFreq.put("categori", categoriDocFreq)
    expectedWdToDocFreq.put("cereal", cerealDocFreq)
    expectedWdToDocFreq.put("linktopage3", linktopage3DocFreq)
    expectedWdToDocFreq.put("0", zeroDocFreq)
    expectedWdToDocFreq.put("1", oneDocFreq)
    expectedWdToDocFreq.put("2", twoDocFreq)
    expectedWdToDocFreq.put("3", threeDocFreq)
    expectedWdToDocFreq.put("page0", page0DocFreq)
    expectedWdToDocFreq.put("page1", page1DocFreq)
    expectedWdToDocFreq.put("page2", page2DocFreq)
    expectedWdToDocFreq.put("page3", page3DocFreq)

    t.checkExpect(testIndex.getWordsToDocFreq, expectedWdToDocFreq)
    // test element wise (test bananadocfreq)

    val expectedIdsToMaxCounts = new HashMap[Int, Double]
    expectedIdsToMaxCounts.put(0, 2.0)
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
  document in README ab visual inspection

  test calc and other functions in index
   */
}

object Main extends App {
  val testIndex = new Index("sol/search/sol/TestWiki.xml")
  Tester.run(IndexTestSuite)
}