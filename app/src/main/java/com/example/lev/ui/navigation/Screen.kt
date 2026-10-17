package com.example.lev.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")

    object Main : Screen("main")
}
