package com.dgsd.android.MangaReader;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dgsd.android.MangaReader.Data.DbField;
import com.dgsd.android.MangaReader.Data.MangaProvider;
import com.dgsd.android.MangaReader.Fragment.SeriesInfoFragment;
import com.dgsd.android.MangaReader.Model.MangaSeries;

public class SeriesInfoActivity extends SherlockFragmentActivity implements SeriesInfoFragment.OnSeriesInfoLoadedListener {
    private static final String KEY_FRAGMENT = "_key_fragment";
    public static final String EXTRA_TITLE = "com.dgsd.android.MangaReader.SeriesInfoActivity._extra_title";

    private FragmentManager mFragmentManager;
    private SeriesInfoFragment mFragment;
    private String mSeriesId;

    private boolean mIsFavourite = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        // Set up the action bar
        final ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        final String title = getIntent().getStringExtra(EXTRA_TITLE);
        if(!TextUtils.isEmpty(title))
            ab.setTitle(title);

        mFragmentManager = getSupportFragmentManager();
        mSeriesId = getIntent().getStringExtra(SeriesInfoFragment.EXTRA_SERIES_ID);

        // Get our fragment instances
        if (savedInstanceState != null) {
            mFragment = (SeriesInfoFragment) mFragmentManager.getFragment(savedInstanceState, KEY_FRAGMENT);
        }

        if (mFragment == null) {
            mFragment = SeriesInfoFragment.newInstance(mSeriesId);
            mFragmentManager.beginTransaction().replace(R.id.container, mFragment).commit();
        }

        mFragment.setOnSeriesInfoLoaded(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFragment != null && mFragment.isAdded())
            mFragmentManager.putFragment(outState, KEY_FRAGMENT, mFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.series_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
         if(mIsFavourite)
            menu.findItem(R.id.favourite).setIcon(R.drawable.ic_action_star_gold);
        else
            menu.findItem(R.id.favourite).setIcon(R.drawable.ic_action_star);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, MangaApp.getHomeClass(this));
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this).addNextIntent(upIntent).startActivities();
                    finish();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }

                return true;
            case R.id.favourite:
                new ToggleFavouriteAsyncTask(getContentResolver(), !mIsFavourite).execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSeriesInfoLoaded(MangaSeries series) {
        if(series == null)
            return;

        mIsFavourite = series.isFavourite;
        invalidateOptionsMenu();
    }

    private class ToggleFavouriteAsyncTask extends AsyncTask<Void, Void, Void> {
        private ContentResolver mResolver;
        private boolean mIsFavourite;

        public ToggleFavouriteAsyncTask(ContentResolver resolver, boolean isFavourite) {
            super();
            mResolver = resolver;
            mIsFavourite = isFavourite;
        }

        @Override
        protected Void doInBackground(Void... args) {
            final ContentValues values = new ContentValues();
            values.put(DbField.FAVOURITE.name, mIsFavourite ? 1 : 0);
            values.put(DbField.SERIES_ID.name, mSeriesId);

            mResolver.insert(MangaProvider.SERIES_FAVOURITE_URI, values);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SeriesInfoActivity.this.mIsFavourite = mIsFavourite;
            invalidateOptionsMenu();
        }
    }
}
