package search.sol

import search.src.FileIO
import search.src.PorterStemmer.stemArray
import search.src.StopWords.isStopWord

import java.io._
import scala.collection.mutable.HashMap

/**
 * Represents a query REPL built off of a specified index
 *
 * @param titleIndex    - the filename of the title index
 * @param documentIndex - the filename of the document index
 * @param wordIndex     - the filename of the word index
 * @param usePageRank   - true if page rank is to be incorporated into scoring
 */
class Query(titleIndex: String, documentIndex: String, wordIndex: String,
            usePageRank: Boolean) {

  // Maps the document ids to the title for each document
  private val idsToTitle = new HashMap[Int, String]

  // Maps the document ids to the euclidean normalization for each document
  private val idsToMaxFreqs = new HashMap[Int, Double]

  // Maps the document ids to the page rank for each document
  private val idsToPageRank = new HashMap[Int, Double]

  // Maps each word to a map of document IDs and frequencies of documents that
  // contain that word
  private val wordsToDocumentFrequencies = new HashMap[String, HashMap[Int, Double]]

  /**
   * Handles a single query and prints out results
   *
   * @param userQuery - the query text
   */
  private def query(userQuery: String) {
    var queryArray: Array[String] = userQuery.toLowerCase.split("""[\W]""")
    queryArray = queryArray.filter(word => !isStopWord(word))
    queryArray = stemArray(queryArray)
    queryArray = queryArray.filter(word => isValidWord(word))

    if (queryArray.isEmpty) {
      System.out.println("Sorry, there were no results")
    }
    else {
      val scoresToIDs = new HashMap[Double, List[Int]]
      val docScores = new Array[Double](idsToTitle.size)
      var i = 0
      for ((id, title) <- idsToTitle) {
        var docScore = 0.0
        for (word <- queryArray) {
          docScore += calcRelvScore(id, word)
        }
        if (usePageRank) {
          docScore *= idsToPageRank(id)
        }
        docScores(i) = docScore
        i += 1
        if (scoresToIDs.contains(docScore)) {
          scoresToIDs(docScore) ::= id
        } else {
          scoresToIDs.put(docScore, List(id))
        }
      }
      val sortedScores = docScores.sortWith((x1, x2) => x1 > x2)
      val uniqueNonzeroScores = sortedScores.filter(x => x > 0).distinct

      var results = List[Int]()
      for (j <- uniqueNonzeroScores.indices) {
        results = results ::: scoresToIDs(uniqueNonzeroScores(j))
      }
      printResults(results.toArray)
    }
  }

  /**
    * Evaluates whether a word is a valid query, by virtue of being in the
    * corpus
    *
    * @param word - the word to evaluate
    * @return true if the word is valid, false otherwise
    */
  private def isValidWord(word: String): Boolean = {
   wordsToDocumentFrequencies.contains(word)
  }

  /**
    * Gets the relevance score of a document to a term i in the query
    *
    * @param id - an Int representing the page ID for the document
    * @param word - a String representing the term i to calculate on
    * @return the relevance score of the document for the term i
    */
  def calcRelvScore(id: Int, word: String): Double = {
    if (wordsToDocumentFrequencies(word).contains(id)) {
      val tf = wordsToDocumentFrequencies(word)(id) / idsToMaxFreqs(id)
      val idf = Math.log10(idsToTitle.size / wordsToDocumentFrequencies(word).size) //Math.log(n / ni)
      tf * idf
    } else {
      0
    }
  }

  /**
   * Format and print up to 10 results from the results list
   *
   * @param results - an array of all results to be printed
   */
  private def printResults(results: Array[Int]) {
    for (i <- 0 until Math.min(10, results.size)) {
      println("\t" + (i + 1) + " " + idsToTitle(results(i)))
    }
  }

  /**
    * Reads in the text files.
    */
  def readFiles(): Unit = {
    FileIO.readTitles(titleIndex, idsToTitle)
    FileIO.readDocuments(documentIndex, idsToMaxFreqs, idsToPageRank)
    FileIO.readWords(wordIndex, wordsToDocumentFrequencies)
  }

  /**
   * Starts the read and print loop for queries
   */
  def run() {
    val inputReader = new BufferedReader(new InputStreamReader(System.in))

    // Print the first query prompt and read the first line of input
    print("search> ")
    var userQuery = inputReader.readLine()

    // Loop until there are no more input lines (EOF is reached)
    while (userQuery != null) {
      // If ":quit" is reached, exit the loop
      if (userQuery == ":quit") {
        inputReader.close()
        return
      }

      // Handle the query for the single line of input
      query(userQuery)

      // Print next query prompt and read next line of input
      print("search> ")
      userQuery = inputReader.readLine()
    }

    inputReader.close()
  }
}

object Query {
  def main(args: Array[String]) {
    try {
      // Run queries with page rank
      var pageRank = false
      var titleIndex = 0
      var docIndex = 1
      var wordIndex = 2
      if (args.size == 4 && args(0) == "--pagerank") {
        pageRank = true;
        titleIndex = 1
        docIndex = 2
        wordIndex = 3
      } else if (args.size != 3) {
        println("Incorrect arguments. Please use [--pagerank] <titleIndex> "
          + "<documentIndex> <wordIndex>")
        System.exit(1)
      }
      val query: Query = new Query(args(titleIndex), args(docIndex), args(wordIndex), pageRank)
      query.readFiles()
      query.run()
    } catch {
      case _: FileNotFoundException =>
        println("One (or more) of the files were not found")
      case _: IOException => println("Error: IO Exception")
    }
  }
}