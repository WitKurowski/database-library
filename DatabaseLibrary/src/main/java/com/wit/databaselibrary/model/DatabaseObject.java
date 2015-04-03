package com.wit.databaselibrary.model;

public class DatabaseObject {
	/**
	 * The unique identifier for this object.
	 */
	private final Long id;

	/**
	 * The numerical value used to determine which instance of an object is most up-to-date.
	 */
	private final Long version;

	/**
	 * Creates a new {@link DatabaseObject}.
	 *
	 * @param id The unique identifier for this object.
	 */
	public DatabaseObject( final Long id ) {
		this(id, 1L);
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
	 * Returns whether this object and its ID are managed remotely.
	 *
	 * @return Whether this object and its ID are managed remotely.
	 */
	public boolean isManagedRemotely() {
		return false;
	}
}