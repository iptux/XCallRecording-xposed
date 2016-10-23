package net.iptux.xposed.callrecording;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.XposedBridge;

final class Utility {
	static final String TAG = "XCallRecording: ";

	// folder name on sdcard, const as it's set in CallRecorderService
	static final String RECORDING_FOLDER = "CallRecordings";

	static final String NOMEDIA = ".nomedia";

	static void d(String fmt, Object... args) {
		if (BuildConfig.DEBUG) {
			log(fmt, args);
		}
	}

	static void log(String fmt, Object... args) {
		XposedBridge.log(TAG + String.format(fmt, args));
	}

	static boolean isExternalStorageAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	static void showToast(Context context, CharSequence text) {
		Toast.makeText(context, TAG + text, Toast.LENGTH_LONG).show();
	}

	static void showToast(Context context, int resId) {
		Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
	}

	static boolean checkStoragePermission(Activity activity, int requestCode) {
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
			// no need to check
			return true;
		}
		final String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
		if (PackageManager.PERMISSION_GRANTED != activity.checkSelfPermission(permission)) {
			if (!activity.shouldShowRequestPermissionRationale(permission)) {
				activity.requestPermissions(new String[] {permission}, requestCode);
			} else {
				showToast(activity, R.string.warning_storage_permission);
			}
			return false;
		} else {
			return true;
		}
	}

	static File getRecordingFolder() {
		return Environment.getExternalStoragePublicDirectory(RECORDING_FOLDER);
	}

	static boolean setRecordingSkipMediaScan(boolean skip) {
		return setSkipMediaScan(getRecordingFolder(), skip);
	}

	static boolean setSkipMediaScan(File folder, boolean skip) {
		if (null == folder || !folder.isDirectory()) {
			return false;
		}
		File nomedia = new File(folder, NOMEDIA);
		try {
			if (skip) {
				if (!nomedia.exists()) {
					nomedia.createNewFile();
				}
			} else {
				if (nomedia.exists()) {
					nomedia.delete();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return skip ^ !nomedia.exists();
	}
}
