package net.iptux.xposed.callrecording;

import de.robv.android.xposed.XSharedPreferences;

class Settings {
	private static final String PREF_RECORD_ENABLE = "record_enabled";
	private static final String PREF_FORCE_AUDIO_SOURCE = "force_audio_source";
	private static final String PREF_RECORD_INCOMING = "record_incoming";
	private static final String PREF_RECORD_OUTGOING = "record_outgoing";
	private static final String PREF_RECORD_DELAY = "record_delay";

	static Settings instance = null;
	static Settings getInstance() {
		if (null == instance) {
			instance = new Settings();
		}
		return instance;
	}

	XSharedPreferences prefs;
	private Settings() {
		prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
	}

	void reload() {
		prefs.reload();
	}

	boolean isRecordEnable() {
		return prefs.getBoolean(PREF_RECORD_ENABLE, true);
	}

	boolean forceAudioSource() {
		return prefs.getBoolean(PREF_FORCE_AUDIO_SOURCE, false);
	}

	boolean isRecordIncoming() {
		return prefs.getBoolean(PREF_RECORD_INCOMING, true);
	}

	boolean isRecordOutgoing() {
		return prefs.getBoolean(PREF_RECORD_OUTGOING, true);
	}

	int getRecordDelay() {
		int delay = 100;
		try {
			delay = Integer.parseInt(prefs.getString(PREF_RECORD_DELAY, "100"));
		} catch (NumberFormatException ex) {
		}
		return delay;
	}
}
