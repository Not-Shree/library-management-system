package com.college.library.mapper;

import com.college.library.dto.CatalogDtos.*;
import com.college.library.entity.Author;
import com.college.library.entity.Category;
import com.college.library.entity.Publisher;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static AuthorResponse toResponse(Author a, long bookCount) {
        return new AuthorResponse(a.getId(), a.getName(), a.getBiography(), bookCount);
    }

    public static PublisherResponse toResponse(Publisher p, long bookCount) {
        return new PublisherResponse(p.getId(), p.getName(), p.getAddress(), p.getWebsite(), bookCount);
    }

    public static CategoryResponse toResponse(Category c, long bookCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), bookCount);
    }
}
