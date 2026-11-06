package com.cloudstorage.navigation

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudstorage.core.designsystem.*

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Drive : BottomNavItem(Screen.Drive.route, "Drive", Icons.Filled.CloudQueue, Icons.Outlined.CloudQueue)
    object Shared : BottomNavItem(Screen.Shared.route, "Shared", Icons.Filled.Group, Icons.Outlined.Group)
    object Recent : BottomNavItem(Screen.Recent.route, "Recent", Icons.Filled.Schedule, Icons.Outlined.Schedule)
    object Starred : BottomNavItem(Screen.Starred.route, "Starred", Icons.Filled.Star, Icons.Outlined.StarBorder)
    object Storage : BottomNavItem(Screen.Storage.route, "Storage", Icons.Filled.Storage, Icons.Outlined.Storage)
}

@Composable
fun AppBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Drive,
        BottomNavItem.Shared,
        BottomNavItem.Recent,
        BottomNavItem.Starred,
        BottomNavItem.Storage
    )

    NavigationBar(
        containerColor = SurfaceDark,
        contentColor = TextPrimaryDark,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, BorderDark)
    ) {
        items.forEach { item ->
            val isSelected = currentRoute?.startsWith(item.route) == true

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = TextMutedDark,
                    unselectedTextColor = TextMutedDark,
                    indicatorColor = SurfaceElevatedDark
                )
            )
        }
    }
}

