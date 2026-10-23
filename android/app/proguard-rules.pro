# Proguard rules for Zhi
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
}

# DataStore and backup JSON store these enum names and read them back with valueOf.
-keep enum com.zhi.app.data.preferences.ServerMode { *; }
-keep enum com.zhi.app.data.preferences.ThemeMode { *; }
