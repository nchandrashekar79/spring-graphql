# spring-graphql

A small, complete **Spring Boot + Spring for GraphQL** example built around a *Books & Authors*
domain. It is meant to be read as much as run: every GraphQL concept here is deliberately
demonstrated rather than merely configured.

## What it demonstrates

| Concept | Where to look |
| --- | --- |
| Schema-first GraphQL (`.graphqls`) | `src/main/resources/graphql/schema.graphqls` |
| Queries, mutations, arguments | `BookController`, `AuthorController` |
| Nested field resolution | `Book.author`, `Author.books` |
| **Batch loading / avoiding N+1** | `@BatchMapping` in both controllers |
| Bean Validation on input types | `BookInput`, `AuthorInput` |
| Typed GraphQL errors | `GlobalGraphQlExceptionHandler` |
| Slice tests, integration tests, batch verification | `src/test/java/...` |
| GraphiQL + schema printer | `application.yml` |

## Tech stack

| Component | Version |
| --- | --- |
| Spring Boot | 4.1.1 |
| Spring for GraphQL | 2.0.5 |
| GraphQL Java | 25.0 |
| java-dataloader | 6.0.0 |
| Java | 21 |
| Build | Maven (wrapper included) |

> **Spring Boot 4 note.** Boot 4 renamed some starters and split test support into per-slice
> artifacts. This project uses `spring-boot-starter-webmvc` (not `-web`) and gets its test support
> from `spring-boot-starter-graphql-test`, `spring-boot-starter-webmvc-test`, and
> `spring-boot-starter-validation-test` — there is no single `spring-boot-starter-test` here.

## Prerequisites

Only a **JDK 21 or newer** is required — Maven itself is provided by the wrapper.

```powershell
java -version
```

## Running

```powershell
.\mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run         # macOS / Linux
```

The app starts on port 8080 with a small catalogue seeded in memory:

- Authors — `1` J.R.R. Tolkien, `2` Ursula K. Le Guin, `3` Douglas Adams
- Books — `1` The Hobbit, `2` The Fellowship of the Ring, `3` The Return of the King,
  `4` A Wizard of Earthsea, `5` The Dispossessed, `6` The Hitchhiker's Guide to the Galaxy

| Endpoint | Purpose |
| --- | --- |
| `POST /graphql` | The GraphQL API |
| `/graphiql` | In-browser IDE — <http://localhost:8080/graphiql> |
| `/graphql/schema` | The schema as SDL text |

> The data is in-memory and resets on every restart. Mutations are therefore not persisted.

## Example operations

Paste these into GraphiQL, or send them with `curl`.

### Query a single book (with its author)

```graphql
query {
  bookById(id: "1") {
    id
    title
    isbn
    publishedYear
    author {
      id
      name
    }
  }
}
```

### Filter books by author

```graphql
query {
  books(authorId: "1") {
    title
    author {
      name
    }
  }
}
```

Omit the `authorId` argument to get every book.

### Nested collections

```graphql
query {
  authors {
    name
    books {
      title
      publishedYear
    }
  }
}
```

### Create a book

```graphql
mutation {
  addBook(input: {
    title: "The Silmarillion"
    isbn: "9780261102736"
    publishedYear: 1977
    authorId: "1"
  }) {
    id
    title
    author {
      name
    }
  }
}
```

### Update, delete, and add an author

```graphql
mutation {
  updateBook(id: "1", input: { title: "The Hobbit (Revised)", authorId: "1" }) {
    id
    title
  }
}
```

```graphql
mutation {
  deleteBook(id: "6")
}
```

```graphql
mutation {
  addAuthor(input: { name: "Terry Pratchett" }) {
    id
    name
  }
}
```

### Errors

An unknown book id produces a `NOT_FOUND` error. This is the work of
`GlobalGraphQlExceptionHandler` — without it you would get an opaque `INTERNAL_ERROR`.

```graphql
mutation {
  updateBook(id: "9999", input: { title: "Nope", authorId: "1" }) {
    id
  }
}
```

```json
{
  "data": null,
  "errors": [
    {
      "message": "No book found with id 9999",
      "locations": [{ "line": 2, "column": 3 }],
      "path": ["updateBook"],
      "extensions": { "classification": "NOT_FOUND" }
    }
  ]
}
```

A blank title fails Bean Validation and is reported as `BAD_REQUEST`:

```graphql
mutation {
  addBook(input: { title: "   ", authorId: "1" }) {
    id
  }
}
```

The message includes the offending field path, for example
`addBook.input.title: title must not be blank`.

### From the command line

```powershell
curl.exe -s http://localhost:8080/graphql `
  -H "Content-Type: application/json" `
  -d '{\"query\":\"{ bookById(id: \\\"1\\\") { title author { name } } }\"}'
```

## Why `@BatchMapping` matters

Consider `books(authorId: "1") { author { name } }`. The naive implementation resolves each book's
author independently:

```text
3 books -> 3 separate author lookups   (the N+1 problem)
```

`@BatchMapping` changes the shape of the call. Spring GraphQL collects every book in the query and
invokes the resolver **once** with the whole list:

```java
@BatchMapping
public Map<Book, Author> author(List<Book> books) { ... }
```

```text
3 books -> 1 author lookup
```

Two details make this work in practice:

1. **The repository has a genuine batch method.** `AuthorRepository.findByIds(...)` answers "these
   authors" in a single pass. Batching in the resolver but looping in the repository would just
   move the N+1 rather than remove it.
2. **Parent objects are used as map keys**, so `Book` and `Author` are records. Records generate
   `equals`/`hashCode`, which is required for the loader to correlate results back to sources.

This is verified rather than asserted — `BookControllerSliceTests` mocks the repositories and
checks the call count:

```java
verify(this.authorRepository, times(1)).findByIds(any());
```

To watch it happen at runtime, enable debug logging:

```powershell
$env:LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_GRAPHQL='DEBUG'; .\mvnw.cmd spring-boot:run
```

## Project layout

```text
src/main/java/com/example/graphql/
  SpringGraphqlApplication.java
  book/
    Book.java, Author.java                 # records; equals/hashCode enable batch loading
    BookInput.java, AuthorInput.java       # validated input types
    BookRepository.java, AuthorRepository.java   # in-memory stores, with batch lookups
    DataSeeder.java                        # seeds the catalogue on startup
    BookController.java, AuthorController.java   # @QueryMapping / @MutationMapping / @BatchMapping
    GlobalGraphQlExceptionHandler.java     # @ControllerAdvice + @GraphQlExceptionHandler
    BookNotFoundException.java, AuthorNotFoundException.java
src/main/resources/
  application.yml                          # GraphiQL, schema printer, virtual threads
  graphql/schema.graphqls                  # the schema (auto-discovered)
src/test/
  java/.../book/BookControllerSliceTests.java      # @GraphQlTest, mocked repos, batch verification
  java/.../book/AuthorControllerSliceTests.java    # @GraphQlTest for the author side
  java/.../GraphQlApiIntegrationTests.java         # @SpringBootTest over real HTTP
  resources/graphql-test/*.graphql                 # named documents used via documentName(...)
```

## Tests

```powershell
.\mvnw.cmd test
```

18 tests across three layers:

- **`BookControllerSliceTests`** — `@GraphQlTest` loads the GraphQL infrastructure and one
  controller with mocked repositories. Fast, and it pins down the batching call count.
- **`AuthorControllerSliceTests`** — the same idea for `Author.books`, including an author with no
  books.
- **`GraphQlApiIntegrationTests`** — `@SpringBootTest` with `@AutoConfigureHttpGraphQlTester` runs
  real HTTP requests against the seeded data, which is where wire-level details such as
  `extensions.classification` are asserted.

A note on that split: `extensions.classification` shows up over a real transport but not in the
server-side slice tester, so error assertions are deliberately divided — messages in the slice
tests, classifications over HTTP. The comment in `BookControllerSliceTests` records why.

## Configuration

`src/main/resources/application.yml`:

| Setting | Effect |
| --- | --- |
| `spring.graphql.graphiql.enabled` | Serves the GraphiQL UI at `/graphiql` |
| `spring.graphql.schema.printer.enabled` | Serves the SDL at `/graphql/schema` |
| `spring.threads.virtual.enabled` | Runs blocking controller methods on virtual threads (Java 21+) |

## Design decisions

- **In-memory persistence.** The subject is GraphQL, not storage. Both repositories expose narrow
  interfaces, so swapping in Spring Data JPA would not touch the controllers.
- **Schema-first.** The `.graphqls` file is the contract; the controllers adapt to it rather than
  the reverse.
- **`Book` holds `authorId`, not an `Author`.** This keeps the graph traversal explicit and is
  precisely what creates the opportunity to batch.
- **Records for domain types.** They supply the value semantics batch loading depends on.

## Possible next steps

Deliberately out of scope, but each is a natural extension:

- **Subscriptions** over WebSocket or SSE (`@SubscriptionMapping` + `spring-boot-starter-websocket`)
- **Cursor pagination** using the GraphQL Cursor Connections spec (`Connection`/`Edge`/`PageInfo`)
- **Security** with Spring Security, including field-level authorization
- **Custom scalars** and a `RuntimeWiringConfigurer`
- **A real database** via Spring Data JPA and H2, plus `@Transactional` mutations
- **A Java GraphQL client** using `HttpGraphQlClient`
