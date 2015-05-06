package org.rsalvaterra.fon;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.SystemClock;

public final class BlacklistProvider extends ContentProvider {

	private static final int BLACKLIST_PERIOD = 300000; // Five minutes

	private static final String TABLE_BLACKLIST = "blacklist";
	private static final String KEY_BSSID = "bssid";
	private static final String KEY_EXPIRY_TIME = "exptime";
	private static final String WHERE_CLAUSE = BlacklistProvider.KEY_BSSID + " = ?";

	private static final Uri BLACKLIST_URI = Uri.parse("content://" + Constants.APP_ID + '.' + BlacklistProvider.TABLE_BLACKLIST);

	private final SQLiteDatabase blacklist = SQLiteDatabase.create(null);

	{
		blacklist.execSQL("CREATE TABLE " + BlacklistProvider.TABLE_BLACKLIST + " (_ID INTEGER PRIMARY KEY ASC, " + BlacklistProvider.KEY_BSSID + " TEXT UNIQUE NOT NULL, " + BlacklistProvider.KEY_EXPIRY_TIME + " LONG NOT NULL)");
	}

	static void addToBlacklist(final ContentResolver resolver, final String bssid) {
		final ContentValues values = new ContentValues();
		values.put(BlacklistProvider.KEY_EXPIRY_TIME, Long.valueOf(SystemClock.elapsedRealtime() + BlacklistProvider.BLACKLIST_PERIOD));
		if (resolver.update(BlacklistProvider.BLACKLIST_URI, values, BlacklistProvider.WHERE_CLAUSE, new String[] { bssid }) == 0) {
			values.put(BlacklistProvider.KEY_BSSID, bssid);
			resolver.insert(BlacklistProvider.BLACKLIST_URI, values);
		}
	}

	static boolean isBlacklisted(final ContentResolver resolver, final String bssid) {
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
		return blacklist.delete(BlacklistProvider.TABLE_BLACKLIST, whereClause, whereArgs);
	}

	@Override
	public String getType(final Uri uri) {
		return null;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		blacklist.insert(BlacklistProvider.TABLE_BLACKLIST, null, values);
		return null;
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] columns, final String selection, final String[] selectionArgs, final String orderBy) {
		return blacklist.query(BlacklistProvider.TABLE_BLACKLIST, columns, selection, selectionArgs, null, null, orderBy);
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String whereClause, final String[] whereArgs) {
		return blacklist.update(BlacklistProvider.TABLE_BLACKLIST, values, whereClause, whereArgs);
	}

}
