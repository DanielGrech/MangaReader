package com.dgsd.android.MangaReader;

import com.dgsd.android.MangaReader.Model.MangaChapter;
import com.dgsd.android.MangaReader.Model.MangaPage;
import com.dgsd.android.MangaReader.Model.MangaSeries;
import com.dgsd.android.MangaReader.Util.Http;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MangaApi {
    public static final String IMAGE_BASE_URL = "http://cdn.mangaeden.com/mangasimg/";

    private static final String MANGA_LIST_URL = "http://www.mangaeden.com/api/list/0";
    private static final String CHAPTER_LIST_URL = "http://www.mangaeden.com/api/manga/";
    private static final String CHAPTER_DOWNLOAD_URL = "http://www.mangaeden.com/en-pdf";
    private static final String CHAPTER_PAGE_LIST_URL = "http://www.mangaeden.com/api/chapter/";

    private static final String KEY_MANGA = "manga";
    private static final String KEY_IMAGES = "images";

    public static List<MangaSeries> getMangaSeries() throws IOException, MangaException {
        final String response = Http.get(MANGA_LIST_URL);

        try {
            JSONArray series = new JSONObject(response).getJSONArray(KEY_MANGA);
            if(series != null && series.length() > 0) {
                final List<MangaSeries> retval = new ArrayList<MangaSeries>(series.length());
                for(int i = 0, len = series.length(); i < len; i++) {
                    MangaSeries mangaSeries = new MangaSeries();
                    mangaSeries.fromJson(series.getJSONObject(i));
                    retval.add(mangaSeries);
                }

                return retval;
            }
        } catch (JSONException e) {
            throw new MangaException(e);
        }

        return null;
    }

    public static void fillChapters(MangaSeries series) throws IOException, MangaException{
        if(series == null)
            return;

        final String response = Http.get(CHAPTER_LIST_URL + series.id);

        try {
            series.fromJson(new JSONObject(response));
        } catch (JSONException e) {
            throw new MangaException(e);
        }
    }

    public static void fillPages(MangaChapter chapter) throws IOException, MangaException{
        if(chapter == null)
            return;

        final String response = Http.get(CHAPTER_PAGE_LIST_URL + chapter.id);

        try {
            chapter.pages = new ArrayList<MangaPage>();
            JSONArray pages = new JSONObject(response).getJSONArray(KEY_IMAGES);
            for(int i = 0, len = pages.length(); i < len; i++) {
                JSONArray pageInfo = pages.getJSONArray(i);

                MangaPage p = new MangaPage();
                p.pageNum = pageInfo.getInt(0);
                p.url = IMAGE_BASE_URL + pageInfo.getString(1);

                chapter.pages.add(p);
            }
        } catch (JSONException e) {
            throw new MangaException(e);
        }
    }


}
