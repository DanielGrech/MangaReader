package com.dgsd.android.MangaReader.Model;

import android.os.Parcel;
import android.os.Parcelable;
import com.dgsd.android.MangaReader.JsonRepresentable;
import org.json.JSONObject;

import java.util.List;

public class MangaChapter implements JsonRepresentable, Parcelable {
    public String id;
    public String title;
    public int releaseRataDi;
    public int sequenceId;
    public List<MangaPage> pages;

    public MangaChapter() {
        id = null;
        title = null;
        releaseRataDi = -1;
        pages = null;
    }

    private static MangaChapter fromParcel(Parcel in) {
        final MangaChapter series = new MangaChapter();

        series.id = in.readString();
        series.title = in.readString();
        series.releaseRataDi = in.readInt();
        series.sequenceId = in.readInt();
        series.pages = in.readArrayList(MangaPage.class.getClassLoader());

        return series;
    }

    @Override
    public JSONObject toJson() {
        return null;
    }

    @Override
    public void fromJson(JSONObject json) {

    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeInt(releaseRataDi);
        dest.writeInt(sequenceId);
        dest.writeList(pages);
    }

    public static final Creator<MangaChapter> CREATOR = new Creator<MangaChapter>() {
        public MangaChapter createFromParcel(Parcel in) {
            return MangaChapter.fromParcel(in);
        }

        public MangaChapter[] newArray(int size) {
            return new MangaChapter[size];
        }
    };
}
