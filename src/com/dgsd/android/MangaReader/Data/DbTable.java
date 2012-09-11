package com.dgsd.android.MangaReader.Data;

/**
 * @author Daniel Grech
 */
public class DbTable {
	private static final String TAG = DbTable.class.getSimpleName();

    public static final DbTable SERIES = new DbTable("series",  new DbField[] {
        DbField.ID,
        DbField.SERIES_ID,
        DbField.IMAGE,
        DbField.TITLE,
        DbField.DESCRIPTION,
        DbField.AUTHOR,
        DbField.LAST_CHAPTER_DATE,
        DbField.CREATED_DATE
    });

    public static final DbTable CHAPTERS = new DbTable("chapters", new DbField[]{
        DbField.ID,
        DbField.SERIES_ID,
        DbField.CHAPTER_ID,
        DbField.TITLE,
        DbField.SEQUENCE_ID,
        DbField.RELEASE_DATE,
    });

    public static final DbTable PAGES = new DbTable("pages", new DbField[] {
        DbField.ID,
        DbField.CHAPTER_ID,
        DbField.PAGE_NUMBER,
        DbField.IMAGE
    });

    public static final DbTable CATEGORIES = new DbTable("categories", new DbField[] {
            DbField.ID,
            DbField.CATEGORY,
            DbField.SERIES_ID
    });

    public static final DbTable SERIES_FAVOURITES = new DbTable("favourite_series", new DbField[] {
            DbField.ID,
            DbField.SERIES_ID,
            DbField.FAVOURITE
    });

	public String name;
	public DbField[] fields;

	public DbTable(String n, DbField[] f) {
		name = n;
		fields = f;
	}

	@Override
	public String toString() {
		return name;
	}


	public String[] getFieldNames() {
		String[] results = new String[fields.length];

		for (int i = 0, size = fields.length; i < size; i++) {
			results[i] = fields[i].name;
		}

		return results;
	}

	public String dropSql() {
		return new StringBuilder().append("DROP TABLE ").append(name).toString();
	}

	public String createSql() {
		StringBuilder builder = new StringBuilder().append("CREATE TABLE ").append(name).append(" ").append("(");

		// Ensure that a comma does not appear on the last iteration
		String comma = "";
		for (DbField field : fields) {
			builder.append(comma);
			comma = ",";

			builder.append(field.name);
			builder.append(" ");
			builder.append(field.type);
			builder.append(" ");

			if (field.constraint != null) {
				builder.append(field.constraint);
			}
		}

		builder.append(")");

		return builder.toString();

	}
}
