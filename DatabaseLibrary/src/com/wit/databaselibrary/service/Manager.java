package com.wit.databaselibrary.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import com.wit.databaselibrary.contentprovider.StorageModificationException;
import com.wit.databaselibrary.contentprovider.contract.Contract;
import com.wit.databaselibrary.model.DatabaseObject;
import com.wit.databaselibrary.model.Order;
import com.wit.databaselibrary.model.id.IdWrapper;
import com.wit.databaselibrary.model.id.SimpleIdWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Manager<T extends DatabaseObject> {
	public static interface OnChangeListener<T extends DatabaseObject> {
		/**
		 * Performs whatever work is necessary in response to some objects in the given collection having changed.
		 *
		 * @param allObjects The entire collection of objects, some of which have changed.
		 */
		public void onChange( final List<T> allObjects );

		/**
		 * Performs whatever work is necessary in response to the given object having been created/updated.
		 *
		 * @param changedObject The object that has been created/updated.
		 */
		public void onChange( final T changedObject );

		/**
		 * Performs whatever work is necessary in response to the object with the given ID having been deleted.
		 *
		 * @param id The ID of the deleted object.
		 */
		public void onDelete( final long id );
	}

	protected static class InterfacingContentObserver<T extends DatabaseObject> extends ContentObserver {
		/**
		 * The {@link OnChangeListener} to call when a change has been made.
		 */
		private final OnChangeListener onChangeListener;

		/**
		 * The {@link Manager} to use when parsing {@link Uri}s.
		 */
		private final Manager<T> manager;

		/**
		 * Whether notifications should only be done for URIs containing IDs.
		 */
		private final boolean notifyStrictlyForDescendants;

		/**
		 * Creates a content observer.
		 *
		 * @param handler The handler to run {@link #onChange} on, or null if none.
		 * @param onChangeListener The {@link OnChangeListener} to call when a change has been made.
		 * @param manager The {@link Manager} to use when parsing {@link Uri}s.
		 * @param notifyStrictlyForDescendants Whether notifications should only be done for URIs containing IDs.
		 */
		public InterfacingContentObserver( final Handler handler, final OnChangeListener onChangeListener,
				final Manager<T> manager, final boolean notifyStrictlyForDescendants ) {
			super( handler );

			this.onChangeListener = onChangeListener;
			this.manager = manager;
			this.notifyStrictlyForDescendants = notifyStrictlyForDescendants;
		}

		@Override
		public void onChange( final boolean selfChange ) {
			onChange( selfChange, null );
		}

		public void onChange( final boolean selfChange, final Uri uri ) {
			final Contract contract = this.manager.getContract();
			final boolean hasId = contract.hasId( uri );
			final List<T> objects = this.manager.get( uri );

			if ( hasId ) {
				final long objectId = contract.getId( uri );
				final boolean noObjectsFound = objects.isEmpty();

				if ( noObjectsFound ) {
					this.onChangeListener.onDelete( objectId );
				} else {
					final T updatedObject = objects.get( 0 );

					this.onChangeListener.onChange( updatedObject );
				}
			} else {
				if ( !this.notifyStrictlyForDescendants ) {
					this.onChangeListener.onChange( objects );
				}
			}
		}
	}

	private final ContentResolver contentResolver;
	private final Handler handler;
	private final Map<OnChangeListener, ContentObserver> contentObservers =
			new HashMap<OnChangeListener, ContentObserver>();
	protected final String packageName;

	protected Manager( final Context context ) {
		this.contentResolver = context.getContentResolver();
		this.handler = new Handler( context.getMainLooper() );
		this.packageName = context.getPackageName();
	}

	/**
	 * Adds, updates, and deletes the specified objects.
	 *
	 * @param objectsToAdd The objects that need to be added.
	 * @param objectsToUpdate The objects that need to be updated.
	 * @param objectsToDelete The objects that need to be deleted.
	 * @throws StorageModificationException An add, update, or delete operation failed.
	 */
	private void apply( final List<T> objectsToAdd, final List<T> objectsToUpdate, final List<T> objectsToDelete )
			throws StorageModificationException {
		final String authority = this.getAuthority();
		final ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();
		final List<ContentProviderOperation> addedObjectContentProviderOperations =
				this.processObjectsToAdd( objectsToAdd );

		contentProviderOperations.addAll( addedObjectContentProviderOperations );

		final List<ContentProviderOperation> modifiedObjectContentProviderOperations =
				this.processObjectsToUpdate( objectsToUpdate );

		contentProviderOperations.addAll( modifiedObjectContentProviderOperations );

		final List<ContentProviderOperation> deletedObjectContentProviderOperations =
				this.processObjectsToDelete( objectsToDelete );

		contentProviderOperations.addAll( deletedObjectContentProviderOperations );

		try {
			this.contentResolver.applyBatch( authority, contentProviderOperations );
		} catch ( final RemoteException remoteException ) {
			Log.e( Manager.class.getSimpleName(),
					"An error happened while attempting to communicate with a remote provider.", remoteException );
		} catch ( OperationApplicationException operationApplicationException ) {
			throw new StorageModificationException( "An add, update, or delete operation failed to be applied.",
					operationApplicationException );
		}
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
	public void delete( final List<T> objects ) throws StorageModificationException {
		final String authority = this.getAuthority();
		final ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>(
				this.processObjectsToDelete( objects ) );

		try {
			this.contentResolver.applyBatch( authority, contentProviderOperations );
		} catch ( final RemoteException remoteException ) {
			Log.e( Manager.class.getSimpleName(),
					"An error happened while attempting to communicate with a remote provider.", remoteException );
		} catch ( OperationApplicationException operationApplicationException ) {
			throw new StorageModificationException( "A delete operation failed to be applied.",
					operationApplicationException );
		}
	}

	/**
	 * Deletes the given {@link DatabaseObject}.
	 *
	 * @param object The {@link DatabaseObject} to delete.
	 * @return 1 if the specified {@link DatabaseObject} was deleted, or 0 if it was not deleted.
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
	 * @param whereClause The where clause to use to narrow down the {@link DatabaseObject}s to delete.
	 * @param whereArgs The values to replace the question marks with in the where clause.
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 */
	public int delete( final String whereClause, final List<String> whereArgs ) {
		final Long id = null;
		final int numberOfRowsDeleted = this.delete( id, whereClause, whereArgs );

		return numberOfRowsDeleted;
	}

	/**
	 * Deletes the given {@link DatabaseObject} if the current saved state of it satisfies the given where clause and
	 * arguments.  Note that the object ID matching condition will be added to the where clause and argument list.
	 *
	 * @param object The {@link DatabaseObject} to delete, assuming it satisfies the given where clause and arguments.
	 * @param whereClause The where clause to use to determine whether the {@link DatabaseObject} should be deleted.
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
	 * Deletes any {@link DatabaseObject} that satisfy the given where clause and arguments using the given ID as part
	 * of the content URI.
	 *
	 * @param id The ID to use as part of the content URI associated with the delete, if any.
	 * @param whereClause The where clause to use to narrow down the {@link DatabaseObject}s to delete.
	 * @param whereArgs The values to replace the question marks with in the where clause.
	 * @return The number of {@link DatabaseObject}s that were deleted.
	 */
	private int delete( final Long id, final String whereClause, final List<String> whereArgs ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri;

		if ( id == null ) {
			contentUri = contract.getContentUri( authority );
		} else {
			contentUri = contract.getContentUri( authority, id );
		}

		final int numberOfRowsDelete = this.contentResolver
				.delete( contentUri, whereClause, whereArgs.toArray( new String[ whereArgs.size() ] ) );

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
		final List<T> objects;

		if ( cursor == null ) {
			objects = Collections.emptyList();
		} else {
			objects = this.getAll( cursor );

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
		final List<Pair<String, Order>> orderBys = Collections.emptyList();
		final Integer limit = null;
		final List<T> objects = this.get( selection, selectionArgs, orderBys, limit );

		return objects;
	}

	public List<T> get( final String selection, final List<String> selectionArgs,
			final int limit ) {
		final List<Pair<String, Order>> orderBys = Collections.emptyList();
		final List<T> objects = this.get( selection, selectionArgs, orderBys, (Integer) limit );

		return objects;
	}

	public List<T> get( final String selection, final List<String> selectionArgs,
			final List<Pair<String, Order>> orderBys, final int limit ) {
		final List<T> objects = this.get( selection, selectionArgs, orderBys, (Integer) limit );

		return objects;
	}

	private List<T> get( final String selection, final List<String> selectionArgs,
			final List<Pair<String, Order>> orderBys, final Integer limit ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<String> projection = contract.getColumnNames();
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

		final Cursor cursor =
				this.contentResolver.query(
						contentUri,
						projection.toArray( new String[ projection.size() ] ),
						selection,
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
		final Contract contract = this.getContract();
		final List<String> projection = contract.getColumnNames();
		final Cursor cursor =
				this.contentResolver.query( uri,
						projection.toArray( new String[ projection.size() ] ),
						null, null, null );
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

	protected abstract Contract getContract();

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
	 * Returns the number of {@link DatabaseObject}s that are saved that satisfy the given selection criteria.
	 *
	 * @param selectionClause The selection clause to use to narrow down the {@link DatabaseObject}s to include in the
	 * count.
	 * @param selectionArgs The values to replace the placeholders with in the selection clause.
	 * @return The number of {@link DatabaseObject}s that are saved that satisfy the given selection criteria.
	 */
	public final int getCount( final String selectionClause, final List<String> selectionArgs ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
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

	protected IdWrapper getId( final T object ) {
		final IdWrapper idWrapper = new SimpleIdWrapper( object.getId() );

		return idWrapper;
	}

	private T performSave( final T object ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final ContentValues contentValues = this.generateContentValues( object );
		final Uri uri =
				this.contentResolver.insert( contentUri, contentValues );
		final List<T> savedObjects = this.get( uri );
		final T savedObject = savedObjects.get( 0 );

		return savedObject;
	}

	private T performUpdate( final T object ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final Long id = object.getId();
		final ContentValues contentValues = this.generateContentValues( object );
		final boolean managedExternally = object.isManagedExternally();
		final String whereClause;
		final List<String> whereArgs = new ArrayList<String>();

		if ( managedExternally ) {
			whereClause = BaseColumns._ID + " = ? AND " + Contract.Columns.VERSION + " < ?";

			whereArgs.add( String.valueOf( id ) );
			whereArgs.add( String.valueOf( object.getVersion() ) );
		} else {
			whereClause = BaseColumns._ID + " = ?";

			whereArgs.add( String.valueOf( id ) );
		}

		final int i = this.contentResolver.update( contentUri, contentValues,
				whereClause, whereArgs.toArray( new String[ whereArgs.size() ] ) );
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
	private List<ContentProviderOperation> processObjectsToAdd( final List<T> objectsToAdd ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();

		for ( final T objectToAdd : objectsToAdd ) {
			final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert( contentUri );
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
	private List<ContentProviderOperation> processObjectsToDelete( final List<T> objectsToDelete ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();

		for ( final T objectToDelete : objectsToDelete ) {
			final long id = objectToDelete.getId();
			final Uri modifiedObjectUri = ContentUris.withAppendedId( contentUri, id );
			final ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete( modifiedObjectUri );
			final String selectionClause = BaseColumns._ID + " = ?";
			final List<String> selectionArgs = new ArrayList<String>();

			selectionArgs.add( String.valueOf( id ) );

			builder.withSelection( selectionClause, selectionArgs.toArray( new String[ selectionArgs.size() ] ) );

			final ContentProviderOperation objectToDeleteContentProviderOperation = builder.build();

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
	private List<ContentProviderOperation> processObjectsToUpdate( final List<T> objectsToUpdate ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final List<ContentProviderOperation> contentProviderOperations = new ArrayList<ContentProviderOperation>();

		for ( final T objectToUpdate : objectsToUpdate ) {
			final long id = objectToUpdate.getId();
			final Uri modifiedObjectUri = ContentUris.withAppendedId( contentUri, id );
			final ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate( modifiedObjectUri );
			final ContentValues contentValues = this.generateContentValues( objectToUpdate );
			final String selectionClause = BaseColumns._ID + " = ?";
			final List<String> selectionArgs = new ArrayList<String>();

			selectionArgs.add( String.valueOf( id ) );

			builder.withSelection( selectionClause, selectionArgs.toArray( new String[ selectionArgs.size() ] ) );
			builder.withValues( contentValues );

			final ContentProviderOperation objectToUpdateContentProviderOperation = builder.build();

			contentProviderOperations.add( objectToUpdateContentProviderOperation );
		}

		return contentProviderOperations;
	}

	/**
	 * Registers the given {@link OnChangeListener} to listen to changes to either particular {@link DatabaseObject}s or
	 * to the table as a whole, depending on whether {@code notifyStrictlyForDescendants} is true or false,
	 * respectively.
	 *
	 * @param onChangeListener The {@link OnChangeListener} to call when a change has been made.
	 * @param notifyStrictlyForDescendants Whether notifications should only be delivered for changes to particular
	 * {@link DatabaseObject}s.  If false, notifications will be only be delivered for changes to the database table in
	 * general.
	 */
	public void registerForUpdates( final OnChangeListener onChangeListener,
			final boolean notifyStrictlyForDescendants ) {
		this.registerForUpdates( null, onChangeListener, notifyStrictlyForDescendants );
	}

	/**
	 * Registers the given {@link OnChangeListener} to listen to changes to the given {@link DatabaseObject}.
	 *
	 * @param t The {@link DatabaseObject} to listen to changes for.
	 * @param onChangeListener The {@link OnChangeListener} to call when a change has been made.
	 */
	public void registerForUpdates( final T t, final OnChangeListener onChangeListener ) {
		final boolean notifyStrictlyForDescendants = false;

		this.registerForUpdates( t, onChangeListener, notifyStrictlyForDescendants );
	}

	/**
	 * Registers the given {@link OnChangeListener} to listen to changes to either the given {@link DatabaseObject} or,
	 * if none is specified, changes to any particular {@link DatabaseObject} or to the table as a whole, depending on
	 * whether {@code notifyStrictlyForDescendants} is true or false, respectively.
	 *
	 * @param t The {@link DatabaseObject} to listen to changes for.
	 * @param onChangeListener The {@link OnChangeListener} to call when a change has been made.
	 * @param notifyStrictlyForDescendants Whether notifications should only be delivered for changes to particular
	 * {@link DatabaseObject}s.  If false, notifications will be only be delivered for changes to the database table in
	 * general.
	 */
	private void registerForUpdates( final T t, final OnChangeListener onChangeListener,
			final boolean notifyStrictlyForDescendants ) {
		final String authority = this.getAuthority();
		final Contract contract = this.getContract();
		final Uri uri;

		if ( t == null ) {
			uri = contract.getContentUri( authority );
		} else {
			final Long id = t.getId();

			uri = contract.getContentUri( authority, id );
		}

		final boolean notifyForDescendants = notifyStrictlyForDescendants;
		final InterfacingContentObserver interfacingContentObserver =
				new InterfacingContentObserver( this.handler, onChangeListener, this, notifyStrictlyForDescendants );

		this.contentObservers.put( onChangeListener, interfacingContentObserver );
		this.contentResolver.registerContentObserver( uri, notifyForDescendants, interfacingContentObserver );
	}

	/**
	 * Replaces the existing collection of saved database objects with the given collection.
	 *
	 * @param replacementObjects The newer collection of database objects that should overwrite the existing
	 * collection.
	 * @throws StorageModificationException One of the replacement operations (either add, update, or delete) failed.
	 */
	public void replace( final List<T> replacementObjects ) throws StorageModificationException {
		final String selection = null;
		final List<String> selectionArgs = Collections.emptyList();

		this.replace( replacementObjects, selection, selectionArgs );
	}

	/**
	 * Replaces the existing collection of saved database objects with the given collection.
	 *
	 * @param replacementObjects The newer collection of database objects that should overwrite the existing
	 * collection.
	 * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE
	 * itself).
	 * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs, in
	 * the order that they appear in the selection. The values will be bound as Strings.
	 * @throws StorageModificationException One of the replacement operations (either add, update, or delete) failed.
	 */
	public void replace( final List<T> replacementObjects, final String selection, final List<String> selectionArgs )
			throws StorageModificationException {
		final List<T> oldObjects;

		if ( selection == null ) {
			oldObjects = this.get();
		} else {
			oldObjects = this.get( selection, selectionArgs );
		}

		final Map<IdWrapper, T> oldObjectIdsToSources = new HashMap<IdWrapper, T>();

		for ( final T oldObject : oldObjects ) {
			final IdWrapper oldObjectIdWrapper = this.getId( oldObject );

			oldObjectIdsToSources.put( oldObjectIdWrapper, oldObject );
		}

		final List<T> objectsToAdd = new ArrayList<T>();
		final List<T> objectsToUpdate = new ArrayList<T>();
		final List<T> objectsToDelete = new ArrayList<T>();

		for ( final T replacementObject : replacementObjects ) {
			final IdWrapper replacementObjectIdWrapper = this.getId( replacementObject );
			final boolean objectAlreadyExisted = oldObjectIdsToSources.containsKey( replacementObjectIdWrapper );

			if ( objectAlreadyExisted ) {
				final Long replacementObjectVersion = replacementObject.getVersion();
				final T oldObject = oldObjectIdsToSources.remove( replacementObjectIdWrapper );
				final Long oldObjectVersion = oldObject.getVersion();

				if ( replacementObjectVersion > oldObjectVersion ) {
					objectsToUpdate.add( replacementObject );
				}
			} else {
				objectsToAdd.add( replacementObject );
			}
		}

		objectsToDelete.addAll( oldObjectIdsToSources.values() );

		this.apply( objectsToAdd, objectsToUpdate, objectsToDelete );
	}

	public List<T> save( final List<T> objects ) {
		final List<T> savedObjects = new ArrayList<T>();

		for ( final T object : objects ) {
			final T savedObject = this.save( object );

			if ( savedObject != null ) {
				savedObjects.add( savedObject );
			}
		}

		return savedObjects;
	}

	public T save( final T object ) {
		final Long id = object.getId();
		final boolean managedExternally = object.isManagedExternally();
		final T savedObject;

		if ( managedExternally ) {
			final T existingObject = this.get( id );

			if ( existingObject == null ) {
				savedObject = this.performSave( object );
			} else {
				savedObject = this.performUpdate( object );
			}
		} else {
			if ( id == null ) {
				savedObject = this.performSave( object );
			} else {
				savedObject = this.performUpdate( object );
			}
		}

		return savedObject;
	}

	public void unregisterForUpdates( final OnChangeListener onChangeListener ) {
		final ContentObserver contentObserver = this.contentObservers.get( onChangeListener );

		this.contentResolver.unregisterContentObserver( contentObserver );
		this.contentObservers.remove( onChangeListener );
	}
}