package com.saboworks.android.wikimind.wikiApiEngine;

/**
 * HTTP GET methods for retrieving data from Wikipedia API
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import com.saboworks.android.wikimind.analysisEngine.Concept;

// Contains methods for handling Wikipedia api calls
public class MediaWikiApiQuery {

    //TODO: Use Volley?

    private final static String PROTOCOL = "https://";
    private final static String WIKI_URL = ".wikipedia.org/w/";
    private final static String PAGE_RAW_TEXT_QUERY =
            "api.php?action=query&prop=revisions&rvprop=content&format=json&utf8=&redirects&titles=";
    private final static String LINKS_QUERY =
            "api.php?action=query&plnamespace=0&pllimit=500&prop=links&format=json&utf8=&titles=";
    private final static String LINKS_QUERY_CONT = "&pltitles=";
    private final static String NEAR_MATCH_QUERY =
            "action=query&generator=search&format=json&gsrwhat=nearmatch&gsrsearch=";
    private final static String NEAR_MATCH_RESULT_LIMIT = "&gsrlimit=1&prop=info";
    private final static String EXTRACTS_QUERY =
            "api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&exlimit=max&titles=";

    // Returns the requested page full text
    public static String getPageFullText(String pageName, String language) throws Exception {

        StringBuilder result = new StringBuilder();
        pageName = URLEncoder.encode(pageName, "UTF-8");
        URL url = new URL(PROTOCOL + language +
                WIKI_URL + PAGE_RAW_TEXT_QUERY + pageName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        // System.out.println("Page " + pageName + " with " + result.length() +
        //        " characters retrieved successfully!");
        return result.toString();
    }

    // Returns the raw String (JSON) result of page links network query
    public static String getPageLinksNetwork(String title, String language, List<String> pageList,
                                             String apiContinue) throws Exception {

        StringBuilder result = new StringBuilder();
        String urlString = PROTOCOL + language +
                WIKI_URL + LINKS_QUERY;
        String concepts = "";
        for (String s : pageList) {
            concepts += URLEncoder.encode(s, "UTF-8") + "|";
        }
        urlString += concepts.substring(0, concepts.length() - 1) + LINKS_QUERY_CONT + concepts +
                URLEncoder.encode(title, "UTF-8") + apiContinue;
        URL url = new URL(urlString);
        //System.out.println(url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                "UTF-8"));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        // System.out.println("Api query for related pages with " + result.length() +
        //        " characters retrieved successfully!");
        return result.toString();
    }

    /* Returns the raw string (JSON) result for query for one page that is the closest match to the
    search string
     */
    public static String findPageName(String title, String language) throws Exception {

        StringBuilder result = new StringBuilder();
        URL url = new URL(PROTOCOL + language + WIKI_URL + "api.php?"
                + NEAR_MATCH_QUERY + URLEncoder.encode(title, "UTF-8") + NEAR_MATCH_RESULT_LIMIT);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }

    // Returns the raw string (JSON) results for query for extracts for pageName and pageList pages
    public static String getIntros (List<Concept> pageList, String pageName, String language,
                                    String apiContinue) throws Exception {

        StringBuilder result = new StringBuilder();
        String urlString = PROTOCOL + language +
                WIKI_URL + EXTRACTS_QUERY;

        String concepts = "";
        for (Concept c : pageList) {
            concepts += URLEncoder.encode(c.getName(), "UTF-8") + "|";
        }

        urlString += concepts + URLEncoder.encode(pageName, "UTF-8") + apiContinue;
        URL url = new URL(urlString);
        // System.out.println(url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        // System.out.println("Api query for page summaries with " + result.length() +
        //        " characters retrieved successfully!");
        return result.toString();
    }
}

