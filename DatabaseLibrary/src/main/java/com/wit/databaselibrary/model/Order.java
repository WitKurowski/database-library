package com.wit.databaselibrary.model;

public enum Order {
	ASCENDING("ASC"), DESCENDING("DESC");

	private final String keyword;

	private Order( final String keyword ) {
		this.keyword = keyword;
	}

	public String getKeyword() {
		return this.keyword;
	}
}