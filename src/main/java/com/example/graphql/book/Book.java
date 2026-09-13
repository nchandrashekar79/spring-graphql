package com.example.graphql.book;

/**
 * A book.
 *
 * <p>Note that this holds an {@code authorId} rather than an {@link Author} instance. The
 * {@code Book.author} GraphQL field is resolved separately by {@link BookController}, which is what
 * lets us batch the lookup and avoid the N+1 query problem.
 *
 * <p>Being a record, this type gets {@code equals}/{@code hashCode} for free - a requirement for
 * batch loading, where parent objects are used as map keys.
 */
public record Book(String id, String title, String isbn, Integer publishedYear, String authorId) {
}
