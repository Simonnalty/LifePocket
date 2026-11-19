# 口袋管家 minify 规则
# Room 自带 consumer rules；Coil 自带 rules。这里只做兜底。
-keep class com.simon.lifepocket.data.** { *; }
-dontwarn org.slf4j.**
