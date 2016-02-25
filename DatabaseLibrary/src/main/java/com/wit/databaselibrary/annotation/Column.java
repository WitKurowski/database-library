package com.wit.databaselibrary.annotation;

import com.wit.databaselibrary.model.ColumnType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface Column {
	String columnName();

	ColumnType columnType();
}
