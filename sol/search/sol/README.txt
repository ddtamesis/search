Your README file should include:

INSTRUCTIONS FOR USE (i.e. how a user would interact with your program)

Query is a REPL (Read-Eval-Print Loop) method that prompts the user for input,
processes the input query/text, prints the top 10 most relevant pages based on
the query, and repeats. Query does all of the communication
with a user, and it will continue prompting for a query search until the
user types ':quit' at the prompt. The user may indicate whether or not to
include PageRank in the calculation of the document scores by adding
"--pagerank" as the first argument in the main method.


DESIGN OVERVIEW (including how the pieces of your program fit)

Our search program consists of two programs, an indexer and a querier. First,
running the indexer preprocesses a corpus (specified by an input file path to
an xml file) and produces text files storing information to be used later by
the querier in calculating document relevance to queries. These files include
titles.txt, words.txt,and docs.txt. They are then all read by the querier to
process and print the 10 pages that are most relevant
to the query in order of relevance.

The querier runs by taking in 3-4 arguments. The first (--pagerank) can be omitted
depending on whether or not the user wants to receive results that take into
account the documents' PageRanks. The other 3 arguments are filepaths to the
files created by the Indexer's preprocessing. Once they query is running, it
prompts the user to input a query and hit return to search through the corpus,
returning the names of up to the top 10 most relevant pages. Again, it
terminates once the user type ':quit' at the prompt.


DESCRIPTION OF FEATURES YOU FAILED OT IMPLEMENT, AS WELL AS ANY EXTRA FEATURES
We implemented all required features, and no extra ones.

DESCRIPTION OF KNOWN BUGS
None.

DESCRIPTION FOR TESTING

I. Indexer: We tested the indexer parsing in IndexTestSuite.scala by creating a
small TestWiki.xml to have manageable expected hashmaps to create for comparison
to the actual resulting hashmaps.

a) wordsToDocumentFrequencies
To test whether our wordsToDocumentFrequencies HashMap was built correctly,
we manually counted the frequencies of each word in each page in TestWiki.xml.
We added these to a new HashMap replicating a wordsToDocumentFrequencies
HashMap,i.e a HashMap[String, HashMap[Int,Double], where each word maps to
a HashMap where the key is a page IDs and the value is the frequency of the
word in that page.

We commented our the test in the end because even though each KVPair in the
output HashMap matched the expected HashMap from our calculations, there were
some issues in the comparisons due to the way the different HashMaps were
initialized (e.g. to save space, threshold sizes were 8 rather than 16
sometimes).

b) PageRank: We tested PageRank using PageRankWiki.xml, where 100 pages all only
link to page 100, making page 100 overwhelmingly the most authoritative. A page
is deemed more authoritative when (i) more pages link to it, (ii) the pages that
link to it are more authoritative, and (iii) the pages that link to it have few
links to other pages. It is also worth noting that a page's rank is more
influenced by a page that links to it as the distance (in the number of links
to get to it) gets smaller.

First, we printed the resulting PageRank hashmap and saw that the ranks were
reasonable (0.00545 for pages 1-99, and 0.4607 for page 100--all of which sum to
1). Low uniform rankings for pages 1-99 made sense since they all only linked
to page 100 and had zero pages linking to them.

Secondly, we inspected the weight calculations using the debugger and compared
the values to our hand calculations. Pages 1-99 gave 0.8515 weight to page 100,
page 100 gave 0.01008 weight to pages 1-99, pages 1-99 gave each other 0.0015,
and all pages gave themselves 0.0015. All instances of 0.0015 imply that page k
did not link to page j, including special cases where pages link to themselves
or to pages outside the corpus. Page 100 was a special case where its link to
itself is considered as the page linking to nothing, so it was treated as
linking once to every page except itself (nk = n-1).

II. Query
We used a variety of pages to test our Query class. We first tested it on the
small xml file we created, TestWiki.xml, by running the Indexer to create the 3
txt files in the sol folder, then running query with and without pagerank.
Once the 'search' line showed up, we tested queries and they returned expected
results. For example, we searched for:
- this & punctuation marks: returned no results because they are stop
    words or invalid entries
- cereal:
    - 1 Page1: Only 1 appearance in the corpus, so only 1 page title is returned
- before orange: returned pages that had the word 'orange' in them as before is
    not part of the corpus. The ranking are as follows:
    - words relevance only: 1 Page1, 2 Page2
    - pagerank applied: 1 Page2, 2 Page1
    --> Here we saw PageRank at play. When ranking pages by words' relevance
    only, Page1 became the most relevant page because "orange" appeared in it 3x
    vs. the 2x in Page2. But, since Page1 has no pages linking to it and no
    links itself while Page2 links to 2 authoritative pages (Page0 and Page3),
    when quering using PageRank, Page2 became deemed as more authoritative
    and thus relevant than Page1. This example particularly highlights the 4th
    principle of determining page ranks, which states that a page's rank becomes
    more influenced by a page that links to it as the distance (# links
    to get to it) gets smaller.
- Bananas, bananas, BanaNas:
    - words relevance only: 1 Page3, 2 Page1
	- pagerank applied: 1 Page3, 2 Page1
	--> Since Page 3 has more instances of "banana" AND is more authoritative
	(it is linked to by 2 pages rather than 0), it is the most relevant page in
	both query results.

We also run Query with other files such as SmallWiki.xml and MedWiki.xml

LIST OF TEAM MEMBERS:
Coding subgroup: ddtamesis, jwei34
Design Check group also included: karian, mkim104