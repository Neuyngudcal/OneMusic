package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.SurfaceActive
import com.example.onemusic.theme.TextPrimary

/**
 * OneMusic Apex Prism Ergonomic Slider with Pill track and tactile thumb
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApexSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    // Số nấc ở giữa (0 = trượt liên tục)
    steps: Int = 0,
    // Gọi khi thả tay – nơi nên lưu giá trị (tránh ghi bộ nhớ ở mỗi bước kéo)
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = Brand,
    inactiveColor: Color = SurfaceActive
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = AppTheme.colors.textPrimary,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveColor
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier
                    .height(6.dp)
                    .clip(PillShape),
                colors = SliderDefaults.colors(
                    activeTrackColor = activeColor,
                    inactiveTrackColor = inactiveColor
                )
            )
        }
    )
}

