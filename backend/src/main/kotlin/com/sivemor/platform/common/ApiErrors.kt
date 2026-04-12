package com.sivemor.platform.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

open class ApiException(
    val status: HttpStatus,
    override val message: String
) : RuntimeException(message)

class BadRequestException(message: String) : ApiException(HttpStatus.BAD_REQUEST, message)
class ForbiddenException(message: String) : ApiException(HttpStatus.FORBIDDEN, message)
class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, message)

data class ApiErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val details: Map<String, String>? = null
)

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = exception.status,
        message = exception.message,
        path = request.requestURI
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val details = exception.bindingResult
            .allErrors
            .filterIsInstance<FieldError>()
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }

        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "Validation failed",
            path = request.requestURI,
            details = details
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.BAD_REQUEST,
        message = exception.message ?: "Constraint violation",
        path = request.requestURI
    )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.BAD_REQUEST,
        message = exception.mostSpecificCause?.message ?: "Malformed JSON request",
        path = request.requestURI
    )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.FORBIDDEN,
        message = exception.message ?: "Access denied",
        path = request.requestURI
    )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        exception: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        logger.error("Unhandled exception for {}", request.requestURI, exception)
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            message = exception.message ?: "Unexpected server error",
            path = request.requestURI
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        path: String,
        details: Map<String, String>? = null
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
        ApiErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = path,
            details = details
        )
    )
}
