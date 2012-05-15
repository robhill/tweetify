package tweetify;

import java.util.List;

/**
 * Represents a search result
 * @author plamere
 */
public class Results<T extends SpotifyItem> {
    private int startIndex;
    private int totalResults;
    private int itemsPerPage;
    private List<T> items;

    Results(int startIndex, int totalResults, int itemsPerpage, List<T> items) {
        this.startIndex = startIndex;
        this.totalResults = totalResults;
        this.itemsPerPage = itemsPerpage;
        this.items = items;
    }

    /**
     * Gets the maximum number of items per returned page
     * @return items per page
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }

    /**
     * Gets the start index for this page
     * @return the start index
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * Gets the total number of results
     * @return the number of results
     */
    public int getTotalResults() {
        return totalResults;
    }

    /**
     * Gets the next page number
     * @return the next page number
     */
    public int getNextPage() {
        int curPage = 1 + getStartIndex() / getItemsPerPage();
        return curPage + 1;
    }

    /**
     * Determines if this is the last page of results
     * @return true if this is the last page
     */
    public boolean isLast() {
        return startIndex + getItemsPerPage() >= getTotalResults();
    }


    /**
     * Gets the items for the search results
     * @return the list of results
     */
    public List<T> getItems() {
        return items;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" startIndex:" + startIndex);
        sb.append(" totalResults: " + totalResults);
        sb.append(" itemsPerPage: " + itemsPerPage);
        int which = 0;
        for (SpotifyItem item : getItems()) {
            sb.append(which + ") " + item.getName() + "\n");
            sb.append(item);
            sb.append("\n");
            which++;
        }
        sb.append("\n");
        return sb.toString();
    }
}
