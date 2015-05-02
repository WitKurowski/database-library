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
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.wit." + tableName;

		return contentType;
	}

	public final String getContentItemType() {
		final String tableName = this.getTableName();
		final String contentType =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.wit." + tableName;

		return contentType;
	}

	public Uri getContentUri( final String authority ) {
		final String contentUriString = this.getContentUriString( authority );
		final Uri contentUri = Uri.parse( contentUriString );

		return contentUri;
	}

	public Uri getContentUri( final String authority, final long id ) {
		final String contentUriString = this.getContentUriString( authority );
		final Uri contentUri = Uri.parse( contentUriString + "/" + id );

		return contentUri;
	}

	/**
	 * Returns the root content URI as a {@link String}.
	 *
	 * @param authority The authority string to use in the root content URI.
	 * @return The root content URI as a {@link String}.
	 */
	private final String getContentUriString( final String authority ) {
		final String tableName = this.getTableName();
		final String contentUriString = "content://" + authority + "/" + tableName;

		return contentUriString;
	}

	/**
	 * Extracts the ID from the given {@link Uri}.
	 *
	 * @param uri The {@link Uri} with the ID that should be extracted.
	 * @return The ID from the given {@link Uri}.
	 * @throws IllegalArgumentException The given {@link Uri} did not have exactly 2 path segments, one for the root
	 * object and one for the specific ID.
	 */
	public final long getId( final Uri uri ) throws IllegalArgumentException {
		final List<String> pathSegments = uri.getPathSegments();
		final int numberOfPathSegments = pathSegments.size();
		final long id;

		if ( numberOfPathSegments == 2 ) {
			id = Long.valueOf( pathSegments.get( 1 ) );
		} else {
			throw new IllegalArgumentException( "Unable to parse ID from URI '" + uri.getPath() + "'." );
		}

		return id;
	}

	public final Map<String, String> getProjectionMap() {
		return this.projectionMap;
	}

	public abstract String getTableName();

	/**
	 * Returns whether the given {@link Uri} contains an ID.
	 *
	 * @param uri The {@link Uri} to check for the existence of an ID.
	 * @return Whether the given {@link Uri} contains an ID.
	 */
	public final boolean hasId( final Uri uri ) {
		final List<String> pathSegments = uri.getPathSegments();
		final int numberOfPathSegments = pathSegments.size();
		final boolean hasId;

		if ( numberOfPathSegments == 2 ) {
			hasId = true;
		} else {
			hasId = false;
		}

		return hasId;
	}

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