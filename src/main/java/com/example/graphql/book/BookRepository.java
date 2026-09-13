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
 * In-memory book store.
 *
 * <p>Deliberately simple - the point of this project is the GraphQL layer, not persistence. Swap in
 * Spring Data JPA here and nothing else needs to change.
 *
 * <p>{@link #findByAuthorIds(Collection)} is what makes batch loading honest: it answers "all books
 * for these authors" in <em>one</em> pass over the store, rather than looping and querying per
 * author.
 */
@Repository
public class BookRepository {

    private final Map<String, Book> books = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong();

    /** Allocates the next id. Called by the seeder and by {@code addBook}. */
    public String nextId() {
        return String.valueOf(this.sequence.incrementAndGet());
    }

    public Book save(Book book) {
        this.books.put(book.id(), book);
        return book;
    }

    public Optional<Book> findById(String id) {
        return Optional.ofNullable(this.books.get(id));
    }

    public List<Book> findAll() {
        return sorted(this.books.values());
    }

    public List<Book> findByAuthorId(String authorId) {
        return sorted(this.books.values().stream().filter((book) -> book.authorId().equals(authorId)).toList());
    }

    /** Batch lookup used by the {@code Author.books} resolver: one pass for many author ids. */
    public List<Book> findByAuthorIds(Collection<String> authorIds) {
        Set<String> wanted = Set.copyOf(authorIds);
        return sorted(this.books.values().stream().filter((book) -> wanted.contains(book.authorId())).toList());
    }

    public boolean deleteById(String id) {
        return this.books.remove(id) != null;
    }

    /** Stable ordering by numeric id, so query results (and tests) are deterministic. */
    private static List<Book> sorted(Collection<Book> values) {
        return values.stream().sorted(Comparator.comparingLong((Book book) -> Long.parseLong(book.id()))).toList();
    }
}
