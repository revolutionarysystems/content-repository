package uk.co.revsys.content.repository.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class SearchResult {

    private double score;
    @JsonUnwrapped
    private AbstractNode node;

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public AbstractNode getNode() {
        return node;
    }

    public void setNode(AbstractNode node) {
        this.node = node;
    }
    
}
