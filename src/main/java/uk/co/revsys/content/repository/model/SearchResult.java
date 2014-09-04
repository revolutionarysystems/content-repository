package uk.co.revsys.content.repository.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class SearchResult {

    private double score;
    @JsonUnwrapped
    private ContentNode node;

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public ContentNode getNode() {
        return node;
    }

    public void setNode(ContentNode node) {
        this.node = node;
    }
    
}
