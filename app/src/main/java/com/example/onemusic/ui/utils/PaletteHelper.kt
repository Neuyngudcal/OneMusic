package com.example.onemusic.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.PlayerFallbackBottom
import com.example.onemusic.theme.PlayerFallbackMid
import com.example.onemusic.theme.PlayerFallbackTop
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PlayerBlackAlbumBottom
import com.example.onemusic.theme.PlayerBlackAlbumMid
import com.example.onemusic.theme.PlayerBlackAlbumTop
import com.example.onemusic.theme.PlayerNeutralAlbumBottom
import com.example.onemusic.theme.PlayerNeutralAlbumMid
import com.example.onemusic.theme.PlayerNeutralAlbumTop
import com.example.onemusic.theme.SurfaceCard

data class ArtworkColors(
    val topColor: Color = PlayerFallbackTop,
    val secondaryColor: Color = PlayerFallbackMid,
    val bottomColor: Color = PlayerFallbackBottom,
    val accentColor: Color = TextPrimary,
    val rawVibrantColor: Color = Brand
)

// In-memory LRU cache: instant 0ms retrieval on track switches
private val artworkColorsCache = LruCache<String, ArtworkColors>(60)

private data class BitmapStats(
    val bottomAvgHsl: FloatArray,
    val bottomDarkFraction: Float,
    val bottomLightFraction: Float,
    val overallDarkFraction: Float,
    val overallLightFraction: Float,
    val dominantBottomColorInt: Int
)

/**
 * Fast spatial sampling (96x96 bitmap) to understand:
 * 1. Bottom edge color (where the artwork actually dissolves into the background)
 * 2. Overall light/dark canvas composition (black album vs light paper album vs full-bleed photo)
 */
private fun analyzeBitmap(bitmap: Bitmap): BitmapStats {
    val width = bitmap.width
    val height = bitmap.height

    var bottomDarkCount = 0
    var bottomLightCount = 0
    var bottomTotalCount = 0
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L

    val hsl = FloatArray(3)

    // 1. Sample bottom 18% rows (the dissolve boundary)
    val startY = (height * 0.82f).toInt().coerceIn(0, height - 1)
    for (y in startY until height step 2) {
        for (x in 0 until width step 2) {
            val pixel = bitmap.getPixel(x, y)
            val a = (pixel ushr 24) and 0xFF
            if (a < 50) continue

            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF

            sumR += r
            sumG += g
            sumB += b
            bottomTotalCount++

            ColorUtils.colorToHSL(pixel, hsl)
            if (hsl[2] < 0.14f) bottomDarkCount++
            else if (hsl[2] > 0.60f) bottomLightCount++
        }
    }

    val avgR = if (bottomTotalCount > 0) (sumR / bottomTotalCount).toInt().coerceIn(0, 255) else 0
    val avgG = if (bottomTotalCount > 0) (sumG / bottomTotalCount).toInt().coerceIn(0, 255) else 0
    val avgB = if (bottomTotalCount > 0) (sumB / bottomTotalCount).toInt().coerceIn(0, 255) else 0
    val dominantBottomInt = (0xFF shl 24) or (avgR shl 16) or (avgG shl 8) or avgB
    val bottomAvgHsl = FloatArray(3)
    ColorUtils.colorToHSL(dominantBottomInt, bottomAvgHsl)

    // 2. Sample overall grid across the entire bitmap
    var overallDarkCount = 0
    var overallLightCount = 0
    var overallTotalCount = 0
    for (y in 0 until height step 4) {
        for (x in 0 until width step 4) {
            val pixel = bitmap.getPixel(x, y)
            val a = (pixel ushr 24) and 0xFF
            if (a < 50) continue

            overallTotalCount++
            ColorUtils.colorToHSL(pixel, hsl)
            if (hsl[2] < 0.14f) overallDarkCount++
            else if (hsl[2] > 0.60f) overallLightCount++
        }
    }

    return BitmapStats(
        bottomAvgHsl = bottomAvgHsl,
        bottomDarkFraction = if (bottomTotalCount > 0) bottomDarkCount.toFloat() / bottomTotalCount else 0f,
        bottomLightFraction = if (bottomTotalCount > 0) bottomLightCount.toFloat() / bottomTotalCount else 0f,
        overallDarkFraction = if (overallTotalCount > 0) overallDarkCount.toFloat() / overallTotalCount else 0f,
        overallLightFraction = if (overallTotalCount > 0) overallLightCount.toFloat() / overallTotalCount else 0f,
        dominantBottomColorInt = dominantBottomInt
    )
}

/**
 * Perceptual Hue-Aware Tuning:
 * Eliminates color mud (e.g. low-lightness yellow turning into olive mud,
 * low-saturation coral red turning into dirty grayish-brown).
 */
/**
 * True-to-Life Adaptive Color Tuning:
 * 1. Preserves the artist's original Hue 100% (No artificial hue shifting like turning lime to forest green).
 * 2. Natural ambient saturation (Soft & organic, never harsh, muddy, or over-saturated).
 * 3. Dynamic Lightness: Scales proportionally with the artwork's actual lightness so bright/pastel albums
 *    stay luminous & airy instead of turning into dark blood-red or heavy mud!
 */
private fun tuneColorForBackground(
    sourceColorInt: Int,
    targetLightness: Float,
    saturationScale: Float = 0.82f
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(sourceColorInt, hsl)

    // 1. Preserve original Hue 100% (True to the artist's vision!)
    // If it's Brat lime green (82°), it stays Brat lime green!
    // If it's San Holo coral pink (8°), it stays coral pink!
    // If it's Kim Petras icy blue (212°), it stays icy blue!

    // 2. Soft, natural ambient saturation (prevents heavy/toxic/muddy oversaturation)
    // FIX: Trước đây sàn tối thiểu là 0.12f, nghĩa là MỌI màu nguồn - kể cả màu gần như
    // trung tính (be/xám của bìa dạng chữ/typography) - đều bị ép nhuộm thêm ít nhất 12%
    // bão hòa giả tạo, tạo ra tông "bẩn" (không xám sạch, cũng không có màu chủ đề rõ ràng).
    // Giờ cho phép về gần 0 để giữ đúng bản chất trung tính khi nguồn thực sự nhạt màu.
    val naturalSat = (hsl[1] * saturationScale).coerceIn(0.0f, 0.72f)

    // 3. Apply target lightness
    hsl[1] = naturalSat
    hsl[2] = targetLightness.coerceIn(0.08f, 0.48f)

    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Fallback HSL processor for external/legacy calls.
 */
fun postProcessHslColor(
    colorInt: Int,
    minLightness: Float = 0.15f,
    maxLightness: Float = 0.25f,
    maxSaturation: Float = 0.45f
): Color {
    return tuneColorForBackground(colorInt, targetLightness = (minLightness + maxLightness) * 0.5f)
}

/**
 * Master-Class Adaptive Artwork Color Extraction Engine:
 * Ranks chromatic swatches to always capture the true soul and vibrant color of the album artwork.
 * Adapts lightness and saturation proportionally so:
 * - Pastel/Light albums (San Holo, Brat) are airy, luminous, and soft (never dark/heavy).
 * - Deep/Moody albums (Kim Petras, Charlie Puth) are rich and atmospheric.
 * - Pure Black albums (Glen Check) stay clean obsidian AMOLED black.
 */
fun extractArtworkColors(
    bitmap: Bitmap,
    palette: Palette,
    defaultTopColor: Color = PlayerFallbackTop,
    defaultBottomColor: Color = PlayerFallbackBottom
): ArtworkColors {
    val stats = analyzeBitmap(bitmap)
    val totalPopulation = palette.swatches.sumOf { it.population }.coerceAtLeast(1)

    // 1. Rank swatches by Chromatic Richness (Saturation * Population)
    // This prioritizes the REAL colors (e.g. San Holo's coral pink, Charlie Puth's red sweater, Brat's lime)
    // and ignores neutral background canvas or black handwritten ink text!
    // FIX: Ngưỡng lọc cũ (saturation >= 0.10) quá dễ dãi - một mảng be/xám gần trung tính,
    // chiếm diện tích lớn (ví dụ nền giấy/kết cấu chữ typography) vẫn lọt qua vòng lọc "chromatic"
    // và thắng nhờ diện tích áp đảo, dù gần như không có màu -> nâng ngưỡng lên 0.24 để loại các
    // mảng nhạt màu ra khỏi danh sách ứng viên làm màu chủ đạo.
    val chromaticSwatches = palette.swatches.filter { swatch ->
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, hsl)
        hsl[1] >= 0.24f && hsl[2] in 0.10f..0.92f
    }.sortedByDescending { swatch ->
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, hsl)
        // FIX: Công thức cũ "population * (0.30 + sat*0.70)" chỉ giảm điểm màu nhạt tối đa 70%,
        // nên 1 mảng lớn nhưng nhạt vẫn có thể thắng 1 mảng nhỏ nhưng rực rỡ. Nhân trực tiếp
        // population * saturation để độ bão hòa ảnh hưởng tỉ lệ thuận, màu càng nhạt càng bị
        // loại điểm mạnh hơn tương ứng.
        swatch.population.toFloat() * hsl[1]
    }

    val bestChromaticSwatch = chromaticSwatches.firstOrNull()
    val secondChromaticSwatch = chromaticSwatches.getOrNull(1)

    // 2. Best raw vibrant accent for top radial glow
    val rawAccentInt = palette.vibrantSwatch?.rgb
        ?: palette.lightVibrantSwatch?.rgb
        ?: bestChromaticSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: defaultTopColor.toArgb()

    // -------------------------------------------------------------
    // SPECIAL CASE: TRUE PURE BLACK ALBUM (e.g. Glen Check - BLEACH)
    // Only triggers when the image is virtually 80%+ black and has minimal chromatic elements
    // -------------------------------------------------------------
    val isTrueBlackAlbum = stats.overallDarkFraction >= 0.80f &&
            (bestChromaticSwatch == null || bestChromaticSwatch.population.toFloat() < totalPopulation * 0.10f)

    if (isTrueBlackAlbum) {
        return ArtworkColors(
            topColor = PlayerBlackAlbumTop,
            secondaryColor = PlayerBlackAlbumMid,
            bottomColor = PlayerBlackAlbumBottom,
            accentColor = Color(rawAccentInt),
            rawVibrantColor = Color(rawAccentInt)
        )
    }

    // -------------------------------------------------------------
    // SPECIAL CASE: NEAR-NEUTRAL / DESATURATED ALBUM (e.g. bìa dạng chữ/typography trên nền
    // be, xám, giấy cũ - "Cheating on You"). Không tìm được swatch nào đủ bão hòa (>= 0.24) để
    // làm màu chủ đạo, hoặc màu tốt nhất chỉ phủ một phần diện tích quá nhỏ để đại diện cho cả
    // bức ảnh. Trước đây rơi vào nhánh "GENERAL CASE" bên dưới và bị ép nhuộm màu giả (sàn tối
    // thiểu saturation), tạo ra tông nâu/xám "bẩn". Giờ trả về nền than chì trung tính sạch,
    // đồng bộ với tông SurfaceCard/ObsidianBlack của toàn app thay vì bịa ra 1 màu chủ đề không có thật.
    // -------------------------------------------------------------
    val isNeutralAlbum = bestChromaticSwatch == null ||
            bestChromaticSwatch.population.toFloat() < totalPopulation * 0.08f

    if (isNeutralAlbum) {
        return ArtworkColors(
            topColor = PlayerNeutralAlbumTop,
            secondaryColor = PlayerNeutralAlbumMid,
            bottomColor = PlayerNeutralAlbumBottom,
            accentColor = Color(rawAccentInt),
            rawVibrantColor = Color(rawAccentInt)
        )
    }

    // -------------------------------------------------------------
    // GENERAL CASE: VIBRANT & CHROMATIC ALBUMS (San Holo, Brat, Kim Petras, Charlie Puth, 5SOS, etc.)
    // Captures the rich, warm, emotional colors of the album!
    // -------------------------------------------------------------
    val primaryColorInt = bestChromaticSwatch?.rgb
        ?: palette.vibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: defaultTopColor.toArgb()

    val secondaryColorInt = secondChromaticSwatch?.rgb
        ?: palette.darkVibrantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: primaryColorInt

    // Sample the source lightness to adapt proportionally:
    val sourceHsl = FloatArray(3)
    ColorUtils.colorToHSL(primaryColorInt, sourceHsl)
    val sourceL = sourceHsl[2]

    // Proportional Lightness Scaling (Adaptive to artwork's real mood):
    // - Light / Pastel albums (e.g. San Holo, Brat): topL is ~0.38 - 0.42 (Airy, luminous, true-to-life)
    // - Medium albums (e.g. Kim Petras, Charlie Puth): topL is ~0.30 - 0.34
    // - Deep albums: topL is ~0.22 - 0.26
    val topL = (sourceL * 0.38f + 0.14f).coerceIn(0.22f, 0.42f)
    val secL = (sourceL * 0.28f + 0.09f).coerceIn(0.16f, 0.32f)
    val botL = (sourceL * 0.16f + 0.05f).coerceIn(0.09f, 0.20f)

    val safeTop = tuneColorForBackground(primaryColorInt, targetLightness = topL, saturationScale = 0.82f)
    val safeSec = tuneColorForBackground(secondaryColorInt, targetLightness = secL, saturationScale = 0.75f)
    val safeBot = tuneColorForBackground(primaryColorInt, targetLightness = botL, saturationScale = 0.60f)

    return ArtworkColors(
        topColor = safeTop,
        secondaryColor = safeSec,
        bottomColor = safeBot,
        accentColor = Color(rawAccentInt),
        rawVibrantColor = Color(rawAccentInt)
    )
}

/**
 * Fallback signature for palette-only callers.
 */
private fun extractArtworkColorsFromPalette(
    palette: Palette,
    defaultTopColor: Color,
    defaultBottomColor: Color
): ArtworkColors {
    val primaryColorInt = palette.darkMutedSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: palette.darkVibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: defaultTopColor.toArgb()

    val secondaryColorInt = palette.darkVibrantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: primaryColorInt

    return ArtworkColors(
        topColor = tuneColorForBackground(primaryColorInt, targetLightness = 0.18f),
        secondaryColor = tuneColorForBackground(secondaryColorInt, targetLightness = 0.13f),
        bottomColor = tuneColorForBackground(primaryColorInt, targetLightness = 0.08f),
        accentColor = Color(primaryColorInt),
        rawVibrantColor = Color(primaryColorInt)
    )
}

@Composable
fun rememberArtworkColors(
    imageUrl: String?,
    defaultTopColor: Color = PlayerFallbackTop,
    defaultBottomColor: Color = PlayerFallbackBottom
): ArtworkColors {
    val context = LocalContext.current
    val cached = imageUrl?.let { artworkColorsCache.get(it) }

    // Retain previous colors across imageUrl changes if new palette is not yet cached.
    // This completely prevents the flash/dip to default dark gray during async extraction!
    var colors by remember(imageUrl) {
        mutableStateOf(cached ?: ArtworkColors(topColor = defaultTopColor, bottomColor = defaultBottomColor))
    }

    // Instant synchronous cache hit on recomposition
    if (cached != null && colors != cached) {
        colors = cached
    }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            colors = ArtworkColors(topColor = defaultTopColor, bottomColor = defaultBottomColor)
            return@LaunchedEffect
        }

        // Return immediately if already cached in memory
        val memoryHit = artworkColorsCache.get(imageUrl)
        if (memoryHit != null) {
            colors = memoryHit
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(96, 96)
                    .scale(Scale.FILL)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    val palette = Palette.from(bitmap)
                        .resizeBitmapArea(96 * 96)
                        .maximumColorCount(16)
                        .generate()

                    val extracted = extractArtworkColors(bitmap, palette, defaultTopColor, defaultBottomColor)

                    // Cache in memory
                    artworkColorsCache.put(imageUrl, extracted)

                    withContext(Dispatchers.Main) {
                        colors = extracted
                    }
                }
            } catch (_: Exception) {
                // Fallback retained
            }
        }
    }

    return colors
}

/**
 * Intelligent background preloader for Next and Previous tracks:
 * Preloads both the high-res bitmap into Coil's memory cache AND computes the Palette colors in RAM.
 * Ensures 0ms latency and zero flicker when the user skips tracks.
 */
suspend fun preloadArtworkAndColors(context: Context, imageUrl: String?) {
    if (imageUrl.isNullOrBlank()) return

    withContext(Dispatchers.IO) {
        try {
            val loader = context.imageLoader

            // 1. Preload full image into Coil memory cache
            val fullRequest = ImageRequest.Builder(context)
                .data(imageUrl)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
            loader.enqueue(fullRequest)

            // 2. Pre-calculate Palette if not yet in cache
            if (artworkColorsCache.get(imageUrl) == null) {
                val thumbRequest = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(96, 96)
                    .scale(Scale.FILL)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(thumbRequest)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    val palette = Palette.from(bitmap)
                        .resizeBitmapArea(96 * 96)
                        .maximumColorCount(16)
                        .generate()

                    val extracted = extractArtworkColors(
                        bitmap = bitmap,
                        palette = palette,
                        defaultTopColor = PlayerFallbackTop,
                        defaultBottomColor = PlayerFallbackBottom
                    )
                    artworkColorsCache.put(imageUrl, extracted)
                }
            }
        } catch (_: Exception) {}
    }
}
