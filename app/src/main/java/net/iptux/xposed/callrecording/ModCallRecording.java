package net.iptux.xposed.callrecording;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;

import java.io.File;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModCallRecording implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
	private static final String PACKAGE_DIALER = "com.android.dialer";
	private static final String CALL_RECORDING_SERVICE = "com.android.services.callrecorder.CallRecorderService";
	private static final String CALL_BUTTON_PRESENTER = "com.android.incallui.CallButtonPresenter";
	private static final String CALL_BUTTON_FRAGMENT = "com.android.incallui.CallButtonFragment";

	private static final String CALL_STATE_NO_CALLS = "NO_CALLS";
	private static final String CALL_STATE_INCALL = "INCALL";
	private static final String CALL_STATE_INCOMING = "INCOMING";
	private static final String CALL_STATE_OUTGOING = "OUTGOING";

	private static Object sCallButtonFragment = null;
	private static int sCallingStateParamIndex = 1;
	private static String sCallingState = CALL_STATE_NO_CALLS;
	private static boolean sRecordIncoming = false;
	private static boolean sRecordOutgoing = false;
	private static String sRecordButtonFieldName = null;
	private static Settings sSettings = Settings.getInstance();

	/* 	This part came from https://github.com/pylerSM/EnableCMCallRecording
		It enables the menu: 3-dot->Settings->Calls->Call Recording Format
		On Samsung Note 3 (SM-N900T) The default 'AMR-WB' format is broken,
		'AAC' needs to be selected to record on the SM-N900T.	*/
	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam)
			throws Throwable {
		if (!PACKAGE_DIALER.equals(resparam.packageName)) {
			return;
		}
		resparam.res.setReplacement(PACKAGE_DIALER, "bool", "call_recording_enabled",
				true);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (PACKAGE_DIALER.equals(lpparam.packageName)) {
			Utility.d("handleLoadPackage: packageName=%s", lpparam.packageName);
			findAndHookMethod(CALL_RECORDING_SERVICE, lpparam.classLoader, "isEnabled", Context.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					sSettings.reload();
					param.setResult(sSettings.isRecordEnable() ? Boolean.TRUE : Boolean.FALSE);
				}
			});
			findAndHookMethod(CALL_RECORDING_SERVICE, lpparam.classLoader, "getAudioSource", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					sSettings.reload();
					if (sSettings.forceAudioSource()) {
						param.setResult(MediaRecorder.AudioSource.VOICE_CALL);
					}
				}
			});
			findAndHookMethod(CALL_RECORDING_SERVICE, lpparam.classLoader, "generateFilename", String.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					String result = (String) param.getResult();
					String trim = result.trim();
					sSettings.reload();
					if (sSettings.isPrependContactName() || sSettings.isSeparateFolder()) {
						Context context = (Context) param.thisObject;
						String number = (String) param.args[0];
						String name = getContactName(context, number);
						if (!TextUtils.isEmpty(name)) {
							if (sSettings.isPrependContactName()) {
								trim = name + '_' + trim;
							}
							number = name;
						}
						if (sSettings.isSeparateFolder()) {
							File folder = new File(Utility.getRecordingFolder(), number);
							if (folder.exists() || folder.mkdirs()) {
								trim = number + File.separator + trim;
							}
						}
					}
					param.setResult(trim);
				}
			});

			final Class<?> CallButtonPresenter = XposedHelpers.findClass(CALL_BUTTON_PRESENTER, lpparam.classLoader);
			XposedBridge.hookAllMethods(CallButtonPresenter, "onStateChange", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					updateCallState(param.args[sCallingStateParamIndex]);
				}
			});

			int version = Build.VERSION.SDK_INT;
			if (version >= 25) {
				// not support yet
			} else if (version >= Build.VERSION_CODES.N) {
				sRecordButtonFieldName = "mRecordButton";
			} else if (version >= Build.VERSION_CODES.LOLLIPOP) {
				sRecordButtonFieldName = "mCallRecordButton";
			} else if (version >= Build.VERSION_CODES.KITKAT) {
				sRecordButtonFieldName = "mRecordButton";
				sCallingStateParamIndex = 0;
			} else {
				// not support
			}

			final Class<?> CallButtonFragment = XposedHelpers.findClass(CALL_BUTTON_FRAGMENT, lpparam.classLoader);
			// CallButtonFragment.setEnabled() is called frequently, start recording here is a good choice
			XposedBridge.hookAllMethods(CallButtonFragment, "setEnabled", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					boolean isEnabled = (boolean) param.args[0];
					sCallButtonFragment = isEnabled ? param.thisObject : null;
				}
			});
		}
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

	void updateCallState(Object state) throws Throwable {
		String newState = state.toString();
		if (sCallingState.equals(newState))
			return;

		Utility.d("updateCallState: %s -> %s", sCallingState, newState);
		if (CALL_STATE_INCALL.equals(newState)) {
			if (CALL_STATE_INCOMING.equals(sCallingState)) {
				sRecordIncoming = true;
			} else if (CALL_STATE_OUTGOING.equals(sCallingState)) {
				sRecordOutgoing = true;
			} else if (CALL_STATE_NO_CALLS.equals(sCallingState)) {
				// NO_CALLS -> INCALL, must be incoming call
				sRecordIncoming = true;
			} else {
				Utility.log("unexpected state: %s -> %s", sCallingState, newState);
				Utility.log("if you see this, please report to developer");
			}
			startRecordingOnDemand(sCallButtonFragment);
		} else if (CALL_STATE_NO_CALLS.equals(newState)) {
			sRecordIncoming = false;
			sRecordOutgoing = false;
		}
		sCallingState = newState;
	}

	void startRecordingOnDemand(Object obj) throws Throwable {
		if (null == obj) {
			return;
		}
		sSettings.reload();
		if (sRecordIncoming && sSettings.isRecordIncoming()) {
			clickView(obj, sRecordButtonFieldName, sSettings.getRecordDelay());
			sRecordIncoming = false;
		}
		if (sRecordOutgoing && sSettings.isRecordOutgoing()) {
			clickView(obj, sRecordButtonFieldName, sSettings.getRecordDelay());
			sRecordOutgoing = false;
		}
	}

	void clickView(Object obj, String name, long delayMillis) throws Throwable {
		View view = (View) getObjectField(obj, name);
		Utility.d("clickView: name=%s, delayMillis=%d", name, delayMillis);
		if (null == view)
			return;

		if (!Utility.isExternalStorageAvailable()) {
			String warning = "External storage not available, recording may not start!";
			Utility.showToast(view.getContext(), warning);
		}

		view.postDelayed(new ViewClicker(view, name), delayMillis);
	}

	class ViewClicker implements Runnable {
		View view;
		String name;
		ViewClicker(View view, String name) {
			this.view = view;
			this.name = name;
		}

		@Override
		public void run() {
			if (!view.isEnabled()) {
				Utility.log("tried to click a disabled view: %s", name);
			}
			try {
				view.performClick();
				Utility.d("view %s clicked", name);
			}
			catch (Throwable e) {
				XposedBridge.log(e);
			}
		}
	}
}
