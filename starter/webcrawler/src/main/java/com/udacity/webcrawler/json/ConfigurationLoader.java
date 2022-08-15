package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

    private final Path path;

    /**
     * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
     */
    public ConfigurationLoader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    /**
     * Loads configuration from this {@link ConfigurationLoader}'s path
     *
     * @return the loaded {@link CrawlerConfiguration}.
     */
    public CrawlerConfiguration load() {
        // Try with resources
        try(Reader reader = Files.newBufferedReader(path)){
            CrawlerConfiguration configuration = read(reader);
            return configuration;
        } catch(IOException e){
            e.getMessage();
        }

        return null;
    }

    /**
     * Loads crawler configuration from the given reader.
     *
     * @param reader a Reader pointing to a JSON string that contains crawler configuration.
     * @return a crawler configuration
     */
    public static CrawlerConfiguration read(Reader reader) {

        // Get the JSON from the reader
        String jsonString = "";
        int valueOfChar;
        try {
            while ((valueOfChar = reader.read()) != -1) {
                jsonString += (char) valueOfChar;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Use the Jackson mapper to map it to a CrawlerConfiguration object
        ObjectMapper mapper = new ObjectMapper();
        CrawlerConfiguration crawlerConfig = null;
        try {
            crawlerConfig = mapper.readValue(jsonString, CrawlerConfiguration.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return crawlerConfig;
    }
}
