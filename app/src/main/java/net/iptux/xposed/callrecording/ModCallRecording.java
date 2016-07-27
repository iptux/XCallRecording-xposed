package net.iptux.xposed.callrecording;

import android.content.Context;
import android.media.MediaRecorder;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModCallRecording implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
	private static final String PACKAGE_NAME = ModCallRecording.class.getPackage().getName();

	private static final String PACKAGE_DIALER = "com.android.dialer";
	private static final String CALL_RECORDING_SERVICE = "com.android.services.callrecorder.CallRecorderService";
	private static final String CALL_BUTTON_PRESENTER = "com.android.incallui.CallButtonPresenter";

	private static final String PREF_RECORD_ENABLE = "record_enabled";
	private static final String PREF_FORCE_AUDIO_SOURCE = "force_audio_source";

	private static final String CALL_STATE_INCALL = "INCALL";
	private static final String CALL_STATE_INCOMING = "INCOMING";
	private static final String CALL_STATE_OUTGOING = "OUTGOING";

	private static String sCallingState = "";
	private static boolean sRecordIncoming = false;
	private static boolean sRecordOutgoing = false;

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (PACKAGE_DIALER.equals(resparam.packageName)) {
			XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
			if (!prefs.getBoolean(PREF_RECORD_ENABLE, true)) {
				return;
			}

			resparam.res.setReplacement(PACKAGE_DIALER, "bool", "call_recording_enabled", true);
			if (prefs.getBoolean(PREF_FORCE_AUDIO_SOURCE, false)) {
				resparam.res.setReplacement(PACKAGE_DIALER, "integer", "call_recording_audio_source", MediaRecorder.AudioSource.VOICE_CALL);
			}
		}
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (PACKAGE_DIALER.equals(lpparam.packageName)) {
			XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
			if (!prefs.getBoolean(PREF_RECORD_ENABLE, true)) {
				return;
			}

			final boolean force_audio_source = prefs.getBoolean(PREF_FORCE_AUDIO_SOURCE, false);

			findAndHookMethod(CALL_RECORDING_SERVICE, lpparam.classLoader, "isEnabled", Context.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					param.setResult(Boolean.TRUE);
				}
			});
			findAndHookMethod(CALL_RECORDING_SERVICE, lpparam.classLoader, "getAudioSource", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					if (force_audio_source) {
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
}
