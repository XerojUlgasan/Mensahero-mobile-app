package com.example.mensahero_mobile_app.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Login : Routes("login")
    data object ServerConnection : Routes("server_connection")
    data object Dashboard : Routes("dashboard")
    data object Keys : Routes("keys")
    data object Settings : Routes("settings")
}
