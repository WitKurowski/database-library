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

public class UpdateFragment extends android.support.v4.app.Fragment {
	private final ViewHolder viewHolder = new ViewHolder();

	@Nullable
	@Override
	public View onCreateView( final LayoutInflater layoutInflater, final ViewGroup container,
			final Bundle savedInstanceState ) {
		final View view = layoutInflater.inflate( R.layout.update, container, false );

		this.viewHolder.ageEditText = (EditText) view.findViewById( R.id.age );
		this.viewHolder.firstNameEditText = (EditText) view.findViewById( R.id.first_name );
		this.viewHolder.idEditText = (EditText) view.findViewById( R.id.id );
		this.viewHolder.updateButton = (Button) view.findViewById( R.id.update );

		final UpdateOnClickListener updateOnClickListener =
				new UpdateOnClickListener( this.viewHolder );

		this.viewHolder.updateButton.setOnClickListener( updateOnClickListener );

		return view;
	}

	private static final class UpdateOnClickListener implements View.OnClickListener {
		private final ViewHolder viewHolder;

		private UpdateOnClickListener( final ViewHolder viewHolder ) {
			this.viewHolder = viewHolder;
		}

		@Override
		public void onClick( final View view ) {
			final String idString = this.viewHolder.idEditText.getText().toString();
			final String ageString = this.viewHolder.ageEditText.getText().toString();
			final String firstName = this.viewHolder.firstNameEditText.getText().toString();

			if ( idString.length() > 0 && ageString.length() > 0 && firstName.length() > 0 ) {
				final Context context = view.getContext();
				final UpdateAsyncTask updateAsyncTask =
						new UpdateAsyncTask( context, this.viewHolder );

				updateAsyncTask.execute();
			} else {
				final Context context = view.getContext();
				final Toast toast =
						Toast.makeText( context, "Please specify a valid ID, age, and first name.",
								Toast.LENGTH_LONG );

				toast.show();
			}
		}

		private static final class UpdateAsyncTask extends AsyncTask<Void, Void, User> {
			private final Context context;
			private final ViewHolder viewHolder;
			private String idString;
			private String ageString;
			private String firstName;

			public UpdateAsyncTask( final Context context,
					final ViewHolder viewHolder ) {
				this.context = context;
				this.viewHolder = viewHolder;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();

				this.idString = this.viewHolder.idEditText.getText().toString();
				this.ageString = this.viewHolder.ageEditText.getText().toString();
				this.firstName = this.viewHolder.firstNameEditText.getText().toString();
			}

			@Override
			protected User doInBackground( final Void... params ) {
				final UserManager userManager = UserManager.getInstance( this.context );
				final long id = Long.parseLong( this.idString );
				final User existingUser = userManager.get( id );
				final User savedUser;

				if ( existingUser == null ) {
					savedUser = null;
				} else {
					final int age = Integer.parseInt( this.ageString );

					existingUser.setAge( age );
					existingUser.setFirstName( this.firstName );

					savedUser = userManager.save( existingUser );
				}

				return savedUser;
			}

			@Override
			protected void onPostExecute( final User user ) {
				super.onPostExecute( user );

				final String message;

				if ( user == null ) {
					message = String.format( "No user with ID \"%1$s\" found.", this.idString );
				} else {
					message = "Updated user: " + user;
				}

				final Toast toast = Toast.makeText( this.context, message, Toast.LENGTH_LONG );

				toast.show();
			}
		}
	}

	private static final class ViewHolder {
		public EditText ageEditText;
		public EditText firstNameEditText;
		public EditText idEditText;
		public Button updateButton;
	}
}