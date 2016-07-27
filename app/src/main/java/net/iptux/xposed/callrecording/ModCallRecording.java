package net.iptux.xposed.callrecording;

import android.content.Context;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ModCallRecording implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
	private static final String PACKAGE_NAME = ModCallRecording.class.getPackage().getName();

	private static final String PACKAGE_DIALER = "com.android.dialer";
	private static final String CALL_RECORDING_SERVICE = "com.android.services.callrecorder.CallRecorderService";

	private static final String PREF_RECORD_ENABLE = "record_enabled";

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (PACKAGE_DIALER.equals(resparam.packageName)) {
			XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
			if (prefs.getBoolean(PREF_RECORD_ENABLE, true)) {
				resparam.res.setReplacement(PACKAGE_DIALER, "bool", "call_recording_enabled", true);
			}
		}
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (PACKAGE_DIALER.equals(lpparam.packageName)) {
			XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME);
			final boolean isEnabled = prefs.getBoolean(PREF_RECORD_ENABLE, true);

			findAndHookMethod(CALL_RECORDING_SERVICE, lpparam.classLoader, "isEnabled", Context.class, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
					if (isEnabled) {
						param.setResult(Boolean.TRUE);
					}
				}
			});
		}
	}
}
