package search.sol

import search.src.PorterStemmer.stem
import search.src.StopWords.isStopWord

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

  private val idsToTitles = new HashMap[Int, String]
  private val titlesToId = new HashMap[String, Int]
//  this.makeIdTitlesHm

  private val idToMaxCounts = new HashMap[Int, Double]
  private val idsToLinks = new HashMap[Int, Set[Int]]
  private val idsToPageRanks = new HashMap[Int, Double]
  private val wordsToDocumentFrequencies = this.mapWordsRelevance

//  /**
//   * adds IDs to Titles in idsToTitles Hashmap of Ints to Strings
//   * adds Titles to IDs in idsToTitles Hashmap of Strings to Ints
//   */
//  def makeIdTitlesHm: Unit = {
//    val titleArray = titleSeq.map(x => x.text.trim).toArray
//    for (i <- IDArray.indices) {
//      idsToTitles.put(IDArray(i), titleArray(i))
//      titlesToId.put(titleArray(i), IDArray(i))
//    }
//  }

  def makeIdsToTitlesHm: HashMap[Int, String] = {
    val titleArray = titleSeq.map(x => x.text.trim).toArray
    for (i <- IDArray.indices) {
      idsToTitles.put(IDArray(i), titleArray(i))
    }
    idsToTitles
  }

  def makeTitlesToIdsHm: HashMap[String, Int] = {
    val titleArray = titleSeq.map(x => x.text.trim).toArray
    for (i <- titleArray.indices) {
      titlesToId.put(titleArray(i), IDArray(i))
    }
    titlesToId
  }

  /*
  reconfigure as loop through pages to build hashmaps
   */
  /**
   * @return the hashmap of words to a hashmap of the document Ids they
   *         appear in along with their frequency in that document
   */
  def mapWordsRelevance: HashMap[String, HashMap[Int, Double]] = {
    val regexLinks = new Regex("""\[\[[^\[]+?\]\]""")
    val regexText = new Regex("""[^\W_]+'[^\W_]+|[^\W_]+""")

    var wordsRelevance = new HashMap[String, HashMap[Int, Double]]

    for (page <- pageSeq) {
      val matchesTextIterator = regexText.findAllMatchIn(page.text)
      val matchesTextList = matchesTextIterator.toList.map { aMatch => aMatch
        .matched }

      val matchesLinkIterator = regexLinks.findAllMatchIn(page.text)
      val matchesLinksList = matchesLinkIterator.toList.map { aMatch => aMatch
        .matched }
      val refinedLinksList = refineLinksList(matchesLinksList)

      val words = matchesTextList.appendedAll(refinedLinksList)
      val noStopWordsList = words.filter(x => !isStopWord(x))
      val finalStemmedList =  noStopWordsList.map(x => stem(x.toLowerCase()))

      val pageID = (page \ "id").text.trim.toInt

      wordsRelevance = wordsRelevanceHelper(pageID, finalStemmedList, wordsRelevance)

      idsToLinks.put(pageID, findIdSet(matchesLinksList))

    }
    wordsRelevance
  }

  def refineLinksList(matchesLinksList: List[String]): List[String] = {
    var refinedLinksList = List[String]()
    for (x <- matchesLinksList) {
      val wordsInLink : Array[String] = dealWithLink(x)
      refinedLinksList = refinedLinksList.appendedAll(wordsInLink)
    }
    refinedLinksList
  }

  def dealWithLink(text: String): Array[String] = { //  text match
      if (text.contains("|")){
        val newText = text.dropWhile(x => !x.toString.equals("|"))
        val split : Array[String] = newText.split("""[\W]""")
        split
      } else {
        val split: Array[String] = text.split("""[\W]""")
        split
      }
  }

  def wordsRelevanceHelper(pageID: Int, words: List[String],
    existingWR: HashMap[String, HashMap[Int, Double]]): HashMap[String,
    HashMap[Int, Double]] = {
    idToMaxCounts.put(pageID, 1.0)

    for (word <- words) {
      // if word not present, add mapping to HashMap for words.txt
      if (!existingWR.contains(word)) {
        val hM: HashMap[Int, Double] = HashMap(pageID -> 1)
        existingWR += (word -> hM)
      } // if page ID # not present, add to hashmap/value of existing key/word
      else if (!existingWR(word).contains(pageID)) {
        existingWR(word) += (pageID -> 1)
      } // increment frequency count
      else {
        val incrementedFreq = existingWR(word)(pageID) + 1.0
        existingWR(word).update(pageID, incrementedFreq)

        if (idToMaxCounts(pageID) < incrementedFreq) {
          idToMaxCounts(pageID) = incrementedFreq
        }
      }
    }
    existingWR
  }

  def findIdSet(listOfLinks: List[String]): Set[Int] = {
    var setOfIds: Set[Int] = Set()

    for (link <- listOfLinks) {
      var linkTitle = link.dropWhile(x => x.toString.equals("["))
      linkTitle = linkTitle.takeWhile(x => !x.toString.equals("]"))
      if (link.contains("|")){
        linkTitle = link.takeWhile(x => !x.toString.equals("|"))
      }
      if (titlesToId.contains(linkTitle)) {
        setOfIds += titlesToId(linkTitle)
      }
    }
    setOfIds
  }

  def calcWeight(jPageID: Int, kPageID: Int): Double = {
    val n = idsToLinks.size
    var nk = idsToLinks(kPageID).size
    if (nk == 0) {
      nk = n - 1
    }

    val epsilon = 0.15
    if (idsToLinks(jPageID).contains(kPageID)) {
      epsilon/n + (1 - epsilon)/nk
    } else {
      epsilon/n
    }
  }

  def calcPageRank: HashMap[Int, Double] = {
    val n = pageSeq.size

    val pageWeights = new Array[Array[Double]](n)
    for (jPageID <- IDArray) {
      val weights = new Array[Double](n)
      for (kPageID <- IDArray) {
        weights :+ calcWeight(jPageID, kPageID)
      }
      pageWeights :+ weights
    }

    val ranking = Array.fill[Double](n)(0)
    val updatedRanking = Array.fill[Double](n)(1/n)

    var sumOfDistances = 1.0

    while (sumOfDistances > 0.001) {
      for (i <- ranking.indices) {
        ranking(i) = updatedRanking(i)
      }
      for (j <- pageWeights.indices) {
        updatedRanking(j) = 0.0
        for (k <- 0 to n - 1) {
          updatedRanking(j) += (pageWeights(j)(k) * ranking(k))
        }
      }

      for (i <- updatedRanking.indices) {
        val distancei = math.pow(updatedRanking(i) - ranking(i), 2)
        sumOfDistances += distancei
      }
      sumOfDistances = math.sqrt(sumOfDistances)
    }

    for (i <- updatedRanking.indices) {
      idsToPageRanks.put(IDArray(i), updatedRanking(i))
    }
    idsToPageRanks
  }
}

object Index {
  def main(args: Array[String]) {
//    val smallWiki = new Index("src/search/src/SmallWiki.xml")
//    System.out.println(smallWiki.makeIdTitlesHm)
//    System.out.println(smallWiki.mapWordsRelevance)

  }
}
