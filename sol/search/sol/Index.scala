package search.sol

import search.src.FileIO.{printDocumentFile, printTitleFile, printWordsFile}
import search.src.PorterStemmer.stem
import search.src.StopWords.isStopWord

import scala.collection.mutable.{HashMap, ListBuffer, Set}
import scala.util.matching.Regex
import scala.xml.{Node, NodeSeq}

/**
 * Provides an XML indexer, produces files for a querier
 *
 * @param inputFile - the filename of the XML wiki to be indexed
 */
class Index(val inputFile: String) {
  private val mainNode: Node = xml.XML.loadFile(inputFile)
  private val pageSeq: NodeSeq = mainNode \ "page"
  private val titleSeq: NodeSeq = mainNode \ "page" \ "title"
  private val idSeq: NodeSeq = mainNode \ "page" \ "id"

  private val IDArray = idSeq.map(x => x.text.trim.toInt).toArray

  private val wordsToDocumentFrequencies = new HashMap[String, HashMap[Int,
    Double]]
  private val idsToMaxCounts = new HashMap[Int, Double]
  private val idsToTitles = new HashMap[Int, String]
  private val titlesToId = new HashMap[String, Int]
  private val idsToLinks = new HashMap[Int, Set[Int]]
  private val idsToPageRanks = new HashMap[Int, Double]

  this.buildTitleIdMaps()
  this.buildWordsLinksMaps()
  this.buildIdsToPageRanks()

  /**
    * Maps page IDs to their Titles (Ints to Strings)
    * and Titles to their IDs (Strings to Ints)
    */
  private def buildTitleIdMaps(): Unit = {
    val titleArray = titleSeq.map(x => x.text.trim).toArray
    for (i <- IDArray.indices) {
      idsToTitles.put(IDArray(i), titleArray(i))
      titlesToId.put(titleArray(i), IDArray(i))
    }
  }

  /**
    * Maps words to document frequencies (wordsToDocumentFrequencies)
    * and page IDs to max word frequencies (idsToMaxCounts)
    * and page IDs to a set of all page IDs linked to by that page (idsToLinks)
    */
  private def buildWordsLinksMaps(): Unit = {
    val regex = new Regex("""\[\[[^\[]+?\]\]|[^\W_]+'[^\W_]+|[^\W_]+""")

    for (page <- pageSeq) {
      val matchesIterator = regex.findAllMatchIn(page.text)

      val matchesList = matchesIterator.toList.map { aMatch => aMatch.matched }

      var refinedTextList: List[String] = refineLinksInList(matchesList)

      refinedTextList = refinedTextList.filter(x => !isStopWord(x))
      refinedTextList = refinedTextList.map(x => stem(x.toLowerCase()))

      val pageID = (page \ "id").text.trim.toInt

      // build 3 hashmaps below
      buildWordFreqMaxCount(pageID, refinedTextList)

      val linksList = matchesList.filter(mtch => isLink(mtch))

      val idSet = toIdSet(linksList, pageID)
      idsToLinks.put(pageID, idSet)
    }
  }

  /**
    * Evaluates whether a match is a link
    *
    * @param mtch - a String representing the match
    * @return true if the match is a link, false otherwise
    */
  private def isLink(mtch: String): Boolean = {
    mtch.matches("""\[\[[^\[]+?\]\]""")
  }

  /**
    * Removes double brackets from a link and extracts text of pipe links
    *
    * @param link - a String representing the link
    * @return - the refined link
    */
  private def refineLink(link: String): String = {
    var linkText = link.drop(2).dropRight(2)
    if (linkText.contains("|")) {
      linkText = linkText.dropWhile(x => !x.toString.equals("|")).drop(1)
    }
    linkText
  }

  /**
    * Takes a list of Strings and refines the links in the list by removing
    * surrounding brackets, extracting the link text for pipe links/category
    * links, and splitting strings with multiple words into separate strings
    *
    * @param lst - a List of Strings representing the text to refine
    * @return the refined list
    */
  def refineLinksInList(lst: List[String]): List[String] = {
    var refinedList = new ListBuffer[String]()

    for (str <- lst) {
      if (isLink(str)) {
        val refinedLinkText = refineLink(str)
        val wordsInLink : List[String] = refinedLinkText.split("""[\W]""")
          .toList
        refinedList.prependToList(wordsInLink)
      } else {
        refinedList += str
      }
    }
    refinedList.toList
  }

  /**
    * Given a single page and its word list, maps and updates words to document
    * frequencies for every word. Also compares updated word
    * frequencies with the existing max counts in idsToMaxCounts and updates
    * the max count if the updated word frequency is greater.
    *
    * @param pageID - an Int representing the page to add word frequencies
    *               for to wordsToDocumentFrequencies hashmap
    * @param words - a List of Strings representing the words to update
    *              frequencies for
    */
  private def buildWordFreqMaxCount(pageID: Int, words: List[String]): Unit = {
    idsToMaxCounts.put(pageID, 1.0)

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

        if (incrementedFreq > idsToMaxCounts(pageID)) {
          idsToMaxCounts(pageID) = incrementedFreq
        }
      }
    }
  }

  /**
    * Given a list of links within a page, creates a set of corresponding page
    * IDs as long as the link title is part of the corpus
    *
    * @param listOfLinks - the list of links
    * @return a set of page IDs of the unique links
    */
  private def toIdSet(listOfLinks: List[String], pageID: Int): Set[Int] = {
    var setOfIds: Set[Int] = Set()

    for (link <- listOfLinks) {
      var linkTitle = link.drop(2).dropRight(2)
      if (link.contains("|")){
        linkTitle = linkTitle.takeWhile(x => !x.toString.equals("|"))
      }
      if (titlesToId.contains(linkTitle) && pageID != titlesToId(linkTitle)) {
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
  private def calcWeight(jPageID: Int, kPageID: Int): Double = {
    val n = idsToLinks.size
    var nk = idsToLinks(kPageID).size
    var hasNoLinks = false
    if (nk == 0) {
      nk = n - 1
      hasNoLinks = true
    }

    val epsilon = 0.15
    if (idsToLinks(kPageID).contains(jPageID) | hasNoLinks && (jPageID != kPageID)) {
      epsilon / n + (1 - epsilon) / nk
    } else {
      epsilon / n
    }
  }

  /**
    * Calculates all PageRanks and maps page IDs to PageRanks in idsToPageRanks
    */
  private def buildIdsToPageRanks(): Unit = {
    val n = IDArray.length

    val pageWeights = new Array[Array[Double]](n)

    var ji = 0
    for (j <- IDArray) {
      val weights = new Array[Double](n)

      var i = 0
      for (k <- IDArray) {
        weights(i) = calcWeight(j, k)
        i += 1
      }

      pageWeights(ji) = weights
      ji += 1
    }

    val ranking = Array.fill[Double](n)(0)
    val updatedRanking = Array.fill[Double](n)(1.0/n)

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

  /*
  these only make shallow copies, should we make deep copies for these?
   */
  /**
    * Makes a deep copy of wordsToDocumentFrequencies
    *
    * @return deep copy of wordsToDocumentFrequencies
    */
  def getWordsToDocFreq: HashMap[String, HashMap[Int, Double]] = {
    val deepCopy = new HashMap[String, HashMap[Int, Double]]
    for ((k, v) <- this.wordsToDocumentFrequencies) {
      val vDeepCopy = new HashMap[Int, Double]
      for ((k1, v1) <- v) {
        vDeepCopy.put(k1, v1)
      }
      deepCopy.put(k, vDeepCopy)
    }
    deepCopy
  }

  def getIdsToMaxCounts: HashMap[Int, Double] = {
    this.idsToMaxCounts.clone()
  }

  def getIdsToTitles: HashMap[Int, String] = {
    this.idsToTitles.clone()
  }

  def getTitlesToId: HashMap[String, Int] = {
    this.titlesToId.clone()
  }

  def getIdsToLinks: HashMap[Int, Set[Int]] = {
    this.idsToLinks.clone()
  }

  def getIdsToPageRanks: HashMap[Int, Double] = {
    this.idsToPageRanks.clone()
  }
}

object Index {
  def main(args: Array[String]) {
    val index = new Index(args(0))
    printTitleFile(args(1), index.getIdsToTitles)
    printDocumentFile(args(2), index.getIdsToMaxCounts, index.getIdsToPageRanks)
    printWordsFile(args(3), index.getWordsToDocFreq)
    val t1 = System.nanoTime
    val duration: Double = (System.nanoTime - t1) / 1e9d
    println("time " + duration)
  }
}
