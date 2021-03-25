package search.sol

import tester.Tester

import scala.collection.mutable.HashMap

class IndexTestSuite {

//  object Index {
//    val testIndex = new Index("TestWiki.xml")
//
//    def idToTitles: mutable.HashMap[Int, String] = {
//      testIndex.idsToTitles
//    }
//  }

  def indexTester(t: Tester): Unit = {
    val testIndex = new Index("sol/search/sol/TestWiki.xml")

    val expectedHm1 = new HashMap[Int, String]
    expectedHm1.put(0, "PageA")
    expectedHm1.put(1, "PageB")
    expectedHm1.put(2, "PageC")
    expectedHm1.put(3, "PageD")
    // t.checkExpect(testIndex.makeIdsToTitlesHm, expectedHm1)

    val expectedHm2 = new HashMap[String, Int]
    expectedHm2.put("PageA", 0)
    expectedHm2.put( "PageB", 1)
    expectedHm2.put("PageC", 2)
    expectedHm2.put("PageD", 3)
    // t.checkExpect(testIndex.makeTitlesToIdsHm, expectedHm2)

//    val expectedHm3 = HashMap()
//    t.checkExpect(testIndex.mapWordsRelevance, expectedHm3)

//    val expectedHm4 = new HashMap[Int, Double]
//    expectedHm4.put(0, )
//    expectedHm4.put(1, )
//    expectedHm4.put(2, )
//    expectedHm4.put(3, )
//    t.checkExpect(testIndex.calcPageRank, expectedHm4)



  }




}

object IndexTestSuite extends App {
  Tester.run(new IndexTestSuite)
}