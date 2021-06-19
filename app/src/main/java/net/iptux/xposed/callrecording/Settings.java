package net.iptux.xposed.callrecording;

import de.robv.android.xposed.XSharedPreferences;

class Settings {
	private static final String PREF_RECORD_ENABLE = "record_enabled";
	private static final String PREF_FORCE_AUDIO_SOURCE = "force_audio_source";
	private static final String PREF_AAC_FORMAT = "aac_format";
	private static final String PREF_RECORD_INCOMING = "record_incoming";
	private static final String PREF_RECORD_OUTGOING = "record_outgoing";
	private static final String PREF_RECORD_DELAY2 = "record_delay2";
	private static final String PREF_PREPEND_CONTACT_NAME = "prepend_contact_name";
	private static final String PREF_SEPARATE_FOLDER = "separate_folder";
	static final String PREF_SKIP_MEDIA_SCAN = "skip_media_scan";
	static final String PREF_RECORDING_FOLDER = "recording_folder";
	static final String PREF_VERSION_NAME = "version_name";

	private static class SingletonHelper {
		private static final Settings INSTANCE = new Settings();
	}

	static Settings getInstance() {
		return SingletonHelper.INSTANCE;
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

	boolean isAACFormat() {
		return prefs.getBoolean(PREF_AAC_FORMAT, false);
	}

	boolean isRecordIncoming() {
		return prefs.getBoolean(PREF_RECORD_INCOMING, true);
	}

	boolean isRecordOutgoing() {
		return prefs.getBoolean(PREF_RECORD_OUTGOING, true);
	}

	int getRecordDelay() {
		int delay = 500;
		try {
			delay = Integer.parseInt(prefs.getString(PREF_RECORD_DELAY2, "500"));
		} catch (NumberFormatException ex) {
		}
		return delay;
	}

	boolean isPrependContactName() {
		return prefs.getBoolean(PREF_PREPEND_CONTACT_NAME, false);
	}

	boolean isSeparateFolder() {
		return prefs.getBoolean(PREF_SEPARATE_FOLDER, false);
	}
}
