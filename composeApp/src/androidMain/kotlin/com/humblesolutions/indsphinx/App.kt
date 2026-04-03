package com.humblesolutions.indsphinx

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.humblesolutions.indsphinx.navigation.Screen
import com.humblesolutions.indsphinx.ui.HomeScreen
import com.humblesolutions.indsphinx.ui.LoginScreen
import com.humblesolutions.indsphinx.ui.ResidentialFormScreen
import com.humblesolutions.indsphinx.ui.SplashScreen
import com.humblesolutions.indsphinx.viewmodel.AuthViewModel

enum class SplashDestination { NOT_LOGGED_IN, NEEDS_AGREEMENT, HOME }

@Composable
@Preview
fun App() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashComplete = { destination ->
                val route = when (destination) {
                    SplashDestination.NOT_LOGGED_IN -> Screen.Login.route
                    SplashDestination.NEEDS_AGREEMENT -> Screen.ResidentialForm.route
                    SplashDestination.HOME -> Screen.Home.route
                }
                navController.navigate(route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Login.route) {
            val viewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = viewModel,
                onAuthSuccess = { needsAgreement ->
                    val route = if (needsAgreement) Screen.ResidentialForm.route else Screen.Home.route
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ResidentialForm.route) {
            ResidentialFormScreen(
                onFormComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ResidentialForm.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
