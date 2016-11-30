package com.saboworks.android.wikimind.analysisEngine;

/**
 * Methods for parsing and handling the JSON data returned by MediaWiki API
 */

import com.saboworks.android.wikimind.wikiApiEngine.MediaWikiApiQuery;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class ConceptRankEngine {

    //TODO: make API query and json keywords constants for easier fixing if the API changes

    /* Find and rank links to other wiki pages(concepts)
       returns most relevant concepts related to the page.
       Returns null if there was no page that matched the search parameters.
    */
    public static List<Concept> getTopConcepts(String pageJson, int numberOfConcepts) {
        JsonReader reader = Json.createReader(new StringReader(pageJson));
        JsonObject queryResults = reader.readObject().getJsonObject("query").getJsonObject("pages");
        Collection<JsonValue> nameList = queryResults.values();
        Object o[] = nameList.toArray();
        JsonObject contentJson = (JsonObject) o[0];

        if (contentJson.containsKey("missing")){
            // No page found in Wikipedia api query, return null
            return null;
        }

        String contentString = contentJson.getJsonArray("revisions").getJsonObject(0).getString("*");
        int pageLength = contentString.length();
        Map<String, Integer> pageRanks = new HashMap<>();

		/* Parse for links to other Wikipedia pages that start with a double '['
		   link ends with a ']' or a '|'
		   if the link has a ':' it is a link to a file and will be ignored */
        for (int i = 0; i < contentString.length() - 2; i++){
            if (contentString.charAt(i) == '[' && contentString.charAt(i+1) == '['){
                String concept = "";
                for (int j = i+2; contentString.charAt(j) != '|' &&
                        contentString.charAt(j) != ']' && contentString.charAt(j) != '#' &&
                        j < contentString.length(); j++){
                    concept += contentString.charAt(j);
                }

				/* Make sure that the string is at least 2 characters and
				that the first character is in upper case (required by MediaWiki API)
				exclude links to files that start "File:" and in some old cases "Image:"
				also exclude category listings */
                if (concept.length() > 1 && !concept.contains("File:") && !concept.contains("Image:")
                        &&  !concept.contains("Category:")){
                    concept = Character.toString(concept.charAt(0)).toUpperCase()+concept.substring(1);
                    if (pageRanks.containsKey(concept)){
                        pageRanks.put(concept, pageRanks.get(concept) +
                                linkRankValue(i, pageLength));
                    } else {
                        pageRanks.put(concept, linkRankValue(i, pageLength));
                    }
                }
            }
        }
        // System.out.println("Page parse complete!");
        return topRankedConcepts(pageRanks, numberOfConcepts);
    }

    /* Returns a list with the wanted number of concepts with highest rankings in descending order,
    if there are less concepts in the map than wanted in the parameter,
    returns all concepts in descending order */
    private static List<Concept> topRankedConcepts(Map<String, Integer> conceptMap, int howManyResults){
        List<Concept> conceptList = new ArrayList<>();
        for (String s : conceptMap.keySet())
            conceptList.add(new Concept(s, conceptMap.get(s)));
        Collections.sort(conceptList);
        Collections.reverse(conceptList);
        if (conceptList.size() > howManyResults)
            return conceptList.subList(0, howManyResults);
        else
            return conceptList;
    }

    /* Give a relevance value to each instance of a link based on the position on the page
    links closer to the top of the page are valued higher.
     */
    private static int linkRankValue(int linkPosition, int pageLength) {
        //TODO: remove hardcoded value and add to constants
        Double value = 3 - 2 * ((double) linkPosition / pageLength);
        return value.intValue();
    }

    public static List<Concept> conceptNetworkRank(String page, String language,
                                                   List<Concept> concepts) {

        boolean batchComplete = false;
        String apiContinue = "";
        HashMap<String, Concept> cHashMap = new HashMap<String, Concept>();
        List<String> cList = new ArrayList<String>();

        if (!concepts.isEmpty()){
            for (Concept c : concepts ){
                cHashMap.put(c.getName(), c);
                cList.add(c.getName());
            }
        } else {
            // Return the empty list
            return concepts;
        }

        /* MediaWiki API query may need several calls, iterate until batchComplete
        TODO: split into separate methods for improved readability
         */
        while (!batchComplete){
            try {
                String jsonString = MediaWikiApiQuery.getPageLinksNetwork(page, language, cList, apiContinue);
                // System.out.println(jsonString);
                JsonReader reader = Json.createReader(new StringReader(jsonString));
                JsonObject fullQueryResults = reader.readObject();
                JsonObject queryResults = fullQueryResults.getJsonObject("query");

				/* Check for spelling normalizations and make new entries in hashmap
				point to the concept objects if necessary */
                if (queryResults.containsKey("normalized")){
                    for (JsonObject jo : queryResults.getJsonArray("normalized")
                            .getValuesAs(JsonObject.class)){
                        String from = jo.getString("from");
                        String to = jo.getString("to");
                        // System.out.println("From: " + from + " -> To: " + to);
                        if (cHashMap.containsKey(from)){
                            // System.out.println("Found un-normalized name!");
                            cHashMap.put(to, cHashMap.get(from));
                            // Remove reference to un-normalized concept name and update concept name
                            cHashMap.remove(from);
                            cHashMap.get(to).setName(to);
                        }
                    }
                }

                JsonObject pages = queryResults.getJsonObject("pages");
                for (String s : pages.keySet()){
                    JsonObject jsonPage = pages.getJsonObject(s);
                    String node = jsonPage.getString("title");

					/* Check if jsonObject's title is same as one of our concepts and
					check if it contains links to other concepts titles */
                    if (cHashMap.containsKey(node) && jsonPage.containsKey("links")){
                        for (JsonObject jo : jsonPage.getJsonArray("links")
                                .getValuesAs(JsonObject.class)){
                            String linkToNode = jo.getString("title");

                            // Adjust the referred node's ranking by current node's rank
                            if (cHashMap.containsKey(linkToNode)){
                                cHashMap.get(linkToNode).addToNetworkRank(cHashMap.
                                        get(node).getRank());
                            } else if (linkToNode.equals(page)){
                                // Flag that the concept has a backlink to the initial page
                                cHashMap.get(node).setHasBackLink(true);
                            } else {
                                System.out.println("Something funky happened: " + linkToNode);
                            }
                        }
                    }
                }

				/* Check if query results are incomplete and update api request parameters
				accordingly. Otherwise change batchComplete to 'true' */
                if (fullQueryResults.containsKey("continue")){
                    JsonObject cont = fullQueryResults.getJsonObject("continue");
                    apiContinue = "&continue=" + cont.getJsonString("continue").getString()
                            + "&plcontinue=" + URLEncoder.encode(cont.getJsonString("plcontinue")
                            .getString(), "UTF-8");
                    // System.out.println("continue query... " + apiContinue);
                } else {
                    batchComplete = true;
                }

            } catch (Exception e) {
                System.out.println("Something went wrong!");
                System.out.println(e.getMessage());
            }
        }
        concepts = new ArrayList<> (cHashMap.values());
        Collections.sort(concepts);
        Collections.reverse(concepts);
        return concepts;
    }

    /* Check if initial page search includes a redirect or normalization and return
    actual page name.
     */
    public static String getRedirects(String pageName, String jsonString){
        JsonReader reader = Json.createReader(new StringReader(jsonString));
        JsonObject queryResults = reader.readObject().getJsonObject("query");
        if (queryResults.containsKey("redirects")){
            String redirectPageName = queryResults.getJsonArray("redirects")
                    .getJsonObject(0).getJsonString("to").getChars().toString();
            // System.out.println("Search term redirected to " + redirectPageName);
            return redirectPageName;
        }
        if (queryResults.containsKey("normalized")){
            String redirectPageName = queryResults.getJsonArray("normalized")
                    .getJsonObject(0).getJsonString("to").getChars().toString();
            // System.out.println("Search term normalized to " + redirectPageName);
            return redirectPageName;
        }
        return pageName;
    }

    // Checks if a page is found in Wikipedia with user's search term
    public static String checkPageName(String name, String language){

        try {
            // System.out.println("Checking if searched page exists in Wikipedia...");
            String jsonString = MediaWikiApiQuery.findPageName(name, language);
            // System.out.println(jsonString);
            JsonReader reader = Json.createReader(new StringReader(jsonString));
            JsonObject result = reader.readObject();
            // Check if api returned query results
            if (result.containsKey("query")){
                JsonObject queryResults = result.getJsonObject("query").getJsonObject("pages");
                Collection<JsonValue> nameList = queryResults.values();
                Object o[] = nameList.toArray();
                if (o.length > 0){
                    JsonObject jo = (JsonObject) o[0];
                    String title = jo.getString("title");
                    // System.out.println("Found " + title + "!");
                    return title;
                }
            }
        } catch (Exception e){
            System.out.println("Page name check failed! " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /* Parse page summaries query, iterate if several queries are needed. Returns a HashMap
    with page names as keys and page summary texts as values.
     */
    public static HashMap<String, String> getSummaries
            (List<Concept> pageList, String pageName, String language){
        boolean batchComplete = false;
        String apiContinue = "";
        HashMap<String, String> summaries = new HashMap<String, String>();

        while (!batchComplete){
            try {
                // System.out.println("Retrieving page summaries...");
                String jsonString = MediaWikiApiQuery.getIntros(pageList, pageName, language, apiContinue);
                // System.out.println(jsonString);
                JsonReader reader = Json.createReader(new StringReader(jsonString));
                JsonObject result = reader.readObject();
                if (result.containsKey("query")){
                    JsonObject queryResults = result.getJsonObject("query").getJsonObject("pages");
                    for (String pageId : queryResults.keySet()){
                        if (Integer.parseInt(pageId)> 0){
                            summaries.put(queryResults.getJsonObject(pageId).getString("title"),
                                    queryResults.getJsonObject(pageId).getString("extract"));
                        }
                    }
                }
                if (result.containsKey("continue")){
                    JsonObject cont = result.getJsonObject("continue");
                    apiContinue = "&continue=" + cont.getJsonString("continue").getString()
                            + "&plcontinue=" + URLEncoder.encode(cont.getJsonString("plcontinue")
                            .getString(), "UTF-8");
                    // System.out.println("continue query... " + apiContinue);
                } else {
                    batchComplete = true;
                }
            } catch (Exception e){
                System.out.println("Something went wrong while getting page summaries! "
                        + e.getMessage());
                e.printStackTrace();
            }
        }
        return summaries;
    }
}

