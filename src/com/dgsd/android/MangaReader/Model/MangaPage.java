package com.dgsd.android.MangaReader.Model;

import android.os.Parcel;
import android.os.Parcelable;
import com.dgsd.android.MangaReader.JsonRepresentable;
import org.json.JSONObject;

public class MangaPage implements JsonRepresentable, Parcelable {
    public int pageNum = -1;
    public String url;

    public MangaPage() {
        pageNum = -1;
        url = null;
    }

    private static MangaPage fromParcel(Parcel in) {
        final MangaPage page = new MangaPage();

        page.pageNum = in.readInt();
        page.url = in.readString();

        return page;
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
        dest.writeInt(pageNum);
        dest.writeString(url);
    }

    public static final Creator<MangaPage> CREATOR = new Creator<MangaPage>() {
        public MangaPage createFromParcel(Parcel in) {
            return MangaPage.fromParcel(in);
        }

        public MangaPage[] newArray(int size) {
            return new MangaPage[size];
        }
    };
}
