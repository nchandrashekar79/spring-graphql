package com.example.graphql.book;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL API for authors.
 *
 * <p>{@link #books(List)} is the batch resolver for the {@code Author.books} field, mirroring the
 * approach in {@link BookController}: many authors, one lookup.
 */
@Controller
public class AuthorController {

    private final AuthorRepository authorRepository;

    private final BookRepository bookRepository;

    public AuthorController(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @QueryMapping
    public List<Author> authors() {
        return this.authorRepository.findAll();
    }

    @QueryMapping
    public Author authorById(@Argument String id) {
        return this.authorRepository.findById(id).orElse(null);
    }

    @MutationMapping
    public Author addAuthor(@Argument @Valid AuthorInput input) {
        return this.authorRepository.save(new Author(this.authorRepository.nextId(), input.name()));
    }

    /** Batch resolver for {@code Author.books}: one store lookup for every author in the query. */
    @BatchMapping
    public Map<Author, List<Book>> books(List<Author> authors) {
        Map<String, List<Book>> booksByAuthorId = this.bookRepository
                .findByAuthorIds(authors.stream().map(Author::id).toList())
                .stream()
                .collect(Collectors.groupingBy(Book::authorId));

        Map<Author, List<Book>> result = new LinkedHashMap<>();
        for (Author author : authors) {
            result.put(author, booksByAuthorId.getOrDefault(author.id(), List.of()));
        }
        return result;
    }
}
