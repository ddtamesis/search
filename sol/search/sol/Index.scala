package search.sol

import search.src.PorterStemmer.stem
import search.src.StopWords.isStopWord
import tester.Tester

import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.util.matching.Regex
import scala.xml.{Node, NodeSeq}

/**
 * Provides an XML indexer, produces files for a querier
 *
 * @param inputFile - the filename of the XML wiki to be indexed
 */
class Index(val inputFile: String) {
  // access the main XML node
  private val mainNode: Node = xml.XML.loadFile(inputFile)
  // select all children w/ tag “page”
  private val pageSeq: NodeSeq = mainNode \ "page"
  // select all children w/ tag “title”
  private val titleSeq: NodeSeq = mainNode \ "page" \ "title"
  // select all children w/ tag “id”
  private val idSeq: NodeSeq = mainNode \ "page" \ "id"

  private val IDArray = idSeq.map(x => x.text.trim.toInt).toArray
  private var matchesLinksList = List[String]()

  private val wordsToDocumentFrequencies = new HashMap[String, HashMap[Int,
    Double]]
  private val idToMaxCounts = new HashMap[Int, Double]
  private val idsToTitles = new HashMap[Int, String]
  private val titlesToId = new HashMap[String, Int]
  private val idsToLinks = new HashMap[Int, Set[Int]]
  private val idsToPageRanks = new HashMap[Int, Double]

  this.buildWordMaps
  this.buildIdsToTitles
  this.buildTitlesToIds
  this.buildIDsToLinks
  this.buildIdsToPageRanks

  /*
  TA Questions:
  1) Should we do all the mapping in one master function at once so we only
  loop through pageSeq once? Or break it up into separate functions, looping
  through the pageSeq in each function for each mapping?
  2) Debugging PageRank shows r' array initializes with 0.0 instead of 1/n.
  3) Debugging shows wordsToDocFreq maps empty string. Should we get
   rid of this?
  4) Should we be excluding page IDs from the words list of each page?
   */

  /**
    * Maps page IDs to their Titles (Ints to Strings)
    */
  def buildIdsToTitles: Unit = {
    val titleArray = titleSeq.map(x => x.text.trim).toArray
    for (i <- IDArray.indices) {
      idsToTitles.put(IDArray(i), titleArray(i))
    }
  }

  /**
    * Maps page Titles to their IDs (Strings to Ints
    */
  def buildTitlesToIds: Unit = {
    val titleArray = titleSeq.map(x => x.text.trim).toArray
    for (i <- titleArray.indices) {
      titlesToId.put(titleArray(i), IDArray(i))
    }
  }

  /**
    * Maps words to document frequencies (wordsToDocumentFrequencies)
    * and page IDs to max word frequencies (idToMaxCounts)
    */
  def buildWordMaps: Unit = {
    val regexLink = new Regex("""\[\[[^\[]+?\]\]""")
    val regexText = new Regex("""[^\W_]+'[^\W_]+|[^\W_]+""")

    for (page <- pageSeq) {
      val matchesTextIterator = regexText.findAllMatchIn(page.text)
      val matchesTextList = matchesTextIterator.toList.map { aMatch => aMatch
        .matched }

      val matchesLinkIterator = regexLink.findAllMatchIn(page.text)
      matchesLinksList = matchesLinkIterator.toList.map { aMatch => aMatch
        .matched }
      val refinedLinksList = refineLinks(matchesLinksList)

      val words = matchesTextList.appendedAll(refinedLinksList)
      val noStopWordsList = words.filter(x => !isStopWord(x))
      val finalStemmedList =  noStopWordsList.map(x => stem(x.toLowerCase()))

      val pageID = (page \ "id").text.trim.toInt

      buildWordFreqMaxCount(pageID, finalStemmedList)
    }
  }

  /**
    * Loops through each link in the matchesLinksList, extracting link text to
    * add to words list by removing page titles in pipe links and surrounding
    * brackets
    *
    * @param matchesLinksList - the list of Strings matched with regexLink
    * @return the list of refined link words
    */
  def refineLinks(matchesLinksList: List[String]): List[String] = {
    var refinedLinksList = List[String]()
    for (link <- matchesLinksList) {
      var linkText = link
      if (link.contains("|")) {
        linkText = link.dropWhile(x => !x.toString.equals("|"))
      }
      val wordsInLink : Array[String] = linkText.split("""[\W]""")

      refinedLinksList = refinedLinksList.appendedAll(wordsInLink)
    }
    refinedLinksList
  }

  /**
    * Given a single page and its word list, maps and updates words to document
    * frequencies for every word. Also compares updated word
    * frequencies with the existing max counts in idToMaxCounts and updates
    * the max count if the updated word frequency is greater.
    *
    * @param pageID - an Int representing the page to add word frequencies
    *               for to wordsToDocumentFrequencies hashmap
    * @param words - a List of Strings representing the words to update
    *              frequencies for
    */
  def buildWordFreqMaxCount(pageID: Int, words: List[String]): Unit = {
    idToMaxCounts.put(pageID, 1.0)

    for (word <- words) {
      // if word not present, add mapping to HashMap for words.txt
      if (!wordsToDocumentFrequencies.contains(word)) {
        wordsToDocumentFrequencies.put(word, HashMap(pageID -> 1.0))
      } // if page ID # not present, add to hashmap/value of existing key/word
      else if (!wordsToDocumentFrequencies(word).contains(pageID)) {
        wordsToDocumentFrequencies(word).put(pageID, 1.0)
      } // increment frequency count
      else {
        val incrementedFreq = wordsToDocumentFrequencies(word)(pageID) + 1.0
        wordsToDocumentFrequencies(word).update(pageID, incrementedFreq)

        if (incrementedFreq > idToMaxCounts(pageID)) {
          idToMaxCounts(pageID) = incrementedFreq
        }
      }
    }
  }

  /**
    * Maps page IDs to a set of all page IDs linked to by that page
    */
  def buildIDsToLinks: Unit = {
    for (page <- pageSeq) {
      val pageID = (page \ "id").text.trim.toInt
      idsToLinks.put(pageID, toIdSet(matchesLinksList))
    }
  }

  /**
    * Given a list of links within a page, creates a set of corresponding page
    * IDs as long as the link title is part of the corpus
    *
    * @param listOfLinks - the list of links
    * @return a set of page IDs of the unique links
    */
  def toIdSet(listOfLinks: List[String]): Set[Int] = {
    var setOfIds: Set[Int] = Set()

    for (link <- listOfLinks) {
      var linkTitle = link.dropWhile(x => x.toString.equals("["))
      linkTitle = linkTitle.takeWhile(x => !x.toString.equals("]"))
      // var linkTitle = link.drop(2).dropRight(2) cleaner?
      if (link.contains("|")){
        linkTitle = linkTitle.takeWhile(x => !x.toString.equals("|"))
      }
      if (titlesToId.contains(linkTitle)) {
        setOfIds += titlesToId(linkTitle)
      }
    }
    setOfIds
  }

  /**
    * Given a page j and a page k, calculates the weight that k gives to j
    *
    * @param jPageID - an Int representing the page ID for page j
    * @param kPageID - an Int representing the page ID for page k
    * @return the weight given by k to j
    */
  def calcWeight(jPageID: Int, kPageID: Int): Double = {
    val n = idsToLinks.size
    var nk = idsToLinks(kPageID).size
    if (nk == 0) {
      nk = n - 1
    }

    val epsilon = 0.15
    if (idsToLinks(jPageID).contains(kPageID) && jPageID != kPageID) {
      epsilon / n + (1 - epsilon) / nk
    } else {
      epsilon / n
    }
  }

  /**
    * Calculates all PageRanks and maps page IDs to PageRanks in idsToPageRanks
    */
  def buildIdsToPageRanks: Unit = {
    val n = IDArray.size

    val pageWeights = new Array[Array[Double]](n)

    var ji = 0
    for (j <- IDArray) {
      val weights = new Array[Double](n)

      var i = 0
      for (k <- IDArray) {
        // weights :+ calcWeight(j, k)
        weights(i) = calcWeight(j, k)
        i += 1
      }

      pageWeights(ji) = weights
      ji += 1
    }

    val ranking = Array.fill[Double](n)(0)
    val updatedRanking = Array.fill[Double](n)(1/n) // fills array with 0.0
    // instead of 1/n

    var distance = 1.0
    while (distance > 0.001) {
      for (i <- ranking.indices) {
        ranking(i) = updatedRanking(i)
      }
      for (j <- 0 until n) {
        updatedRanking(j) = 0.0
        for (k <- 0 until n) {
          updatedRanking(j) += (pageWeights(j)(k) * ranking(k))
        }
      }

      var sumOfDistances = 0.0
      for (i <- updatedRanking.indices) {
        val distancei = math.pow(updatedRanking(i) - ranking(i), 2)
        sumOfDistances += distancei
      }
      distance = math.sqrt(sumOfDistances)
    }

    for (i <- updatedRanking.indices) {
      idsToPageRanks.put(IDArray(i), updatedRanking(i))
    }
  }

  object IndexTest {
    def testBuildIdsToTitles(t: Tester): Unit = {
      val testIndex = new Index("sol/search/sol/TestWiki.xml")

      val expectedWdToDocFreq = new HashMap[String, HashMap[Int,
        Double]]

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


      val expectedIdToMaxCounts = new HashMap[Int, Double]


      val expectedIdsToTitles = new HashMap[Int, String]
      expectedIdsToTitles.put(0, "PageA")
      expectedIdsToTitles.put(1, "PageB")
      expectedIdsToTitles.put(2, "PageC")
      expectedIdsToTitles.put(3, "PageD")
      t.checkExpect(testIndex.idsToTitles, expectedIdsToTitles)

      val expectedTitlesToId = new HashMap[String, Int]
      expectedTitlesToId.put("PageA", 0)
      expectedTitlesToId.put( "PageB", 1)
      expectedTitlesToId.put("PageC", 2)
      expectedTitlesToId.put("PageD", 3)
      t.checkExpect(testIndex.titlesToId, expectedTitlesToId)

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
      t.checkExpect(testIndex.idsToLinks, expectedIdsToLinks)

      val expectedIdsToPageRanks = new HashMap[Int, Double]
    }
  }

}

object IndexTest extends App {
  Tester.run(IndexTest)
}

object Index {
  def main(args: Array[String]) {
     val smallWiki = new Index("src/search/src/SmallWiki.xml")
    System.out.println(smallWiki.wordsToDocumentFrequencies)
    val pageRankWiki = new Index("src/search/src/PageRankWiki.xml")
    System.out.println(pageRankWiki.wordsToDocumentFrequencies)
// use FileIO here
  }
}
