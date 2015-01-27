package com.wit.databaselibrary.contentprovider.contract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

public abstract class Contract {
	private static int getNextAvailableTableCode() {
		return Contract.nextAvailableTableCode++;
	}

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH );
	private static int nextAvailableTableCode = 0;
	private final String authority;
	private final int objectCode = Contract.getNextAvailableTableCode();
	private final int objectIdCode = Contract.getNextAvailableTableCode();
	private final Map<String, String> projectionMap =
			new HashMap<String, String>();
	private final List<String> columnNames;

	public Contract( final String authority, final List<String> columnNames ) {
		this.authority = authority;
		this.columnNames = columnNames;

		this.setupUriMatcher();
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

	public String getContentType() {
		final String tableName = this.getTableName();
		final String contentType =
				"vnd.android.cursor.dir/vnd.wit." + tableName;

		return contentType;
	}

	public Uri getContentUri() {
		final String tableName = this.getTableName();
		final Uri contentUri =
				Uri.parse( "content://" + this.authority + "/" + tableName );

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

	private void setupUriMatcher() {
		final String tableName = this.getTableName();

		Contract.URI_MATCHER.addURI( this.authority, tableName, this.objectCode );
		Contract.URI_MATCHER.addURI( this.authority, tableName + "/#",
				this.objectIdCode );
	}

	public final boolean uriMatches( final Uri uri ) {
		return this.uriMatches( uri, true, true );
	}

	private final boolean uriMatches( final Uri uri,
			final boolean matchOnObjectCode, final boolean matchOnObjectIdCode ) {
		final int matchResult = Contract.URI_MATCHER.match( uri );
		final boolean match;

		if ( ( matchOnObjectCode && ( matchResult == this.objectCode ) )
				|| ( matchOnObjectIdCode && ( matchResult == this.objectIdCode ) ) ) {
			match = true;
		} else {
			match = false;
		}

		return match;
	}

	public final boolean uriMatchesObject( final Uri uri ) {
		return this.uriMatches( uri, true, false );
	}

	public final boolean uriMatchesObjectId( final Uri uri ) {
		return this.uriMatches( uri, false, true );
	}
}