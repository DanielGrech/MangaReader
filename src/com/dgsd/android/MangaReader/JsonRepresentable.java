package com.dgsd.android.MangaReader;

import org.json.JSONException;
import org.json.JSONObject;

public interface JsonRepresentable {
    public JSONObject toJson();
    public void fromJson(JSONObject json) throws JSONException;
}
