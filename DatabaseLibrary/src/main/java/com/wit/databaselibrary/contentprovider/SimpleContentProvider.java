package com.wit.databaselibrary.contentprovider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.wit.databaselibrary.contentprovider.contract.Contract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SimpleContentProvider extends ContentProvider {
	private final List<Contract> contracts;

	public SimpleContentProvider( final List<Contract> contracts ) {
		this.contracts = contracts;
	}

	private String adjustSelection( final Uri uri, String selection, final String authority ) {
		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatchesObjectId( uri, authority ) ) {
				selection = contract.addSelectionById( uri, selection );

				break;
			}
		}

		return selection;
	}

	@Override
	public ContentProviderResult[] applyBatch( final ArrayList<ContentProviderOperation> contentProviderOperations )
			throws OperationApplicationException {
		final List<ContentProviderResult> contentProviderResults = new ArrayList<ContentProviderResult>();
		final Set<Uri> uris = new HashSet<Uri>();

		for ( final ContentProviderOperation contentProviderOperation : contentProviderOperations ) {
			final ContentProviderResult contentProviderResult = contentProviderOperation.apply( this, null, 0 );
			final Uri newObjectUri = contentProviderResult.uri;

			if ( newObjectUri == null ) {
				final int count = contentProviderResult.count;

				if ( count >= 1 ) {
					final Uri uri = contentProviderOperation.getUri();

					uris.add( uri );
				}
			} else {
				uris.add( newObjectUri );
			}

			contentProviderResults.add( contentProviderResult );
		}

		final Context context = this.getContext();
		final ContentResolver contentResolver = context.getContentResolver();

		if ( !contentProviderOperations.isEmpty() ) {
			final Uri uri = contentProviderOperations.get( 0 ).getUri();
			final List<String> pathSegments = uri.getPathSegments();
			final Uri baseUri;

			if ( pathSegments.size() == 1 ) {
				baseUri = uri;
			} else {
				final String scheme = uri.getScheme();
				final String authority = uri.getAuthority();
				final String rootPathSegment = pathSegments.get( 0 );
				final Uri.Builder baseUriBuilder = new Uri.Builder();

				baseUriBuilder.scheme( scheme );
				baseUriBuilder.authority( authority );
				baseUriBuilder.appendPath( rootPathSegment );

				baseUri = baseUriBuilder.build();
			}

			uris.add( baseUri );
		}

		for ( final Uri uri : uris ) {
			contentResolver.notifyChange( uri, null );
		}

		return contentProviderResults.toArray( new ContentProviderResult[ contentProviderResults.size() ] );
	}

	@Override
	public int delete( final Uri uri, final String selection,
			final String[] selectionArgs ) {
		final String authority = this.getAuthority();
		final String tableName = this.getTableName( uri, authority );
		final SQLiteOpenHelper databaseHelper = this.getDatabaseHelper();
		final SQLiteDatabase sqLiteDatabase =
				databaseHelper.getWritableDatabase();
		final int count =
				sqLiteDatabase.delete( tableName, selection, selectionArgs );
		final Context context = this.getContext();
		final ContentResolver contentResolver = context.getContentResolver();

		contentResolver.notifyChange( uri, null );

		final Contract contract = this.getContract( uri );
		final Uri rootUri = contract.getContentUri( authority );

		contentResolver.notifyChange( rootUri, null );

		return count;
	}

	protected abstract String getAuthority();

	/**
	 * Returns the {@link Contract} that successfully matches the given {@link Uri} by object or object ID.
	 *
	 * @param uri The {@link Uri} to try to match.
	 * @return The {@link Contract} that successfully matches the given {@link Uri} by object or object ID.
	 * @throws IllegalArgumentException The given {@link Uri} did not match any {@link Contract} by object or object ID.
	 */
	private final Contract getContract( final Uri uri ) throws IllegalArgumentException {
		final String authority = this.getAuthority();
		Contract contract = null;

		for ( final Contract currentContract : this.contracts ) {
			if ( currentContract.uriMatchesObject( uri, authority ) ||
					currentContract.uriMatchesObjectId( uri, authority ) ) {
				contract = currentContract;
			}
		}

		if ( contract == null ) {
			throw new IllegalArgumentException( "Unknown URI, \"" + uri +
					"\". Please ensure that it has been added to the list of Contracts passed into the " +
					SimpleContentProvider.class.getSimpleName() + " class." );
		}

		return contract;
	}

	/**
	 * Returns the {@link Contract} that successfully matches the given {@link Uri} by object.
	 *
	 * @param uri The {@link Uri} to try to match.
	 * @return The {@link Contract} that successfully matches the given {@link Uri} by object.
	 * @throws IllegalArgumentException The given {@link Uri} did not match any {@link Contract} by object.
	 */
	private final Contract getContractByMatchingObject( final Uri uri ) throws IllegalArgumentException {
		final String authority = this.getAuthority();
		Contract contract = null;

		for ( final Contract currentContract : this.contracts ) {
			if ( currentContract.uriMatchesObject( uri, authority ) ) {
				contract = currentContract;
			}
		}

		if ( contract == null ) {
			throw new IllegalArgumentException( "Unknown URI, \"" + uri +
					"\". Please ensure that it has been added to the list of Contracts passed into the " +
					SimpleContentProvider.class.getSimpleName() + " class." );
		}

		return contract;
	}

	protected abstract SQLiteOpenHelper getDatabaseHelper();

	protected abstract String getDatabaseName();

	protected abstract int getDatabaseVersion();

	private String getTableName( final Uri uri, final String authority ) {
		String tableName = null;

		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatches( uri, authority ) ) {
				tableName = contract.getTableName();

				break;
			}
		}

		if ( tableName == null ) {
			throw new IllegalArgumentException( "Unknown URI, \"" + uri +
					"\". Are you sure you added it to the list of Contracts passed into the " +
					SimpleContentProvider.class.getSimpleName() + " class?" );
		}

		return tableName;
	}

	@Override
	public String getType( final Uri uri ) {
		final String authority = this.getAuthority();
		String contentType = null;

		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatchesObject( uri, authority ) ) {
				contentType = contract.getContentType();

				break;
			} else if ( contract.uriMatchesObjectId( uri, authority ) ) {
				contentType = contract.getContentItemType();

				break;
			}
		}

		if ( contentType == null ) {
			throw new IllegalArgumentException( "Unknown URI, \"" + uri +
					"\". Are you sure you added it to the list of Contracts passed into the " +
					SimpleContentProvider.class.getSimpleName() + " class?" );
		}

		return contentType;
	}

	@Override
	public Uri insert( final Uri uri, ContentValues contentValues ) {
		final Contract contract = this.getContractByMatchingObject( uri );
		final SQLiteOpenHelper databaseHelper = this.getDatabaseHelper();
		final SQLiteDatabase sqLiteDatabase =
				databaseHelper.getWritableDatabase();

		if ( contentValues == null ) {
			contentValues = new ContentValues();
		}

		final String tableName = contract.getTableName();
		final String nullColumnHack;

		if ( contentValues.size() == 0 ) {
			nullColumnHack = BaseColumns._ID;
		} else {
			nullColumnHack = null;
		}

		final long rowId =
				sqLiteDatabase.insert( tableName, nullColumnHack, contentValues );

		if ( rowId > 0 ) {
			final Uri contentUriWithAppendedId =
					ContentUris.withAppendedId( uri, rowId );
			final Context context = this.getContext();
			final ContentResolver contentResolver = context.getContentResolver();

			contentResolver.notifyChange( contentUriWithAppendedId, null );

			final Uri rootUri = uri;

			contentResolver.notifyChange( rootUri, null );

			return contentUriWithAppendedId;
		} else {
			throw new SQLException( "Failed to insert row into " + uri );
		}
	}

	@Override
	public Cursor query( final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder ) {
		final String authority = this.getAuthority();
		Map<String, String> projectionMap = null;

		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatches( uri, authority ) ) {
				projectionMap = contract.getProjectionMap();
			}
		}

		if ( projectionMap == null ) {
			throw new IllegalArgumentException( "Unknown URI, \"" + uri +
					"\". Are you sure you added it to the list of Contracts passed into the " +
					SimpleContentProvider.class.getSimpleName() + " class?" );
		}

		String newSelection;

		if ( selection == null ) {
			newSelection = this.adjustSelection( uri, "", authority );
		} else {
			newSelection = this.adjustSelection( uri, selection, authority );
		}

		final SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
		final String tableName = this.getTableName( uri, authority );

		sqLiteQueryBuilder.setTables( tableName );
		sqLiteQueryBuilder.setProjectionMap( projectionMap );

		final SQLiteOpenHelper databaseHelper = this.getDatabaseHelper();
		final SQLiteDatabase sqLiteDatabase =
				databaseHelper.getReadableDatabase();
		final Cursor cursor =
				sqLiteQueryBuilder.query( sqLiteDatabase, projection,
						newSelection, selectionArgs, null, null, sortOrder );

		cursor.setNotificationUri( this.getContext().getContentResolver(), uri );

		return cursor;
	}

	@Override
	public int update( final Uri uri, final ContentValues contentValues,
			final String selection, final String[] selectionArgs ) {
		final SQLiteOpenHelper databaseHelper = this.getDatabaseHelper();
		final SQLiteDatabase sqLiteDatabase =
				databaseHelper.getWritableDatabase();
		final String authority = this.getAuthority();
		final String tableName = this.getTableName( uri, authority );
		final int count =
				sqLiteDatabase.update( tableName, contentValues, selection,
						selectionArgs );
		final Context context = this.getContext();
		final ContentResolver contentResolver = context.getContentResolver();

		contentResolver.notifyChange( uri, null );

		final Contract contract = this.getContract( uri );
		final Uri rootUri = contract.getContentUri( authority );

		contentResolver.notifyChange( rootUri, null );

		return count;
	}
}