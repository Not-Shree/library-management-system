package com.college.library.dto;

import com.college.library.entity.CopyStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BookDtos {

    private BookDtos() {
    }

    public record BookRequest(
            @NotBlank @Pattern(regexp = "^[0-9Xx-]{10,17}$", message = "ISBN must be 10 or 13 digits (dashes allowed)") String isbn,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 200) String subtitle,
            @NotNull(message = "Author is required") Long authorId,
            Long publisherId,
            @NotNull(message = "Category is required") Long categoryId,
            @NotBlank @Size(max = 40) String language,
            @Size(max = 40) String edition,
            @Min(1450) @Max(2100) Integer publicationYear,
            @Size(max = 2000) String description,
            @Size(max = 30) String shelfNumber,
            @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "Cover URL must start with http:// or https://") String coverImageUrl,
            /** Only used when creating a book: how many physical copies to register straight away. */
            @Min(0) @Max(50) Integer initialCopies) {
    }

    public record BookResponse(Long id, String isbn, String title, String subtitle,
                               Long authorId, String authorName, Long publisherId, String publisherName,
                               Long categoryId, String categoryName, String language, String edition,
                               Integer publicationYear, String description, String shelfNumber, String coverImageUrl,
                               long totalCopies, long availableCopies, long activeReservations,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    /** Filters for the book search. Every field is optional. */
    public record BookSearchCriteria(String q, Long categoryId, Long authorId, Long publisherId, String language,
                                     Integer yearFrom, Integer yearTo, Boolean available) {
    }

    /** Add one copy with a chosen code, or several copies with generated codes. */
    public record AddCopiesRequest(
            @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "Use letters, numbers and dashes") String copyCode,
            @Min(1) @Max(50) Integer quantity,
            LocalDate acquiredDate,
            @Size(max = 255) String notes) {
    }

    public record UpdateCopyRequest(@NotNull CopyStatus status, @Size(max = 255) String notes) {
    }

    public record BookCopyResponse(Long id, Long bookId, String bookTitle, String copyCode, String status,
                                   LocalDate acquiredDate, String notes, String currentBorrower, LocalDate dueDate) {
    }
}
