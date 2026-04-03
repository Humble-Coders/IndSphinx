package com.humblesolutions.indsphinx.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object ResidentialForm : Screen("residential_form")
    data object Home : Screen("home")
}
