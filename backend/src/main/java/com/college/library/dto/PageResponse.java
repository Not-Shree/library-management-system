package com.college.library.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON shape for paginated results (instead of serialising Spring's Page). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, page.getContent());
    }
}
