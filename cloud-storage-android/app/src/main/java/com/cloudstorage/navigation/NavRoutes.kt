package com.cloudstorage.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object MfaVerify : Screen("mfa_verify/{mfaToken}/{email}") {
        fun createRoute(mfaToken: String, email: String) = "mfa_verify/$mfaToken/$email"
    }
    object Main : Screen("main")
    object Drive : Screen("drive?folderId={folderId}") {
        fun createRoute(folderId: String? = null) = if (folderId != null) "drive?folderId=$folderId" else "drive"
    }
    object Shared : Screen("shared")
    object Recent : Screen("recent")
    object Starred : Screen("starred")
    object Trash : Screen("trash")
    object Storage : Screen("storage")
    object Search : Screen("search")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Preview : Screen("preview/{fileId}") {
        fun createRoute(fileId: String) = "preview/$fileId"
    }
}

