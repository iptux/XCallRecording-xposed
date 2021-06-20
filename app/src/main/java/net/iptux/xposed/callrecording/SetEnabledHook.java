package net.iptux.xposed.callrecording;

class SetEnabledHook extends MethodHook {
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		boolean isEnabled = (boolean) param.args[0];
		OnStateChangeHook.sCallButtonFragment = isEnabled ? param.thisObject : null;
	}
}
