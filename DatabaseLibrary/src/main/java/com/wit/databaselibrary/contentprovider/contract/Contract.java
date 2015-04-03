package com.wit.databaselibrary.contentprovider.contract;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Contract {
	public static abstract class Columns implements BaseColumns {
		/**
		 * The version of an object.
		 */
		public static final String VERSION = "version";
	}

	private final UriMatcher uriMatcher = new UriMatcher(
			UriMatcher.NO_MATCH );
	private final int objectCode = 1;
	private final int objectIdCode = 2;
	private final Map<String, String> projectionMap =
			new HashMap<String, String>();
	private final List<String> columnNames;
	private boolean uriMatcherPrepared = false;

	public Contract( final List<String> columnNames ) {
		this.columnNames = Collections.unmodifiableList( columnNames );

		this.setupProjectionMap();
	}

	public final String addSelectionById( final Uri uri,
			final String existingSelection ) {
		final String newSelection =
				existingSelection + BaseColumns._ID + " = "
						+ uri.getLastPathSegment();

		return newSelection;
	}

	public final List<String> getColumnNames() {
		return this.columnNames;
	}

	public final String getContentType() {
		final String tableName = this.getTableName();
		final String contentType =
				ContentResolver.CURSOR_DIR_BASE_TYPE+ "/vnd.wit." + tableName;

		return contentType;
	}

	public final String getContentItemType() {
		final String tableName = this.getTableName();
		final String contentType =
				ContentResolver.CURSOR_ITEM_BASE_TYPE+ "/vnd.wit." + tableName;

		return contentType;
	}

	public Uri getContentUri( final String authority ) {
		final String tableName = this.getTableName();
		final Uri contentUri =
				Uri.parse( "content://" + authority + "/" + tableName );

		return contentUri;
	}

	public Uri getContentUri( final String authority, final long id ) {
		final String tableName = this.getTableName();
		final Uri contentUri =
				Uri.parse( "content://" + authority + "/" + tableName + "/" + id );

		return contentUri;
	}

	public final Map<String, String> getProjectionMap() {
		return this.projectionMap;
	}

	public abstract String getTableName();

	private void setupProjectionMap() {
		for ( final String columnName : this.columnNames ) {
			this.projectionMap.put( columnName, columnName );
		}
	}

	private void prepareUriMatcher( final String authority ) {
		final String tableName = this.getTableName();

		this.uriMatcher.addURI( authority, tableName, this.objectCode );
		this.uriMatcher.addURI( authority, tableName + "/#",
				this.objectIdCode );
	}

	public final boolean uriMatches( final Uri uri, final String authority ) {
		return this.uriMatches( uri, true, true, authority );
	}

	private final synchronized boolean uriMatches( final Uri uri,
			final boolean matchOnObjectCode, final boolean matchOnObjectIdCode, final String authority ) {
		if ( !this.uriMatcherPrepared ) {
			this.prepareUriMatcher( authority );
			this.uriMatcherPrepared = true;
		}

		final int matchResult = this.uriMatcher.match( uri );
		final boolean match;

		if ( ( matchOnObjectCode && ( matchResult == this.objectCode ) )
				|| ( matchOnObjectIdCode && ( matchResult == this.objectIdCode ) ) ) {
			match = true;
		} else {
			match = false;
		}

		return match;
	}

	public final boolean uriMatchesObject( final Uri uri, final String authority ) {
		return this.uriMatches( uri, true, false, authority );
	}

	public final boolean uriMatchesObjectId( final Uri uri, final String authority ) {
		return this.uriMatches( uri, false, true, authority );
	}
}