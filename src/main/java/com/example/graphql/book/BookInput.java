package com.example.graphql.book;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input payload for creating or updating a {@link Book}.
 *
 * <p>The constraints below are enforced by Bean Validation when a controller parameter is annotated
 * with {@code @Valid}. Failures are turned into {@code BAD_REQUEST} GraphQL errors by
 * {@link GlobalGraphQlExceptionHandler}.
 */
public record BookInput(

        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @Size(max = 20, message = "isbn must be at most 20 characters")
        String isbn,

        @Min(value = 1450, message = "publishedYear must be 1450 or later")
        Integer publishedYear,

        @NotBlank(message = "authorId must not be blank")
        String authorId) {
}
