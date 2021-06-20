package net.iptux.xposed.callrecording;

class IsEnabledHook extends MethodHook {
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Settings settings = getSettings();
		if (settings.isRecordEnable()) {
			param.setResult(Boolean.TRUE);
		}
	}
}
