package com.wit.basicimplementationsample.contentprovider.contract;

import com.wit.basicimplementationsample.model.User;
import com.wit.databaselibrary.contentprovider.contract.Contract;

public class UserContract extends Contract<User> {
	private static final UserContract USER_CONTRACT = new UserContract();

	public static UserContract getInstance() {
		return UserContract.USER_CONTRACT;
	}

	public UserContract() {
		super( User.class );
	}

	public static final class Columns extends Contract.Columns {
		public static final String TABLE_NAME = "USER";
		public static final String AGE = "age";
		public static final String FIRST_NAME = "first_name";
	}
}