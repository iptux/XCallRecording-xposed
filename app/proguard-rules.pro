-repackageclasses net.iptux.xposed.callrecording

# remove debug logging
-assumenosideeffects class net.iptux.xposed.callrecording.Utility {
	*** d(...);
}
