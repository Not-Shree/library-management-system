package com.college.library.mapper;

import com.college.library.dto.BookDtos.BookCopyResponse;
import com.college.library.dto.BookDtos.BookResponse;
import com.college.library.entity.Book;
import com.college.library.entity.BookCopy;
import com.college.library.entity.Borrowing;

public final class BookMapper {

    private BookMapper() {
    }

    public static BookResponse toResponse(Book b, long totalCopies, long availableCopies, long activeReservations) {
        return new BookResponse(b.getId(), b.getIsbn(), b.getTitle(), b.getSubtitle(),
                b.getAuthor().getId(), b.getAuthor().getName(),
                b.getPublisher() != null ? b.getPublisher().getId() : null,
                b.getPublisher() != null ? b.getPublisher().getName() : null,
                b.getCategory().getId(), b.getCategory().getName(),
                b.getLanguage(), b.getEdition(), b.getPublicationYear(), b.getDescription(), b.getShelfNumber(),
                b.getCoverImageUrl(), totalCopies, availableCopies, activeReservations,
                b.getCreatedAt(), b.getUpdatedAt());
    }

    /** activeLoan may be null when the copy is on the shelf. */
    public static BookCopyResponse toCopyResponse(BookCopy c, Borrowing activeLoan) {
        return new BookCopyResponse(c.getId(), c.getBook().getId(), c.getBook().getTitle(), c.getCopyCode(),
                c.getStatus().name(), c.getAcquiredDate(), c.getNotes(),
                activeLoan != null ? activeLoan.getMember().getFullName() + " (" + activeLoan.getMember().getMemberCode() + ")" : null,
                activeLoan != null ? activeLoan.getDueDate() : null);
    }
}
