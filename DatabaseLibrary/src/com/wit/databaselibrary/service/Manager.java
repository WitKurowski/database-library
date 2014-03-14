package com.wit.databaselibrary.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.wit.databaselibrary.contentprovider.SimpleContentProvider;
import com.wit.databaselibrary.model.DatabaseObject;

public abstract class Manager<T extends DatabaseObject> {
	protected ContentResolver contentResolver;

	public int delete( final List<T> objects ) {
		int numberOfRowsDeleted = 0;

		for ( final T object : objects ) {
			numberOfRowsDeleted += this.delete( object );
		}

		return numberOfRowsDeleted;
	}

	public int delete( final T object ) {
		final Uri contentUri = this.getContentUri();
		final Integer id = object.getId();
		final String whereClause =
				SimpleContentProvider.DatabaseBaseColumns.ID + "="
						+ id.intValue();

		final int numberOfRowsDelete =
				this.contentResolver.delete( contentUri, whereClause, null );

		return numberOfRowsDelete;
	}

	protected abstract ContentValues generateContentValues( final T object );

	public List<T> get() {
		final Uri contentUri = this.getContentUri();
		final List<String> projection = this.getProjection();
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
		}

		cursor.close();

		return objects;
	}

	protected abstract T get( final Cursor cursor );

	public T get( final int id ) {
		final Uri contentUri = this.getContentUri();
		final List<String> projection = this.getProjection();
		final String selection =
				SimpleContentProvider.DatabaseBaseColumns.ID + " = ?";
		final List<String> selectionArgs = new ArrayList<String>();

		selectionArgs.add( Integer.toString( id ) );

		final Cursor cursor =
				this.contentResolver.query(
						contentUri,
						projection.toArray( new String[ projection.size() ] ),
						selection,
						selectionArgs.toArray( new String[ selectionArgs.size() ] ),
						null );
		final T object;

		if ( ( cursor != null ) && cursor.moveToNext() ) {
			object = this.get( cursor );
		} else {
			object = null;
		}

		cursor.close();

		return object;
	}

	public List<T> get( final String selection, final List<String> selectionArgs ) {
		final Uri contentUri = this.getContentUri();
		final List<String> projection = this.getProjection();
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
		}

		cursor.close();

		return objects;
	}

	private T get( final Uri uri ) {
		final List<String> projection = this.getProjection();
		final Cursor cursor =
				this.contentResolver.query( uri,
						projection.toArray( new String[ projection.size() ] ),
						null, null, null );
		final T object;

		if ( ( cursor != null ) && cursor.moveToNext() ) {
			object = this.get( cursor );
		} else {
			object = null;
		}

		return object;
	}

	protected abstract Uri getContentUri();

	protected abstract List<String> getProjection();

	public T save( final T object ) {
		final Uri contentUri = this.getContentUri();
		final Integer id = object.getId();
		final ContentValues contentValues = this.generateContentValues( object );
		final T savedObject;

		if ( id == null ) {
			final Uri uri =
					this.contentResolver.insert( contentUri, contentValues );

			savedObject = this.get( uri );
		} else {
			final String whereClause =
					SimpleContentProvider.DatabaseBaseColumns.ID + "="
							+ id.intValue();

			this.contentResolver.update( contentUri, contentValues,
					whereClause, null );

			savedObject = object;
		}

		return savedObject;
	}
}