package com.example.graphql.book;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Slice test for {@link BookController}.
 *
 * <p>{@code @GraphQlTest} loads the Spring GraphQL infrastructure plus the named controller, with no
 * web transport and no server. The repositories are mocked, which keeps the test fast and - more
 * importantly - lets us assert exactly how often they are called, which is how the batch loading
 * behaviour is proven rather than assumed.
 *
 * <p>Error assertions here check the <em>message</em> produced by
 * {@link GlobalGraphQlExceptionHandler}. The {@code extensions.classification} value is a
 * wire-level detail that only materialises over a real transport, so it is asserted in
 * {@code GraphQlApiIntegrationTests} instead.
 */
@GraphQlTest(BookController.class)
class BookControllerSliceTests {

    private static final Author TOLKIEN = new Author("1", "J.R.R. Tolkien");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private AuthorRepository authorRepository;

    @Test
    void returnsBookWithItsAuthor() {
        given(this.bookRepository.findById("1"))
                .willReturn(Optional.of(new Book("1", "The Hobbit", "9780261102217", 1937, "1")));
        given(this.authorRepository.findByIds(any())).willReturn(List.of(TOLKIEN));

        this.graphQlTester.documentName("bookById")
                .variable("id", "1")
                .execute()
                .path("bookById.title").entity(String.class).isEqualTo("The Hobbit")
                .path("bookById.isbn").entity(String.class).isEqualTo("9780261102217")
                .path("bookById.publishedYear").entity(Integer.class).isEqualTo(1937)
                .path("bookById.author.name").entity(String.class).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    void returnsNullForAnUnknownBook() {
        given(this.bookRepository.findById("404")).willReturn(Optional.empty());

        this.graphQlTester.documentName("bookById")
                .variable("id", "404")
                .execute()
                .path("bookById")
                .valueIsNull();
    }

    @Test
    void resolvesAuthorsForManyBooksInASingleBatch() {
        given(this.authorRepository.existsById("1")).willReturn(true);
        given(this.bookRepository.findByAuthorId("1")).willReturn(List.of(
                new Book("1", "The Hobbit", null, 1937, "1"),
                new Book("2", "The Fellowship of the Ring", null, 1954, "1"),
                new Book("3", "The Return of the King", null, 1955, "1")));
        given(this.authorRepository.findByIds(any())).willReturn(List.of(TOLKIEN));

        this.graphQlTester.documentName("booksByAuthor")
                .variable("authorId", "1")
                .execute()
                .path("books[*].author.name")
                .entityList(String.class)
                .containsExactly("J.R.R. Tolkien", "J.R.R. Tolkien", "J.R.R. Tolkien");

        // The whole point of @BatchMapping: three books resolved their author with ONE
        // lookup. Without it this would have been called once per book (the N+1 problem).
        verify(this.authorRepository, times(1)).findByIds(any());
    }

    @Test
    void createsABook() {
        given(this.authorRepository.findById("1")).willReturn(Optional.of(TOLKIEN));
        given(this.bookRepository.nextId()).willReturn("7");
        given(this.bookRepository.save(any(Book.class))).willAnswer((invocation) -> invocation.getArgument(0));
        given(this.authorRepository.findByIds(any())).willReturn(List.of(TOLKIEN));

        this.graphQlTester.documentName("addBook")
                .variable("input", Map.of("title", "The Silmarillion", "isbn", "9780261102736",
                        "publishedYear", 1977, "authorId", "1"))
                .execute()
                .path("addBook.id").entity(String.class).isEqualTo("7")
                .path("addBook.title").entity(String.class).isEqualTo("The Silmarillion")
                .path("addBook.author.id").entity(String.class).isEqualTo("1");
    }

    @Test
    void deletesABook() {
        given(this.bookRepository.deleteById("1")).willReturn(true);

        this.graphQlTester.documentName("deleteBook")
                .variable("id", "1")
                .execute()
                .path("deleteBook").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void rejectsABlankTitleWithBadRequest() {
        this.graphQlTester.documentName("addBook")
                .variable("input", Map.of("title", "   ", "authorId", "1"))
                .execute()
                .errors()
                .satisfy((errors) -> assertThat(errors).anySatisfy((error) -> assertThat(error.getMessage())
                        .contains("title must not be blank")));
    }

    @Test
    void reportsAnUnknownBookAsNotFound() {
        given(this.bookRepository.findById("9999")).willReturn(Optional.empty());

        this.graphQlTester.documentName("updateBook")
                .variable("id", "9999")
                .variable("input", Map.of("title", "Nope", "authorId", "1"))
                .execute()
                .errors()
                .satisfy((errors) -> assertThat(errors).anySatisfy((error) -> assertThat(error.getMessage())
                        .contains("No book found with id 9999")));
    }

    @Test
    void reportsAnUnknownAuthorWhenFiltering() {
        given(this.authorRepository.existsById("404")).willReturn(false);

        this.graphQlTester.documentName("booksByAuthor")
                .variable("authorId", "404")
                .execute()
                .errors()
                .satisfy((errors) -> assertThat(errors).anySatisfy((error) ->
                        assertThat(error.getMessage()).contains("No author found with id 404")));
    }
}
