package com.dgsd.android.MangaReader.Service;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.dgsd.android.MangaReader.MangaApi;
import com.dgsd.android.MangaReader.BuildConfig;
import com.dgsd.android.MangaReader.Data.DbField;
import com.dgsd.android.MangaReader.Data.MangaProvider;
import com.dgsd.android.MangaReader.Model.MangaChapter;
import com.dgsd.android.MangaReader.Model.MangaPage;
import com.dgsd.android.MangaReader.Model.MangaSeries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ApiService extends IntentService {
    private static final String TAG = ApiService.class.getSimpleName();

    public static final String ACTION_RESULT_READY = "com.dgsd.android.MangaReader.Service.ApiService._action_result_ready";

    public static final String EXTRA_REQUEST_TYPE = "com.dgsd.android.MangaReader.Service.ApiService._extra_request_type";
    public static final String EXTRA_RESULT_TYPE = "com.dgsd.android.MangaReader.Service.ApiService._extra_result_type";
    public static final String EXTRA_MANGA_SERIES = "com.dgsd.android.MangaReader.Service.ApiService._extra_manga_series";
    public static final String EXTRA_MANGA_CHAPTER = "com.dgsd.android.MangaReader.Service.ApiService._extra_manga_chapter";

    public static final int REQUEST_TYPE_MANGA_LIST = 0x01;
    public static final int REQUEST_TYPE_CHAPTER_LIST = 0x02;
    public static final int REQUEST_TYPE_PAGES = 0x03;


    public static final int RESULT_SUCCESS = 0x01;
    public static final int RESULT_NO_DATA = 0x02;
    public static final int RESULT_ERROR = 0x03;

    /**
     * Maximum number of operations to queue before execution.
     *
     * This may actually take longer to insert all the rows, but will
     * appear faster as we can start reading the data without waiting
     * for the entire set
     */
    public static final int MAX_BATCH_SIZE = 50;

    public ApiService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final int type = intent.getIntExtra(EXTRA_REQUEST_TYPE, -1);
        try {
            switch (type) {
                case REQUEST_TYPE_MANGA_LIST: {
                    List<MangaSeries> series = MangaApi.getMangaSeries();

                    Iterator<MangaSeries> iter = series.iterator();
                    int counter = 0;
                    final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                    while(iter.hasNext()) {
                        final MangaSeries ms = iter.next();
                        ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(MangaProvider.SERIES_LIST_URI);
                        b.withYieldAllowed(true);
                        b.withValue(DbField.SERIES_ID.name, ms.id);
                        b.withValue(DbField.IMAGE.name, ms.image);
                        b.withValue(DbField.TITLE.name, ms.title);
                        ops.add(b.build());

                        //Try to free up a bit of memory..
                        iter.remove();

                        if(counter++ > MAX_BATCH_SIZE) {
                            getContentResolver().applyBatch(MangaProvider.AUTHORITY, ops);
                            ops.clear();
                            counter = 0;
                        }
                    }

                    getContentResolver().applyBatch(MangaProvider.AUTHORITY, ops);
                    broadcastResult(type, RESULT_SUCCESS);

                    break;
                }

                case REQUEST_TYPE_CHAPTER_LIST: {
                    final MangaSeries series = intent.getParcelableExtra(EXTRA_MANGA_SERIES);
                    MangaApi.fillChapters(series);

                    if(series.chapters == null || series.chapters.isEmpty()) {
                        broadcastResult(type, RESULT_NO_DATA);
                        return;
                    }

                    final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(series.chapters.size() + 5);

                    ContentProviderOperation.Builder updateBuilder = ContentProviderOperation.newInsert(MangaProvider.SERIES_LIST_URI);
                    updateBuilder.withValue(DbField.DESCRIPTION.name, series.description);
                    updateBuilder.withValue(DbField.AUTHOR.name, series.author);
                    updateBuilder.withValue(DbField.LAST_CHAPTER_DATE.name, series.lastChapterRataDi);
                    updateBuilder.withValue(DbField.CREATED_DATE.name, series.createdRataDi);
                    updateBuilder.withValue(DbField.SERIES_ID.name, series.id);
                    updateBuilder.withValue(DbField.IMAGE.name, series.image);
                    updateBuilder.withValue(DbField.TITLE.name, series.title);
                    ops.add(updateBuilder.build());

                    int counter = 0;
                    for(MangaChapter chapter : series.chapters) {
                        ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(MangaProvider.SERIES_CHAPTERS_URI);
                        b.withYieldAllowed(true);

                        b.withValue(DbField.CHAPTER_ID.name, chapter.id);
                        b.withValue(DbField.SEQUENCE_ID.name, chapter.sequenceId);
                        b.withValue(DbField.SERIES_ID.name, series.id);
                        b.withValue(DbField.TITLE.name, chapter.title);
                        b.withValue(DbField.RELEASE_DATE.name, chapter.releaseRataDi);

                        ops.add(b.build());

                        if(counter++ > MAX_BATCH_SIZE) {
                            getContentResolver().applyBatch(MangaProvider.AUTHORITY, ops);
                            ops.clear();
                            counter = 0;
                        }
                    }

                    getContentResolver().applyBatch(MangaProvider.AUTHORITY, ops);
                    broadcastResult(type, RESULT_SUCCESS);

                    break;
                }

                case REQUEST_TYPE_PAGES: {
                    final MangaChapter chapter = new MangaChapter();
                    chapter.id = intent.getStringExtra(EXTRA_MANGA_CHAPTER);

                    MangaApi.fillPages(chapter);
                    if(chapter.pages == null || chapter.pages.isEmpty()) {
                        broadcastResult(type, RESULT_NO_DATA);
                        return;
                    }

                    final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(chapter.pages.size());
                    for(MangaPage page : chapter.pages) {
                        ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(MangaProvider.PAGE_URI);
                        b.withValue(DbField.CHAPTER_ID.name, chapter.id);
                        b.withValue(DbField.PAGE_NUMBER.name, page.pageNum);
                        b.withValue(DbField.IMAGE.name, page.url);

                        ops.add(b.build());
                    }

                    getContentResolver().applyBatch(MangaProvider.AUTHORITY, ops);
                    broadcastResult(type, RESULT_SUCCESS);
                }
            }
        } catch (Exception e) {
            if(BuildConfig.DEBUG)
                Log.e(TAG, "Error executing API request", e);

            broadcastResult(type, RESULT_SUCCESS);
        }
    }

    private void broadcastResult(int requestCode, int resultCode) {
        Intent intent = new Intent(ACTION_RESULT_READY);
        intent.putExtra(EXTRA_RESULT_TYPE, resultCode);
        intent.putExtra(EXTRA_REQUEST_TYPE, requestCode);
        sendBroadcast(intent);
    }

    public static final void requestGetMangaList(Context c) {
        final Intent intent = new Intent(c, ApiService.class);

        intent.putExtra(EXTRA_REQUEST_TYPE, REQUEST_TYPE_MANGA_LIST);

        c.startService(intent);
    }

    public static final void requestGetChapterList(Context c, MangaSeries series) {
        final Intent intent = new Intent(c, ApiService.class);

        intent.putExtra(EXTRA_REQUEST_TYPE, REQUEST_TYPE_CHAPTER_LIST);
        intent.putExtra(EXTRA_MANGA_SERIES, series);

        c.startService(intent);
    }

    public static final void requestPages(Context c, String chapterId) {
        final Intent intent = new Intent(c, ApiService.class);

        intent.putExtra(EXTRA_REQUEST_TYPE, REQUEST_TYPE_PAGES);
        intent.putExtra(EXTRA_MANGA_CHAPTER, chapterId);

        c.startService(intent);
    }

}
