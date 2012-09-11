package com.dgsd.android.MangaReader.Fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.dgsd.android.MangaReader.Data.DbField;
import com.dgsd.android.MangaReader.Data.DbTable;
import com.dgsd.android.MangaReader.Data.MangaProvider;
import com.dgsd.android.MangaReader.R;
import com.dgsd.android.MangaReader.SeriesInfoActivity;
import com.dgsd.android.MangaReader.Util.Anim;
import com.handlerexploit.prime.widgets.RemoteImageView;

import static android.widget.AbsListView.OnScrollListener;

public class SeriesListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {
    private static final String TAG = SeriesListFragment.class.getSimpleName();

    public static final String KEY_FAVOURITES_ONLY = "_key_favourites_only";

    public static final int CURSOR_COL_TITLE = 1;
    public static final int CURSOR_COL_SERIES_ID = 2;
    public static final int CURSOR_COL_IMAGE = 3;
    public static final int CURSOR_COL_FAVOURITE = 4;

    private static final String[] PROJECTION = {
            DbTable.SERIES + "." + DbField.ID.name,
            DbTable.SERIES + "." + DbField.TITLE.name,
            DbTable.SERIES + "." + DbField.SERIES_ID.name,
            DbTable.SERIES + "." + DbField.IMAGE.name,
            DbTable.SERIES_FAVOURITES + "." + DbField.FAVOURITE.name
    };

    private boolean mGridIsBusy = false;
    private GridView mGrid;
    private SeriesListAdapter mAdapter;
    private View mEmptyView;
    private boolean mShowFavouritesOnly = false;

    private String mLastFilterConstraint;

    private AdapterView.OnItemLongClickListener mOnItemLongClickListener;

    public static final SeriesListFragment newInstance(boolean favouritesOnly) {
        SeriesListFragment frag = new SeriesListFragment();

        Bundle args = new Bundle();
        args.putBoolean(KEY_FAVOURITES_ONLY, favouritesOnly);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShowFavouritesOnly = getArguments() == null ? false : getArguments().getBoolean(KEY_FAVOURITES_ONLY, false);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_series_list, parent, false);

        mGrid = (GridView) v.findViewById(R.id.grid);
        mEmptyView = v.findViewById(android.R.id.empty);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new SeriesListAdapter();

        mGrid.setAdapter(mAdapter);
        mGrid.setLayoutAnimation(Anim.getListViewDealAnimator());
        mGrid.setOnItemClickListener(this);
        mGrid.setOnItemLongClickListener(mOnItemLongClickListener);
        mGrid.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    case OnScrollListener.SCROLL_STATE_IDLE:
                        mGridIsBusy = false;

                        final int count = view.getChildCount();
                        for (int i = 0; i < count; i++) {
                            ViewHolder holder = (ViewHolder) view.getChildAt(i).getTag();
                            if(holder == null)
                                return;

                            if (holder.image.getTag() != null) {
                                holder.image.setImageURL((String) holder.image.getTag());
                                holder.image.setTag(null);
                            }
                        }

                        break;
                    case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        mGridIsBusy = true;
                        break;
                    case OnScrollListener.SCROLL_STATE_FLING:
                        mGridIsBusy = true;
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        final String sel;
        if(mShowFavouritesOnly && !TextUtils.isEmpty(mLastFilterConstraint))
            sel = DbField.TITLE + " LIKE '%" + mLastFilterConstraint + "%' AND " + DbField.FAVOURITE + "=1";
        else if(mShowFavouritesOnly)
            sel = DbField.FAVOURITE + "=1";
        else if(!TextUtils.isEmpty(mLastFilterConstraint))
            sel = DbField.TITLE + " LIKE '%" + mLastFilterConstraint + "%'";
        else
            sel = null;

        return new CursorLoader(getActivity(), MangaProvider.SERIES_LIST_URI, PROJECTION, sel, null, DbField.TITLE + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        if(holder != null) {
            final Intent intent = new Intent(getActivity(), SeriesInfoActivity.class);
            intent.putExtra(SeriesInfoActivity.EXTRA_TITLE, holder.text.getText());
            intent.putExtra(SeriesInfoFragment.EXTRA_SERIES_ID, holder.seriesId);

            startActivity(intent);
        }
    }

    public void filter(String filter) {
        mLastFilterConstraint = filter;
        getLoaderManager().restartLoader(0, null, this).forceLoad();
    }

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
        this.mOnItemLongClickListener = listener;
    }

    public void reload() {
        getLoaderManager().restartLoader(0, null, this).forceLoad();
    }

    public void setShowFavouritesOnly(boolean favouritesOnly) {
        mShowFavouritesOnly = favouritesOnly;
        getLoaderManager().restartLoader(0, null, this).forceLoad();
    }

    private class SeriesListAdapter extends SimpleCursorAdapter implements SectionIndexer, SimpleCursorAdapter.ViewBinder {

        private AlphabetIndexer mIndexer;

        public SeriesListAdapter() {
            super(getActivity(), R.layout.grid_item_series_list, null,
                    new String[]{DbField.ID.name}, new int[]{R.id.container}, 0);
            setViewBinder(this);
            mIndexer = new AlphabetIndexer(null, CURSOR_COL_TITLE, " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        @Override
        public Cursor swapCursor(Cursor cursor) {
            mIndexer.setCursor(cursor);
            return super.swapCursor(cursor);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            mIndexer.setCursor(cursor);
            super.changeCursor(cursor);
        }

        @Override
        public Object[] getSections() {
            return mIndexer.getSections();
        }

        @Override
        public int getPositionForSection(int pos) {
            return mIndexer.getPositionForSection(pos);
        }

        @Override
        public int getSectionForPosition(int pos) {
            return mIndexer.getSectionForPosition(pos);
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int col) {
            ViewHolder holder = (ViewHolder) view.getTag();
            if(holder == null)
                holder = new ViewHolder(view);

            holder.seriesId = cursor.getString(CURSOR_COL_SERIES_ID);

            holder.text.setText(cursor.getString(CURSOR_COL_TITLE));
            holder.image.setImageDrawable(null);

            if(cursor.getInt(CURSOR_COL_FAVOURITE) == 1)
                holder.favourite.setVisibility(View.VISIBLE);
            else
                holder.favourite.setVisibility(View.GONE);

            final String imageUrl = cursor.getString(CURSOR_COL_IMAGE);
            if(!mGridIsBusy)
                holder.image.setImageURL(imageUrl);
            else
                holder.image.setTag(imageUrl);

            return true;
        }
    }

    public static class ViewHolder {
        public RemoteImageView image;
        public TextView text;
        public ImageView favourite;
        public String seriesId;

        public ViewHolder(View v) {
            text = (TextView) v.findViewById(R.id.text);
            image = (RemoteImageView) v.findViewById(R.id.image);
            favourite = (ImageView) v.findViewById(R.id.fav_icon);

            v.setTag(this);
        }
    }
}
