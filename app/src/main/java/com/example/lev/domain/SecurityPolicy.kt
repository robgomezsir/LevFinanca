package com.example.lev.domain

data class SecurityPolicy(
    val lockAfterMinutes: Long,
    val biometricEnabled: Boolean,
    val hideSensitiveValues: Boolean,
) {
    val lockAfterSeconds: Long = lockAfterMinutes * 60L

    fun shouldLockAfterBackground(secondsInBackground: Long): Boolean = secondsInBackground >= lockAfterSeconds

    companion object {
        fun maskForLockScreen(value: String): String = if (value.isBlank()) "" else "R$ •••••••"
    }
}
