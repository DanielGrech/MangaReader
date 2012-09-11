package com.dgsd.android.MangaReader.Fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.dgsd.android.MangaReader.ChapterActivity;
import com.dgsd.android.MangaReader.Data.DbField;
import com.dgsd.android.MangaReader.Data.DbTable;
import com.dgsd.android.MangaReader.Data.MangaProvider;
import com.dgsd.android.MangaReader.Model.MangaSeries;
import com.dgsd.android.MangaReader.R;
import com.dgsd.android.MangaReader.Service.ApiService;
import com.dgsd.android.MangaReader.Util.Anim;
import com.dgsd.android.MangaReader.Util.TimeUtil;
import com.handlerexploit.prime.widgets.RemoteImageView;
import com.nineoldandroids.animation.Animator;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class SeriesInfoFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, SimpleCursorAdapter.ViewBinder {
    private static final String TAG = SeriesInfoFragment.class.getSimpleName();

    public static final String EXTRA_SERIES_ID = "com.dgsd.android.MangaReader.Fragment.SeriesInfoFragment._extra_series_id";

    public static final int LOADER_ID_SERIES_INFO = 0x0;
    public static final int LOADER_ID_CHAPTERS = 0x1;
    public static final int LOADER_ID_CATEGORIES = 0x2;

    private static final int SERIES_INFO_COL_TITLE = 1;
    private static final int SERIES_INFO_COL_SERIES_ID = 2;
    private static final int SERIES_INFO_COL_IMAGE = 3;
    private static final int SERIES_INFO_COL_DESCRIPTION = 4;
    private static final int SERIES_INFO_COL_AUTHOR = 5;
    private static final int SERIES_INFO_COL_LAST_CHAPTER_DATE = 6;
    private static final int SERIES_INFO_COL_CREATED_DATE = 7;
    private static final int SERIES_INFO_COL_FAVOURITE = 8;

    private static final int CHAPTER_COL_CHAPTER_ID = 2;
    private static final int CHAPTER_COL_TITLE = 3;
    private static final int CHAPTER_COL_SEQUENCE_NUM = 4;
    private static final int CHAPTER_COL_RELEASE_DATE = 5;

    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();

    private boolean mGridIsBusy = false;
    private SimpleCursorAdapter mAdapter;
    private ListView mList;
    private View mEmptyView;
    private RemoteImageView mImage;
    private TextView mAuthor;
    private TextView mCreated;
    private TextView mLastChapter;
    private TextView mDescription;
    private TextView mChaptersHeading;
    private ViewGroup mLoadingView;

    private OnSeriesInfoLoadedListener mOnSeriesInfoLoadedListener;

    private String mSeriesId;
    private MangaSeries mMangaSeries;
    private Time mTime = new Time();
    private StringBuilder mStringBuilder = new StringBuilder();
    private SparseArray<String> mIdToDateStringCache = new SparseArray<String>();

    public static final SeriesInfoFragment newInstance(String seriesId) {
        SeriesInfoFragment frag = new SeriesInfoFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_SERIES_ID, seriesId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            mSeriesId = getArguments().getString(EXTRA_SERIES_ID);
        }

        if(savedInstanceState != null)
            mSeriesId = savedInstanceState.getString(EXTRA_SERIES_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_series_info, parent, false);

        mList = (ListView) v.findViewById(R.id.list);
        mEmptyView = v.findViewById(android.R.id.empty);
        mImage = (RemoteImageView) v.findViewById(R.id.image);

        mAuthor = (TextView) v.findViewById(R.id.author);
        mCreated = (TextView) v.findViewById(R.id.created);
        mLastChapter = (TextView) v.findViewById(R.id.last_chapter_date);
        mDescription = (TextView) v.findViewById(R.id.description);

        mChaptersHeading = (TextView) v.findViewById(R.id.chapters_heading);
        mLoadingView = (ViewGroup) v.findViewById(R.id.loading_wrapper);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_2, null,
                new String[]{DbField.TITLE.name, DbField.RELEASE_DATE.name}, new int[]{android.R.id.text1, android.R.id.text2}, 0);
        mAdapter.setViewBinder(this);

        mList.setAdapter(mAdapter);
        mList.setLayoutAnimation(Anim.getListViewDealAnimator());
        mList.setOnItemClickListener(this);
        mList.setEmptyView(mEmptyView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_SERIES_ID, mSeriesId);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        getLoaderManager().initLoader(LOADER_ID_SERIES_INFO, null, this);
        getLoaderManager().initLoader(LOADER_ID_CHAPTERS, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        switch(id) {
            case LOADER_ID_SERIES_INFO: {
                final String[] proj = {
                        DbTable.SERIES + "." + DbField.ID.name,
                        DbTable.SERIES + "." + DbField.TITLE.name,
                        DbTable.SERIES + "." + DbField.SERIES_ID.name,
                        DbTable.SERIES + "." + DbField.IMAGE.name,
                        DbTable.SERIES + "." + DbField.DESCRIPTION.name,
                        DbTable.SERIES + "." + DbField.AUTHOR.name,
                        DbTable.SERIES + "." + DbField.LAST_CHAPTER_DATE.name,
                        DbTable.SERIES + "." + DbField.CREATED_DATE.name,
                        DbTable.SERIES_FAVOURITES + "." + DbField.FAVOURITE.name
                };
                final String sel = DbTable.SERIES + "." + DbField.SERIES_ID + " = ?";
                final String[] selArgs = {mSeriesId == null ? "" : mSeriesId};
                return new CursorLoader(getActivity(), MangaProvider.SERIES_LIST_URI, proj, sel, selArgs, DbField.TITLE + " ASC");
            }

            case LOADER_ID_CHAPTERS: {
                final Uri uri = Uri.withAppendedPath(MangaProvider.SERIES_CHAPTERS_URI, mSeriesId);
                return new CursorLoader(getActivity(), uri, null, null, null, DbField.SEQUENCE_ID + " DESC, " + DbField.RELEASE_DATE + " DESC");
            }
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch(loader.getId()) {
            case LOADER_ID_SERIES_INFO: {
                if(cursor != null && cursor.moveToFirst()) {
                    mMangaSeries = new MangaSeries();
                    mMangaSeries.title = cursor.getString(SERIES_INFO_COL_TITLE);
                    mMangaSeries.id = cursor.getString(SERIES_INFO_COL_SERIES_ID);
                    mMangaSeries.image = cursor.getString(SERIES_INFO_COL_IMAGE);
                    mMangaSeries.description = cursor.getString(SERIES_INFO_COL_DESCRIPTION);
                    mMangaSeries.author = cursor.getString(SERIES_INFO_COL_AUTHOR);
                    mMangaSeries.lastChapterRataDi = cursor.getInt(SERIES_INFO_COL_LAST_CHAPTER_DATE);
                    mMangaSeries.createdRataDi = cursor.getInt(SERIES_INFO_COL_CREATED_DATE);
                    mMangaSeries.isFavourite = cursor.getInt(SERIES_INFO_COL_FAVOURITE) == 1;

                    if(mOnSeriesInfoLoadedListener != null)
                        mOnSeriesInfoLoadedListener.onSeriesInfoLoaded(mMangaSeries);

                    mImage.setImageURL(mMangaSeries.image);
                    if(TextUtils.isEmpty(mMangaSeries.description))
                        mDescription.setVisibility(View.GONE);
                    else {
                        mDescription.setText(Html.fromHtml(mMangaSeries.description));
                        mDescription.setVisibility(View.VISIBLE);
                    }

                    if(TextUtils.isEmpty(mMangaSeries.author))
                        mAuthor.setVisibility(View.GONE);
                    else {
                        mAuthor.setText(mMangaSeries.author);
                        mAuthor.setVisibility(View.VISIBLE);
                    }

                    Time t = new Time();
                    if(mMangaSeries.lastChapterRataDi > 0) {
                        t.setJulianDay(TimeUtil.getJulianDay(mMangaSeries.lastChapterRataDi));
                        mLastChapter.setText("Last release on " + TimeUtil.getDateString(getActivity(), t.toMillis(true)));
                        mLastChapter.setVisibility(View.VISIBLE);
                    } else {
                        mLastChapter.setVisibility(View.GONE);
                    }

                    if(mMangaSeries.createdRataDi > 0) {
                        t.setJulianDay(TimeUtil.getJulianDay(mMangaSeries.createdRataDi));
                        mCreated.setText("Created on " + TimeUtil.getDateString(getActivity(), t.toMillis(true)));
                        mCreated.setVisibility(View.VISIBLE);
                    }  else {
                        mCreated.setVisibility(View.GONE);
                    }


                    final int visibility, chapterHeadingVisibility;
                    final float alpha, chapterHeadingAlpha;
                    if(!mMangaSeries.hasBeenFilled()) {
                        ApiService.requestGetChapterList(getActivity(), mMangaSeries);
                        visibility = View.VISIBLE;
                        chapterHeadingVisibility = View.GONE;
                        alpha = 1.0f;
                        chapterHeadingAlpha = 0.0f;
                    } else {
                        visibility = View.GONE;
                        chapterHeadingVisibility = View.VISIBLE;
                        alpha = 0.0f;
                        chapterHeadingAlpha = 1.0f;
                    }

                    animate(mLoadingView).setDuration(300).setInterpolator(INTERPOLATOR).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            mLoadingView.setVisibility(visibility);
                        }
                    }).alpha(alpha).start();

                    animate(mChaptersHeading).setDuration(300).setInterpolator(INTERPOLATOR).setListener(new Animator.AnimatorListener() {
                        @Override public void onAnimationStart(Animator animator) {}
                        @Override public void onAnimationCancel(Animator animator) {}
                        @Override public void onAnimationRepeat(Animator animator) {}

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            mChaptersHeading.setVisibility(chapterHeadingVisibility);
                        }
                    }).alpha(chapterHeadingAlpha).start();

                    animate(mEmptyView).setDuration(300).setInterpolator(INTERPOLATOR).setListener(new Animator.AnimatorListener() {
                        @Override public void onAnimationStart(Animator animator) {}
                        @Override public void onAnimationCancel(Animator animator) {}
                        @Override public void onAnimationRepeat(Animator animator) {}

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            mEmptyView.setVisibility(chapterHeadingVisibility);
                        }
                    }).alpha(chapterHeadingAlpha).start();
                }

                break;
            }
            case LOADER_ID_CHAPTERS: {
                mAdapter.swapCursor(cursor);
                break;
            }
            case LOADER_ID_CATEGORIES: {

            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
        String chapterId = (String) view.getTag();
        if(!TextUtils.isEmpty(chapterId)) {
            final Intent intent = new Intent(getActivity(), ChapterActivity.class);
            intent.putExtra(ChapterActivity.EXTRA_CHAPTER_ID, chapterId);
            intent.putExtra(ChapterActivity.EXTRA_SERIES_TITLE, mMangaSeries.title);
            intent.putExtra(ChapterActivity.EXTRA_TITLE, ((TextView) view.findViewById(android.R.id.text1)).getText());
            startActivity(intent);
        }
    }

    @Override
    public boolean setViewValue(View view, Cursor cursor, int id) {
        final ViewGroup parent = (ViewGroup) view.getParent();
        parent.setTag(cursor.getString(CHAPTER_COL_CHAPTER_ID));

        String text = null;
        switch(id) {
            case CHAPTER_COL_RELEASE_DATE:
                text = mIdToDateStringCache.get(cursor.getInt(0));
                if(TextUtils.isEmpty(text)) {
                    final int jd = TimeUtil.getJulianDay(cursor.getInt(id));
                    mTime.setJulianDay(jd);

                    mStringBuilder.setLength(0);
                    mStringBuilder.append("Released on ");

                    mStringBuilder.append(TimeUtil.getDateString(getActivity(), mTime.toMillis(true)));

                    text = mStringBuilder.toString();
                    mIdToDateStringCache.put(cursor.getInt(0), text);
                }

                break;
            case CHAPTER_COL_TITLE:
                text = cursor.getString(id);
                if(TextUtils.isEmpty(text) || TextUtils.equals("null", text))
                    text = cursor.getString(CHAPTER_COL_SEQUENCE_NUM);
                else
                    text = text.trim();

                break;
        }


        ((TextView) view).setText(text);

        return true;
    }

    public void setOnSeriesInfoLoaded(OnSeriesInfoLoadedListener listener) {
        this.mOnSeriesInfoLoadedListener = listener;
    }

    public void reloadInfo() {
        getLoaderManager().restartLoader(LOADER_ID_SERIES_INFO, null, this).forceLoad();
    }

    public static interface OnSeriesInfoLoadedListener {
        public void onSeriesInfoLoaded(MangaSeries series);
    }
}
