package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.PillShape

/**
 * OneMusic Apex Prism Glassmorphic Confirmation Dialog
 * - 2.5D Specular light reflection border (ApexReflectiveBorderBrush)
 * - Deep 16dp AMOLED glass shadow and 28dp smooth squircle corners
 * - Tactile spring-bounce Pill action buttons
 */
@Composable
fun ApexConfirmDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    dismissButtonText: String = "Hủy",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ApexDialogContainer(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
                // 1. Dialog Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        lineHeight = 26.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Dialog Message Body
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Action Buttons Row (Pill-shaped with spring bounce & haptic feedback)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dismiss Button (Subtle Glass Pill)
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(AppTheme.colors.surfaceControl)
                            .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onDismiss()
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Confirm Button (Solid Pill: ApexRose for destructive, Brand for normal)
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isDestructive) AppTheme.colors.danger else AppTheme.colors.textPrimary)
                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                onConfirm()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isDestructive) AppTheme.colors.onAccent else AppTheme.colors.onInverse,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
}
