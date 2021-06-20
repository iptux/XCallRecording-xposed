package net.iptux.xposed.callrecording;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import java.io.File;

class GenerateFilenameHook extends MethodHook {
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		String result = (String) param.getResult();
		String trim = result.trim();
		Settings settings = getSettings();
		if (settings.isPrependContactName() || settings.isSeparateFolder()) {
			Context context = (Context) param.thisObject;
			String number = (String) param.args[0];
			String name = getContactName(context, number);
			if (!TextUtils.isEmpty(name)) {
				if (settings.isPrependContactName()) {
					trim = name + '_' + trim;
				}
				number = name;
			}
			if (settings.isSeparateFolder()) {
				File folder = new File(Utility.getRecordingFolder(), number);
				if (folder.exists() || folder.mkdirs()) {
					trim = number + File.separator + trim;
				}
			}
		}
		param.setResult(trim);
	}

	String getContactName(Context context, String number) {
		if (null == context || TextUtils.isEmpty(number)) {
			return null;
		}
		Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(lookupUri, new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
			if (null == cursor || cursor.getCount() == 0) {
				return null;
			}
			cursor.moveToNext();
			String name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
			return name;
		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}
	}
}
