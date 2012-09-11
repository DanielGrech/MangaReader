package com.dgsd.android.MangaReader;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.dgsd.android.MangaReader.Data.DbField;
import com.dgsd.android.MangaReader.Data.MangaProvider;
import com.dgsd.android.MangaReader.Fragment.PageFragment;
import com.dgsd.android.MangaReader.Model.MangaPage;
import com.dgsd.android.MangaReader.Receiver.PortableReceiver;
import com.dgsd.android.MangaReader.Receiver.Receiver;
import com.dgsd.android.MangaReader.Service.ApiService;
import com.dgsd.android.MangaReader.Util.Anim;
import com.dgsd.android.MangaReader.View.PhotoView.PageView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;

public class ChapterActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, Receiver {
    private static final String TAG = ChapterActivity.class.getSimpleName();

    public static final String EXTRA_TITLE = "com.dgsd.android.MangaReader.SeriesInfoActivity._extra_title";
    public static final String EXTRA_SERIES_TITLE = "com.dgsd.android.MangaReader.SeriesInfoActivity._extra_series_title";
    public static final String EXTRA_CHAPTER_ID = "com.dgsd.android.MangaReader.SeriesInfoActivity._extra_chapter_id";

    private static final String KEY_LOADING_COUNTER = "_loading_counter";

    private PageAdapter mAdapter;
    private ViewPager mPager;

    private ShareActionProvider mShareActionProvider;

    private String mChapterId;

    protected PortableReceiver mReceiver;
    protected int mLoadingCounter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        } catch(Exception e) {
            //This may throw an exception on some phones when resuming the app..
            if(BuildConfig.DEBUG) {
                Log.e(TAG, "Error requesting Window.FEATURE_INDETERMINATE_PROGRESS", e);
            }
        }

        setContentView(R.layout.activity_chapter);

        if (savedInstanceState != null) {
            mLoadingCounter = savedInstanceState.getInt(KEY_LOADING_COUNTER, 0);

            if(mLoadingCounter > 0) {
                setSupportProgressBarIndeterminateVisibility(true);
            }
        }

        mReceiver = new PortableReceiver();
        mReceiver.setReceiver(this);

        mChapterId = getIntent().getStringExtra(EXTRA_CHAPTER_ID);

        // Set up the action bar
        final ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        final String title = getIntent().getStringExtra(EXTRA_TITLE);
        final String subtitle = getIntent().getStringExtra(EXTRA_SERIES_TITLE);
        if(!TextUtils.isEmpty(title))
            ab.setTitle(title);

        if(!TextUtils.isEmpty(subtitle))
            ab.setSubtitle(subtitle);

        mAdapter = new PageAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(20);
        mPager.setOffscreenPageLimit(2);
        mPager.setLayoutAnimation(Anim.getListViewDealAnimator());
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int pos) {
                setShareActionIntent();
            }

            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void setShareActionIntent() {
        MangaPage page = mAdapter.getPageAt(mPager.getCurrentItem());
        if(mShareActionProvider !=  null && page != null && !TextUtils.isEmpty(page.url)) {
            final String chapterTitle = getIntent().getStringExtra(EXTRA_TITLE);
            final String seriesTitle = getIntent().getStringExtra(EXTRA_SERIES_TITLE);

            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, chapterTitle + " - " + seriesTitle);
            intent.putExtra(android.content.Intent.EXTRA_TEXT, page.url);

            mShareActionProvider.setShareIntent(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.page, menu);
        mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.share).getActionProvider();
        mShareActionProvider.setShareHistoryFileName(null);
        return super.onCreateOptionsMenu(menu);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        final Uri uri = Uri.withAppendedPath(MangaProvider.CHAPTER_URI, mChapterId);
        final String[] proj = {DbField.ID.name, DbField.PAGE_NUMBER.name, DbField.IMAGE.name};

        return new CursorLoader(this, uri, proj, null, null, DbField.PAGE_NUMBER + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.clear();
        if(cursor != null && cursor.moveToFirst()) {
            mAdapter.startUpdate(mPager);

            final int urlCol = cursor.getColumnIndex(DbField.IMAGE.name);
            final int pageNumCol = cursor.getColumnIndex(DbField.PAGE_NUMBER.name);
            do {
                MangaPage page = new MangaPage();
                page.url = cursor.getString(urlCol);
                page.pageNum = cursor.getInt(pageNumCol);
                mAdapter.add(page);
            } while(cursor.moveToNext());

            mAdapter.notifyDataSetChanged();
            setShareActionIntent();
        } else {
            showProgressBar();
            ApiService.requestPages(this, mChapterId);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.clear();
    }


    @Override
    public void onResume() {
        super.onResume();

        // Register the receiver
        registerReceiver(mReceiver, new IntentFilter(ApiService.ACTION_RESULT_READY), null, null);
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final int resultType = intent.getIntExtra(ApiService.EXTRA_RESULT_TYPE, -1);
//        final int requestType = intent.getIntExtra(ApiService.EXTRA_REQUEST_TYPE, -1);
        if(resultType == ApiService.RESULT_SUCCESS)
            getSupportLoaderManager().restartLoader(0, null, this);


        hideProgressBar();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_LOADING_COUNTER, mLoadingCounter);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mLoadingCounter > 0)
            setSupportProgressBarIndeterminateVisibility(true);
        else
            setSupportProgressBarIndeterminateVisibility(false);
    }

    public void showProgressBar() {
        mLoadingCounter++;
        setSupportProgressBarIndeterminateVisibility(true);
    }

    public void hideProgressBar() {
        mLoadingCounter--;

        if (mLoadingCounter < 0) {
            mLoadingCounter = 0;
        }

        // Check if we are waiting for any other progressable items.
        if (mLoadingCounter == 0) {
            setSupportProgressBarIndeterminateVisibility(false);
        }
    }

    private class PageAdapter extends FragmentPagerAdapter implements PageView.OnPhotoTapListener {
        private List<MangaPage> mPages;

        public PageAdapter(FragmentManager fm) {
            super(fm);
        }

        public MangaPage getPageAt(int pos) {
            if(mPages == null || pos < 0 || pos >= mPages.size())
                return null;
            else
                return mPages.get(pos);
        }

        @Override
        public Fragment getItem(int pos) {
            PageFragment frag = PageFragment.newInstance(mPages.get(pos));
            frag.setOnPhotoTapListener(this);
            return frag;
        }

        @Override
        public int getCount() {
            return mPages == null ? 0 : mPages.size();
        }

        public void add(MangaPage page) {
            if(mPages == null)
                mPages = new ArrayList<MangaPage>();

            mPages.add(page);
        }

        public void clear() {
            if(mPages != null) {
                mPages.clear();
                this.notifyDataSetChanged();
            }
        }

        @Override
        public void onPhotoTap(View view, float x, float y) {
            final ActionBar ab = getSupportActionBar();
            if(ab.isShowing())
                ab.hide();
            else
                ab.show();
        }
    }
}
