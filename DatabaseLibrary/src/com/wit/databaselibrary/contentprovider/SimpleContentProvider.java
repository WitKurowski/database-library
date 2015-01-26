package com.wit.databaselibrary.contentprovider;

import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.wit.databaselibrary.contentprovider.contract.Contract;

public abstract class SimpleContentProvider extends ContentProvider {
	private final List<Contract> contracts;

	public SimpleContentProvider( final List<Contract> contracts ) {
		this.contracts = contracts;

		for ( final Contract contract : contracts ) {
			contract.setupUriMatcher();
			contract.setupColumnNames();
			contract.setupProjectionMap();
		}
	}

	private String adjustSelection( final Uri uri, String selection ) {
		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatchesObjectId( uri ) ) {
				selection = contract.addSelectionById( uri, selection );

				break;
			}
		}

		return selection;
	}

	@Override
	public int delete( final Uri uri, final String selection,
			final String[] selectionArgs ) {
		final String tableName = this.getTableName( uri );
		final String newSelection = this.adjustSelection( uri, selection );
		final SQLiteOpenHelper databaseHelper = this.getDatabaseHelper();
		final SQLiteDatabase sqLiteDatabase =
				databaseHelper.getWritableDatabase();
		final int count =
				sqLiteDatabase.delete( tableName, newSelection, selectionArgs );

		this.getContext().getContentResolver().notifyChange( uri, null );

		return count;
	}

	protected abstract SQLiteOpenHelper getDatabaseHelper();

	protected abstract String getDatabaseName();

	protected abstract int getDatabaseVersion();

	private String getTableName( final Uri uri ) {
		String tableName = null;

		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatches( uri ) ) {
				tableName = contract.getTableName();

				break;
			}
		}

		if ( tableName == null ) {
			throw new IllegalArgumentException( "Unknown URI: " + uri );
		}

		return tableName;
	}

	@Override
	public String getType( final Uri uri ) {
		Contract contract = null;

		for ( final Contract currentContract : this.contracts ) {
			if ( contract.uriMatchesObject( uri ) ) {
				contract = currentContract;
			}
		}

		if ( contract == null ) {
			throw new IllegalArgumentException( "Unknown URI: " + uri );
		}

		final String contentType = contract.getContentType();

		return contentType;
	}

	@Override
	public Uri insert( final Uri uri, ContentValues contentValues ) {
		Contract contract = null;

		for ( final Contract currentContract : this.contracts ) {
			if ( currentContract.uriMatchesObject( uri ) ) {
				contract = currentContract;
			}
		}

		if ( contract == null ) {
			throw new IllegalArgumentException( "Unknown URI: " + uri );
		}

		final SQLiteOpenHelper databaseHelper = this.getDatabaseHelper();
		final SQLiteDatabase sqLiteDatabase =
				databaseHelper.getWritableDatabase();

		if ( contentValues == null ) {
			contentValues = new ContentValues();
		}

		final String tableName = contract.getTableName();
		final Uri contentUri = contract.getContentUri();
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
					ContentUris.withAppendedId( contentUri, rowId );

			this.getContext().getContentResolver().notifyChange(
					contentUriWithAppendedId, null );

			return contentUriWithAppendedId;
		} else {
			throw new SQLException( "Failed to insert row into " + uri );
		}
	}

	@Override
	public Cursor query( final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder ) {
		Map<String, String> projectionMap = null;

		for ( final Contract contract : this.contracts ) {
			if ( contract.uriMatches( uri ) ) {
				projectionMap = contract.getProjectionMap();
			}
		}

		if ( projectionMap == null ) {
			throw new IllegalArgumentException( "Unknown URI: " + uri );
		}

		String newSelection;

		if ( selection == null ) {
			newSelection = this.adjustSelection( uri, "" );
		} else {
			newSelection = this.adjustSelection( uri, selection );
		}

		final SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
		final String tableName = this.getTableName( uri );

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
		final String tableName = this.getTableName( uri );

		final int count =
				sqLiteDatabase.update( tableName, contentValues, selection,
						selectionArgs );

		this.getContext().getContentResolver().notifyChange( uri, null );

		return count;
	}
}