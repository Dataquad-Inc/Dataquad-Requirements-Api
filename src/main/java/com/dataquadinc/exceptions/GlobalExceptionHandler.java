package com.dataquadinc.exceptions;

import java.time.LocalDateTime;

import com.dataquadinc.dto.ResponseBean;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {



	@ExceptionHandler(RequirementAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleRequirementAlreadyExists(RequirementAlreadyExistsException ex) {
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.OK.value(), ex.getMessage(),
				LocalDateTime.now());
		return new ResponseEntity<>(errorResponse, HttpStatus.OK);
	}
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
		// Return a meaningful response with a 400 Bad Request status
		return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(RequirementNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleRequirementNotFound(RequirementNotFoundException ex) {
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.OK.value(), ex.getMessage(),
				LocalDateTime.now());
		return new ResponseEntity<>(errorResponse, HttpStatus.OK);
	}

	@ExceptionHandler(NoJobsAssignedToRecruiterException.class)
	public ResponseEntity<ErrorResponse> handleNoJobsAssignedToRecruiter(NoJobsAssignedToRecruiterException ex) {
		// Prepare the error response
		ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.NOT_FOUND.value(),  // Using 404 to indicate resource not found
				ex.getMessage(),
				LocalDateTime.now()
		);

		// Return the response entity with 404 status
		return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
	}


	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
		ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"An unexpected error occurred", LocalDateTime.now());
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
		StringBuilder errorMessage = new StringBuilder();
		ex.getConstraintViolations().forEach(violation -> {
			errorMessage.append(violation.getMessage()).append("\n");
		});
		return new ResponseEntity<>(errorMessage.toString(), HttpStatus.BAD_REQUEST);
	}


}
