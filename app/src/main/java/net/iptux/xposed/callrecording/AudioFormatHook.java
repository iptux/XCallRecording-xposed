package net.iptux.xposed.callrecording;

class AudioFormatHook extends MethodHook {
	Object result;

	AudioFormatHook(Object result) {
		this.result = result;
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Settings settings = getSettings();
		if (settings.isAACFormat()) {
			param.setResult(this.result);
		}
	}
}
