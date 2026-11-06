package com.cloudstorage.feature.auth.google

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*
import com.cloudstorage.core.designsystem.components.GoogleSignInButton

@Composable
fun GoogleLoginScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToMfa: (mfaToken: String, email: String) -> Unit,
    viewModel: GoogleLoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is GoogleLoginUiState.Success -> onNavigateToMain()
            is GoogleLoginUiState.MfaRequired -> onNavigateToMfa(state.mfaToken, state.email ?: "")
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .background(SurfaceDark, RoundedCornerShape(16.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(SurfaceElevatedDark, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Zacode Cloud",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Welcome to Zacode Cloud",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    fontSize = 20.sp
                )

                Text(
                    text = "Sign in securely with your Google Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Error Banner if present
            if (uiState is GoogleLoginUiState.Error) {
                val errorMsg = (uiState as GoogleLoginUiState.Error).message
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .border(1.dp, AccentRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errorMsg,
                        color = AccentRed,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary Google Sign-In Action
            GoogleSignInButton(
                text = "Continue with Google",
                isLoading = uiState is GoogleLoginUiState.Loading,
                onClick = { viewModel.signInWithGoogle(context) }
            )

            // Security Trust Information
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevatedDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Protected with Google OAuth 2.0 & hardware-backed credential storage. No passwords required.",
                    fontSize = 11.sp,
                    color = TextMutedDark,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

