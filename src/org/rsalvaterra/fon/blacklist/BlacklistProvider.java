package org.rsalvaterra.fon.blacklist;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.SystemClock;

public final class BlacklistProvider extends ContentProvider {

	private static final int MATCHES = 1;
	private static final int DATABASE_VERSION = 1;
	private static final int BLACKLIST_PERIOD = 300000; // Five minutes

	private static final String AUTHORITY = BlacklistProvider.class.getPackage().getName();
	private static final String TABLE_BLACKLIST = "blacklist";
	private static final String KEY_BSSID = "bssid";
	private static final String KEY_EXPIRY_TIME = "exptime";
	private static final String WHERE_CLAUSE = BlacklistProvider.KEY_BSSID + " = ?";

	private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	private static final Uri BLACKLIST_URI = Uri.parse(new StringBuilder().append("content://").append(BlacklistProvider.AUTHORITY).append('/').append(BlacklistProvider.TABLE_BLACKLIST).toString());

	static {
		BlacklistProvider.MATCHER.addURI(BlacklistProvider.AUTHORITY, BlacklistProvider.TABLE_BLACKLIST, BlacklistProvider.MATCHES);
	}

	private SQLiteOpenHelper helper;

	public static void addToBlacklist(final ContentResolver resolver, final String bssid) {
		final ContentValues values = new ContentValues();
		values.put(BlacklistProvider.KEY_EXPIRY_TIME, Long.valueOf(SystemClock.elapsedRealtime() + BlacklistProvider.BLACKLIST_PERIOD));
		if (resolver.update(BlacklistProvider.BLACKLIST_URI, values, BlacklistProvider.WHERE_CLAUSE, new String[] { bssid }) == 0) {
			values.put(BlacklistProvider.KEY_BSSID, bssid);
			resolver.insert(BlacklistProvider.BLACKLIST_URI, values);
		}
	}

	public static boolean isBlacklisted(final ContentResolver resolver, final String bssid) {
		boolean blacklisted = false;
		final Cursor cursor = resolver.query(BlacklistProvider.BLACKLIST_URI, new String[] { BlacklistProvider.KEY_BSSID, BlacklistProvider.KEY_EXPIRY_TIME }, BlacklistProvider.WHERE_CLAUSE, new String[] { bssid }, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				if (cursor.getLong(cursor.getColumnIndex(BlacklistProvider.KEY_EXPIRY_TIME)) > SystemClock.elapsedRealtime()) {
					blacklisted = true;
				} else {
					resolver.delete(BlacklistProvider.BLACKLIST_URI, BlacklistProvider.WHERE_CLAUSE, new String[] { bssid });
				}
			}
			cursor.close();
		}
		return blacklisted;
	}

	@Override
	public int delete(final Uri uri, final String whereClause, final String[] whereArgs) {
		if (BlacklistProvider.MATCHER.match(uri) == BlacklistProvider.MATCHES) {
			return helper.getWritableDatabase().delete(BlacklistProvider.TABLE_BLACKLIST, whereClause, whereArgs);
		}
		return 0;
	}

	@Override
	public String getType(final Uri arg0) {
		return null;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		if (BlacklistProvider.MATCHER.match(uri) == BlacklistProvider.MATCHES) {
			helper.getWritableDatabase().insert(BlacklistProvider.TABLE_BLACKLIST, null, values);
		}
		return null;
	}

	@Override
	public boolean onCreate() {
		helper = new SQLiteOpenHelper(getContext(), null, null, BlacklistProvider.DATABASE_VERSION) {

			@Override
			public void onCreate(final SQLiteDatabase db) {
				db.execSQL("CREATE TABLE " + BlacklistProvider.TABLE_BLACKLIST + " (_ID INTEGER PRIMARY KEY ASC, " + BlacklistProvider.KEY_BSSID + " TEXT UNIQUE NOT NULL, " + BlacklistProvider.KEY_EXPIRY_TIME + " LONG NOT NULL)");
			}

			@Override
			public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
				db.execSQL("DROP TABLE IF EXISTS " + BlacklistProvider.TABLE_BLACKLIST);
				onCreate(db);
			}

		};
		return false;
	}

	@Override
	public Cursor query(final Uri uri, final String[] columns, final String selection, final String[] selectionArgs, final String orderBy) {
		if (BlacklistProvider.MATCHER.match(uri) == BlacklistProvider.MATCHES) {
			return helper.getWritableDatabase().query(BlacklistProvider.TABLE_BLACKLIST, columns, selection, selectionArgs, null, null, orderBy);
		}
		return null;
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String whereClause, final String[] whereArgs) {
		if (BlacklistProvider.MATCHER.match(uri) == BlacklistProvider.MATCHES) {
			return helper.getWritableDatabase().update(BlacklistProvider.TABLE_BLACKLIST, values, whereClause, whereArgs);
		}
		return 0;
	}

}
