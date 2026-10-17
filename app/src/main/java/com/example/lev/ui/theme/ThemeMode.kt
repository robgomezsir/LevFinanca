package com.example.lev.ui.theme

/** Modo de tema persistido em SystemSetting ("theme_mode"). */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Aparência dos cards persistida em SystemSetting ("appearance"). */
enum class Appearance(val key: String) {
    SOLID("solid"),
    GLASS("glass"),
    ;

    companion object {
        fun fromKey(key: String?): Appearance = entries.firstOrNull { it.key == key } ?: SOLID
    }
}

const val THEME_MODE_KEY = "theme_mode"
const val APPEARANCE_KEY = "appearance"
const val DYNAMIC_COLOR_KEY = "dynamic_color"
