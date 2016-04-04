package com.wit.basicimplementationsample;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wit.basicimplementationsample.model.User;
import com.wit.basicimplementationsample.service.UserManager;

public class AddFragment extends android.support.v4.app.Fragment {
	private final ViewHolder viewHolder = new ViewHolder();

	@Nullable
	@Override
	public View onCreateView( final LayoutInflater layoutInflater, final ViewGroup container,
			final Bundle savedInstanceState ) {
		final View view = layoutInflater.inflate( R.layout.add, container, false );

		this.viewHolder.addButton = (Button) view.findViewById( R.id.add );
		this.viewHolder.ageEditText = (EditText) view.findViewById( R.id.age );
		this.viewHolder.firstNameEditText = (EditText) view.findViewById( R.id.first_name );

		final AddOnClickListener addOnClickListener = new AddOnClickListener( this.viewHolder );

		this.viewHolder.addButton.setOnClickListener( addOnClickListener );

		return view;
	}

	private static final class AddOnClickListener implements View.OnClickListener {
		private final ViewHolder viewHolder;

		private AddOnClickListener( final ViewHolder viewHolder ) {
			this.viewHolder = viewHolder;
		}

		@Override
		public void onClick( final View view ) {
			final String ageString = this.viewHolder.ageEditText.getText().toString();
			final String firstName = this.viewHolder.firstNameEditText.getText().toString();

			if ( ageString.length() > 0 && firstName.length() > 0 ) {
				final Context context = view.getContext();
				final int age = Integer.parseInt( ageString );
				final User newUser = new User( age, firstName );
				final AddAsyncTask addAsyncTask =
						new AddAsyncTask( context, newUser, this.viewHolder );

				addAsyncTask.execute();
			} else {
				final Context context = view.getContext();
				final Toast toast =
						Toast.makeText( context, "Please specify a valid age and first name.",
								Toast.LENGTH_LONG );

				toast.show();
			}
		}

		private static final class AddAsyncTask extends AsyncTask<Void, Void, User> {
			private final Context context;
			private final User newUser;
			private final ViewHolder viewHolder;

			public AddAsyncTask( final Context context, final User newUser,
					final ViewHolder viewHolder ) {
				this.context = context;
				this.newUser = newUser;
				this.viewHolder = viewHolder;
			}

			@Override
			protected User doInBackground( final Void... params ) {
				final UserManager userManager = UserManager.getInstance( this.context );
				final User savedUser = userManager.save( this.newUser );

				return savedUser;
			}

			@Override
			protected void onPostExecute( final User user ) {
				super.onPostExecute( user );

				final Toast toast =
						Toast.makeText( this.context, "Created user: " + user, Toast.LENGTH_LONG );

				toast.show();

				this.viewHolder.ageEditText.setText( null );
				this.viewHolder.firstNameEditText.setText( null );
			}
		}
	}

	private static final class ViewHolder {
		public Button addButton;
		public EditText ageEditText;
		public EditText firstNameEditText;
	}
}