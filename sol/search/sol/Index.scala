package search.sol

import search.src.PorterStemmer.{stem}
import search.src.StopWords.isStopWord

import scala.collection.immutable.HashMap
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

  def mapRelevance: HashMap[Int, List[String]] = {
    var idTermsHm: HashMap[Int, List[String]] = HashMap()
    val regexLinks = new Regex("""\[\[[^\[]+?\]\]""")
    val regexText = new Regex("""[^\W_]+'[^\W_]+|[^\W_]+""")
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

      val relevantWords = stemmedList.appendedAll(matchesLinksList)
      val pageID = (page \ "id").text.trim.toInt
      idTermsHm += (pageID -> relevantWords)
    }
    idTermsHm
  }

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
    System.out.println(smallWiki.mapRelevance)
  }
}
