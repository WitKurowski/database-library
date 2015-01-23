package com.wit.databaselibrary.contentprovider.databaseinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

import com.wit.databaselibrary.contentprovider.SimpleContentProvider.DatabaseBaseColumns;

public abstract class DatabaseInfo {
	private static int getNextAvailableTableCode() {
		return DatabaseInfo.nextAvailableTableCode++;
	}

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH );
	private static int nextAvailableTableCode = 0;
	private final String authority;
	private final int objectCode = DatabaseInfo.getNextAvailableTableCode();
	private final int objectIdCode = DatabaseInfo.getNextAvailableTableCode();
	private final Map<String, String> projectionMap =
			new HashMap<String, String>();
	private final List<String> columnNames = new ArrayList<String>();

	public DatabaseInfo( final String authority ) {
		this.authority = authority;
	}

	protected final void addColumnName( final String columnName ) {
		this.columnNames.add( columnName );
	}

	public final String addSelectionById( final Uri uri,
			final String existingSelection ) {
		final String newSelection =
				existingSelection + BaseColumns._ID + " = "
						+ uri.getLastPathSegment();

		return newSelection;
	}

	protected String getAuthority() {
		return this.authority;
	}

	public final List<String> getColumnNames() {
		return this.columnNames;
	}

	public abstract DatabaseBaseColumns getColumns();

	public String getContentType() {
		final String tableName = this.getTableName();
		final String contentType =
				"vnd.android.cursor.dir/vnd.wit." + tableName;

		return contentType;
	}

	public Uri getContentUri() {
		final String tableName = this.getTableName();
		final String authority = this.getAuthority();
		final Uri contentUri =
				Uri.parse( "content://" + authority + "/" + tableName );

		return contentUri;
	}

	private final int getObjectCode() {
		return this.objectCode;
	}

	private final int getObjectIdCode() {
		return this.objectIdCode;
	}

	public final Map<String, String> getProjectionMap() {
		return this.projectionMap;
	}

	public abstract String getTableName();

	public abstract void setupColumnNames();

	public final void setupProjectionMap() {
		final List<String> columnNames = this.getColumnNames();
		final Map<String, String> projectionMap = this.getProjectionMap();

		for ( final String columnName : columnNames ) {
			projectionMap.put( columnName, columnName );
		}
	}

	public final void setupUriMatcher() {
		final String tableName = this.getTableName();
		final int objectCode = this.getObjectCode();
		final int objectIdCode = this.getObjectIdCode();

		DatabaseInfo.URI_MATCHER.addURI( this.authority, tableName, objectCode );
		DatabaseInfo.URI_MATCHER.addURI( this.authority, tableName + "/#",
				objectIdCode );
	}

	public final boolean uriMatches( final Uri uri ) {
		return this.uriMatches( uri, true, true );
	}

	private final boolean uriMatches( final Uri uri,
			final boolean matchOnObjectCode, final boolean matchOnObjectIdCode ) {
		final int matchResult = DatabaseInfo.URI_MATCHER.match( uri );
		final int objectCode = this.getObjectCode();
		final int objectIdCode = this.getObjectIdCode();
		final boolean match;

		if ( ( matchOnObjectCode && ( matchResult == objectCode ) )
				|| ( matchOnObjectIdCode && ( matchResult == objectIdCode ) ) ) {
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