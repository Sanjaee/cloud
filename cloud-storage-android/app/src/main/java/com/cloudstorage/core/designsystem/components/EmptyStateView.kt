package com.cloudstorage.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudstorage.core.designsystem.TextMutedDark
import com.cloudstorage.core.designsystem.TextPrimaryDark

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.FolderOpen,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                fontSize = 16.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedDark,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

