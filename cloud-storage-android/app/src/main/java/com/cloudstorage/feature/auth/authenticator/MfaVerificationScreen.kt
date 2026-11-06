package com.cloudstorage.feature.auth.authenticator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudstorage.core.designsystem.*

@Composable
fun MfaVerificationScreen(
    onNavigateToMain: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MfaVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val code by viewModel.code.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is MfaVerificationUiState.Success) {
            onNavigateToMain()
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentEmerald.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, AccentEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Two-Factor Authentication",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    fontSize = 19.sp
                )

                if (viewModel.email.isNotEmpty()) {
                    Text(
                        text = "Akun: ${viewModel.email}",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Text(
                    text = "Masukkan 6-digit kode dari Google Authenticator atau salah satu Kode Cadangan (Backup Code) Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }

            // Error Message
            if (uiState is MfaVerificationUiState.Error) {
                val errorMsg = (uiState as MfaVerificationUiState.Error).message
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

            // Code Input
            OutlinedTextField(
                value = code,
                onValueChange = { viewModel.onCodeChanged(it) },
                placeholder = {
                    Text(
                        text = "123456 atau 58B5-A215",
                        color = TextMutedDark,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceElevatedDark,
                    unfocusedContainerColor = SurfaceElevatedDark,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Submit Button
            Button(
                onClick = { viewModel.verify() },
                enabled = uiState !is MfaVerificationUiState.Loading && code.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BackgroundDark,
                    disabledContainerColor = Color.White.copy(alpha = 0.4f),
                    disabledContentColor = BackgroundDark.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState is MfaVerificationUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BackgroundDark,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Verifikasi & Masuk",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Back button
            TextButton(
                onClick = onNavigateBack,
                colors = ButtonDefaults.textButtonColors(contentColor = TextMutedDark)
            ) {
                Text(text = "Batalkan dan kembali ke menu login", fontSize = 12.sp)
            }
        }
    }
}

