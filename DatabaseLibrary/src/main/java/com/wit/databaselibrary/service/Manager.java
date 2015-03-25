package com.wit.databaselibrary.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Pair;

import com.wit.databaselibrary.contentprovider.contract.Contract;
import com.wit.databaselibrary.model.DatabaseObject;
import com.wit.databaselibrary.model.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Manager<T extends DatabaseObject> {
	public interface OnUpdateListener {
		public void onUpdate();
	}

	protected class InterfacingContentObserver extends ContentObserver {
		private final Manager.OnUpdateListener onUpdateListener;

		/**
		 * Creates a content observer.
		 *
		 * @param handler The handler to run {@link #onChange} on, or null if none.
		 */
		public InterfacingContentObserver( final Handler handler, final Manager.OnUpdateListener onUpdateListener ) {
			super(handler);

			this.onUpdateListener = onUpdateListener;
		}

		@Override
		public void onChange( boolean selfChange ) {
			onChange( selfChange, null );
		}

		@Override
		public void onChange( boolean selfChange, Uri uri ) {
			this.onUpdateListener.onUpdate();
		}
	}

	private final ContentResolver contentResolver;
	private final Handler handler;
	private final Map<Manager.OnUpdateListener, ContentObserver> contentObservers = new HashMap<>();
	protected final String packageName;

	protected Manager( final Context context ) {
		this.contentResolver = context.getContentResolver();
		this.handler = new Handler( context.getMainLooper() );
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
		final List<Pair<String, Order>> orderBys = Collections.emptyList();
		final List<T> objects = this.get( selection, selectionArgs, orderBys, null );

		return objects;
	}

	public List<T> get( final String selection, final List<String> selectionArgs,
			final Integer limit ) {
		final List<Pair<String, Order>> orderBys = Collections.emptyList();
		final List<T> objects = this.get( selection, selectionArgs, orderBys, limit );

		return objects;
	}

	public List<T> get( final String selection, final List<String> selectionArgs,
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

	private T performSave( final T object ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final ContentValues contentValues = this.generateContentValues( object );
		final Uri uri =
				this.contentResolver.insert( contentUri, contentValues );

		final T savedObject = this.get( uri );

		return savedObject;
	}

	private T performUpdate( final T object ) {
		final Contract contract = this.getContract();
		final String authority = this.getAuthority();
		final Uri contentUri = contract.getContentUri( authority );
		final Long id = object.getId();
		final ContentValues contentValues = this.generateContentValues( object );
		final String whereClause = BaseColumns._ID + "=" + id.intValue();
		final int i = this.contentResolver.update( contentUri, contentValues,
				whereClause, null );
		final T savedObject;

		if (i == 1) {
			savedObject = object;
		} else {
			savedObject = null;
		}

		return savedObject;
	}

	public void registerForUpdates( final Manager.OnUpdateListener onUpdateListener ) {
		this.registerForUpdates( null, onUpdateListener );
	}

	public void registerForUpdates( final T t, final Manager.OnUpdateListener onUpdateListener ) {
		final String authority = this.getAuthority();
		final Contract contract = this.getContract();
		final Uri uri;

		if ( t == null ) {
			uri = contract.getContentUri( authority );
		} else {
			final Long id = t.getId();

			uri = contract.getContentUri( authority, id);
		}

		final boolean notifyForDescendants = true;
		final InterfacingContentObserver interfacingContentObserver = new InterfacingContentObserver( this.handler, onUpdateListener );

		this.contentObservers.put( onUpdateListener, interfacingContentObserver );
		this.contentResolver.registerContentObserver( uri, notifyForDescendants, interfacingContentObserver );
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
		final boolean managedRemotely = object.isManagedRemotely();
		final T savedObject;

		if ( managedRemotely ) {
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

	public void unregisterForUpdates( final Manager.OnUpdateListener onUpdateListener ) {
		final ContentObserver contentObserver = this.contentObservers.get( onUpdateListener );

		this.contentResolver.unregisterContentObserver( contentObserver );
		this.contentObservers.remove( onUpdateListener );
	}
}