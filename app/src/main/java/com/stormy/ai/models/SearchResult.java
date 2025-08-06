package com.stormy.ai.models;

/**
 * Represents a search result from Apache Lucene with relevance scoring and metadata.
 */
public class SearchResult {
    private final String id;
    private final String title;
    private final String content;
    private final float relevanceScore;
    private String highlightedContent;
    private int startPosition = -1;
    private int endPosition = -1;

    public SearchResult(String id, String title, String content, float relevanceScore) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.relevanceScore = relevanceScore;
        this.highlightedContent = content;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public float getRelevanceScore() {
        return relevanceScore;
    }

    public String getHighlightedContent() {
        return highlightedContent;
    }

    public void setHighlightedContent(String highlightedContent) {
        this.highlightedContent = highlightedContent;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    /**
     * Set the position range for highlighting
     */
    public void setPosition(int start, int end) {
        this.startPosition = start;
        this.endPosition = end;
    }

    /**
     * Check if this result has position information
     */
    public boolean hasPosition() {
        return startPosition >= 0 && endPosition >= 0;
    }

    /**
     * Get a snippet of the content around the match
     */
    public String getSnippet(int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }

        if (hasPosition()) {
            // Create snippet around the match position
            int snippetStart = Math.max(0, startPosition - maxLength / 4);
            int snippetEnd = Math.min(content.length(), endPosition + maxLength / 4);
            
            String snippet = content.substring(snippetStart, snippetEnd);
            if (snippetStart > 0) {
                snippet = "..." + snippet;
            }
            if (snippetEnd < content.length()) {
                snippet = snippet + "...";
            }
            return snippet;
        } else {
            // Just take the first part
            return content.substring(0, Math.min(maxLength, content.length())) + "...";
        }
    }

    /**
     * Get relevance score as a percentage
     */
    public double getRelevancePercentage() {
        return Math.min(100.0, relevanceScore * 100.0);
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", relevanceScore=" + relevanceScore +
                ", hasPosition=" + hasPosition() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchResult that = (SearchResult) o;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}