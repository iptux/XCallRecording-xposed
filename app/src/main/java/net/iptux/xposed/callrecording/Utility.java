package net.iptux.xposed.callrecording;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

final class Utility {
	static final String TAG = "XCallRecording: ";

	static boolean isExternalStorageAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	static void showToast(Context context, CharSequence text) {
		Toast.makeText(context, TAG + text, Toast.LENGTH_LONG).show();
	}
}
