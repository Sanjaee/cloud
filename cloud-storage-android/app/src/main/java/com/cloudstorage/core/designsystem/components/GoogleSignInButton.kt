package com.cloudstorage.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cloudstorage.core.designsystem.*

@Composable
fun GoogleSignInButton(
    text: String = "Login with Google",
    isLoading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White.copy(alpha = 0.6f),
            disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF1F1F1F),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Connecting...",
                fontSize = 15.sp,
                color = Color(0xFF1F1F1F)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Authentic 4-color Google "G" Icon drawn cleanly with Canvas
                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val stroke = size * 0.22f
        val radius = (size - stroke) / 2f
        val center = Offset(size / 2f, size / 2f)

        // Draw Google G arcs: Red (top), Yellow (bottom-left), Green (bottom-right), Blue (middle-right)
        // Red top arc: 220 to 330 deg
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 195f,
            sweepAngle = 135f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size - stroke, size - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Yellow bottom-left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size - stroke, size - stroke),
            style = Stroke(width = stroke)
        )
        // Green bottom-right arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 30f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size - stroke, size - stroke),
            style = Stroke(width = stroke)
        )
        // Blue horizontal bar and right arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size - stroke, size - stroke),
            style = Stroke(width = stroke)
        )

        // Blue horizontal cross bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x - 2f, center.y),
            end = Offset(size - stroke / 2, center.y),
            strokeWidth = stroke
        )
    }
}

