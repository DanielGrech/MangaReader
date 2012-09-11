package com.dgsd.android.MangaReader.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragment;
import com.dgsd.android.MangaReader.Model.MangaPage;
import com.dgsd.android.MangaReader.R;
import com.dgsd.android.MangaReader.View.PhotoView.PageView;
import com.handlerexploit.prime.widgets.RemoteImageView;
import com.nineoldandroids.animation.Animator;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class PageFragment extends SherlockFragment implements PageView.OnImageBitmapLoadedListener {
    private static final String TAG = PageFragment.class.getSimpleName();

    public static final String EXTRA_MANGA_PAGE = "com.dgsd.android.MangaReader.Fragment.SeriesInfoFragment._extra_manga_page";

    private static final String KEY_IMAGE_LOADED = "_image_loaded";

    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();

    private PageView mView;
    private MangaPage mPage;
    private ViewGroup mLoadingView;

    private PageView.OnPhotoTapListener mOnPhotoTapListener;

    private boolean mImageLoaded = false;

    public static final PageFragment newInstance(MangaPage page) {
        PageFragment frag = new PageFragment();

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_MANGA_PAGE, page);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            mPage = getArguments().getParcelable(EXTRA_MANGA_PAGE);
        }

        if(savedInstanceState != null) {
            mPage = savedInstanceState.getParcelable(EXTRA_MANGA_PAGE);
            mImageLoaded = savedInstanceState.getBoolean(KEY_IMAGE_LOADED, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_page, parent, false);

        mView = (PageView) v.findViewById(R.id.image);
        mView.setZoomable(true);
        mView.setImageURL(mPage.url);
        mView.setOnImageBitmapLoadedListener(this);
        mView.setPhotoTapListener(new PageView.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float x, float y) {
                if(mOnPhotoTapListener != null)
                    mOnPhotoTapListener.onPhotoTap(view, x, y);
            }
        });

        mLoadingView = (ViewGroup) v.findViewById(R.id.loading_wrapper);
        if(mImageLoaded)
            mLoadingView.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IMAGE_LOADED, mImageLoaded);
        outState.putParcelable(EXTRA_MANGA_PAGE, mPage);
    }

    @Override
    public void onImageLoaded(RemoteImageView view) {
        mImageLoaded = true;
        animate(mLoadingView).alpha(0.0f)
                             .setDuration(300)
                             .setInterpolator(INTERPOLATOR)
                             .setListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animator) {}
            @Override public void onAnimationCancel(Animator animator) {}
            @Override public void onAnimationRepeat(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                mLoadingView.setVisibility(View.GONE);
            }
        }).start();
    }

    public void setOnPhotoTapListener(PageView.OnPhotoTapListener listener) {
        this.mOnPhotoTapListener = listener;
    }
}
