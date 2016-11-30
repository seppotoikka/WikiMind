package com.saboworks.android.wikimind.controller;

import com.saboworks.android.wikimind.analysisEngine.SearchResult;

/**
 * Callback interface
 */

public interface AsyncResponse {
    void updateGUI(SearchResult result);
}
