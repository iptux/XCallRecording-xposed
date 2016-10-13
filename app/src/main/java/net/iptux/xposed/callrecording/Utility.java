package net.iptux.xposed.callrecording;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import de.robv.android.xposed.XposedBridge;

final class Utility {
	static final String TAG = "XCallRecording: ";

	static void d(String fmt, Object... args) {
		if (BuildConfig.DEBUG) {
			XposedBridge.log(TAG + String.format(fmt, args));
		}
	}

	static boolean isExternalStorageAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	static void showToast(Context context, CharSequence text) {
		Toast.makeText(context, TAG + text, Toast.LENGTH_LONG).show();
	}
}
