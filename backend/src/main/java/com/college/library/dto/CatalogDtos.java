package com.college.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Authors, publishers and categories. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record AuthorRequest(@NotBlank @Size(max = 120) String name, @Size(max = 1000) String biography) {
    }

    public record AuthorResponse(Long id, String name, String biography, long bookCount) {
    }

    public record PublisherRequest(@NotBlank @Size(max = 120) String name, @Size(max = 255) String address,
                                   @Size(max = 255) String website) {
    }

    public record PublisherResponse(Long id, String name, String address, String website, long bookCount) {
    }

    public record CategoryRequest(@NotBlank @Size(max = 80) String name, @Size(max = 255) String description) {
    }

    public record CategoryResponse(Long id, String name, String description, long bookCount) {
    }
}
