package com.cloudstorage.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudstorage.core.designsystem.PrimaryBlue
import com.cloudstorage.core.designsystem.TextMutedDark
import com.cloudstorage.core.designsystem.TextSecondaryDark
import com.cloudstorage.domain.model.Breadcrumb

@Composable
fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onBreadcrumbClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val subCrumbs = breadcrumbs.filter { it.id != null }
    val isRoot = subCrumbs.isEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "My Drive",
            fontSize = 13.sp,
            fontWeight = if (isRoot) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isRoot) PrimaryBlue else TextSecondaryDark,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onBreadcrumbClick(null) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )

        subCrumbs.forEachIndexed { index, crumb ->
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(16.dp)
            )

            val isLast = index == subCrumbs.lastIndex
            Text(
                text = crumb.name,
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isLast) PrimaryBlue else TextSecondaryDark,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBreadcrumbClick(crumb.id) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

