package com.college.library.controller;

import com.college.library.dto.CatalogDtos.*;
import com.college.library.service.CatalogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Authors, publishers and categories. Reading is public (needed for catalogue filters). */
@RestController
@RequestMapping("/api")
@Tag(name = "Authors, publishers & categories")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // ---------------------------------------------------------------- authors
    @GetMapping("/authors")
    public List<AuthorResponse> authors(@RequestParam(required = false) String q) {
        return catalogService.authors(q);
    }

    @PostMapping("/authors")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse createAuthor(@Valid @RequestBody AuthorRequest req) {
        return catalogService.saveAuthor(null, req);
    }

    @PutMapping("/authors/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public AuthorResponse updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequest req) {
        return catalogService.saveAuthor(id, req);
    }

    @DeleteMapping("/authors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable Long id) {
        catalogService.deleteAuthor(id);
    }

    // ---------------------------------------------------------------- publishers
    @GetMapping("/publishers")
    public List<PublisherResponse> publishers(@RequestParam(required = false) String q) {
        return catalogService.publishers(q);
    }

    @PostMapping("/publishers")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PublisherResponse createPublisher(@Valid @RequestBody PublisherRequest req) {
        return catalogService.savePublisher(null, req);
    }

    @PutMapping("/publishers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public PublisherResponse updatePublisher(@PathVariable Long id, @Valid @RequestBody PublisherRequest req) {
        return catalogService.savePublisher(id, req);
    }

    @DeleteMapping("/publishers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePublisher(@PathVariable Long id) {
        catalogService.deletePublisher(id);
    }

    // ---------------------------------------------------------------- categories
    @GetMapping("/categories")
    public List<CategoryResponse> categories(@RequestParam(required = false) String q) {
        return catalogService.categories(q);
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return catalogService.saveCategory(null, req);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return catalogService.saveCategory(id, req);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
    }
}
