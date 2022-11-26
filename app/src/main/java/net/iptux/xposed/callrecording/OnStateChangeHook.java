package net.iptux.xposed.callrecording;

import android.os.Build;
import android.view.View;

import static de.robv.android.xposed.XposedHelpers.getObjectField;

class OnStateChangeHook extends MethodHook {
	static final String CALL_STATE_NO_CALLS = "NO_CALLS";
	static final String CALL_STATE_INCALL = "INCALL";
	static final String CALL_STATE_INCOMING = "INCOMING";
	static final String CALL_STATE_OUTGOING = "OUTGOING";
	static final String CALL_STATE_PENDING_OUTGOING = "PENDING_OUTGOING";

	static Object sCallButtonFragment = null;

	final String recordButtonFieldName;
	final int callingStateParamIndex;

	String callingState = CALL_STATE_NO_CALLS;
	boolean recordIncoming = false;
	boolean recordOutgoing = false;

	OnStateChangeHook() {
		final int version = Build.VERSION.SDK_INT;

		if (version >= Build.VERSION_CODES.O) {
			recordButtonFieldName = "button";
		} else if (version >= Build.VERSION_CODES.N_MR1) {
			recordButtonFieldName = "mCallRecordButton";
		} else if (version >= Build.VERSION_CODES.N) {
			recordButtonFieldName = "mRecordButton";
		} else if (version >= Build.VERSION_CODES.LOLLIPOP) {
			recordButtonFieldName = "mCallRecordButton";
		} else if (version >= Build.VERSION_CODES.KITKAT) {
			recordButtonFieldName = "mRecordButton";
		} else {
			// not support
			recordButtonFieldName = null;
		}

		if (version >= Build.VERSION_CODES.LOLLIPOP) {
			callingStateParamIndex = 1;
		} else {
			callingStateParamIndex = 0;
		}
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		updateCallState(param.args[callingStateParamIndex]);
	}

	void updateCallState(Object state) throws Throwable {
		String newState = state.toString();
		if (callingState.equals(newState)) {
			return;
		}

		Utility.d("updateCallState: %s -> %s", callingState, newState);
		if (CALL_STATE_INCALL.equals(newState)) {
			if (CALL_STATE_INCOMING.equals(callingState)) {
				recordIncoming = true;
			} else if (CALL_STATE_OUTGOING.equals(callingState)) {
				recordOutgoing = true;
			} else if (CALL_STATE_NO_CALLS.equals(callingState)) {
				// NO_CALLS -> INCALL, must be incoming call
				recordIncoming = true;
			} else if (CALL_STATE_PENDING_OUTGOING.equals(callingState)) {
				// PENDING_OUTGOING -> INCALL
				recordOutgoing = true;
			} else {
				Utility.log("unexpected state: %s -> %s", callingState, newState);
				Utility.log("if you see this, please report to developer");
			}
			startRecordingOnDemand(sCallButtonFragment);
		} else if (CALL_STATE_NO_CALLS.equals(newState)) {
			recordIncoming = false;
			recordOutgoing = false;
		}
		callingState = newState;
	}

	void startRecordingOnDemand(Object obj) throws Throwable {
		if (null == obj) {
			return;
		}

		Settings settings = getSettings();
		if (recordIncoming && settings.isRecordIncoming()) {
			clickView(obj, recordButtonFieldName, settings.getRecordDelay());
			recordIncoming = false;
		}
		if (recordOutgoing && settings.isRecordOutgoing()) {
			clickView(obj, recordButtonFieldName, settings.getRecordDelay());
			recordOutgoing = false;
		}
	}

	void clickView(Object obj, String name, long delayMillis) throws Throwable {
		View view = (View) getObjectField(obj, name);
		Utility.d("clickView: name=%s, delayMillis=%d", name, delayMillis);
		if (null == view) {
			return;
		}

		if (!Utility.isExternalStorageAvailable()) {
			String warning = "External storage not available, recording may not start!";
			Utility.showToast(view.getContext(), warning);
		}

		view.postDelayed(new ViewClicker(view, name), delayMillis);
	}
}
