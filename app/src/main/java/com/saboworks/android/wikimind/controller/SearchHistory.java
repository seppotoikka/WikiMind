package com.saboworks.android.wikimind.controller;

import com.saboworks.android.wikimind.analysisEngine.SearchResult;

import java.util.ArrayList;

/**
 * Created by Shallop on 12-Oct-16.
 */

public class SearchHistory {

    private ArrayList<SearchResult> queryResults;

    public SearchHistory() {
        this.queryResults = new ArrayList<>();
    }

    public void addResult(SearchResult s) {
        queryResults.add(s);
    }

    // Returns previous result or null if there is no previous result
    public SearchResult goBack() {
        if (queryResults.size() < 2) {
            return null;
        }
        else {
            int index = queryResults.size() - 1;
            queryResults.remove(index);
            return queryResults.get(index - 1);
        }
    }

    public SearchResult getLatestResult() {
        if (queryResults != null && !queryResults.isEmpty())
            return queryResults.get(queryResults.size() - 1);
        else
            return null;
    }

    public boolean isEmpty() {
        return queryResults.isEmpty();
    }
}
