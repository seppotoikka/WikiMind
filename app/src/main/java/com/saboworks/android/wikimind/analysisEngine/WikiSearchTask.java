package com.saboworks.android.wikimind.analysisEngine;

/**
 * Singe search action by user. Makes multiple queries to Wikipedia API getting parsed results
 * from ConceptRankEngine
 */

import android.os.AsyncTask;

import java.util.HashMap;
import java.util.List;

import com.saboworks.android.wikimind.controller.AsyncResponse;
import com.saboworks.android.wikimind.wikiApiEngine.MediaWikiApiQuery;

public class WikiSearchTask extends AsyncTask<String, Integer, SearchResult> {

    //used to store reference to main thread
    public AsyncResponse delegate = null;

    /* MediaWiki API supports query of 50 pages in one call
    (the searched page + 49 related concepts)
    Returns 15 most relevant links
     */

    private final static int NUMBER_OF_CONCEPTS = 49;
    private final static int NUMBER_OF_RESULTS = 15;

    protected SearchResult doInBackground(String... strings) {
        //TODO: throw exception if strings array size != 2
        try {
            String title = strings[0];
            String language = strings[1];
            if (title != null && !title.equals("")) {
                // Check if an article is found in Wikipedia with the given search string
                String checkedTitle = ConceptRankEngine.checkPageName(title, language);
                if (checkedTitle != null) {
                    System.out.println("Initiating search: ");

                    // Gets the JSON full page text in as a string
                    String pageRawString = MediaWikiApiQuery.getPageFullText(checkedTitle,
                            language);

                    // Double check for normalized page name and redirects for special fringe cases
                    checkedTitle = ConceptRankEngine.getRedirects(checkedTitle, pageRawString);

                    SearchResult searchResult = new SearchResult(checkedTitle,
                            language);

                    /* Ranks links to other wikipedia pages in terms of relevance and
                    returns top links
                     */
                    List<Concept> conceptList = ConceptRankEngine.getTopConcepts(pageRawString,
                            NUMBER_OF_CONCEPTS);

                    if (conceptList != null) {
                        // Adjust rankings based on other relevant pages linking to a certain page
                        conceptList = ConceptRankEngine.conceptNetworkRank(checkedTitle, language,
                                conceptList);

                        // Take into account only the top NUMBER_OF_RESULTS concepts
                        if (conceptList.size() > NUMBER_OF_RESULTS) {
                            conceptList = conceptList.subList(0, NUMBER_OF_RESULTS);
                        }

                        // Get summary texts for all top pages
                        HashMap<String, String> pageSummaries;
                        pageSummaries = ConceptRankEngine.getSummaries(conceptList, checkedTitle,
                                language);

                        // Set summary texts for each related concept and the searched page itself
                        for (Concept c : conceptList) {
                            c.setSummaryText(pageSummaries.get(c.getName()));
                            searchResult.addRelatedConcept(c);
                        }
                        searchResult.setPageSummaryText(pageSummaries.get(checkedTitle));

                        System.out.println("Search successful!");

                        return searchResult;
                    } else {
                        noPageFoundUpdate("Page summaries");
                    }
                } else {
                    noPageFoundUpdate("API query");
                }
            } else {
                noPageFoundUpdate("Search parameter");
            }
        } catch (Exception e) {
            e.printStackTrace();
            noPageFoundUpdate("HTTPGet");

        }
        return null;
    }


    protected void onPostExecute(SearchResult result) {
        //Call to main thread passing results. NOTE: result _can_ be null!
        delegate.updateGUI(result);
    }

    private void noPageFoundUpdate(String s) {
        System.out.println(s + " part of query yielded no results.");
    }
}

