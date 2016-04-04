package com.wit.basicimplementationsample.service;

import android.content.Context;

import com.wit.basicimplementationsample.contentprovider.BasicImplementationSampleContentProvider;
import com.wit.basicimplementationsample.contentprovider.contract.UserContract;
import com.wit.basicimplementationsample.model.User;
import com.wit.databaselibrary.service.Manager;

public class UserManager extends Manager<User> {
	private static UserManager userManager;

	public synchronized static UserManager getInstance( final Context context ) {
		if ( UserManager.userManager == null ) {
			UserManager.userManager = new UserManager( context );
		}

		return UserManager.userManager;
	}

	protected UserManager( final Context context ) {
		super( context, UserContract.getInstance(), User.class );
	}

	@Override
	protected String getAuthority() {
		return BasicImplementationSampleContentProvider.AUTHORITY;
	}
}