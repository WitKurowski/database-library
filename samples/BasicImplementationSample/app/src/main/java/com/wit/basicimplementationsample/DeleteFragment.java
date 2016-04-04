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

public class DeleteFragment extends android.support.v4.app.Fragment {
	private final ViewHolder viewHolder = new ViewHolder();

	@Nullable
	@Override
	public View onCreateView( final LayoutInflater layoutInflater, final ViewGroup container,
			final Bundle savedInstanceState ) {
		final View view = layoutInflater.inflate( R.layout.delete, container, false );

		this.viewHolder.deleteAllButton = (Button) view.findViewById( R.id.delete_all );
		this.viewHolder.deleteButton = (Button) view.findViewById( R.id.delete );
		this.viewHolder.idEditText = (EditText) view.findViewById( R.id.id );

		final DeleteOnClickListener deleteOnClickListener =
				new DeleteOnClickListener( this.viewHolder );

		this.viewHolder.deleteButton.setOnClickListener( deleteOnClickListener );

		final DeleteAllOnClickListener deleteAllOnClickListener = new DeleteAllOnClickListener();

		this.viewHolder.deleteAllButton.setOnClickListener( deleteAllOnClickListener );

		return view;
	}

	private static final class DeleteAllOnClickListener implements View.OnClickListener {
		@Override
		public void onClick( final View view ) {
			final Context context = view.getContext();
			final DeleteAllAsyncTask
					deleteAllAsyncTask = new DeleteAllAsyncTask( context );

			deleteAllAsyncTask.execute();
		}

		private static final class DeleteAllAsyncTask extends AsyncTask<Void, Void, Integer> {
			private final Context context;

			public DeleteAllAsyncTask( final Context context ) {
				this.context = context;
			}

			@Override
			protected Integer doInBackground( final Void... params ) {
				final UserManager userManager = UserManager.getInstance( this.context );
				final int numberOfDeletions = userManager.delete();

				return numberOfDeletions;
			}

			@Override
			protected void onPostExecute( final Integer numberOfDeletions ) {
				super.onPostExecute( numberOfDeletions );

				final String message =
						String.format( "%1$s user(s) successfully deleted.", numberOfDeletions );
				final Toast toast = Toast.makeText( this.context, message, Toast.LENGTH_LONG );

				toast.show();
			}
		}
	}

	private static final class DeleteOnClickListener implements View.OnClickListener {
		private final ViewHolder viewHolder;

		private DeleteOnClickListener( final ViewHolder viewHolder ) {
			this.viewHolder = viewHolder;
		}

		@Override
		public void onClick( final View view ) {
			final String idString = this.viewHolder.idEditText.getText().toString();

			if ( idString.length() > 0 ) {
				final Context context = view.getContext();
				final DeleteAsyncTask
						deleteAsyncTask = new DeleteAsyncTask( context, this.viewHolder );

				deleteAsyncTask.execute();
			} else {
				final Context context = view.getContext();
				final Toast toast =
						Toast.makeText( context, "Please specify a valid ID.", Toast.LENGTH_LONG );

				toast.show();
			}
		}

		private static final class DeleteAsyncTask extends AsyncTask<Void, Void, Integer> {
			private final Context context;
			private final ViewHolder viewHolder;
			private String idString;

			public DeleteAsyncTask( final Context context,
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
			protected Integer doInBackground( final Void... params ) {
				final UserManager userManager = UserManager.getInstance( this.context );
				final long id = Long.parseLong( this.idString );
				final User existingUser = userManager.get( id );
				final int numberOfDeletions;

				if ( existingUser == null ) {
					numberOfDeletions = 0;
				} else {
					numberOfDeletions = userManager.delete( existingUser );
				}

				return numberOfDeletions;
			}

			@Override
			protected void onPostExecute( final Integer numberOfDeletions ) {
				super.onPostExecute( numberOfDeletions );

				final String message;

				if ( numberOfDeletions == 0 ) {
					message = String.format( "No user with ID \"%1$s\" found.", this.idString );
				} else {
					message = String.format( "User with ID \"%1$s\" successfully deleted.",
							this.idString );
				}

				final Toast toast = Toast.makeText( this.context, message, Toast.LENGTH_LONG );

				toast.show();

				this.viewHolder.idEditText.setText( null );
			}
		}
	}

	private static final class ViewHolder {
		public Button deleteAllButton;
		public Button deleteButton;
		public EditText idEditText;
	}
}