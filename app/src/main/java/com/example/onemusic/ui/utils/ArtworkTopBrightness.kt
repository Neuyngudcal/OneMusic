package com.example.onemusic.ui.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val topBrightnessCache = LruCache<String, Boolean>(60)

// Phần trên của ảnh nằm dưới thanh trạng thái + nút Quay lại / Chia sẻ (ảnh vuông, tràn ngang)
private const val TOP_SAMPLE_FRACTION = 0.25f
// Độ sáng tương đối (WCAG, thang tuyến tính) mà tại đó icon đen và trắng tương phản ngang nhau:
// (L + 0.05) / 0.05 == 1.05 / (L + 0.05) → L ≈ 0.179. Trên mức này icon đen dễ đọc hơn.
// Lưu ý: thang tuyến tính nên màu "nhìn sáng" (vàng nhạt, xanh trời, pastel) chỉ khoảng 0.3–0.5.
private const val LIGHT_LUMINANCE_THRESHOLD = 0.179

private fun isTopAreaLight(bitmap: Bitmap): Boolean {
    val rows = (bitmap.height * TOP_SAMPLE_FRACTION).toInt().coerceAtLeast(1)
    var sum = 0.0
    var count = 0
    for (y in 0 until rows step 2) {
        for (x in 0 until bitmap.width step 2) {
            val pixel = bitmap.getPixel(x, y)
            if ((pixel ushr 24) and 0xFF < 50) continue
            sum += ColorUtils.calculateLuminance(pixel)
            count++
        }
    }
    return count > 0 && sum / count > LIGHT_LUMINANCE_THRESHOLD
}

/**
 * true nếu phần trên ảnh bìa sáng (nên dùng icon đen), false nếu tối, null khi chưa đo xong / không có ảnh.
 */
@Composable
fun rememberArtworkTopIsLight(imageUrl: String?): Boolean? {
    val context = LocalContext.current
    var isLight by remember(imageUrl) { mutableStateOf(imageUrl?.let { topBrightnessCache.get(it) }) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank() || isLight != null) return@LaunchedEffect
        isLight = withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(96, 96)
                    .scale(Scale.FILL)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    isTopAreaLight(result.drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888))
                        .also { topBrightnessCache.put(imageUrl, it) }
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
    return isLight
}
