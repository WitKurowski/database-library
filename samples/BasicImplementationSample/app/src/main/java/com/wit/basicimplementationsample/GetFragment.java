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

import java.util.List;

public class GetFragment extends android.support.v4.app.Fragment {
	private final ViewHolder viewHolder = new ViewHolder();

	@Nullable
	@Override
	public View onCreateView( final LayoutInflater layoutInflater, final ViewGroup container,
			final Bundle savedInstanceState ) {
		final View view = layoutInflater.inflate( R.layout.get, container, false );

		this.viewHolder.getButton = (Button) view.findViewById( R.id.get );
		this.viewHolder.getAllButton = (Button) view.findViewById( R.id.get_all );
		this.viewHolder.idEditText = (EditText) view.findViewById( R.id.id );

		final GetOnClickListener getOnClickListener = new GetOnClickListener( this.viewHolder );

		this.viewHolder.getButton.setOnClickListener( getOnClickListener );

		final GetAllOnClickListener getAllOnClickListener = new GetAllOnClickListener();

		this.viewHolder.getAllButton.setOnClickListener( getAllOnClickListener );

		return view;
	}

	private static final class GetAllOnClickListener implements View.OnClickListener {
		@Override
		public void onClick( final View view ) {
			final Context context = view.getContext();
			final GetAllAsyncTask getAllAsyncTask = new GetAllAsyncTask( context );

			getAllAsyncTask.execute();
		}

		private static final class GetAllAsyncTask extends AsyncTask<Void, Void, List<User>> {
			private final Context context;

			public GetAllAsyncTask( final Context context ) {
				this.context = context;
			}

			@Override
			protected List<User> doInBackground( final Void... params ) {
				final UserManager userManager = UserManager.getInstance( this.context );
				final List<User> existingUsers = userManager.get();

				return existingUsers;
			}

			@Override
			protected void onPostExecute( final List<User> users ) {
				super.onPostExecute( users );

				final String message;

				if ( users.isEmpty() ) {
					message = "No users found.";
				} else {
					message = String.format( "Retrieved %1$d user(s): %2$s", users.size(), users );
				}

				final Toast toast = Toast.makeText( this.context, message, Toast.LENGTH_LONG );

				toast.show();
			}
		}
	}

	private static final class GetOnClickListener implements View.OnClickListener {
		private final ViewHolder viewHolder;

		private GetOnClickListener( final ViewHolder viewHolder ) {
			this.viewHolder = viewHolder;
		}

		@Override
		public void onClick( final View view ) {
			final String idString = this.viewHolder.idEditText.getText().toString();

			if ( idString.length() > 0 ) {
				final Context context = view.getContext();
				final GetAsyncTask getAsyncTask = new GetAsyncTask( context, this.viewHolder );

				getAsyncTask.execute();
			} else {
				final Context context = view.getContext();
				final Toast toast =
						Toast.makeText( context, "Please specify a valid ID.", Toast.LENGTH_LONG );

				toast.show();
			}
		}

		private static final class GetAsyncTask extends AsyncTask<Void, Void, User> {
			private final Context context;
			private final ViewHolder viewHolder;
			private String idString;

			public GetAsyncTask( final Context context,
					final ViewHolder viewHolder ) {
				this.context = context;
				this.viewHolder = viewHolder;
			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();

				this.idString = this.viewHolder.idEditText.getText().toString();
			}

			@Override
			protected User doInBackground( final Void... params ) {
				final UserManager userManager = UserManager.getInstance( this.context );
				final long id = Long.parseLong( this.idString );
				final User existingUser = userManager.get( id );

				return existingUser;
			}

			@Override
			protected void onPostExecute( final User user ) {
				super.onPostExecute( user );

				final String message;

				if ( user == null ) {
					message = String.format( "No user with ID \"%1$s\" found.", this.idString );
				} else {
					message = "Retrieved user: " + user;
				}

				final Toast toast = Toast.makeText( this.context, message, Toast.LENGTH_LONG );

				toast.show();
			}
		}
	}

	private static final class ViewHolder {
		public Button getAllButton;
		public Button getButton;
		public EditText idEditText;
	}
}