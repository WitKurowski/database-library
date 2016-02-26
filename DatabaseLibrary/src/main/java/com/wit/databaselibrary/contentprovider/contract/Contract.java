package com.wit.databaselibrary.contentprovider.contract;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.BaseColumns;

import com.wit.databaselibrary.annotation.Column;
import com.wit.databaselibrary.annotation.Table;
import com.wit.databaselibrary.model.ColumnType;
import com.wit.databaselibrary.model.DatabaseObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Contract<T extends DatabaseObject> {
	public static abstract class Columns implements BaseColumns {
		/**
		 * The version of an object.
		 */
		public static final String VERSION = "version";
	}

	private static List<Field> getAllFields( final Class<?> clazz ) {
		final List<Field> fields = new ArrayList<>( Arrays.asList( clazz.getDeclaredFields() ) );
		final Class<?> superclass = clazz.getSuperclass();

		if ( superclass != Object.class ) {
			final List<Field> newFields = Contract.getAllFields( superclass );

			fields.addAll( newFields );
		}

		return fields;
	}

	private final UriMatcher uriMatcher = new UriMatcher( UriMatcher.NO_MATCH );
	private final int objectCode = 1;
	private final int objectIdCode = 2;
	private final Map<String, String> projectionMap = new HashMap<String, String>();
	private final List<String> columnNames = new ArrayList<String>();
	private final Class<T> databaseObjectClass;
	private final String createTableSqlString;
	private boolean uriMatcherPrepared = false;

	public Contract( final Class<T> databaseObjectClass ) {
		this.databaseObjectClass = databaseObjectClass;

		this.setupProjectionMap();
		this.setupColumnNameList();

		this.createTableSqlString = this.generateCreateTableSqlString();
	}

	public final String addSelectionById( final Uri uri, final String existingSelection ) {
		final String newSelection = existingSelection + BaseColumns._ID + " = " +
				"" + uri.getLastPathSegment();

		return newSelection;
	}

	/**
	 * Generates the SQL string needed to create a database table for the
	 * {@link
	 * DatabaseObject} associated with this {@link Contract}.
	 *
	 * @return The SQL string needed to create a database table for the {@link
	 * DatabaseObject} associated with this {@link Contract}.
	 */
	private String generateCreateTableSqlString() {
		final StringBuilder createSqlStringBuilder = new StringBuilder();

		createSqlStringBuilder.append( "CREATE TABLE" );

		final Table tableAnnotation = this.databaseObjectClass.getAnnotation( Table.class );
		final String tableName = tableAnnotation.tableName();

		createSqlStringBuilder.append( " " );
		createSqlStringBuilder.append( tableName );
		createSqlStringBuilder.append( " " );
		createSqlStringBuilder.append( "(" );
		createSqlStringBuilder.append( " " );

		final List<Field> declaredFields = Contract.getAllFields( this.databaseObjectClass );

		for ( final Field declaredField : declaredFields ) {
			final Column columnAnnotation = declaredField.getAnnotation( Column.class );

			if ( columnAnnotation != null ) {
				final String columnName = columnAnnotation.columnName();
				final ColumnType columnType = columnAnnotation.columnType();

				createSqlStringBuilder.append( columnName );
				createSqlStringBuilder.append( " " );

				final String columnTypeString;

				switch ( columnType ) {
					case INTEGER:
						columnTypeString = "INTEGER";

						break;
					case LONG:
						columnTypeString = "INTEGER";

						break;
					case STRING:
						columnTypeString = "TEXT";

						break;
					default:
						throw new IllegalArgumentException( "Found unknown " +
								"column type '" + columnType + "'." );
				}

				createSqlStringBuilder.append( columnTypeString );

				if ( columnName.equals( BaseColumns._ID ) ) {
					createSqlStringBuilder.append( " " );
					createSqlStringBuilder.append( "PRIMARY KEY" );
				}

				createSqlStringBuilder.append( "," );
				createSqlStringBuilder.append( " " );
			}
		}

		createSqlStringBuilder
				.replace( createSqlStringBuilder.length() - 2, createSqlStringBuilder.length(),
						"" );

		createSqlStringBuilder.append( " " );
		createSqlStringBuilder.append( ")" );

		final String createSqlString = createSqlStringBuilder.toString();

		return createSqlString;
	}

	public final List<String> getColumnNames() {
		return this.columnNames;
	}

	public final String getContentType() {
		final String tableName = this.getTableName();
		final String contentType = ContentResolver.CURSOR_DIR_BASE_TYPE +
				"/vnd.wit." + tableName;

		return contentType;
	}

	public final String getContentItemType() {
		final String tableName = this.getTableName();
		final String contentType = ContentResolver.CURSOR_ITEM_BASE_TYPE +
				"/vnd.wit." + tableName;

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
	private String getContentUriString( final String authority ) {
		final String tableName = this.getTableName();
		final String contentUriString = "content://" + authority + "/" +
				tableName;

		return contentUriString;
	}

	/**
	 * Returns the SQL string needed to create a database table for the {@link
	 * DatabaseObject} associated with this {@link Contract}.
	 *
	 * @return The SQL string needed to create a database table for the {@link
	 * DatabaseObject} associated with this {@link Contract}.
	 */
	public final String getCreateTableSqlString() {
		return this.createTableSqlString;
	}

	/**
	 * Extracts the ID from the given {@link Uri}.
	 *
	 * @param uri The {@link Uri} with the ID that should be extracted.
	 * @return The ID from the given {@link Uri}.
	 * @throws IllegalArgumentException The given {@link Uri} did not have
	 * exactly 2 path segments, one for the root object and one for the
	 * specific ID.
	 */
	public final long getId( final Uri uri ) throws IllegalArgumentException {
		final List<String> pathSegments = uri.getPathSegments();
		final int numberOfPathSegments = pathSegments.size();
		final long id;

		if ( numberOfPathSegments == 2 ) {
			id = Long.valueOf( pathSegments.get( 1 ) );
		} else {
			throw new IllegalArgumentException( "Unable to parse ID from URI '" + uri.getPath() +
					"'." );
		}

		return id;
	}

	public final Map<String, String> getProjectionMap() {
		return this.projectionMap;
	}

	public final String getTableName() {
		final Table tableAnnotation = this.databaseObjectClass.getAnnotation( Table.class );
		final String tableName = tableAnnotation.tableName();

		return tableName;
	}

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
		final List<Field> declaredFields = Contract.getAllFields( this.databaseObjectClass );

		for ( final Field declaredField : declaredFields ) {
			final Column columnAnnotation = declaredField.getAnnotation( Column.class );

			if ( columnAnnotation != null ) {
				final String columnName = columnAnnotation.columnName();

				this.projectionMap.put( columnName, columnName );
			}
		}
	}

	private void setupColumnNameList() {
		this.columnNames.addAll( this.projectionMap.keySet() );
	}

	private void prepareUriMatcher( final String authority ) {
		final String tableName = this.getTableName();

		this.uriMatcher.addURI( authority, tableName, this.objectCode );
		this.uriMatcher.addURI( authority, tableName + "/#", this.objectIdCode );
	}

	public final boolean uriMatches( final Uri uri, final String authority ) {
		return this.uriMatches( uri, true, true, authority );
	}

	private synchronized boolean uriMatches( final Uri uri, final boolean matchOnObjectCode,
			final boolean matchOnObjectIdCode, final String authority ) {
		if ( !this.uriMatcherPrepared ) {
			this.prepareUriMatcher( authority );
			this.uriMatcherPrepared = true;
		}

		final int matchResult = this.uriMatcher.match( uri );
		final boolean match;

		if ( ( matchOnObjectCode && ( matchResult == this.objectCode ) ) ||
				( matchOnObjectIdCode && ( matchResult == this.objectIdCode ) ) ) {
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