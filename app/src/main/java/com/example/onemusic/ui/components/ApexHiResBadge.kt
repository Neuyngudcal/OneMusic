package com.example.onemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.HiResGoldGradient
import com.example.onemusic.theme.CharcoalBlack

/**
 * OneMusic Apex Hi-Res Audio Gold Metallic Badge
 * Synchronized from Japan Audio Society (JAS) Hi-Res Gold standard.
 */
@Composable
fun ApexHiResBadge(
    modifier: Modifier = Modifier,
    text: String = "HI-RES"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(HiResGoldGradient)
            .padding(horizontal = 4.5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CharcoalBlack,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        )
    }
}
