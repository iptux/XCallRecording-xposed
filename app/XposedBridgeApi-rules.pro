# proguard rules for XposedBridgeApi

-keepclasseswithmembers class * implements de.robv.android.xposed.IXposedHookCmdInit {
	public void initCmdApp(***);
}
-keepclasseswithmembers class * implements de.robv.android.xposed.IXposedHookInitPackageResources {
	public void handleInitPackageResources(***);
}
-keepclasseswithmembers class * implements de.robv.android.xposed.IXposedHookLoadPackage {
	public void handleLoadPackage(***);
}
-keepclasseswithmembers class * implements de.robv.android.xposed.IXposedHookZygoteInit {
	public void initZygote(***);
}
