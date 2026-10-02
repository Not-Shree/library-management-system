package com.college.library.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Builds a safe Pageable: page size is capped and sort fields come from a whitelist in each controller. */
final class Paging {

    static final int MAX_SIZE = 100;

    private Paging() {
    }

    static Pageable of(int page, int size, String property, String direction) {
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), Sort.by(dir, property).and(Sort.by("id")));
    }
}
