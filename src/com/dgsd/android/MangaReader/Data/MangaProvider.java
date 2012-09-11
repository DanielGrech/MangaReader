package com.dgsd.android.MangaReader.Data;

import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import com.dgsd.android.MangaReader.BuildConfig;

import java.sql.SQLException;


public class MangaProvider extends ContentProvider {
    private static final String TAG = MangaProvider.class.getSimpleName();

    public static final String AUTHORITY = "com.dgsd.android.MangaReader.Data.MangaProvider";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private static final UriMatcher mURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final int SERIES_LIST = 0x1;
    public static final int SERIES = 0x2;
    public static final int CHAPTERS_QUERY = 0x3;
    public static final int CHAPTERS_LIST = 0x4;
    public static final int CHAPTER = 0x5;
    public static final int PAGE_LIST = 0x6;
    public static final int SERIES_FAVOURITE = 0x7;
    public static final int SEARCH_QUERY = 0x8;

    public static final Uri SERIES_LIST_URI = Uri.withAppendedPath(BASE_URI, "series_list");
    public static final Uri SERIES_FAVOURITE_URI = Uri.withAppendedPath(BASE_URI, "series_favourite");
    public static final Uri SERIES_INFO_URI = Uri.withAppendedPath(BASE_URI, "series");
    public static final Uri SERIES_CHAPTERS_URI = Uri.withAppendedPath(BASE_URI, "chapter_list");
    public static final Uri CHAPTER_URI = Uri.withAppendedPath(BASE_URI, "chapter");
    public static final Uri PAGE_URI = Uri.withAppendedPath(BASE_URI, "page");
    public static final Uri CATEGORIES_URI = Uri.withAppendedPath(BASE_URI, "categories");

    private Db mDb;

    static {
        mURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_QUERY);
        mURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_QUERY);
        mURIMatcher.addURI(AUTHORITY, "series_list", SERIES_LIST);
        mURIMatcher.addURI(AUTHORITY, "series_favourite", SERIES_FAVOURITE);
        mURIMatcher.addURI(AUTHORITY, "series/#", SERIES);
        mURIMatcher.addURI(AUTHORITY, "chapter_list", CHAPTERS_LIST);
        mURIMatcher.addURI(AUTHORITY, "chapter_list/*", CHAPTERS_QUERY);
        mURIMatcher.addURI(AUTHORITY, "chapter/*", CHAPTER);
        mURIMatcher.addURI(AUTHORITY, "page", PAGE_LIST);
    }

    @Override
    public boolean onCreate() {
        mDb = Db.getInstance(getContext());
        return true;
    }

    @Override
    public String getType(final Uri uri) {
        if (mURIMatcher.match(uri) != UriMatcher.NO_MATCH) {
            return uri.toString();
        } else {
            return null;
        }
    }

    @Override
    public Cursor query(final Uri uri, String[] proj, final String sel, final String[] selArgs, final String sort) {
        final int type = mURIMatcher.match(uri);
        if(type == UriMatcher.NO_MATCH) {
            if(BuildConfig.DEBUG)
                Log.w(TAG, "No match for URI: " + uri);

            return null;
        }

        try {
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

            switch (type) {
                case SERIES_LIST:
                    qb.setTables(DbTable.SERIES.name + " LEFT OUTER JOIN " + DbTable.SERIES_FAVOURITES +
                            " ON " + DbTable.SERIES.name + "." + DbField.SERIES_ID + " = " +
                            DbTable.SERIES_FAVOURITES + "." + DbField.SERIES_ID);
                    break;
                case SERIES:
                    qb.setTables(DbTable.SERIES.name + " LEFT OUTER JOIN " + DbTable.SERIES_FAVOURITES +
                            " ON " + DbTable.SERIES.name + "." + DbField.SERIES_ID + " = " +
                            DbTable.SERIES_FAVOURITES + "." + DbField.SERIES_ID);

                    qb.appendWhere(DbField.ID + "='" + uri.getLastPathSegment() + "'");
                    break;
                case CHAPTERS_QUERY:
                    qb.setTables(DbTable.CHAPTERS.name);
                    qb.appendWhere(DbField.SERIES_ID + "='" + uri.getLastPathSegment() + "'");
                    break;
                case CHAPTER:
                    qb.setTables(DbTable.PAGES.name);
                    qb.appendWhere(DbField.CHAPTER_ID + "='" + uri.getLastPathSegment() + "'");
                    break;
                case SEARCH_QUERY:
                    //TODO
                    qb.setTables(DbTable.SERIES.name);

                    String searchTerm = uri.getLastPathSegment();

                    StringBuilder where = new StringBuilder().append(DbField.TITLE.name).append(" LIKE ");
                    DatabaseUtils.appendValueToSql(where, "%" + searchTerm + "%");

                    qb.appendWhere(where);

                    proj = new String[] {
                            DbField.ID.name,
                            DbField.TITLE.name + " as " + SearchManager.SUGGEST_COLUMN_TEXT_1,
                            DbField.TITLE.name + " as " + SearchManager.SUGGEST_COLUMN_QUERY,
                            DbField.SERIES_ID.name + " || ',' || " + DbField.TITLE
                                    + " as " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
                    };


                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            Cursor cursor = qb.query(mDb.getReadableDatabase(), proj, sel, selArgs, null, null, sort, null);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);

            return cursor;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error querying data", e);
            }

            return null;
        }
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final int type = mURIMatcher.match(uri);
        try {
            final SQLiteDatabase db = mDb.getWritableDatabase();

            String table = null;
            switch (type) {
                case SERIES_LIST:
                    table = DbTable.SERIES.name;
                    break;
                case CHAPTERS_LIST:
                    table = DbTable.CHAPTERS.name;
                    break;
                case PAGE_LIST:
                    table = DbTable.PAGES.name;
                    break;
                case SERIES_FAVOURITE:
                    table = DbTable.SERIES_FAVOURITES.name;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            long id = db.replaceOrThrow(table, null, values);
            if (id > 0) {
                Uri newUri = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(uri, null);
                return newUri;
            } else {
                throw new SQLException("Failed to insert row into " + uri);
            }

        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, "Error inserting data", e);

            return null;
        }
    }

    @Override
    public int delete(final Uri uri, final String sel, final String[] selArgs) {
        final int type = mURIMatcher.match(uri);

        try {
            String table = null;
            switch (type) {
                case SERIES_LIST:
                    table = DbTable.SERIES.name;
                    break;
                case CHAPTERS_QUERY:
                    table = DbTable.CHAPTERS.name;
                    break;
                case PAGE_LIST:
                    table = DbTable.PAGES.name;
                    break;
                case SERIES_FAVOURITE:
                    table = DbTable.SERIES_FAVOURITES.name;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            int rowsAffected = mDb.getWritableDatabase().delete(table, sel, selArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return rowsAffected;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error deleting data", e);
            }
            return 0;
        }
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String sel, final String[] selArgs) {
        final int type = mURIMatcher.match(uri);

        try {
            String id = null;
            String table = null;
            switch (type) {
                case SERIES_LIST:
                    table = DbTable.SERIES.name;
                    break;
                case CHAPTERS_QUERY:
                    table = DbTable.CHAPTERS.name;
                    break;
                case PAGE_LIST:
                    table = DbTable.PAGES.name;
                    break;
                case SERIES_FAVOURITE:
                    table = DbTable.SERIES_FAVOURITES.name;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }

            final SQLiteDatabase db = mDb.getWritableDatabase();

            final int rowsAffected = db.update(table, values, sel, selArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return rowsAffected;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error deleting data", e);
            }
            return 0;
        }
    }
}