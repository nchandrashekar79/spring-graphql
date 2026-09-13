package com.example.graphql.book;

import java.util.List;
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

/** Slice test for {@link AuthorController}, including its batch resolver for {@code Author.books}. */
@GraphQlTest(AuthorController.class)
class AuthorControllerSliceTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private AuthorRepository authorRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @Test
    void returnsAuthorsWithTheirBooksInASingleBatch() {
        Author tolkien = new Author("1", "J.R.R. Tolkien");
        Author leGuin = new Author("2", "Ursula K. Le Guin");
        given(this.authorRepository.findAll()).willReturn(List.of(tolkien, leGuin));
        given(this.bookRepository.findByAuthorIds(any())).willReturn(List.of(
                new Book("1", "The Hobbit", null, 1937, "1"),
                new Book("2", "The Fellowship of the Ring", null, 1954, "1"),
                new Book("4", "A Wizard of Earthsea", null, 1968, "2")));

        this.graphQlTester.documentName("authorsWithBooks")
                .execute()
                .path("authors[0].name").entity(String.class).isEqualTo("J.R.R. Tolkien")
                .path("authors[0].books[*].title").entityList(String.class)
                .containsExactly("The Hobbit", "The Fellowship of the Ring")
                .path("authors[1].name").entity(String.class).isEqualTo("Ursula K. Le Guin")
                .path("authors[1].books[*].title").entityList(String.class)
                .containsExactly("A Wizard of Earthsea");

        // Two authors, one lookup for books.
        verify(this.bookRepository, times(1)).findByAuthorIds(any());
    }

    @Test
    void returnsAnEmptyBookListForAnAuthorWithNoBooks() {
        Author unpublished = new Author("9", "Nobody Yet");
        given(this.authorRepository.findAll()).willReturn(List.of(unpublished));
        given(this.bookRepository.findByAuthorIds(any())).willReturn(List.of());

        List<Object> books = this.graphQlTester.documentName("authorsWithBooks")
                .execute()
                .path("authors[0].books")
                .entityList(Object.class)
                .get();

        assertThat(books).isEmpty();
    }

    @Test
    void returnsAuthorById() {
        given(this.authorRepository.findById("1")).willReturn(Optional.of(new Author("1", "J.R.R. Tolkien")));

        this.graphQlTester.document("query { authorById(id: \"1\") { id name } }")
                .execute()
                .path("authorById.name").entity(String.class).isEqualTo("J.R.R. Tolkien");
    }

    @Test
    void createsAnAuthor() {
        given(this.authorRepository.nextId()).willReturn("4");
        given(this.authorRepository.save(any(Author.class)))
                .willAnswer((invocation) -> invocation.getArgument(0));

        this.graphQlTester.document("mutation { addAuthor(input: { name: \"Terry Pratchett\" }) { id name } }")
                .execute()
                .path("addAuthor.id").entity(String.class).isEqualTo("4")
                .path("addAuthor.name").entity(String.class).isEqualTo("Terry Pratchett");
    }

    @Test
    void rejectsABlankAuthorName() {
        this.graphQlTester.document("mutation { addAuthor(input: { name: \"  \" }) { id } }")
                .execute()
                .errors()
                .satisfy((errors) -> assertThat(errors).anySatisfy((error) -> assertThat(error.getMessage())
                        .contains("name must not be blank")));
    }
}
