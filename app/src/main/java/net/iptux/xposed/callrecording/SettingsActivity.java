package net.iptux.xposed.callrecording;

import android.content.pm.PackageManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.preference.SwitchPreference;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
	static final int REQUEST_STORAGE_PERMISSION = 0x10ae;

	SwitchPreference mPrefSkipMediaScan;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);

		findPreference(Settings.PREF_VERSION_NAME).setSummary(BuildConfig.VERSION_NAME);
		mPrefSkipMediaScan = (SwitchPreference) findPreference(Settings.PREF_SKIP_MEDIA_SCAN);
		mPrefSkipMediaScan.setOnPreferenceChangeListener(this);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
		case REQUEST_STORAGE_PERMISSION:
			if (grantResults.length > 0 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
				Utility.setRecordingSkipMediaScan(mPrefSkipMediaScan.isChecked());
			}
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		switch (key) {
		case Settings.PREF_SKIP_MEDIA_SCAN:
			if (Utility.checkStoragePermission(this, REQUEST_STORAGE_PERMISSION)) {
				Utility.setRecordingSkipMediaScan((Boolean) newValue);
			}
			break;
		default:
			return false;
		}
		return true;
	}
}
