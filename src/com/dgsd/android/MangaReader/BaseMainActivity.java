package com.dgsd.android.MangaReader;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dgsd.android.MangaReader.Util.Api;
import com.dgsd.android.MangaReader.Util.Diagnostics;

import java.net.URLDecoder;

public abstract class BaseMainActivity extends SherlockFragmentActivity {
    protected String mLastSearchQuery;

    protected abstract void onSearchItemPressed(String seriesId, String name);
    protected abstract void onDoSearch(String searchTerm);
    protected abstract void onFilter(String filter);

    protected MenuItem mSearchItem;

    @Override
    public boolean onSearchRequested() {
        if(Api.isMin(Api.HONEYCOMB)) {
            mSearchItem.expandActionView();
            ((SearchView) mSearchItem.getActionView()).setQuery(mLastSearchQuery, false);
            return false;
        } else {
            if(!TextUtils.isEmpty(mLastSearchQuery)) {
                startSearch(mLastSearchQuery, true, null, false);
                onFilter(null);
                return false;
            } else {
                return super.onSearchRequested();
            }
        }
    }

    @Override
    public void onNewIntent(Intent inIntent) {
        setIntent(inIntent);
        if(!Intent.ACTION_SEARCH.equals(inIntent.getAction()))
            return;

        //Make sure the keyboard is hidden
        Diagnostics.hideKeyboard(this);

        String query = inIntent.getStringExtra(SearchManager.QUERY);
        if(query != null) {
            query = URLDecoder.decode(query).replaceAll("\n", " ");
        } else {
            return;
        }

        //Save our query string so we can restore it next time the user searches
        mLastSearchQuery = query;

        String extra = inIntent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
        boolean handled = false;
        if(!TextUtils.isEmpty(extra)) {
            String[] items = extra.split(",");
            if(items != null && items.length == 2) {
                try {
                    onSearchItemPressed(items[0], items[1]);
                    handled = true;
                } catch(Exception e) {
                    //o well..
                }
            }
        }

        if(!handled)
            onDoSearch(mLastSearchQuery);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main, menu);

        if(Api.isMin(Api.HONEYCOMB)) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = new SearchView(this);

            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            searchView.setSearchableInfo(info);
            searchView.setIconifiedByDefault(true);

            int id = getResources().getIdentifier("android:id/search_src_text", null, null);
            if(id != 0) {
                View v = searchView.findViewById(id);
                if(v instanceof TextView)
                    ((TextView) v).setTextColor(Color.WHITE);
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String filter) {
                    mLastSearchQuery = filter;
                    onFilter(filter);
                    return false;
                }
            });

            mSearchItem = menu.findItem(R.id.search);
            mSearchItem.setActionView(searchView);
            mSearchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_ALWAYS);
            mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    final ActionBar ab = getSupportActionBar();
                    if(TextUtils.isEmpty(mLastSearchQuery))
                        ab.setSubtitle(null);
                    else
                        ab.setSubtitle("Search for: '" + mLastSearchQuery + "'");
                    return true;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.about:
//                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.search:
                onSearchRequested();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
