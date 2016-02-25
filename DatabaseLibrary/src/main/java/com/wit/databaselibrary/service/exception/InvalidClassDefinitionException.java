package com.wit.databaselibrary.service.exception;

public class InvalidClassDefinitionException extends RuntimeException {
	public InvalidClassDefinitionException() {
	}

	public InvalidClassDefinitionException( final Throwable cause ) {
		super( cause );
	}

	public InvalidClassDefinitionException( final String message ) {
		super( message );
	}

	public InvalidClassDefinitionException( final String message, final Throwable cause ) {
		super( message, cause );
	}

	public InvalidClassDefinitionException( final String message, final Throwable cause,
			final boolean enableSuppression, final boolean writableStackTrace ) {
		super( message, cause, enableSuppression, writableStackTrace );
	}
}