package com.college.library.service;

import com.college.library.dto.CatalogDtos.*;
import com.college.library.entity.Author;
import com.college.library.entity.Category;
import com.college.library.entity.Publisher;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.DuplicateResourceException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.CatalogMapper;
import com.college.library.repository.AuthorRepository;
import com.college.library.repository.BookRepository;
import com.college.library.repository.CategoryRepository;
import com.college.library.repository.PublisherRepository;
import com.college.library.util.TextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Authors, publishers and categories — the small lookup tables of the catalogue. */
@Service
public class CatalogService {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final AuditService auditService;

    public CatalogService(AuthorRepository authorRepository, PublisherRepository publisherRepository,
                          CategoryRepository categoryRepository, BookRepository bookRepository, AuditService auditService) {
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.auditService = auditService;
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows, int idIndex, int countIndex) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] r : rows) map.put((Long) r[idIndex], ((Number) r[countIndex]).longValue());
        return map;
    }

    private static BusinessRuleException inUse(String what) {
        return new BusinessRuleException(HttpStatus.CONFLICT, "IN_USE", "This " + what + " is linked to books and cannot be deleted");
    }

    // ---------------------------------------------------------------- authors
    @Transactional(readOnly = true)
    public List<AuthorResponse> authors(String q) {
        Map<Long, Long> counts = toCountMap(bookRepository.countActiveBooksByAuthor(), 0, 1);
        List<Author> list = TextUtils.hasText(q) ? authorRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim())
                : authorRepository.findAllByOrderByNameAsc();
        return list.stream().map(a -> CatalogMapper.toResponse(a, counts.getOrDefault(a.getId(), 0L))).toList();
    }

    @Transactional
    public AuthorResponse saveAuthor(Long id, AuthorRequest req) {
        String name = req.name().trim();
        boolean duplicate = id == null ? authorRepository.existsByNameIgnoreCase(name)
                : authorRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicate) throw new DuplicateResourceException("DUPLICATE_AUTHOR", "Author \"" + name + "\" already exists");
        Author a = id == null ? new Author() : authorRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Author", id));
        a.setName(name);
        a.setBiography(TextUtils.clean(req.biography()));
        authorRepository.save(a);
        auditService.log(id == null ? "CREATE_AUTHOR" : "UPDATE_AUTHOR", "Author", a.getId(), name);
        return CatalogMapper.toResponse(a, 0);
    }

    @Transactional
    public void deleteAuthor(Long id) {
        Author a = authorRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Author", id));
        if (bookRepository.existsByAuthor_Id(id)) throw inUse("author");
        authorRepository.delete(a);
        auditService.log("DELETE_AUTHOR", "Author", id, a.getName());
    }

    // ---------------------------------------------------------------- publishers
    @Transactional(readOnly = true)
    public List<PublisherResponse> publishers(String q) {
        Map<Long, Long> counts = toCountMap(bookRepository.countActiveBooksByPublisher(), 0, 1);
        List<Publisher> list = TextUtils.hasText(q) ? publisherRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim())
                : publisherRepository.findAllByOrderByNameAsc();
        return list.stream().map(p -> CatalogMapper.toResponse(p, counts.getOrDefault(p.getId(), 0L))).toList();
    }

    @Transactional
    public PublisherResponse savePublisher(Long id, PublisherRequest req) {
        String name = req.name().trim();
        boolean duplicate = id == null ? publisherRepository.existsByNameIgnoreCase(name)
                : publisherRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicate) throw new DuplicateResourceException("DUPLICATE_PUBLISHER", "Publisher \"" + name + "\" already exists");
        Publisher p = id == null ? new Publisher() : publisherRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Publisher", id));
        p.setName(name);
        p.setAddress(TextUtils.clean(req.address()));
        p.setWebsite(TextUtils.clean(req.website()));
        publisherRepository.save(p);
        auditService.log(id == null ? "CREATE_PUBLISHER" : "UPDATE_PUBLISHER", "Publisher", p.getId(), name);
        return CatalogMapper.toResponse(p, 0);
    }

    @Transactional
    public void deletePublisher(Long id) {
        Publisher p = publisherRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Publisher", id));
        if (bookRepository.existsByPublisher_Id(id)) throw inUse("publisher");
        publisherRepository.delete(p);
        auditService.log("DELETE_PUBLISHER", "Publisher", id, p.getName());
    }

    // ---------------------------------------------------------------- categories
    @Transactional(readOnly = true)
    public List<CategoryResponse> categories(String q) {
        Map<Long, Long> counts = toCountMap(bookRepository.countActiveBooksByCategory(), 0, 2);
        List<Category> list = TextUtils.hasText(q) ? categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim())
                : categoryRepository.findAllByOrderByNameAsc();
        return list.stream().map(c -> CatalogMapper.toResponse(c, counts.getOrDefault(c.getId(), 0L))).toList();
    }

    @Transactional
    public CategoryResponse saveCategory(Long id, CategoryRequest req) {
        String name = req.name().trim();
        boolean duplicate = id == null ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id);
        if (duplicate) throw new DuplicateResourceException("DUPLICATE_CATEGORY", "Category \"" + name + "\" already exists");
        Category c = id == null ? new Category() : categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        c.setName(name);
        c.setDescription(TextUtils.clean(req.description()));
        categoryRepository.save(c);
        auditService.log(id == null ? "CREATE_CATEGORY" : "UPDATE_CATEGORY", "Category", c.getId(), name);
        return CatalogMapper.toResponse(c, 0);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category c = categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        if (bookRepository.existsByCategory_Id(id)) throw inUse("category");
        categoryRepository.delete(c);
        auditService.log("DELETE_CATEGORY", "Category", id, c.getName());
    }
}
