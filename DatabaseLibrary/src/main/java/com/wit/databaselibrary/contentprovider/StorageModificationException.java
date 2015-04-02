package com.wit.databaselibrary.contentprovider;

/**
 * Used when an attempt to add, update, or delete data from storage has failed.
 */
public class StorageModificationException extends Exception {
	public StorageModificationException( final String detailedMessage, final Throwable throwable ) {
		super( detailedMessage, throwable );
	}
}