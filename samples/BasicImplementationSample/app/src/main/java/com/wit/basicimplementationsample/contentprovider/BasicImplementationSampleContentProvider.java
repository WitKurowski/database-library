package com.wit.basicimplementationsample.contentprovider;

import android.content.Context;

import com.wit.basicimplementationsample.contentprovider.contract.UserContract;
import com.wit.databaselibrary.contentprovider.SimpleContentProvider;
import com.wit.databaselibrary.contentprovider.contract.Contract;

import java.util.HashSet;
import java.util.Set;

public class BasicImplementationSampleContentProvider extends SimpleContentProvider {
	public static final String AUTHORITY =
			BasicImplementationSampleContentProvider.class.getName();
	private static final String DATABASE_NAME = "basic-implementation-sample.db";
	private static final int DATABASE_VERSION = 1;
	private static final Set<Contract> CONTRACTS = new HashSet<>();

	static {
		BasicImplementationSampleContentProvider.CONTRACTS.add( UserContract.getInstance() );
	}

	public BasicImplementationSampleContentProvider() {
		super( BasicImplementationSampleContentProvider.CONTRACTS );
	}

	@Override
	protected SimpleDatabaseHelper createDatabaseHelper() {
		final Context context = this.getContext();
		final SimpleDatabaseHelper simpleDatabaseHelper = new SimpleDatabaseHelper( context,
				BasicImplementationSampleContentProvider.DATABASE_NAME,
				BasicImplementationSampleContentProvider.DATABASE_VERSION,
				BasicImplementationSampleContentProvider.CONTRACTS );

		return simpleDatabaseHelper;
	}

	@Override
	protected String getAuthority() {
		return BasicImplementationSampleContentProvider.AUTHORITY;
	}
}