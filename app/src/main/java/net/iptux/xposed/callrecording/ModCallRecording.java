package net.iptux.xposed.callrecording;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.view.View;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModCallRecording implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
	private static final String PACKAGE_DIALER = "com.android.dialer";
	private static final String CALL_RECORDING_SERVICE = "com.android.services.callrecorder.CallRecorderService";
	private static final String CALL_BUTTON_PRESENTER = "com.android.incallui.CallButtonPresenter";
	private static final String CALL_BUTTON_FRAGMENT = "com.android.incallui.CallButtonFragment";

	private static final String CALL_STATE_INCALL = "INCALL";
	private static final String CALL_STATE_INCOMING = "INCOMING";
	private static final String CALL_STATE_OUTGOING = "OUTGOING";

	private static String sCallingState = "";
	private static boolean sRecordIncoming = false;
	private static boolean sRecordOutgoing = false;
	private static String sRecordButtonFieldName = null;
	private static Settings sSettings = Settings.getInstance();

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (PACKAGE_DIALER.equals(resparam.packageName)) {
			resparam.res.setReplacement(PACKAGE_DIALER, "bool", "call_recording_enabled", true);
			resparam.res.setReplacement(PACKAGE_DIALER, "integer", "call_recording_audio_source", MediaRecorder.AudioSource.VOICE_CALL);
		}
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (PACKAGE_DIALER.equals(lpparam.packageName)) {
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
					param.setResult(trim);
				}
			});
			final Class<?> CallButtonPresenter = XposedHelpers.findClass(CALL_BUTTON_PRESENTER, lpparam.classLoader);
			XposedBridge.hookAllMethods(CallButtonPresenter, "onStateChange", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					updateCallState(param.args[0]);
				}
			});

			int version = Build.VERSION.SDK_INT;
			if (version >= 24) {
				// not support yet
			} else if (version >= Build.VERSION_CODES.LOLLIPOP) {
				sRecordButtonFieldName = "mCallRecordButton";
			} else if (version >= Build.VERSION_CODES.KITKAT) {
				sRecordButtonFieldName = "mRecordButton";
			} else {
				// not support
			}

			final Class<?> CallButtonFragment = XposedHelpers.findClass(CALL_BUTTON_FRAGMENT, lpparam.classLoader);
			// CallButtonFragment.setEnabled() is called frequently, start recording here is a good choice
			XposedBridge.hookAllMethods(CallButtonFragment, "setEnabled", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					sSettings.reload();
					if (sRecordIncoming && sSettings.isRecordIncoming()) {
						startRecordingByClickView(param.thisObject, sRecordButtonFieldName, sSettings.getRecordDelay());
						sRecordIncoming = false;
					}
					if (sRecordOutgoing && sSettings.isRecordOutgoing()) {
						startRecordingByClickView(param.thisObject, sRecordButtonFieldName, sSettings.getRecordDelay());
						sRecordOutgoing = false;
					}
				}
			});
		}
	}

	void updateCallState(Object state) {
		String newState = state.toString();
		if (sCallingState.equals(newState))
			return;

		if (CALL_STATE_INCALL.equals(newState)) {
			if (CALL_STATE_INCOMING.equals(sCallingState)) {
				sRecordIncoming = true;
			} else if (CALL_STATE_OUTGOING.equals(sCallingState)) {
				sRecordOutgoing = true;
			}
		}
		sCallingState = newState;
	}

	void startRecordingByClickView(Object obj, String name, long delayMillis) throws Throwable {
		final View view = (View) getObjectField(obj, name);
		if (null == view)
			return;

		view.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					view.performClick();
				}
				catch (Throwable e) {
					XposedBridge.log(e);
				}
			}
		}, delayMillis);
	}
}
