package com.logshield.logshieldv2.model;

import java.util.List;

/**
 * Wraps a paginated list of results with metadata.
 *
 * Every paginated endpoint returns this structure so clients
 * always know their position in the full dataset.
 *
 * Generic type T allows reuse across different response types —
 * LogEntryResponse, String, or any future type.
 */
public class PagedResponse<T> {

    // The actual page of data
    private List<T> content;

    // Current page number — zero-based
    private int page;

    // Number of items per page
    private int size;

    // Total number of items across all pages
    private long totalElements;

    // Total number of pages
    private int totalPages;

    // Convenience flags for the client
    private boolean first;
    private boolean last;

    public PagedResponse() {}

    public PagedResponse(List<T> content, int page, int size,
                         long totalElements) {
        this.content       = content;
        this.page          = page;
        this.size          = size;
        this.totalElements = totalElements;
        // Calculate total pages — ceiling division
        this.totalPages    = size == 0 ? 0
                : (int) Math.ceil((double) totalElements / size);
        this.first         = page == 0;
        this.last          = page >= this.totalPages - 1;
    }

    public List<T>  getContent()       { return content; }
    public int      getPage()          { return page; }
    public int      getSize()          { return size; }
    public long     getTotalElements() { return totalElements; }
    public int      getTotalPages()    { return totalPages; }
    public boolean  isFirst()          { return first; }
    public boolean  isLast()           { return last; }

    public void setContent(List<T> content)            { this.content = content; }
    public void setPage(int page)                      { this.page = page; }
    public void setSize(int size)                      { this.size = size; }
    public void setTotalElements(long totalElements)   { this.totalElements = totalElements; }
    public void setTotalPages(int totalPages)          { this.totalPages = totalPages; }
    public void setFirst(boolean first)                { this.first = first; }
    public void setLast(boolean last)                  { this.last = last; }
}