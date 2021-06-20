package net.iptux.xposed.callrecording;

import de.robv.android.xposed.XC_MethodHook;

class MethodHook extends XC_MethodHook {
	static Settings getSettings() {
		Settings settings = Settings.getInstance();
		settings.reload();
		return settings;
	}
}
