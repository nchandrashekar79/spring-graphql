package com.example.graphql.book;

/**
 * A book author.
 *
 * <p>The {@code Author.books} GraphQL field is resolved separately by {@link AuthorController}.
 * As a record it provides the {@code equals}/{@code hashCode} pair required for batch loading.
 */
public record Author(String id, String name) {
}
