package net.iptux.xposed.callrecording;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.SwitchPreference;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);

		findPreference(Settings.PREF_VERSION_NAME).setSummary(BuildConfig.VERSION_NAME);
		findPreference(Settings.PREF_SKIP_MEDIA_SCAN).setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		switch (key) {
		case Settings.PREF_SKIP_MEDIA_SCAN:
			Utility.setRecordingSkipMediaScan((Boolean) newValue);
			break;
		default:
			return false;
		}
		return true;
	}
}
