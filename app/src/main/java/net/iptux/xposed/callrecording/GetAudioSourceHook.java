package net.iptux.xposed.callrecording;

import android.media.MediaRecorder;

class GetAudioSourceHook extends MethodHook {
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Settings settings = getSettings();
		if (settings.forceAudioSource()) {
			param.setResult(MediaRecorder.AudioSource.VOICE_CALL);
		}
	}
}
