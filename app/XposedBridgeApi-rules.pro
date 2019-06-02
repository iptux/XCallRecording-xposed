# proguard rules for XposedBridgeApi

# keep entry classes for Xposed modules
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources {}
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {}
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit {}
-keep class * implements de.robv.android.xposed.IXposedMod {}
