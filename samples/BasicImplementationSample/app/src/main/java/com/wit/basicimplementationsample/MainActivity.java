package com.wit.basicimplementationsample;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabWidget;

public class MainActivity extends AppCompatActivity {
	private static final class ViewHolder {
		public FrameLayout tabContentFrameLayout;
		public FragmentTabHost fragmentTabHost;
		public TabWidget tabWidget;
	}

	private final ViewHolder viewHolder = new ViewHolder();

	@Override
	protected void onCreate( final Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		this.requestWindowFeature( Window.FEATURE_ACTION_BAR );
		setContentView( R.layout.main );

		this.setupToolbar();
		this.setupViewHolder();
		this.setupTabs();
	}

	private void setupTab( final String tag, final int indicatorLabelResourceId,
			final Class<? extends Fragment> fragmentClass ) {
		final TabHost.TabSpec tabSpec = this.viewHolder.fragmentTabHost.newTabSpec( tag );
		final String indicatorLabel = this.getString( indicatorLabelResourceId );

		tabSpec.setIndicator( indicatorLabel );

		this.viewHolder.fragmentTabHost.addTab( tabSpec, fragmentClass, null );
	}

	private void setupTabs() {
		final FragmentManager supportFragmentManager = this.getSupportFragmentManager();

		this.viewHolder.fragmentTabHost.setup( this, supportFragmentManager,
				android.R.id.tabcontent );

		this.setupTab( "add", R.string.add, AddFragment.class );
		this.setupTab( "get", R.string.get, GetFragment.class );
		this.setupTab( "update", R.string.update, UpdateFragment.class );
		this.setupTab( "delete", R.string.delete, DeleteFragment.class );
	}

	private void setupToolbar() {
		final Toolbar toolbar = (Toolbar) this.findViewById( R.id.toolbar );

		this.setSupportActionBar( toolbar );
	}

	private void setupViewHolder() {
		this.viewHolder.tabContentFrameLayout =
				(FrameLayout) this.findViewById( android.R.id.tabcontent );
		this.viewHolder.fragmentTabHost =
				(FragmentTabHost) this.findViewById( android.R.id.tabhost );
		this.viewHolder.tabWidget =
				(TabWidget) this.findViewById( android.R.id.tabs );
	}
}