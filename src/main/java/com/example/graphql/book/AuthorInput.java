package com.example.graphql.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Input payload for creating an {@link Author}. */
public record AuthorInput(

        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name must be at most 120 characters")
        String name) {
}
