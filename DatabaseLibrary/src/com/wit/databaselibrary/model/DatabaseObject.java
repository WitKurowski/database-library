package com.wit.databaselibrary.model;

public class DatabaseObject {
	/**
	 * The unique identifier for this object.
	 */
	private final Long id;

	/**
	 * The numerical value used to determine which instance of an object is most up-to-date.
	 */
	private Long version;

	/**
	 * Creates a new {@link DatabaseObject}.
	 *
	 * @param id The unique identifier for this object.
	 */
	public DatabaseObject( final Long id ) {
		this( id, 1L );
	}

	/**
	 * Creates a new {@link DatabaseObject}.
	 *
	 * @param id The unique identifier for this object.
	 * @param version The numerical value used to determine which instance of an object is most up-to-date.
	 */
	public DatabaseObject( final Long id, final Long version ) {
		this.id = id;
		this.version = version;
	}

	/**
	 * Returns the unique identifier for this object.
	 *
	 * @return The unique identifier for this object.
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Returns the version of this object, which is incremented every single time the object is modified.
	 *
	 * @return The version of this object, which is incremented every single time the object is modified.
	 */
	public Long getVersion() {
		return this.version;
	}

	/**
	 * Returns whether this object's ID is managed externally.
	 *
	 * @return Whether this object's ID is managed externally.
	 */
	public boolean isIdManagedExternally() {
		return false;
	}

	/**
	 * Returns whether this object's version number is managed externally.
	 *
	 * @return Whether this object's version number is managed externally.
	 */
	public boolean isVersionManagedExternally() {
		return false;
	}

	/**
	 * Sets the numerical value used to determine which instance of an object is most up-to-date.
	 *
	 * @param version The numerical value used to determine which instance of an object is most up-to-date.
	 */
	public void setVersion( final Long version ) {
		this.version = version;
	}
}