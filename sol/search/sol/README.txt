Your README file should include:

Instructions for use, describing how a user would interact with your program.
A brief overview of your design, including how the pieces of your program fit.
A description of features you failed to implement, as well as any extra features you implemented.
A description of any known bugs in your program.
A description of how you tested your program.
A list of the people with whom you collaborated.


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
links to other pages. It is also worth nothing that a page's rank is more
influenced by a page that links to it as the distance (in the number of links
to get to it) is smaller.

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
