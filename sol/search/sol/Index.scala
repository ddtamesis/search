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
  val mainNode: Node = xml.XML.loadFile(inputFile)
  // select all children w/ tag “page”
  val pageSeq: NodeSeq = mainNode \ "page"
  // select all children w/ tag “title”
  val titleSeq: NodeSeq = mainNode \ "page" \ "title"
  // select all children w/ tag “id”
  val idSeq: NodeSeq = mainNode \ "page" \ "id"

  /**
   * maps IDs to Titles in a Hashmap of Ints to Strings
   * @return Hashmap of IDs to Titles
   */
  def makeIdTitlesHm: HashMap[Int,String] = {
    val IDArray = idSeq.map(x => x.text.trim.toInt).toArray
    val titleArray = titleSeq.map(x => x.text.trim).toArray
    var idTitleHm: HashMap[Int, String] = HashMap()
    for (i <- IDArray.indices) {
      idTitleHm += (IDArray(i) -> titleArray(i))
    }
    idTitleHm
  }

  def mapWordsRelevance: HashMap[String, HashMap[Int, Double]] = {
    var idTermsHm: HashMap[Int, List[String]] = HashMap()
    val regexLinks = new Regex("""\[\[[^\[]+?\]\]""")
    val regexText = new Regex("""[^\W_]+'[^\W_]+|[^\W_]+""")

    var wordsRelevance : HashMap[String, HashMap[Int, Double]] = HashMap()

    for (page <- pageSeq) {
      val matchesTextIterator = regexText.findAllMatchIn(page.text)
      val matchesTextList = matchesTextIterator.toList.map { aMatch => aMatch
        .matched }
      val noStopWordsList = matchesTextList.filter(x => !isStopWord(x))
      val stemmedList =  noStopWordsList.map(x => stem(x))

      val matchesLinkIterator = regexLinks.findAllMatchIn(page.text)
      val matchesLinksList = matchesLinkIterator.toList.map { aMatch => aMatch
        .matched }
//      val refinedLinksList = matchesLinksList.map(x => dealWithLink(x))

      val words = stemmedList.appendedAll(matchesLinksList)
      val pageID = (page \ "id").text.trim.toInt
      idTermsHm += (pageID -> words)

      for (word <- words) {
        // if word not present, add mapping to HashMap for words.txt
        if (!wordsRelevance.contains(word)) {
          val hM : HashMap[Int, Double] = HashMap(pageID -> 1)
          wordsRelevance += (word -> hM)
        } // if page ID # not present, add to hashmap/value of existing key/word
        else if (!wordsRelevance(word).contains(pageID)){
          wordsRelevance(word) += (pageID -> 1)
        } // increment frequency count !! problem: words only contains 1
        // copy, never reaches this. otherwise seems correct!!
        else {
          val incrementedFreq = wordsRelevance(word)(pageID) + 1.0
          wordsRelevance(word)(pageID) -> incrementedFreq
        }
      }
    }
    wordsRelevance
  }

//  /**
//   * @return the hashmap of words to a hashmap of the document Ids they appear
//   *         in along with their frequency in that document
//   */
//  def mapWordsRelevance: HashMap[String, HashMap[Int, Double]] = {
//    var wordsRelevance : HashMap[String, HashMap[Int, Double]] = HashMap()
//    var idToFrequency = HashMap[Int, Double]
//    for (word <- mapIDsToWords.values.toList) {
//      if (wordsRelevance.contains(word)
//    }
//  }


  //  def dealWithLink(text: String): String = text match {
//    case Some(String) + "|" + Some(String) =>
//      text.dropWhile(x => x.toString == "|")
//    case "[" + Some(String) + "]" => text.dropWhile(x => x.toString == "[")
//      .takeWhile(x => x.toString != "|")
//  }

}

object Index {
  def main(args: Array[String]) {
    val smallWiki = new Index("src/search/src/SmallWiki.xml")
//    System.out.println(smallWiki.makeIdTitlesHm)
    System.out.println(smallWiki.mapWordsRelevance)
  }
}
