package com.example.graphql.book;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL API for books.
 *
 * <p>{@code @QueryMapping}/{@code @MutationMapping} bind a method to a field under the {@code Query}
 * or {@code Mutation} type. The field name defaults to the method name, and the {@code @Argument}
 * parameter is bound by name (which is why {@code -parameters} compilation matters - Spring Boot's
 * parent POM enables it for you).
 *
 * <p>The interesting part is {@link #author(List)}. Resolving {@code Book.author} one book at a time
 * would trigger one author lookup per book - the classic N+1 problem. A {@code @BatchMapping}
 * instead receives <em>all</em> the parent books collected across the query and resolves them in a
 * single call. Three books, one lookup.
 */
@Controller
public class BookController {

    private final BookRepository bookRepository;

    private final AuthorRepository authorRepository;

    public BookController(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @QueryMapping
    public Book bookById(@Argument String id) {
        return this.bookRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Book> books(@Argument String authorId) {
        if (authorId == null) {
            return this.bookRepository.findAll();
        }
        if (!this.authorRepository.existsById(authorId)) {
            throw new AuthorNotFoundException(authorId);
        }
        return this.bookRepository.findByAuthorId(authorId);
    }

    @MutationMapping
    public Book addBook(@Argument @Valid BookInput input) {
        Author author = requireAuthor(input.authorId());
        return this.bookRepository.save(new Book(
                this.bookRepository.nextId(), input.title(), input.isbn(), input.publishedYear(), author.id()));
    }

    @MutationMapping
    public Book updateBook(@Argument String id, @Argument @Valid BookInput input) {
        Book existing = this.bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        Author author = requireAuthor(input.authorId());
        return this.bookRepository.save(new Book(
                existing.id(), input.title(), input.isbn(), input.publishedYear(), author.id()));
    }

    @MutationMapping
    public boolean deleteBook(@Argument String id) {
        return this.bookRepository.deleteById(id);
    }

    /**
     * Batch resolver for {@code Book.author}: called once per request with every book that needs its
     * author resolved, and answers with a single store lookup.
     */
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        Set<String> authorIds = books.stream().map(Book::authorId).collect(Collectors.toSet());

        Map<String, Author> authorsById = new LinkedHashMap<>();
        for (Author author : this.authorRepository.findByIds(authorIds)) {
            authorsById.put(author.id(), author);
        }

        Map<Book, Author> authorsByBook = new LinkedHashMap<>();
        for (Book book : books) {
            authorsByBook.put(book, authorsById.get(book.authorId()));
        }
        return authorsByBook;
    }

    private Author requireAuthor(String authorId) {
        return this.authorRepository.findById(authorId).orElseThrow(() -> new AuthorNotFoundException(authorId));
    }
}
