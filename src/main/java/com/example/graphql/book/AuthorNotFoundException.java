package com.example.graphql.book;

/**
 * Thrown when an operation targets an author that does not exist.
 *
 * <p>Mapped to a {@code NOT_FOUND} GraphQL error by {@link GlobalGraphQlExceptionHandler}.
 */
public class AuthorNotFoundException extends RuntimeException {

    public AuthorNotFoundException(String id) {
        super("No author found with id " + id);
    }
}
