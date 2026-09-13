package com.example.graphql.book;

/**
 * Thrown when an operation targets a book that does not exist.
 *
 * <p>Mapped to a {@code NOT_FOUND} GraphQL error by {@link GlobalGraphQlExceptionHandler}.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(String id) {
        super("No book found with id " + id);
    }
}
