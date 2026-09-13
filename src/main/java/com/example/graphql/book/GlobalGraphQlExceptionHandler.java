package com.example.graphql.book;

import java.util.stream.Collectors;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import jakarta.validation.ConstraintViolationException;

import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Turns domain and validation failures into well-formed GraphQL errors.
 *
 * <p>Without this, an exception thrown from a data fetcher surfaces as a generic
 * {@code INTERNAL_ERROR} with a deliberately unhelpful message. Declaring
 * {@code @GraphQlExceptionHandler} methods in a {@code @ControllerAdvice} applies them to every
 * controller, and lets us attach a meaningful {@code extensions.classification} so clients can
 * react programmatically.
 *
 * <p>Note the injected {@code GraphqlErrorBuilder}: it arrives pre-populated with the current
 * {@code DataFetchingEnvironment}, so the resulting error is attributed to the right field.
 */
@ControllerAdvice
public class GlobalGraphQlExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleBookNotFound(BookNotFoundException ex, GraphqlErrorBuilder<?> errorBuilder) {
        return errorBuilder.errorType(ErrorType.NOT_FOUND).message(ex.getMessage()).build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleAuthorNotFound(AuthorNotFoundException ex, GraphqlErrorBuilder<?> errorBuilder) {
        return errorBuilder.errorType(ErrorType.NOT_FOUND).message(ex.getMessage()).build();
    }

    /** Bean Validation failures, e.g. a blank book title. */
    @GraphQlExceptionHandler
    public GraphQLError handleConstraintViolation(ConstraintViolationException ex,
            GraphqlErrorBuilder<?> errorBuilder) {

        String message = ex.getConstraintViolations()
                .stream()
                .map((violation) -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return errorBuilder.errorType(ErrorType.BAD_REQUEST).message(message).build();
    }

    /** Argument binding failures, e.g. a value that cannot be converted to the target type. */
    @GraphQlExceptionHandler
    public GraphQLError handleBindingFailure(BindException ex, GraphqlErrorBuilder<?> errorBuilder) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(GlobalGraphQlExceptionHandler::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        return errorBuilder.errorType(ErrorType.BAD_REQUEST).message(message).build();
    }

    /** Renders one binding error as "field: reason", falling back when there is no detail. */
    private static String describe(ObjectError error) {
        String reason = error.getDefaultMessage();
        if (reason == null) {
            reason = "invalid value";
        }
        return (error instanceof FieldError fieldError) ? fieldError.getField() + ": " + reason : reason;
    }
}
