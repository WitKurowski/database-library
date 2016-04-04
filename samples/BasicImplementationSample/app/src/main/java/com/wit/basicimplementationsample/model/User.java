package com.wit.basicimplementationsample.model;

import com.wit.basicimplementationsample.contentprovider.contract.UserContract;
import com.wit.databaselibrary.annotation.Column;
import com.wit.databaselibrary.annotation.Table;
import com.wit.databaselibrary.model.ColumnType;
import com.wit.databaselibrary.model.DatabaseObject;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Table( tableName = UserContract.Columns.TABLE_NAME )
public class User extends DatabaseObject {
	@Column( columnName = UserContract.Columns.AGE, columnType = ColumnType.INTEGER )
	private Integer age;

	@Column( columnName = UserContract.Columns.FIRST_NAME, columnType = ColumnType.STRING )
	private String firstName;

	public User( final Integer age, final String firstName ) {
		super();

		this.age = age;
		this.firstName = firstName;
	}

	public Integer getAge() {
		return this.age;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setAge( final Integer age ) {
		this.age = age;
	}

	public void setFirstName( final String firstName ) {
		this.firstName = firstName;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString( this );
	}
}