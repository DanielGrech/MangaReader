package com.dgsd.android.MangaReader.Data;

import android.content.Context;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class Db extends SQLiteAssetHelper {
    private static final String TAG = Db.class.getSimpleName();

    private static final int VERSION = 1;
    public static final String DB_NAME = "manga";

    private static Db mInstance;

    public static Db getInstance(Context c) {
        if(mInstance == null)
            mInstance = new Db(c);

        return mInstance;
    }

    protected Db(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, VERSION);
    }
}

