package com.dgsd.android.MangaReader;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dgsd.android.MangaReader.Data.DbField;
import com.dgsd.android.MangaReader.Data.MangaProvider;
import com.dgsd.android.MangaReader.Fragment.SeriesInfoFragment;
import com.dgsd.android.MangaReader.Fragment.SeriesListFragment;

public class MainActivity extends BaseMainActivity implements AdapterView.OnItemLongClickListener, ActionBar.OnNavigationListener {
    private static final String KEY_FRAGMENT = "_key_fragment";
    private static final String KEY_NAV_ITEM = "_key_nav_item";

    private static final int NAV_ITEM_ALL = 0;
    private static final int NAV_ITEM_FAV = 1;

    private ActionMode mLongPressActionMode;

    private FragmentManager mFragmentManager;
    private SeriesListFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        // Set up the action bar
        final ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setHomeButtonEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ab.setListNavigationCallbacks(new ArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item,
                getResources().getStringArray(R.array.main_nav_items)), this);

        mFragmentManager = getSupportFragmentManager();

        // Get our fragment instances
        if (savedInstanceState != null) {
            ab.setSelectedNavigationItem(savedInstanceState.getInt(KEY_NAV_ITEM, 0));
            mFragment = (SeriesListFragment) mFragmentManager.getFragment(savedInstanceState, KEY_FRAGMENT);
        }

        if (mFragment == null) {
            mFragment = SeriesListFragment.newInstance(false);
            mFragmentManager.beginTransaction().replace(R.id.container, mFragment).commit();
        }

        mFragment.setOnItemLongClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLongPressActionMode != null)
            mLongPressActionMode.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_NAV_ITEM, getSupportActionBar().getSelectedNavigationIndex());
        if (mFragment != null && mFragment.isAdded())
            mFragmentManager.putFragment(outState, KEY_FRAGMENT, mFragment);
    }

    @Override
    protected void onSearchItemPressed(String seriesId, String name) {
        final Intent intent = new Intent(this, SeriesInfoActivity.class);
        intent.putExtra(SeriesInfoActivity.EXTRA_TITLE, name);
        intent.putExtra(SeriesInfoFragment.EXTRA_SERIES_ID, seriesId);

        startActivity(intent);
    }

    @Override
    protected void onDoSearch(String searchTerm) {
        onFilter(searchTerm);
    }

    @Override
    protected void onFilter(String filter) {
        mFragment.filter(filter);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long id) {
        SeriesListFragment.ViewHolder holder = (SeriesListFragment.ViewHolder) view.getTag();
        if(holder != null && !TextUtils.isEmpty(holder.seriesId)) {
            if (mLongPressActionMode != null) {
                mLongPressActionMode.finish();
            }

            mLongPressActionMode = startActionMode(new LongPressActionMode(holder.text.getText(), holder.seriesId,
                    holder.favourite.getVisibility() == View.VISIBLE));
        }

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(int pos, long itemId) {
        mFragment.setShowFavouritesOnly(pos == NAV_ITEM_FAV);
        return true;
    }

    private final class LongPressActionMode implements ActionMode.Callback {
        private CharSequence mTitle;
        private CharSequence mSeriesId;
        private boolean mCurrentFavouriteState;

        public LongPressActionMode(CharSequence title, CharSequence seriesId, boolean favourite) {
            mTitle = title;
            mSeriesId = seriesId;
            mCurrentFavouriteState = favourite;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSupportMenuInflater().inflate(R.menu.series_list_contextual, menu);

            if(mCurrentFavouriteState)
                menu.findItem(R.id.favourite).setIcon(R.drawable.ic_action_star_gold);
            else
                menu.findItem(R.id.favourite).setIcon(R.drawable.ic_action_star);

            mode.setTitle(mTitle);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();

            switch (id) {
                case R.id.favourite:
                    new ToggleFavouriteAsyncTask(mSeriesId, !mCurrentFavouriteState).execute();
                    mFragment.reload();
                    if(mLongPressActionMode != null)
                        mLongPressActionMode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }

    private class ToggleFavouriteAsyncTask extends AsyncTask<Void, Void, Void> {
        private boolean mIsFavourite;
        private CharSequence mSeriesId;

        public ToggleFavouriteAsyncTask(CharSequence seriesId, boolean isFavourite) {
            super();
            mSeriesId = seriesId;
            mIsFavourite = isFavourite;
        }

        @Override
        protected Void doInBackground(Void... args) {
            final ContentValues values = new ContentValues();
            values.put(DbField.FAVOURITE.name, mIsFavourite ? 1 : 0);
            values.put(DbField.SERIES_ID.name, mSeriesId == null ? null : mSeriesId.toString());

            getContentResolver().insert(MangaProvider.SERIES_FAVOURITE_URI, values);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }

}
