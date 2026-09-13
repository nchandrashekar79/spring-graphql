package com.example.graphql;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test: real HTTP transport on a random port, real controllers, and the real seeded
 * in-memory repositories.
 *
 * <p>Assertions deliberately avoid exact collection sizes that a mutating test could change, so the
 * suite stays order-independent while sharing one application context.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
class GraphQlApiIntegrationTests {

    @Autowired
    private HttpGraphQlTester graphQlTester;

    @Test
    void findsASeededBookWithItsAuthor() {
        this.graphQlTester.documentName("bookById")
                .variable("id", "1")
                .execute()
                .path("bookById.title").entity(String.class).isEqualTo("The Hobbit")
                .path("bookById.isbn").entity(String.class).isEqualTo("9780261102217")
                .path("bookById.publishedYear").entity(Integer.class).isEqualTo(1937)
                .path("bookById.author.name").entity(String.class).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    void filtersBooksByAuthor() {
        List<String> authorIds = this.graphQlTester.documentName("booksByAuthor")
                .variable("authorId", "2")
                .execute()
                .path("books[*].author.id")
                .entityList(String.class)
                .get();

        assertThat(authorIds).containsOnly("2").hasSize(2);
    }

    @Test
    void listsAuthorsWithTheirBooks() {
        this.graphQlTester.documentName("authorsWithBooks")
                .execute()
                .path("authors[*].name").entityList(String.class)
                .contains("J.R.R. Tolkien", "Ursula K. Le Guin", "Douglas Adams");
    }

    @Test
    void addsABookThenReadsItBack() {
        String newId = this.graphQlTester.documentName("addBook")
                .variable("input", Map.of(
                        "title", "The Silmarillion",
                        "isbn", "9780261102736",
                        "publishedYear", 1977,
                        "authorId", "1"))
                .execute()
                .path("addBook.id").entity(String.class)
                .get();

        assertThat(newId).isNotBlank();

        this.graphQlTester.documentName("bookById")
                .variable("id", newId)
                .execute()
                .path("bookById.title").entity(String.class).isEqualTo("The Silmarillion")
                .path("bookById.author.name").entity(String.class).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    void addsAnAuthorThenReadsItBack() {
        String newId = this.graphQlTester
                .document("mutation { addAuthor(input: { name: \"Terry Pratchett\" }) { id } }")
                .execute()
                .path("addAuthor.id").entity(String.class)
                .get();

        assertThat(newId).isNotBlank();

        this.graphQlTester.document("query findAuthor($id: ID!) { authorById(id: $id) { name } }")
                .variable("id", newId)
                .execute()
                .path("authorById.name").entity(String.class).isEqualTo("Terry Pratchett");
    }

    @Test
    void reportsAnUnknownBookAsNotFound() {
        this.graphQlTester.documentName("updateBook")
                .variable("id", "9999")
                .variable("input", Map.of("title", "Nope", "authorId", "1"))
                .execute()
                .errors()
                .satisfy((errors) -> assertThat(errors).anySatisfy((error) -> {
                    assertThat(error.getMessage()).contains("No book found with id 9999");
                    assertThat(error.getExtensions()).containsEntry("classification", "NOT_FOUND");
                }));
    }

    @Test
    void reportsInvalidInputAsBadRequest() {
        this.graphQlTester.documentName("addBook")
                .variable("input", Map.of("title", " ", "authorId", "1"))
                .execute()
                .errors()
                .satisfy((errors) -> assertThat(errors).anySatisfy((error) -> {
                    assertThat(error.getMessage()).contains("title must not be blank");
                    assertThat(error.getExtensions()).containsEntry("classification", "BAD_REQUEST");
                }));
    }
}
