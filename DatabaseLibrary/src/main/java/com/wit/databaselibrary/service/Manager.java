package com.wit.databaselibrary.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.wit.databaselibrary.contentprovider.contract.Contract;
import com.wit.databaselibrary.model.DatabaseObject;

import java.util.ArrayList;
import java.util.List;

public abstract class Manager<T extends DatabaseObject> {
	private final ContentResolver contentResolver;
	protected final String packageName;

	protected Manager( final Context context ) {
		this.contentResolver = context.getContentResolver();
		this.packageName = context.getPackageName();
	}

	public int delete( final List<T> objects ) {
		int numberOfRowsDeleted = 0;

		for ( final T object : objects ) {
			numberOfRowsDeleted += this.delete( object );
		}

		return numberOfRowsDeleted;
	}

	public int delete( final T object ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final Long id = object.getId();
		final String whereClause = BaseColumns._ID + "=" + id.intValue();

		final int numberOfRowsDelete =
				this.contentResolver.delete( contentUri, whereClause, null );

		return numberOfRowsDelete;
	}

	protected abstract ContentValues generateContentValues( final T object );

	public List<T> get() {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<String> projection = contract.getColumnNames();
		final Cursor cursor =
				this.contentResolver.query( contentUri,
						projection.toArray( new String[ projection.size() ] ),
						null, null, null );
		final List<T> objects = new ArrayList<T>();

		if ( cursor != null ) {
			while ( cursor.moveToNext() ) {
				final T object = this.get( cursor );

				objects.add( object );
			}

			cursor.close();
		}

		return objects;
	}

	protected abstract T get( final Cursor cursor );

	public T get( final long id ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<String> projection = contract.getColumnNames();
		final String selection = BaseColumns._ID + " = ?";
		final List<String> selectionArgs = new ArrayList<String>();

		selectionArgs.add( Long.toString( id ) );

		final Cursor cursor =
				this.contentResolver.query(
						contentUri,
						projection.toArray( new String[ projection.size() ] ),
						selection,
						selectionArgs.toArray( new String[ selectionArgs.size() ] ),
						null );
		final T object;

		if ( cursor == null ) {
			object = null;
		} else {
			if ( cursor.moveToNext() ) {
				object = this.get( cursor );
			} else {
				object = null;
			}

			cursor.close();
		}

		return object;
	}

	public List<T> get( final String selection, final List<String> selectionArgs ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<String> projection = contract.getColumnNames();
		final Cursor cursor =
				this.contentResolver.query(
						contentUri,
						projection.toArray( new String[ projection.size() ] ),
						selection,
						selectionArgs.toArray( new String[ selectionArgs.size() ] ),
						null );
		final List<T> objects = new ArrayList<T>();

		if ( cursor != null ) {
			while ( cursor.moveToNext() ) {
				final T object = this.get( cursor );

				objects.add( object );
			}

			cursor.close();
		}

		return objects;
	}

	private T get( final Uri uri ) {
		final Contract contract = this.getContract();
		final List<String> projection = contract.getColumnNames();
		final Cursor cursor =
				this.contentResolver.query( uri,
						projection.toArray( new String[ projection.size() ] ),
						null, null, null );
		final T object;

		if ( cursor == null ) {
			object = null;
		} else {
			if ( cursor.moveToNext() ) {
				object = this.get( cursor );
			} else {
				object = null;
			}

			cursor.close();
		}

		return object;
	}

	protected abstract String getAuthority();

	protected abstract Contract getContract();

	public List<T> save( final List<T> objects ) {
		final List<T> savedObjects = new ArrayList<T>();

		for ( final T object : objects ) {
			final T savedObject = this.save( object );

			savedObjects.add( savedObject );
		}

		return savedObjects;
	}

	public T save( final T object ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final Long id = object.getId();
		final ContentValues contentValues = this.generateContentValues( object );
		final T savedObject;

		if ( id == null ) {
			final Uri uri =
					this.contentResolver.insert( contentUri, contentValues );

			savedObject = this.get( uri );
		} else {
			final String whereClause = BaseColumns._ID + "=" + id.intValue();

			this.contentResolver.update( contentUri, contentValues,
					whereClause, null );

			savedObject = object;
		}

		return savedObject;
	}
}