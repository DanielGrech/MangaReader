package com.dgsd.android.MangaReader.Model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.dgsd.android.MangaReader.MangaApi;
import com.dgsd.android.MangaReader.JsonRepresentable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MangaSeries implements JsonRepresentable, Parcelable {

    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_AUTHOR = "author";
    private static final String KEY_CHAPTERS = "chapters";
    private static final String KEY_ID = "i";
    private static final String KEY_IMAGE = "im";
    private static final String KEY_TITLE = "t";
    private static final String KEY_LAST_CHAPTER_RATA_DI = "last_chapter_date";
    private static final String KEY_CREATED_RATA_DI = "created";
    private static final String KEY_CATEGORIES = "categories";


    private static final int CHAPTER_INDEX_SEQUENCE_ID = 0;
    private static final int CHAPTER_INDEX_RELEASE_RATA_DI = 1;
    private static final int CHAPTER_INDEX_CHAPTER_TITLE = 2;
    private static final int CHAPTER_INDEX_ID = 3;

    public String id;
    public String image;
    public String title;
    public String description;
    public List<String> categories;
    public String author;
    public int lastChapterRataDi;
    public int createdRataDi;
    public boolean isFavourite;
    public List<MangaChapter> chapters;

    public MangaSeries() {
        id = null;
        image = null;
        title = null;
        description = null;
        categories = null;
        author = null;
        lastChapterRataDi = -1;
        createdRataDi = -1;
        chapters = null;
        isFavourite = false;
    }

    private static MangaSeries fromParcel(Parcel in) {
        final MangaSeries series = new MangaSeries();

        series.id = in.readString();
        series.image = in.readString();
        series.title = in.readString();
        series.description = in.readString();

        series.categories = new ArrayList<String>();
        in.readStringList(series.categories);

        series.author = in.readString();
        series.lastChapterRataDi = in.readInt();
        series.createdRataDi = in.readInt();

        series.isFavourite = in.readInt() == 1;

        series.chapters = in.readArrayList(MangaChapter.class.getClassLoader());

        return series;
    }

    @Override
    public JSONObject toJson() {
        return null;
    }

    @Override
    public void fromJson(JSONObject json) throws JSONException {

        id = json.optString(KEY_ID, id);
        image = json.optString(KEY_IMAGE, image);
        if(!TextUtils.isEmpty(image) && !image.startsWith(MangaApi.IMAGE_BASE_URL))
            image = MangaApi.IMAGE_BASE_URL + image;

        title = json.optString(KEY_TITLE, title);
        description = json.optString(KEY_DESCRIPTION, description);
        author = json.optString(KEY_AUTHOR, author);
        lastChapterRataDi = json.optInt(KEY_LAST_CHAPTER_RATA_DI, lastChapterRataDi);
        createdRataDi = json.optInt(KEY_CREATED_RATA_DI, createdRataDi);

        if(json.has(KEY_CATEGORIES)) {
            categories = new ArrayList<String>();
            JSONArray cats = json.getJSONArray(KEY_CATEGORIES);
            for(int i = 0, len = cats.length(); i < len; i++)
                categories.add(cats.getString(i));
        }


        JSONArray chapterArray = json.optJSONArray(KEY_CHAPTERS);
        if(chapterArray != null && chapterArray.length() > 0) {
            chapters = new ArrayList<MangaChapter>();
            for(int i = 0, len = chapterArray.length(); i < len; i++) {
                final MangaChapter chapter = new MangaChapter();

                JSONArray chapterInfo = chapterArray.getJSONArray(i);
                chapter.sequenceId = chapterInfo.optInt(CHAPTER_INDEX_SEQUENCE_ID, -1);
                chapter.releaseRataDi = chapterInfo.optInt(CHAPTER_INDEX_RELEASE_RATA_DI, -1);
                chapter.title = chapterInfo.optString(CHAPTER_INDEX_CHAPTER_TITLE);
                chapter.id = chapterInfo.optString(CHAPTER_INDEX_ID);

                chapters.add(chapter);
            }
        }
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(image);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeStringList(categories);
        dest.writeString(author);
        dest.writeInt(lastChapterRataDi);
        dest.writeInt(createdRataDi);
        dest.writeInt(isFavourite ? 1 : 0);
        dest.writeList(chapters);
    }

    public boolean hasBeenFilled() {
        boolean hasDesc = !TextUtils.isEmpty(description);
        boolean hasAuthor = !TextUtils.isEmpty(author);
        boolean hasLastChapterDate = lastChapterRataDi > 0;
        boolean hasCreatedDate = createdRataDi > 0;

        return hasDesc || hasAuthor || hasLastChapterDate || hasCreatedDate;
    }

    public static final Creator<MangaSeries> CREATOR = new Creator<MangaSeries>() {
        public MangaSeries createFromParcel(Parcel in) {
            return MangaSeries.fromParcel(in);
        }

        public MangaSeries[] newArray(int size) {
            return new MangaSeries[size];
        }
    };
}
