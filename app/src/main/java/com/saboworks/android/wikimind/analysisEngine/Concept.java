package com.saboworks.android.wikimind.analysisEngine;

/**
 * A single MediaWiki page defined to be relevant by initial text analysis of searched page
 */

import java.util.HashMap;

public class Concept implements Comparable<Concept> {
    private String name;

    // Initial rank from page text analysis
    private float rankValue;

    // Adjusted rank from references by other concepts in the link network
    private float networkRankValue;

    // Links back to the searched page
    private boolean hasBackLink;

    // Lowering this value will reduce the relevance of pages that do not link back to original page
    private static final float NO_BACKLINK_PENALTY = 0.1f;

    // Lowering this value will reduce the relevance of pages that are linked to by other pages
    private static final float NETWORK_RANK_FACTOR = 0.2f;

    private HashMap<String, Concept> linksTo;
    private String summaryText;

    public Concept (String name, float rank) {
        this.name = name;
        this.rankValue = rank;
        this.hasBackLink = false;
        this.networkRankValue = 0;
        setSummaryText("");
        this.linksTo = new HashMap<>();
    }

    // Called if another related page has a link to this page
    public void addToNetworkRank(double value) {
        this.networkRankValue += value * NETWORK_RANK_FACTOR;
    }

    /* Returns the final page relevance value based on initial text analysis weight
    and value from other pages linking to this page. Adjusted based on if this page links
    back to the original page.
     */
    public float getFinalRank() {
        if (this.hasBackLink) {
            return this.networkRankValue + this.rankValue;
        } else {
            return (this.networkRankValue + this.rankValue) * NO_BACKLINK_PENALTY;
        }
    }

    public float getRank() {
        return this.rankValue;
    }

    public String getName() {
        return this.name;
    }

    public boolean getHasBackLink() {
        return this.hasBackLink;
    }

    public void setHasBackLink(boolean b) {
        this.hasBackLink = b;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Concept c) {
        return Math.round(this.getFinalRank()) - Math.round(c.getFinalRank());
    }

    public void addLink(Concept c) {
        this.linksTo.put(c.getName(), c);
    }

    public HashMap<String, Concept> getLinks() {
        return this.linksTo;
    }

    public boolean hasLinkTo (String conceptName) {
        return this.linksTo.containsKey(conceptName);
    }

    public void setSummaryText(String s) {
        this.summaryText = s;
    }

    public String getSummaryText() {
        return this.summaryText;
    }



}

