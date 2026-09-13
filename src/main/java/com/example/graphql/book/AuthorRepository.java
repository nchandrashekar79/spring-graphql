package com.example.graphql.book;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/**
 * In-memory author store.
 *
 * <p>{@link #findByIds(Collection)} backs the {@code Book.author} batch resolver so that resolving
 * the author of many books costs a single lookup.
 */
@Repository
public class AuthorRepository {

    private final Map<String, Author> authors = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong();

    /** Allocates the next id. Called by the seeder and by {@code addAuthor}. */
    public String nextId() {
        return String.valueOf(this.sequence.incrementAndGet());
    }

    public Author save(Author author) {
        this.authors.put(author.id(), author);
        return author;
    }

    public Optional<Author> findById(String id) {
        return Optional.ofNullable(this.authors.get(id));
    }

    public boolean existsById(String id) {
        return this.authors.containsKey(id);
    }

    public List<Author> findAll() {
        return sorted(this.authors.values());
    }

    /** Batch lookup used by the {@code Book.author} resolver: one pass for many ids. */
    public List<Author> findByIds(Collection<String> ids) {
        Set<String> wanted = Set.copyOf(ids);
        return sorted(this.authors.values().stream().filter((author) -> wanted.contains(author.id())).toList());
    }

    /** Stable ordering by numeric id, so query results (and tests) are deterministic. */
    private static List<Author> sorted(Collection<Author> values) {
        return values.stream()
                .sorted(Comparator.comparingLong((Author author) -> Long.parseLong(author.id())))
                .toList();
    }
}
