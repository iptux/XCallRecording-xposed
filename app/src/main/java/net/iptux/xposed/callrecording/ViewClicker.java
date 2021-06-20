package net.iptux.xposed.callrecording;

import android.view.View;

class ViewClicker implements Runnable {
	View view;
	String name;

	ViewClicker(View view, String name) {
		this.view = view;
		this.name = name;
	}

	@Override
	public void run() {
		if (!view.isEnabled()) {
			Utility.log("tried to click a disabled view: %s", name);
		}
		try {
			view.performClick();
			Utility.d("view %s clicked", name);
		}
		catch (Throwable e) {
			Utility.log(e);
		}
	}
}
