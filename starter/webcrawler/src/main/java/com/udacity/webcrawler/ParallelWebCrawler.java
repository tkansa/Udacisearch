package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;

  private final List<Pattern> ignoredUrls;

  private final PageParserFactory pageParserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          PageParserFactory pageParserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.pageParserFactory = pageParserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);

    // You have to use synchronizedMap/Set to handle data coming from multiple threads
    Map<String, Integer> wordCounts = Collections.synchronizedMap(new HashMap<>());
    Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());
    for(String url : startingUrls){
      System.out.println(url);
      // need to create a recursive task for the pool and invoke it in here
      pool.invoke(new ParallelWebCrawlerAction(url, clock, timeout, maxDepth, ignoredUrls, pageParserFactory, wordCounts, visitedUrls, deadline));

    }
    if(wordCounts.isEmpty()){
      return new CrawlResult.Builder()
              .setWordCounts(wordCounts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }
    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }

  class ParallelWebCrawlerAction extends RecursiveAction {

    //private static final Logger logger = LoggerFactory.getLogger(ParallelWebCrawlerAction.class);

    private String url;
    private Clock clock;
    private Duration timeout;
    private int maxDepth;
    private List<Pattern> ignoredUrls;
    private PageParserFactory pageParserFactory;
    private Map<String, Integer> wordCounts;
    private Set<String> visitedUrls;
    private Instant deadline;

    public ParallelWebCrawlerAction(String url, Clock clock, Duration timeout, int maxDepth, List<Pattern> ignoredUrls, PageParserFactory pageParserFactory, Map<String, Integer> wordCounts, Set<String> visitedUrls, Instant deadline) {
      this.url = url;
      this.clock = clock;
      this.timeout = timeout;
      this.maxDepth = maxDepth;
      this.ignoredUrls = ignoredUrls;
      this.pageParserFactory = pageParserFactory;
      this.wordCounts = wordCounts;
      this.visitedUrls = visitedUrls;
      this.deadline = deadline;
    }

    @Override
    protected void compute() {

      // checks to stop the recursion
      // if maxDepth is reached or time is up
      if(maxDepth == 0 || clock.instant().isAfter(deadline)){
        // break out
        return;
      }

      // go thru the ignored urls, to skip if necessary
      for(Pattern pattern: ignoredUrls){
        if(pattern.matcher(url).matches()){
          return;
        }
      }

      // check to make sure it's not a visited url
      if(visitedUrls.contains(url)){
        return;
      }

      // Parse the url with the parser factory which was written for us
      PageParser.Result result = pageParserFactory.get(url).parse();

      // map the results to the synchronized map
      for(Map.Entry<String, Integer> entry : result.getWordCounts().entrySet()){
        wordCounts.compute(entry.getKey(), (k, v) -> (v== null) ? entry.getValue() : entry.getValue() + v);
      }

      // recurse
      List<ParallelWebCrawlerAction> actions = new ArrayList<>();
      for(String url : result.getLinks()){
        actions.add(new ParallelWebCrawlerAction(url, clock, timeout, maxDepth - 1, ignoredUrls, pageParserFactory, wordCounts, visitedUrls, deadline));
      }
      invokeAll(actions);

    }
  }
}
