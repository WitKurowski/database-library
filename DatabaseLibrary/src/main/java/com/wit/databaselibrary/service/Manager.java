package com.wit.databaselibrary.service;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import com.wit.databaselibrary.annotation.Column;
import com.wit.databaselibrary.contentprovider.StorageModificationException;
import com.wit.databaselibrary.contentprovider.contract.Contract;
import com.wit.databaselibrary.model.ColumnType;
import com.wit.databaselibrary.model.DatabaseObject;
import com.wit.databaselibrary.model.Order;
import com.wit.databaselibrary.service.exception.InvalidClassDefinitionException;

import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Manager<T extends DatabaseObject> {
	private final ContentResolver contentResolver;
	private final Class<T> parameterClass;
	private final Contract contract;
	protected final String packageName;

	protected Manager( final Context context, final Contract contract,
			final Class<T> parameterClass ) {
		this.contentResolver = context.getContentResolver();
		this.parameterClass = parameterClass;
		this.contract = contract;
		this.packageName = context.getPackageName();
	}

	/**
	 * Adds and updates the specified objects.
	 *
	 * @param objectsToAdd The objects that need to be added.
	 * @param objectsToUpdate The objects that need to be updated.
	 * @return The latest version of the objects that have been saved or updated.
	 * @throws StorageModificationException An add or update operation failed.
	 */
	private List<T> apply( final List<T> objectsToAdd, final List<T> objectsToUpdate )
			throws StorageModificationException {
		final List<T> objectsToDelete = Collections.emptyList();
		final List<T> savedObjects = this.apply( objectsToAdd, objectsToUpdate, objectsToDelete );

		return savedObjects;
	}

	/**
	 * Adds, updates, and deletes the specified objects.
	 *
	 * @param objectsToAdd The objects that need to be added.
	 * @param objectsToUpdate The objects that need to be updated.
	 * @param objectsToDelete The objects that need to be deleted.
	 * @return The latest version of the objects that have been saved or updated.
	 * @throws StorageModificationException An add, update, or delete operation failed.
	 */
	private List<T> apply( final List<T> objectsToAdd, final List<T> objectsToUpdate,
			final List<T> objectsToDelete ) throws StorageModificationException {
		final String authority = this.getAuthority();
		final ArrayList<ContentProviderOperation> contentProviderOperations =
				new ArrayList<ContentProviderOperation>();
		final List<ContentProviderOperation> addedObjectContentProviderOperations =
				this.processObjectsToAdd( objectsToAdd );

		contentProviderOperations.addAll( addedObjectContentProviderOperations );

		final List<ContentProviderOperation> modifiedObjectContentProviderOperations =
				this.processObjectsToUpdate( objectsToUpdate );

		contentProviderOperations.addAll( modifiedObjectContentProviderOperations );

		final List<ContentProviderOperation> deletedObjectContentProviderOperations =
				this.processObjectsToDelete( objectsToDelete );

		contentProviderOperations.addAll( deletedObjectContentProviderOperations );

		final List<Long> ids = new ArrayList<Long>();

		try {
			final ContentProviderResult[] contentProviderResults =
					this.contentResolver.applyBatch( authority, contentProviderOperations );

			for ( final ContentProviderResult contentProviderResult : contentProviderResults ) {
				final Uri uri = contentProviderResult.uri;

				if ( uri != null ) {
					final String idString = uri.getLastPathSegment();
					final long id = Long.parseLong( idString );

					ids.add( id );
				}
			}
		} catch ( final RemoteException remoteException ) {
			Log.e( Manager.class.getSimpleName(),
					"An error happened while attempting to communicate with a remote provider.",
					remoteException );
		} catch ( OperationApplicationException operationApplicationException ) {
			final String errorMessage;

			if ( objectsToDelete.isEmpty() ) {
				errorMessage = "An add or update operation failed to be applied.";
			} else {
				errorMessage = "An add, update, or delete operation failed to be applied.";
			}

			throw new StorageModificationException( errorMessage, operationApplicationException );
		}

		final List<T> savedObjects = this.get( ids );

		return savedObjects;
	}

	protected Triple<List<T>, List<T>, List<T>> categorize( final Collection<T> existingObjects,
			final Collection<T> newObjects ) {
		final Map<Long, T> existingObjectIdsToSources = new HashMap<Long, T>();

		for ( final T existingObject : existingObjects ) {
			final Long existingObjectId = existingObject.getId();

			existingObjectIdsToSources.put( existingObjectId, existingObject );
		}

		final List<T> objectsToAdd = new ArrayList<T>();
		final List<T> objectsToUpdate = new ArrayList<T>();
		final List<T> objectsNotIncluded = new ArrayList<T>();

		for ( final T newObject : newObjects ) {
			final Long newObjectId = newObject.getId();
			final boolean objectAlreadyExisted =
					existingObjectIdsToSources.containsKey( newObjectId );

			if ( objectAlreadyExisted ) {
				final Long newObjectVersion = newObject.getVersion();
				final T existingObject = existingObjectIdsToSources.remove( newObjectId );
				final Long existingObjectVersion = existingObject.getVersion();

				if ( newObjectVersion > existingObjectVersion ) {
					objectsToUpdate.add( newObject );
				}
			} else {
				objectsToAdd.add( newObject );
			}
		}

		objectsNotIncluded.addAll( existingObjectIdsToSources.values() );

		final Triple<List<T>, List<T>, List<T>> objectsToAddUpdateAndDeleteTriple =
				Triple.of( objectsToAdd, objectsToUpdate, objectsNotIncluded );

		return objectsToAddUpdateAndDeleteTriple;
	}

	/**
	 * Deletes all {@link DatabaseObject}s associated with this {@link Manager}.
	 *
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 */
	public int delete() {
		final String whereClause = null;
		final List<String> whereArgs = Collections.emptyList();
		final int numberOfRowsDeleted = this.delete( (Long) null, whereClause, whereArgs );

		return numberOfRowsDeleted;
	}

	/**
	 * Deletes the given {@link DatabaseObject}s.
	 *
	 * @param objects The {@link DatabaseObject}s to delete.
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 * @throws StorageModificationException A delete operation failed.
	 */
	public void delete( final Collection<T> objects ) throws StorageModificationException {
		final String authority = this.getAuthority();
		final ArrayList<ContentProviderOperation> contentProviderOperations =
				new ArrayList<ContentProviderOperation>( this.processObjectsToDelete( objects ) );

		try {
			this.contentResolver.applyBatch( authority, contentProviderOperations );
		} catch ( final RemoteException remoteException ) {
			Log.e( Manager.class.getSimpleName(),
					"An error happened while attempting to communicate with a remote provider.",
					remoteException );
		} catch ( OperationApplicationException operationApplicationException ) {
			throw new StorageModificationException( "A delete operation failed to be applied.",
					operationApplicationException );
		}
	}

	/**
	 * Deletes the given {@link DatabaseObject}.
	 *
	 * @param object The {@link DatabaseObject} to delete.
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 */
	public int delete( final T object ) {
		final long id = object.getId();
		final String whereClause = BaseColumns._ID + " = ?";
		final List<String> whereArgs = Collections.singletonList( String.valueOf( id ) );
		final int numberOfRowsDelete = this.delete( id, whereClause, whereArgs );

		return numberOfRowsDelete;
	}

	/**
	 * Deletes any {@link DatabaseObject} that satisfy the given where clause and arguments.
	 *
	 * @param whereClause The where clause to use to narrow down the {@link DatabaseObject}s to
	 * delete.
	 * @param whereArgs The values to replace the question marks with in the where clause.
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 */
	public int delete( final String whereClause, final List<String> whereArgs ) {
		final Long id = null;
		final int numberOfRowsDeleted = this.delete( id, whereClause, whereArgs );

		return numberOfRowsDeleted;
	}

	/**
	 * Deletes the given {@link DatabaseObject} if the current saved state of it satisfies the
	 * given where clause and arguments.  Note that the object ID matching condition will be added
	 * to the where clause and argument list.
	 *
	 * @param object The {@link DatabaseObject} to delete, assuming it satisfies the given where
	 * clause and arguments.
	 * @param whereClause The where clause to use to determine whether the {@link DatabaseObject}
	 * should be deleted.
	 * @param whereArgs The values to replace the question marks with in the where clause.
	 * @return 1 if the specified {@link DatabaseObject} was deleted, or 0 if it was not deleted.
	 */
	public int delete( final T object, final String whereClause, final List<String> whereArgs ) {
		final Long id = object.getId();
		final String narrowedWhereClause = BaseColumns._ID + " = ? AND " + whereClause;
		final List<String> narrowedWhereArgs = new ArrayList<String>();

		narrowedWhereArgs.add( String.valueOf( id ) );
		narrowedWhereArgs.addAll( whereArgs );

		final int numberOfRowsDeleted = this.delete( id, narrowedWhereClause, narrowedWhereArgs );

		return numberOfRowsDeleted;
	}

	/**
	 * Deletes any {@link DatabaseObject} that satisfy the given where clause and arguments using
	 * the given ID as part of the content URI.
	 *
	 * @param id The ID to use as part of the content URI associated with the delete, if any.
	 * @param whereClause The where clause to use to narrow down the {@link DatabaseObject}s to
	 * delete.
	 * @param whereArgs The values to replace the question marks with in the where clause.
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 */
	private int delete( final Long id, final String whereClause, final List<String> whereArgs ) {
		final String authority = this.getAuthority();
		final Uri contentUri;

		if ( id == null ) {
			contentUri = this.contract.getContentUri( authority );
		} else {
			contentUri = this.contract.getContentUri( authority, id );
		}

		final int numberOfRowsDelete = this.contentResolver.delete( contentUri, whereClause,
				whereArgs.toArray( new String[ whereArgs.size() ] ) );

		return numberOfRowsDelete;
	}

	protected ContentValues generateContentValues( final T object ) {
		final ContentValues contentValues = new ContentValues();
		final Long id = object.getId();

		contentValues.put( BaseColumns._ID, id );

		final Long version = object.getVersion();

		contentValues.put( Contract.Columns.VERSION, version );

		final Field[] declaredFields = this.parameterClass.getDeclaredFields();

		for ( final Field declaredField : declaredFields ) {
			final Column column = declaredField.getAnnotation( Column.class );
			final String columnName = column.columnName();

			if ( !columnName.equals( BaseColumns._ID ) &&
					!columnName.equals( Contract.Columns.VERSION ) ) {
				final ColumnType columnType = column.columnType();
				final String name = declaredField.getName();
				final String getterMethodName = "get" + name.substring( 0, 1 ).toUpperCase() +
						name.substring( 1 );
				final Method declaredGetterMethod;

				try {
					declaredGetterMethod = this.parameterClass
							.getDeclaredMethod( getterMethodName, new Class<?>[ 0 ] );
				} catch ( final NoSuchMethodException noSuchMethodException ) {
					throw new InvalidClassDefinitionException( "Unable to get find method '" +
							getterMethodName + "()'.", noSuchMethodException );

				}

				switch ( columnType ) {
					case INTEGER: {
						final Integer value;

						try {
							value = (Integer) declaredGetterMethod
									.invoke( object, new Object[ 0 ] );
						} catch ( final InvocationTargetException invocationTargetException ) {
							throw new IllegalStateException( "Unable to get value of type '" +
									columnType + "' from method '" + getterMethodName + "()'.",
									invocationTargetException );
						} catch ( final IllegalAccessException illegalAccessException ) {
							throw new InvalidClassDefinitionException(
									"Unable to get value of type '" + columnType +
											"' from method '" + getterMethodName + "()'.",
									illegalAccessException );
						}

						contentValues.put( columnName, value );

						break;
					}
					case LONG: {
						final Long value;

						try {
							value = (long) declaredGetterMethod.invoke( object, new Object[ 0 ] );
						} catch ( final IllegalAccessException illegalAccessException ) {
							throw new IllegalStateException( "Unable to get value of type '" +
									columnType + "' from method '" + getterMethodName + "()'.",
									illegalAccessException );
						} catch ( final InvocationTargetException invocationTargetException ) {
							throw new IllegalStateException( "Unable to get value of type '" +
									columnType + "' from method '" + getterMethodName + "()'.",
									invocationTargetException );
						}

						contentValues.put( columnName, value );

						break;
					}
					case STRING: {
						final String value;

						try {
							value = (String) declaredGetterMethod.invoke( object, new Object[ 0
									] );
						} catch ( final IllegalAccessException illegalAccessException ) {
							throw new IllegalStateException( "Unable to get value of type '" +
									columnType + "' from method '" + getterMethodName + "()'.",
									illegalAccessException );
						} catch ( final InvocationTargetException invocationTargetException ) {
							throw new IllegalStateException( "Unable to get value of type '" +
									columnType + "' from method '" + getterMethodName + "()'.",
									invocationTargetException );
						}

						contentValues.put( columnName, value );

						break;
					}
				}
			}
		}

		return contentValues;
	}

	public List<T> get() {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final List<String> projection = this.contract.getColumnNames();
		final Cursor cursor = this.contentResolver
				.query( contentUri, projection.toArray( new String[ projection.size() ] ), null,
						null, null );
		final List<T> objects;

		if ( cursor == null ) {
			objects = Collections.emptyList();
		} else {
			objects = this.getAll( cursor );

			cursor.close();
		}

		return objects;
	}

	protected T get( final Cursor cursor ) {
		final int idIndex = cursor.getColumnIndex( BaseColumns._ID );
		final Long id = cursor.getLong( idIndex );
		final int versionIndex = cursor.getColumnIndex( Contract.Columns.VERSION );
		final Long version = cursor.getLong( versionIndex );
		final Constructor<T> declaredConstructor;

		try {
			declaredConstructor =
					this.parameterClass.getDeclaredConstructor( Long.class, Long.class );
		} catch ( final NoSuchMethodException noSuchMethodException ) {
			throw new IllegalStateException(
					"No constructor defined that takes the ID and version of the DatabaseObject.",
					noSuchMethodException );
		}

		final T databaseObject;

		try {
			databaseObject = declaredConstructor.newInstance( id, version );
		} catch ( final InstantiationException | IllegalAccessException |
				InvocationTargetException exception ) {
			throw new IllegalStateException( "Unable to create a new instance of DatabaseObject.",
					exception );
		}

		final Field[] declaredFields = this.parameterClass.getDeclaredFields();

		for ( final Field declaredField : declaredFields ) {
			final Column column = declaredField.getAnnotation( Column.class );
			final String columnName = column.columnName();
			final int index = cursor.getColumnIndex( columnName );

			if ( index != idIndex && index != versionIndex ) {
				final ColumnType columnType = column.columnType();
				final Object value;
				final Class<?> setterParameterClass;

				switch ( columnType ) {
					case INTEGER:
						value = cursor.getInt( index );
						setterParameterClass = Integer.class;

						break;
					case LONG:
						value = cursor.getLong( index );
						setterParameterClass = Long.class;

						break;
					case STRING:
						value = cursor.getString( index );
						setterParameterClass = String.class;

						break;
					default:
						throw new IllegalArgumentException( "Found unknown column type '" +
								columnType + "'." );
				}

				final String name = declaredField.getName();
				final String setterMethodName = "set" + name.substring( 0, 1 ).toUpperCase() +
						name.substring( 1 );

				try {
					final Method declaredMethod = this.parameterClass
							.getDeclaredMethod( setterMethodName, setterParameterClass );

					declaredMethod.invoke( databaseObject, value );
				} catch ( final InvocationTargetException invocationTargetException ) {
					throw new IllegalStateException(
							"Unable to set value '" + value + "' through method '" +
									setterMethodName + "()'.", invocationTargetException );
				} catch ( final NoSuchMethodException noSuchMethodException ) {
					throw new InvalidClassDefinitionException( "Unable to set value '" + value +
							"' through method '" + setterMethodName + "()'.",
							noSuchMethodException );
				} catch ( final IllegalAccessException illegalAccessException ) {
					throw new InvalidClassDefinitionException( "Unable to set value '" + value +
							"' through method '" + setterMethodName + "()'.",
							illegalAccessException );
				}
			}
		}

		return databaseObject;
	}

	private List<T> get( final Collection<Long> ids ) {
		final List<T> objects = new ArrayList<T>();

		if ( !ids.isEmpty() ) {
			final String authority = this.getAuthority();
			final Uri contentUri = this.contract.getContentUri( authority );
			final List<String> projection = this.contract.getColumnNames();
			String selectionClause = BaseColumns._ID + " IN (?)";
			final List<String> selectionArgs = new ArrayList<String>();
			final StringBuilder idsString = new StringBuilder();

			idsString.append( "(" );

			for ( final Long id : ids ) {
				idsString.append( id );
				idsString.append( ',' );
			}

			idsString.deleteCharAt( idsString.length() - 1 );
			idsString.append( ")" );

			selectionClause = selectionClause.replace( "(?)", idsString );

			final Cursor cursor = this.contentResolver
					.query( contentUri, projection.toArray( new String[ projection.size() ] ),
							selectionClause,
							selectionArgs.toArray( new String[ selectionArgs.size() ] ), null );

			if ( cursor != null ) {
				while ( cursor.moveToNext() ) {
					final T object = this.get( cursor );

					objects.add( object );
				}

				cursor.close();
			}
		}

		return objects;
	}

	public List<T> get( final List<String> projection, final List<Pair<String, Order>> orderBys,
			final List<String> groupByColumns ) {
		final String selectionClause = null;
		final List<String> selectionArgs = Collections.emptyList();
		final Integer limit = null;
		final List<T> objects =
				this.get( projection, selectionClause, selectionArgs, orderBys, groupByColumns,
						limit );

		return objects;
	}

	public T get( final long id ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final List<String> projection = this.contract.getColumnNames();
		final String selectionClause = BaseColumns._ID + " = ?";
		final List<String> selectionArgs = new ArrayList<String>();

		selectionArgs.add( Long.toString( id ) );

		final Cursor cursor = this.contentResolver
				.query( contentUri, projection.toArray( new String[ projection.size() ] ),
						selectionClause,
						selectionArgs.toArray( new String[ selectionArgs.size() ] ), null );
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

	public List<T> get( final String selectionClause, final List<String> selectionArgs ) {
		final List<String> projection = this.contract.getColumnNames();
		final List<Pair<String, Order>> orderBys = Collections.emptyList();
		final List<String> groupByColumns = Collections.emptyList();
		final Integer limit = null;
		final List<T> objects =
				this.get( projection, selectionClause, selectionArgs, orderBys, groupByColumns,
						limit );

		return objects;
	}

	public List<T> get( final String selectionClause, final List<String> selectionArgs,
			final int limit ) {
		final List<String> projection = this.contract.getColumnNames();
		final List<Pair<String, Order>> orderBys = Collections.emptyList();
		final List<String> groupByColumns = Collections.emptyList();
		final List<T> objects =
				this.get( projection, selectionClause, selectionArgs, orderBys, groupByColumns,
						limit );

		return objects;
	}

	public List<T> get( final String selectionClause, final List<String> selectionArgs,
			final List<Pair<String, Order>> orderBys, final int limit ) {
		final List<String> projection = this.contract.getColumnNames();
		final List<String> groupByColumns = Collections.emptyList();
		final List<T> objects =
				this.get( projection, selectionClause, selectionArgs, orderBys, groupByColumns,
						limit );

		return objects;
	}

	private List<T> get( final List<String> projection, final String selectionClause,
			final List<String> selectionArgs, final List<Pair<String, Order>> orderBys,
			final List<String> groupByColumns, final Integer limit ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final StringBuilder selectionAndGroupByClauseStringBuilder = new StringBuilder();

		if ( selectionClause == null ) {
			if ( !groupByColumns.isEmpty() ) {
				selectionAndGroupByClauseStringBuilder.append( "1 = 1" );
			}
		} else {
			selectionAndGroupByClauseStringBuilder.append( selectionClause );
		}

		if ( !groupByColumns.isEmpty() ) {
			selectionAndGroupByClauseStringBuilder.append( ") GROUP BY (" );

			for ( final String groupByColumn : groupByColumns ) {
				if ( groupByColumns.indexOf( groupByColumn ) != 0 ) {
					selectionAndGroupByClauseStringBuilder.append( ", " );
				}

				selectionAndGroupByClauseStringBuilder.append( groupByColumn );
			}
		}

		final String selectionAndGroupByClause = selectionAndGroupByClauseStringBuilder.toString();
		StringBuilder sortOrder = new StringBuilder();

		if ( orderBys.isEmpty() ) {
			sortOrder.append( BaseColumns._ID );
		} else {
			for ( final Pair<String, Order> orderBy : orderBys ) {
				if ( sortOrder.length() != 0 ) {
					sortOrder.append( ", " );
				}

				sortOrder.append( orderBy.first );
				sortOrder.append( " " );
				sortOrder.append( orderBy.second.getKeyword() );
			}
		}

		if ( limit != null ) {
			sortOrder.append( " LIMIT " + limit );
		}

		final Cursor cursor = this.contentResolver
				.query( contentUri, projection.toArray( new String[ projection.size() ] ),
						selectionAndGroupByClause,
						selectionArgs.toArray( new String[ selectionArgs.size() ] ),
						sortOrder.toString() );
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

	private List<T> get( final Uri uri ) {
		final List<String> projection = this.contract.getColumnNames();
		final Cursor cursor = this.contentResolver
				.query( uri, projection.toArray( new String[ projection.size() ] ), null, null,
						null );
		final List<T> objects;

		if ( cursor == null ) {
			objects = Collections.emptyList();
		} else {
			objects = this.getAll( cursor );

			cursor.close();
		}

		return objects;
	}

	private List<T> getAll( final Cursor cursor ) {
		final List<T> objects = new ArrayList<T>();

		while ( cursor.moveToNext() ) {
			final T object = this.get( cursor );

			objects.add( object );
		}

		return objects;
	}

	protected abstract String getAuthority();

	/**
	 * Returns the number of {@link DatabaseObject}s that are saved.
	 *
	 * @return The number of {@link DatabaseObject}s that are saved.
	 */
	public final int getCount() {
		final String selectionClause = null;
		final List<String> selectionArgs = Collections.emptyList();
		final int count = this.getCount( selectionClause, selectionArgs );

		return count;
	}

	/**
	 * Returns the number of {@link DatabaseObject}s that are saved that satisfy the given
	 * selection criteria.
	 *
	 * @param selectionClause The selection clause to use to narrow down the {@link
	 * DatabaseObject}s to include in the count.
	 * @param selectionArgs The values to replace the placeholders with in the selection clause.
	 * @return The number of {@link DatabaseObject}s that are saved that satisfy the given
	 * selection criteria.
	 */
	public final int getCount( final String selectionClause, final List<String> selectionArgs ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final String[] projection = new String[]{ "count(*)" };
		final Cursor cursor = this.contentResolver.query( contentUri, projection, selectionClause,
				selectionArgs.toArray( new String[ selectionArgs.size() ] ), null );
		final int cursorCount = cursor.getCount();
		final int count;

		if ( cursorCount == 0 ) {
			count = 0;
		} else {
			cursor.moveToFirst();

			count = cursor.getInt( 0 );
		}

		cursor.close();

		return count;
	}

	protected T merge( final T oldObject, final long newId ) {
		final Long version = oldObject.getVersion();
		final Constructor<T> declaredConstructor;

		try {
			declaredConstructor =
					this.parameterClass.getDeclaredConstructor( Long.class, Long.class );
		} catch ( final NoSuchMethodException noSuchMethodException ) {
			throw new IllegalStateException(
					"No constructor defined that takes the ID and version of the DatabaseObject.",
					noSuchMethodException );
		}

		final T newObject;

		try {
			newObject = declaredConstructor.newInstance( newId, version );
		} catch ( final InstantiationException | IllegalAccessException |
				InvocationTargetException exception ) {
			throw new IllegalStateException( "Unable to create a new instance of DatabaseObject.",
					exception );
		}

		final Field[] declaredFields = this.parameterClass.getDeclaredFields();

		for ( final Field declaredField : declaredFields ) {
			final Column column = declaredField.getAnnotation( Column.class );
			final String columnName = column.columnName();
			final ColumnType columnType = column.columnType();

			if ( !columnName.equals( BaseColumns._ID ) &&
					!columnName.equals( Contract.Columns.VERSION ) ) {
				final String name = declaredField.getName();
				final String getterMethodName = "get" + name.substring( 0, 1 ).toUpperCase() +
						name.substring( 1 );
				final Method declaredGetterMethod;

				try {
					declaredGetterMethod = this.parameterClass
							.getDeclaredMethod( getterMethodName, new Class<?>[ 0 ] );
				} catch ( final NoSuchMethodException noSuchMethodException ) {
					throw new InvalidClassDefinitionException( "Unable to get find method '" +
							getterMethodName + "'().", noSuchMethodException );
				}

				final Object value;

				try {
					value = declaredGetterMethod.invoke( oldObject, new Object[ 0 ] );
				} catch ( final InvocationTargetException invocationTargetException ) {
					throw new IllegalStateException(
							"Unable to get value of type '" + columnType + "' from method '" +
									getterMethodName + "()'.", invocationTargetException );
				} catch ( final IllegalAccessException illegalAccessException ) {
					throw new InvalidClassDefinitionException( "Unable to get value of type '" +
							columnType + "' from method '" + getterMethodName + "()'.",
							illegalAccessException );
				}

				final Class<?> setterParameterClass;

				switch ( columnType ) {
					case INTEGER:
						setterParameterClass = Integer.class;

						break;
					case LONG:
						setterParameterClass = Long.class;

						break;
					case STRING:
						setterParameterClass = String.class;

						break;
					default:
						throw new IllegalArgumentException( "Found unknown column type '" +
								columnType + "'." );
				}

				final String setterMethodName = "set" + name.substring( 0, 1 ).toUpperCase() +
						name.substring( 1 );

				try {
					final Method declaredSetterMethod = this.parameterClass
							.getDeclaredMethod( setterMethodName, setterParameterClass );

					declaredSetterMethod.invoke( newObject, value );
				} catch ( final InvocationTargetException invocationTargetException ) {
					throw new IllegalStateException(
							"Unable to set value '" + value + "' through method '" +
									setterMethodName + "()'.", invocationTargetException );
				} catch ( final NoSuchMethodException noSuchMethodException ) {
					throw new InvalidClassDefinitionException( "Unable to set value '" + value +
							"' through method '" + setterMethodName + "()'.",
							noSuchMethodException );
				} catch ( final IllegalAccessException illegalAccessException ) {
					throw new InvalidClassDefinitionException( "Unable to set value '" + value +
							"' through method '" + setterMethodName + "()'.",
							illegalAccessException );
				}
			}
		}

		return newObject;
	}

	private T performSave( final T object ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final ContentValues contentValues = this.generateContentValues( object );
		final Uri uri = this.contentResolver.insert( contentUri, contentValues );
		final String idString = uri.getLastPathSegment();
		final long id = Long.parseLong( idString );
		final T mergedObject = this.merge( object, id );

		return mergedObject;
	}

	/**
	 * Updates the stored data related to the given {@link DatabaseObject} with the data in that
	 * {@link DatabaseObject}.
	 *
	 * @param object The {@link DatabaseObject} to use in the update.
	 * @return The updated {@link DatabaseObject} from local storage or null if no update was done.
	 * @throws IllegalArgumentException The given {@link DatabaseObject} is managed internally and
	 * it is out of sync with local storage.
	 * @throws IllegalStateException The given {@link DatabaseObject} has already been deleted
	 * from local storage.
	 */
	private T performUpdate( final T object )
			throws IllegalArgumentException, IllegalStateException {
		final String authority = this.getAuthority();
		final long id = object.getId();
		final StringBuilder whereClauseBuilder = new StringBuilder();
		final List<String> whereArgs = new ArrayList<String>();

		whereClauseBuilder.append( BaseColumns._ID + " = ?" );
		whereArgs.add( String.valueOf( id ) );

		final boolean versionManagedExternally = object.isVersionManagedExternally();

		if ( versionManagedExternally ) {
			whereClauseBuilder.append( " AND " + Contract.Columns.VERSION + " < ?" );

			whereArgs.add( String.valueOf( object.getVersion() ) );
		} else {
			final T savedObject = this.get( id );

			if ( savedObject == null ) {
				throw new IllegalStateException(
						"Attempting to update a deleted object with: " + object );
			} else {
				final long savedObjectVersion = savedObject.getVersion();
				final long newObjectVersion = object.getVersion();

				if ( newObjectVersion < savedObjectVersion ) {
					throw new IllegalArgumentException(
							"Attempting to update a stale object.  The stored version of the " +
									"object is '" +
									savedObjectVersion +
									"' while the passed in object has a version of '" +
									newObjectVersion + "'." );
				}

				whereClauseBuilder.append( " AND " + Contract.Columns.VERSION + " = ?" );

				whereArgs.add( String.valueOf( savedObjectVersion ) );

				object.setVersion( newObjectVersion + 1 );
			}
		}

		final Uri contentUri = this.contract.getContentUri( authority, id );
		final ContentValues contentValues = this.generateContentValues( object );
		final String whereClause = whereClauseBuilder.toString();
		final int i = this.contentResolver.update( contentUri, contentValues, whereClause,
				whereArgs.toArray( new String[ whereArgs.size() ] ) );
		final T savedObject;

		if ( i == 1 ) {
			savedObject = object;
		} else {
			savedObject = null;
		}

		return savedObject;
	}

	/**
	 * Creates an "insert" {@link ContentProviderOperation} for each of the given objects.
	 *
	 * @param objectsToAdd The objects to create {@link ContentProviderOperation} for.
	 * @return The created {@link ContentProviderOperation}s.
	 */
	private List<ContentProviderOperation> processObjectsToAdd( final Collection<T> objectsToAdd
	) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final List<ContentProviderOperation> contentProviderOperations =
				new ArrayList<ContentProviderOperation>();

		for ( final T objectToAdd : objectsToAdd ) {
			final ContentProviderOperation.Builder builder =
					ContentProviderOperation.newInsert( contentUri );
			final ContentValues contentValues = this.generateContentValues( objectToAdd );

			builder.withValues( contentValues );

			final ContentProviderOperation objectToAddContentProviderOperation = builder.build();

			contentProviderOperations.add( objectToAddContentProviderOperation );
		}

		return contentProviderOperations;
	}

	/**
	 * Creates a "delete" {@link ContentProviderOperation} for each of the given objects.
	 *
	 * @param objectsToDelete The objects to create {@link ContentProviderOperation} for.
	 * @return The created {@link ContentProviderOperation}s.
	 */
	private List<ContentProviderOperation> processObjectsToDelete(
			final Collection<T> objectsToDelete ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final List<ContentProviderOperation> contentProviderOperations =
				new ArrayList<ContentProviderOperation>();

		for ( final T objectToDelete : objectsToDelete ) {
			final long id = objectToDelete.getId();
			final Uri modifiedObjectUri = ContentUris.withAppendedId( contentUri, id );
			final ContentProviderOperation.Builder builder =
					ContentProviderOperation.newDelete( modifiedObjectUri );
			final String selectionClause = BaseColumns._ID + " = ?";
			final List<String> selectionArgs = new ArrayList<String>();

			selectionArgs.add( String.valueOf( id ) );

			builder.withSelection( selectionClause,
					selectionArgs.toArray( new String[ selectionArgs.size() ] ) );

			final ContentProviderOperation objectToDeleteContentProviderOperation = builder
					.build();

			contentProviderOperations.add( objectToDeleteContentProviderOperation );
		}

		return contentProviderOperations;
	}

	/**
	 * Creates an "update" {@link ContentProviderOperation} for each of the given objects.
	 *
	 * @param objectsToUpdate The objects to create {@link ContentProviderOperation} for.
	 * @return The created {@link ContentProviderOperation}s.
	 */
	private List<ContentProviderOperation> processObjectsToUpdate(
			final Collection<T> objectsToUpdate ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final List<ContentProviderOperation> contentProviderOperations =
				new ArrayList<ContentProviderOperation>();

		for ( final T objectToUpdate : objectsToUpdate ) {
			final long id = objectToUpdate.getId();
			final Uri modifiedObjectUri = ContentUris.withAppendedId( contentUri, id );
			final ContentProviderOperation.Builder builder =
					ContentProviderOperation.newUpdate( modifiedObjectUri );
			final ContentValues contentValues = this.generateContentValues( objectToUpdate );
			final String selectionClause = BaseColumns._ID + " = ?";
			final List<String> selectionArgs = new ArrayList<String>();

			selectionArgs.add( String.valueOf( id ) );

			builder.withSelection( selectionClause,
					selectionArgs.toArray( new String[ selectionArgs.size() ] ) );
			builder.withValues( contentValues );

			final ContentProviderOperation objectToUpdateContentProviderOperation = builder
					.build();

			contentProviderOperations.add( objectToUpdateContentProviderOperation );
		}

		return contentProviderOperations;
	}

	/**
	 * Replaces the existing collection of saved database objects with the given collection.
	 *
	 * @param replacementObjects The newer collection of database objects that should overwrite
	 * the existing collection.
	 * @return The latest version of the objects that have been saved or updated.
	 * @throws StorageModificationException One of the replacement operations (either add, update,
	 * or delete) failed.
	 */
	public List<T> replace( final Collection<T> replacementObjects )
			throws StorageModificationException {
		final String selectionClause = null;
		final List<String> selectionArgs = Collections.emptyList();
		final List<T> objectsChanged =
				this.replace( replacementObjects, selectionClause, selectionArgs );

		return objectsChanged;
	}

	/**
	 * Replaces the existing collection of saved database objects with the given collection.
	 *
	 * @param replacementObjects The newer collection of database objects that should overwrite
	 * the existing collection.
	 * @param selectionClause A filter declaring which rows to replace, formatted as an SQL WHERE
	 * clause (excluding the WHERE itself).
	 * @param selectionArgs You may include ?s in the selection clause, which will be replaced by
	 * the values from selectionArgs, in the order that they appear in the selection. The values
	 * will be bound as Strings.
	 * @return The latest version of the objects that have been saved or updated.
	 * @throws StorageModificationException One of the replacement operations (either add, update,
	 * or delete) failed.
	 */
	public List<T> replace( final Collection<T> replacementObjects, final String selectionClause,
			final List<String> selectionArgs ) throws StorageModificationException {
		final List<T> existingObjects;

		if ( selectionClause == null ) {
			existingObjects = this.get();
		} else {
			existingObjects = this.get( selectionClause, selectionArgs );
		}

		final Triple<List<T>, List<T>, List<T>> objectsToAddUpdateAndDeleteTriple =
				this.categorize( existingObjects, replacementObjects );
		final List<T> objectsToAdd = objectsToAddUpdateAndDeleteTriple.getLeft();
		final List<T> objectsToUpdate = objectsToAddUpdateAndDeleteTriple.getMiddle();
		final List<T> objectsToDelete = objectsToAddUpdateAndDeleteTriple.getRight();
		final List<T> objectsChanged = this.apply( objectsToAdd, objectsToUpdate,
				objectsToDelete );

		return objectsChanged;
	}

	public List<T> save( final Collection<T> objects ) throws StorageModificationException {
		final List<T> existingObjects = this.get();
		final Triple<List<T>, List<T>, List<T>> objectsToAddUpdateAndDeleteTriple =
				this.categorize( existingObjects, objects );
		final List<T> objectsToAdd = objectsToAddUpdateAndDeleteTriple.getLeft();
		final List<T> objectsToUpdate = objectsToAddUpdateAndDeleteTriple.getMiddle();
		final List<T> savedObjects = this.apply( objectsToAdd, objectsToUpdate );

		return savedObjects;
	}

	/**
	 * Saves/updates the given {@link DatabaseObject} to/in local storage.  If the version of the
	 * {@link DatabaseObject} is managed internally, the save will succeed only if the given
	 * {@link DatabaseObject} has never been saved before or the saved {@link DatabaseObject}
	 * version is the same as the given {@link DatabaseObject} version. If the version of the
	 * {@link DatabaseObject} is managed externally, the save will succeed only if the given
	 * {@link DatabaseObject} has never been saved before or the saved {@link DatabaseObject}
	 * version is older than the given {@link DatabaseObject} version.
	 *
	 * @param object The {@link DatabaseObject} to save/update.
	 * @return The newly saved object, or {@code null} if no save was done.
	 * @throws IllegalArgumentException The given {@link DatabaseObject} is managed internally and
	 * it is out of sync with local storage.
	 */
	public T save( final T object ) {
		final boolean idManagedExternally = object.isIdManagedExternally();
		final T savedObject;

		if ( idManagedExternally ) {
			final long id = object.getId();
			final T existingObject = this.get( id );

			if ( existingObject == null ) {
				T attemptedSaveObject = null;

				try {
					attemptedSaveObject = this.performSave( object );
				} catch ( final SQLException sqlException ) {
					final T justAddedDuplicateObject = this.get( id );

					if ( justAddedDuplicateObject == null ) {
						throw sqlException;
					} else {
						attemptedSaveObject = this.performUpdate( object );
					}
				}

				savedObject = attemptedSaveObject;
			} else {
				savedObject = this.performUpdate( object );
			}
		} else {
			final Long id = object.getId();

			if ( id == null ) {
				savedObject = this.performSave( object );
			} else {
				savedObject = this.performUpdate( object );
			}
		}

		return savedObject;
	}

	/**
	 * Updates all objects with the given field values.
	 *
	 * @param contentValues The new field values. The key is the column name for the field. A null
	 * value will remove an existing field value.
	 * @return The number of objects updated.
	 */
	protected final int update( final ContentValues contentValues ) {
		final String whereClause = null;
		final List<String> whereArgs = Collections.emptyList();
		final int numberOfUpdatedObjects = this.update( contentValues, whereClause, whereArgs );

		return numberOfUpdatedObjects;
	}

	/**
	 * Updates all objects that match the where clause with the given field values.
	 *
	 * @param contentValues The new field values. The key is the column name for the field. A null
	 * value will remove an existing field value.
	 * @param whereClause A filter to apply to rows before updating, formatted as an SQL WHERE
	 * clause (excluding the WHERE itself).
	 * @param whereArgs The values with which to replace the question marks in the where clause.
	 * @return The number of objects updated.
	 */
	protected final int update( final ContentValues contentValues, final String whereClause,
			final List<String> whereArgs ) {
		final String authority = this.getAuthority();
		final Uri contentUri = this.contract.getContentUri( authority );
		final int numberOfUpdatedObjects = this.contentResolver
				.update( contentUri, contentValues, whereClause,
						whereArgs.toArray( new String[ whereArgs.size() ] ) );

		return numberOfUpdatedObjects;
	}
}