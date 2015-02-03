package com.wit.databaselibrary.model;

public class DatabaseObject {
	private final Long id;

	public DatabaseObject( final Long id ) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}
}