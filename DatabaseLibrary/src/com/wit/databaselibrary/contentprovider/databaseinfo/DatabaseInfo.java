package com.wit.databaselibrary.contentprovider.databaseinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.UriMatcher;
import android.net.Uri;

import com.wit.databaselibrary.contentprovider.SimpleContentProvider.DatabaseBaseColumns;

public abstract class DatabaseInfo {
	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH );
	private static int nextAvailableTableCode = 0;

	private static int getNextAvailableTableCode() {
		return DatabaseInfo.nextAvailableTableCode++;
	}

	private final String authority;
	private final int objectCode = DatabaseInfo.getNextAvailableTableCode();
	private final int objectIdCode = DatabaseInfo.getNextAvailableTableCode();
	private final Map<String, String> projectionMap =
			new HashMap<String, String>();
	private final List<String> columnNames = new ArrayList<String>();

	public DatabaseInfo( final String authority ) {
		this.authority = authority;
	}

	protected void addColumnName( final String columnName ) {
		this.columnNames.add( columnName );
	}

	public String addSelectionById( final Uri uri,
			final String existingSelection ) {
		final String newSelection =
				existingSelection + DatabaseBaseColumns.ID + " = "
						+ uri.getLastPathSegment();

		return newSelection;
	}

	public List<String> getColumnNames() {
		return this.columnNames;
	}

	public abstract DatabaseBaseColumns getColumns();

	private int getObjectCode() {
		return this.objectCode;
	}

	private int getObjectIdCode() {
		return this.objectIdCode;
	}

	public Map<String, String> getProjectionMap() {
		return this.projectionMap;
	}

	public abstract String getTableName();

	public abstract void setupColumnNames();

	public void setupProjectionMap() {
		final List<String> columnNames = this.getColumnNames();
		final Map<String, String> projectionMap = this.getProjectionMap();

		for ( final String columnName : columnNames ) {
			projectionMap.put( columnName, columnName );
		}
	}

	public void setupUriMatcher() {
		final String tableName = this.getTableName();
		final int objectCode = this.getObjectCode();
		final int objectIdCode = this.getObjectIdCode();

		DatabaseInfo.URI_MATCHER.addURI( this.authority, tableName, objectCode );
		DatabaseInfo.URI_MATCHER.addURI( this.authority, tableName + "/#",
				objectIdCode );
	}

	public boolean uriMatches( final Uri uri ) {
		return this.uriMatches( uri, true, true );
	}

	private boolean uriMatches( final Uri uri, final boolean matchOnObjectCode,
			final boolean matchOnObjectIdCode ) {
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

	public boolean uriMatchesObject( final Uri uri ) {
		return this.uriMatches( uri, true, false );
	}

	public boolean uriMatchesObjectId( final Uri uri ) {
		return this.uriMatches( uri, false, true );
	}
}