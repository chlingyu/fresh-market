package com.freshmarket.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 分页响应封装类
 */
@Schema(description = "分页响应格式")
public class PageResponse<T> {

    @Schema(description = "数据内容")
    private List<T> content;

    @Schema(description = "分页信息")
    private PageInfo page;

    public PageResponse() {}

    public PageResponse(List<T> content, PageInfo page) {
        this.content = content;
        this.page = page;
    }

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public PageInfo getPage() {
        return page;
    }

    public void setPage(PageInfo page) {
        this.page = page;
    }

    @Schema(description = "分页信息")
    public static class PageInfo {
        @Schema(description = "当前页码", example = "0")
        private int page;

        @Schema(description = "每页数量", example = "20")
        private int size;

        @Schema(description = "总记录数", example = "100")
        private long totalElements;

        @Schema(description = "总页数", example = "5")
        private int totalPages;

        @Schema(description = "是否有下一页")
        private boolean hasNext;

        @Schema(description = "是否有上一页")
        private boolean hasPrevious;

        public PageInfo() {}

        public PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        // Getters and Setters
        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public boolean isHasNext() {
            return hasNext;
        }

        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }

        public boolean isHasPrevious() {
            return hasPrevious;
        }

        public void setHasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
        }
    }
}