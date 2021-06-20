package net.iptux.xposed.callrecording;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.XposedBridge;

final class Utility {
	static final String TAG = "XCallRecording: ";

	// folder name on sdcard, const as it's set in CallRecorderService
	static final String RECORDING_FOLDER = "CallRecordings";
	static final String RECORDING_FOLDER_LOS17 = "Call Recordings";

	static final String NOMEDIA = ".nomedia";

	static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

	static void d(String fmt, Object... args) {
		if (BuildConfig.DEBUG) {
			log(fmt, args);
		}
	}

	static void log(String fmt, Object... args) {
		XposedBridge.log(TAG + String.format(fmt, args));
	}

	static void log(Throwable throwable) {
		XposedBridge.log(throwable);
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
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
			return Environment.getExternalStoragePublicDirectory(RECORDING_FOLDER);
		} else {
			File music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
			return new File(music, RECORDING_FOLDER_LOS17);
		}
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

	static void openRecordingFolder(Context context) {
		File file = getRecordingFolder();
		showToast(context, file.toString());
		if (openFolder(context, getDocumentUri(file))) {
			return;
		}
		if (openFolder(context, Uri.fromFile(file))) {
			return;
		}
	}

	static boolean openFolder(Context context, Uri uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
		if (intent.resolveActivity(context.getPackageManager()) == null) {
			return false;
		}
		context.startActivity(intent);
		return true;
	}

	static Uri getDocumentUri(File file) {
		String storage = Environment.getExternalStorageDirectory().toString();
		String documentId = file.toString();
		if (documentId.startsWith(storage)) {
			int offset = storage.length();
			if (!storage.endsWith(File.separator)) {
				offset += File.separator.length();
			}
			documentId = "primary:" + documentId.substring(offset);
		}
		return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, documentId);
	}
}
