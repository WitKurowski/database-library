package com.wit.databaselibrary.model.id;

import com.wit.databaselibrary.model.DatabaseObject;

/**
 * A wrapper for the simplest and most common use of {@link DatabaseObject} IDs.
 */
public class SimpleIdWrapper implements IdWrapper {
	private final Long id;

	public SimpleIdWrapper( final Long id ) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	@Override
	public boolean equals( final Object o ) {
		if ( this == o ) {
			return true;
		}

		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final SimpleIdWrapper simpleId = (SimpleIdWrapper) o;

		if ( !this.id.equals( simpleId.id ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
}