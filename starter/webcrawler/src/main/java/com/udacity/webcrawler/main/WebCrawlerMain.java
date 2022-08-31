package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
      // I think this is where it's using the config to determine which WebCrawler is instantiated
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    String path = config.getResultPath();
    if(!path.isEmpty()){
        resultWriter.write(Path.of(path));
    }
    else {
        //There may be a standard Writer implementation in java.io
        // (*cough* OutputStreamWriter *cough*) that converts System.out into a
        // Writer that can be passed to CrawlResultWriter#write(Writer).
        Writer writer = new OutputStreamWriter(System.out);

        resultWriter.write(writer);
        writer.flush();
    }
    // TODO: Write the crawl results to a JSON file (or System.out if the file name is empty)
    // TODO: Write the profile data to a text file (or System.out if the file name is empty)
  }

  public static void main(String[] args) throws Exception {

   if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

      System.out.println(args[0]);
    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();

  }
}
