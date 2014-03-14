package com.wit.databaselibrary.model;

public class DatabaseObject {
	private final Integer id;

	public DatabaseObject( final Integer id ) {
		this.id = id;
	}

	public Integer getId() {
		return this.id;
	}
}