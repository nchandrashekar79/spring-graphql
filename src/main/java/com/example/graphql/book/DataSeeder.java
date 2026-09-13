package com.example.graphql.book;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a small, fixed catalogue on startup so the queries in the README (and the integration
 * tests) always have something to work with.
 *
 * <p>Ids are allocated from each repository's sequence, which makes them deterministic:
 * authors are {@code 1..3} and books are {@code 1..6}.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final AuthorRepository authorRepository;

    private final BookRepository bookRepository;

    public DataSeeder(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Author tolkien = createAuthor("J.R.R. Tolkien");
        Author leGuin = createAuthor("Ursula K. Le Guin");
        Author adams = createAuthor("Douglas Adams");

        createBook("The Hobbit", "9780261102217", 1937, tolkien);
        createBook("The Fellowship of the Ring", "9780261102354", 1954, tolkien);
        createBook("The Return of the King", "9780261102361", 1955, tolkien);
        createBook("A Wizard of Earthsea", "9780547773742", 1968, leGuin);
        createBook("The Dispossessed", "9780061054884", 1974, leGuin);
        createBook("The Hitchhiker's Guide to the Galaxy", "9780345391803", 1979, adams);
    }

    private Author createAuthor(String name) {
        return this.authorRepository.save(new Author(this.authorRepository.nextId(), name));
    }

    private void createBook(String title, String isbn, int publishedYear, Author author) {
        this.bookRepository.save(new Book(
                this.bookRepository.nextId(), title, isbn, publishedYear, author.id()));
    }
}
