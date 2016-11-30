package com.saboworks.android.wikimind.analysisEngine;

/**
 * Created by Shallop on 12-Oct-16.
 */

import java.util.ArrayList;

public class SearchResult {

    private String pageName;
    private String pageSummaryText;
    private ArrayList<Concept> relatedConcepts;
    private String language;

    public SearchResult (String name, String language) {
        this.pageName = name;
        this.pageSummaryText = "";
        this.relatedConcepts = new ArrayList<>();
        this.language = language;
    }

    public String getPageName() {
        return pageName;
    }

    public String getPageSummaryText() {
        return pageSummaryText;
    }

    public void setPageSummaryText(String text) {
        pageSummaryText = text;
    }

    public void addRelatedConcept(Concept c) {
        this.relatedConcepts.add(c);
    }

    public ArrayList<Concept> getRelatedConcepts() {
        return this.relatedConcepts;
    }

    public String getLanguage() {
        return this.language;
    }

}