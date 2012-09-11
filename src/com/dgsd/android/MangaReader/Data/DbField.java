package com.dgsd.android.MangaReader.Data;

/**
 * @author Daniel Grech
 */
public class DbField {
    public static final DbField ID = new DbField("_id", "integer", "primary key");

    public static final DbField SERIES_ID = new DbField("series_id", "text");
    public static final DbField CHAPTER_ID = new DbField("chapter_id", "text");
    public static final DbField SEQUENCE_ID = new DbField("chapter_sequence_num", "text");
    public static final DbField IMAGE = new DbField("image", "text");
    public static final DbField TITLE = new DbField("title", "text");
    public static final DbField DESCRIPTION = new DbField("description", "text");
    public static final DbField CATEGORY = new DbField("category", "text");
    public static final DbField AUTHOR = new DbField("author", "text");
    public static final DbField LAST_CHAPTER_DATE = new DbField("last_chapter_date", "integer");
    public static final DbField RELEASE_DATE = new DbField("release_date", "integer");
    public static final DbField CREATED_DATE = new DbField("created_date", "integer");
    public static final DbField FAVOURITE = new DbField("is_favourite", "integer");
    public static final DbField PAGE_NUMBER = new DbField("page_number", "integer");

	public String name;
	public String type;
	public String constraint;

	public DbField(String n, String t) {
		this(n, t, null);
	}

	public DbField(String n, String t, String c) {
		name = n;
		type = t;
		constraint = c;
	}

	@Override
	public String toString() {
		return name;
	}
}
