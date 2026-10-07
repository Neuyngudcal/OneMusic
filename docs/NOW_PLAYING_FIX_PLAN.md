# Kế hoạch sửa lỗi màn hình Now Playing (dành cho người mới)

Tài liệu này hướng dẫn **từng bước** cách sửa các vấn đề của màn hình phát nhạc toàn màn hình (Now Playing) trong OneMusic. Mỗi mục cho biết: triệu chứng người dùng thấy, file và vị trí cần sửa, vì sao lỗi xảy ra, code sửa mẫu và cách kiểm tra lại.

> Số dòng ghi trong tài liệu là số dòng tại thời điểm viết (28/09/2026). Nếu file đã thay đổi, hãy dùng **Ctrl+F** tìm theo đoạn code được trích, đừng tin tuyệt đối vào số dòng.

---

## 0. Chuẩn bị trước khi sửa

### 0.1 Các file liên quan

| File | Vai trò |
|---|---|
| `app/src/main/java/com/example/onemusic/ui/screens/player/NowPlayingSheet.kt` | Toàn bộ giao diện Now Playing (ảnh bìa, lời bài hát, hàng đợi, thanh tua, nút điều khiển). **Phần lớn việc sửa nằm ở đây.** |
| `app/src/main/java/com/example/onemusic/playback/MusicPlayerController.kt` | "Bộ não" phát nhạc: giữ hàng đợi, vị trí phát, shuffle, video bìa động. |
| `app/src/main/java/com/example/onemusic/MainActivity.kt` | Nơi mở `NowPlayingSheet` và truyền dữ liệu từ controller vào (khoảng dòng 680–713). |

### 0.2 Năm khái niệm Compose cần nắm (đọc 10 phút trước khi bắt tay)

1. **Recomposition (vẽ lại):** khi một `State` mà hàm `@Composable` đọc thay đổi, Compose chạy lại hàm đó. Đọc state ở hàm cha → cả hàm cha vẽ lại. Đọc ở hàm con → chỉ hàm con vẽ lại.
2. **`remember { ... }`:** giữ giá trị qua các lần vẽ lại. Lambda viết bên trong `remember {}` sẽ **giữ nguyên biến tại thời điểm tạo** — đây là nguồn gốc của nhiều lỗi bên dưới.
3. **`rememberUpdatedState(x)`:** tạo một "hộp" luôn chứa giá trị **mới nhất** của `x`. Dùng khi một lambda sống lâu (trong `pointerInput`, `LaunchedEffect`, `remember`) cần đọc giá trị mới.
4. **`pointerInput(key)` / `LaunchedEffect(key)`:** khối code bên trong chỉ khởi động lại khi `key` đổi. `pointerInput(Unit)` = **không bao giờ** khởi động lại → mọi biến nó dùng bị "đóng băng".
5. **`derivedStateOf { }`:** tính một giá trị từ state khác, nhưng chỉ báo "thay đổi" khi **kết quả** thực sự đổi. Dùng để giảm số lần vẽ lại.

### 0.3 Quy trình cho mỗi mục

1. Tạo branch riêng: `git checkout -b fix/now-playing-<số-mục>`.
2. Tái hiện lỗi trên máy/giả lập theo phần **"Cách tái hiện"**.
3. Sửa theo hướng dẫn.
4. Build lại và kiểm tra theo phần **"Kiểm tra"**.
5. Commit, mỗi mục một commit.

### 0.4 Thứ tự làm đề xuất

| Thứ tự | Mục | Độ khó | Ghi chú |
|---|---|---|---|
| 1 | [Mục 1 – Thanh tua](#mục-1--tua-nhạc-sai-vị-trí-sau-khi-chuyển-bài) | ⭐ | Sửa vài dòng |
| 2 | [Mục 2 – Vuốt xóa hàng đợi](#mục-2--vuốt-xóa-trong-hàng-đợi-xóa-nhầm-bài) | ⭐ | Sửa vài dòng |
| 3 | [Mục 3 – Pager tự phát nhầm bài](#mục-3--pager-ảnh-bìa-tự-phát-nhầm-bài) | ⭐⭐ | |
| 4 | [Mục 4 – Video bìa chạy nền](#mục-4--video-bìa-động-vẫn-chạy-khi-đã-đóng-now-playing) | ⭐⭐ | Sửa 2 file |
| 5 | [Mục 5 – Lời bài hát](#mục-5--lời-bài-hát-highlight-sai--giữ-vị-trí-cuộn-cũ) | ⭐⭐ | |
| 6 | [Mục 6 – Ảnh bìa nhảy kích thước](#mục-6--ảnh-bìa-nhảy-kích-thước-khi-thoát-hàng-đợi) | ⭐ | |
| 7 | [Mục 7 – Các lỗi nhỏ về cảm giác](#mục-7--các-lỗi-nhỏ-về-cảm-giác-sử-dụng) | ⭐ | 4 việc nhỏ |
| 8 | [Mục 8 – Hiệu năng](#mục-8--hiệu-năng-màn-hình-vẽ-lại-25-lầngiây) | ⭐⭐⭐ | Sửa 3 file, nên nhờ người review |
| 9 | [Mục 9 – Shuffle](#mục-9--shuffle-hiển-thị-sai-thứ-tự-phát) | ⭐⭐⭐⭐ | Cần trưởng nhóm chốt thiết kế trước |
| 10 | [Mục 10 – Dọn dẹp](#mục-10--dọn-dẹp) | ⭐ | |

---

## Mục 1 – Tua nhạc sai vị trí sau khi chuyển bài

**Triệu chứng:** Mở Now Playing, chuyển sang một bài dài/ngắn hơn hẳn, kéo thanh tua đến giữa → nhạc nhảy đến vị trí sai (tính theo độ dài bài cũ).

**Cách tái hiện:** Mở Now Playing ở bài dài 2 phút → bấm Next sang bài dài 6 phút → kéo thanh tua đến giữa. Nhạc sẽ nhảy tới ~1:00 thay vì ~3:00.

**Vị trí:** `NowPlayingSheet.kt`, hàm `CapsuleSlider` (khoảng dòng 223–306).

**Vì sao lỗi:** Slider dùng `.pointerInput(Unit)`. Key là `Unit` nên khối xử lý cử chạm **chỉ được tạo một lần**, và nó giữ mãi lambda `onValueChangeFinished` của lần đầu. Lambda đó (ở dòng ~1982) dùng biến `totalDur` = độ dài bài **lúc mở sheet**.

**Cách sửa:**

1. Ở đầu hàm `CapsuleSlider`, ngay sau dòng `var lastHapticFraction ...`, thêm:

```kotlin
val currentOnValueChange by rememberUpdatedState(onValueChange)
val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
```

2. Bên trong khối `pointerInput(Unit) { ... }`, thay **tất cả**:
   - `onValueChange(frac)` → `currentOnValueChange(frac)` (có 2 chỗ)
   - `onValueChangeFinished?.invoke()` → `currentOnValueChangeFinished?.invoke()`

**Sửa kèm – thanh tua giật lùi 1 khung hình khi thả tay (tùy chọn nhưng nên làm):**

Trong `NowPlayingSheet`, gần dòng `var scrubFraction by remember ...` (~dòng 504), thêm:

```kotlin
var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }
LaunchedEffect(pendingSeekFraction) {
    if (pendingSeekFraction != null) {
        delay(300L)            // chờ vị trí thật từ player cập nhật
        pendingSeekFraction = null
    }
}
```

Rồi ở phần thanh tua (~dòng 1967 và 1982):

```kotlin
// Trước
val sliderValue = if (isScrubbing) scrubFraction else rawSliderValue
// Sau
val sliderValue = when {
    isScrubbing -> scrubFraction
    pendingSeekFraction != null -> pendingSeekFraction!!
    else -> rawSliderValue
}
```

```kotlin
onValueChangeFinished = {
    if (totalDur > 0) {
        onSeek((scrubFraction * totalDur).toLong())
        pendingSeekFraction = scrubFraction   // thêm dòng này
    }
    isScrubbing = false
},
```

**Kiểm tra:**
- [ ] Lặp lại bước tái hiện: tua đến giữa bài 6 phút → nhạc tới ~3:00.
- [ ] Thả tay khỏi thanh tua: thanh không bị giật lùi rồi nhảy lại.

---

## Mục 2 – Vuốt xóa trong hàng đợi xóa nhầm bài

**Triệu chứng:** Trong tab Hàng đợi, sau khi kéo sắp xếp hoặc xóa một bài, vuốt trái để xóa bài khác → bài bị xóa không phải bài vừa vuốt.

**Cách tái hiện:** Mở hàng đợi có ≥ 5 bài phía sau → vuốt xóa bài thứ 1 của "Tiếp tục phát" → vuốt xóa bài (lúc này) đang ở vị trí thứ 3 → kiểm tra xem bài nào biến mất.

**Vị trí:** `NowPlayingSheet.kt`, khoảng dòng 1699–1761 (trong `itemsIndexed(items = upNextItems ...)`).

**Vì sao lỗi:** `rememberSwipeToDismissBoxState(confirmValueChange = { ... actualIndex ... })` được `remember` theo key của hàng (key cố định theo id bài). Lambda `confirmValueChange` vì vậy giữ `actualIndex` **của lần đầu hàng đó xuất hiện**, trong khi vị trí thật của bài đã đổi.

**Cách sửa:**

1. Di chuyển dòng sau từ bên trong phần nội dung (~dòng 1759) lên **ngay dưới** `val canDismiss = ...` (~dòng 1702):

```kotlin
val latestActualIndex = rememberUpdatedState(actualIndex)
```

2. Trong `confirmValueChange`, đổi:

```kotlin
// Trước
currentOnRemove.value?.invoke(actualIndex)
// Sau
currentOnRemove.value?.invoke(latestActualIndex.value)
```

3. Xóa dòng `val latestActualIndex = rememberUpdatedState(actualIndex)` cũ ở bên dưới (nếu để lại sẽ báo lỗi khai báo trùng). Code kéo-sắp-xếp bên dưới vẫn dùng `latestActualIndex.value` như cũ, không cần sửa.

**Kiểm tra:**
- [ ] Lặp lại bước tái hiện: đúng bài vừa vuốt bị xóa.
- [ ] Kéo sắp xếp 2–3 bài rồi vuốt xóa: vẫn đúng bài.

---

## Mục 3 – Pager ảnh bìa tự phát nhầm bài

**Triệu chứng:** Khi hàng đợi thay đổi (bấm "Xóa" lịch sử, xóa bài nằm trước bài đang phát), nhạc có thể tự nhảy sang một bài khác dù người dùng không vuốt.

**Cách tái hiện:** Phát bài số 6 trong một danh sách 10 bài → mở Hàng đợi → bấm "Xóa" ở mục Lịch sử → quay lại ảnh bìa. Xem bài đang phát có bị đổi không. (Lỗi phụ thuộc thời điểm nên có thể cần thử vài lần.)

**Vị trí:** `NowPlayingSheet.kt`, khoảng dòng 627–634:

```kotlin
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.settledPage }.collect { settledPage ->
        if (queue.isNotEmpty() && settledPage in queue.indices && settledPage != currentPlaybackIndex) {
            onPlayQueueIndexUpdated(settledPage)
        }
    }
}
```

**Vì sao lỗi:**
1. Khối này gọi "phát bài" **mỗi khi trang của pager đổi**, kể cả khi trang đổi do code (hàng đợi ngắn lại → pager tự kẹp về trang cuối), không phải do người dùng vuốt.
2. Biến `queue` bên trong bị "đóng băng" ở lần chạy đầu (vì `LaunchedEffect(pagerState)` không bao giờ khởi động lại).

**Cách sửa:** Chỉ phát bài mới khi **người dùng thực sự kéo** pager. Thay toàn bộ khối trên bằng:

```kotlin
val latestQueue by rememberUpdatedState(queue)
val isPagerDragged by pagerState.interactionSource.collectIsDraggedAsState()
var isUserSwipe by remember { mutableStateOf(false) }

LaunchedEffect(isPagerDragged) {
    if (isPagerDragged) isUserSwipe = true
}

// Khi pager dừng hẳn sau một lần người dùng vuốt → phát bài ở trang đó
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
        if (!scrolling && isUserSwipe) {
            isUserSwipe = false
            val page = pagerState.currentPage
            if (page in latestQueue.indices && page != currentPlaybackIndex) {
                onPlayQueueIndexUpdated(page)
            }
        }
    }
}
```

`collectIsDraggedAsState` đã được import sẵn ở đầu file.

**Kiểm tra:**
- [ ] Vuốt ảnh bìa sang trái/phải → chuyển bài như trước.
- [ ] Vuốt dở rồi thả về trang cũ → **không** chuyển bài.
- [ ] Bấm Next/Prev → ảnh bìa trượt theo, không phát thêm bài khác.
- [ ] Lặp lại bước tái hiện → bài đang phát không đổi.

---

## Mục 4 – Video bìa động vẫn chạy khi đã đóng Now Playing

**Triệu chứng:** Với bài có bìa động (motion artwork), sau khi đóng Now Playing, video vẫn tiếp tục được giải mã ngầm → hao pin, nóng máy. Mỗi lần chuyển bài khi sheet đang đóng, video bài mới cũng tự chạy.

**Vị trí:**
- `MusicPlayerController.kt`: `setupMotionPlayer` (dòng ~199 `playWhenReady = true`) và `updateMotionPlayerMedia` (dòng ~250 `motionExoPlayer?.playWhenReady = true`).
- `MainActivity.kt`: chỗ `if (isPlayerExpanded) { NowPlayingSheet(...) }` (~dòng 682).

**Vì sao lỗi:** Controller luôn đặt `playWhenReady = true` cho player video, không quan tâm màn hình có đang hiển thị hay không. `NowPlayingSheet` khi đóng cũng không tạm dừng player này.

**Cách sửa:** Cho controller biết khi nào được phép chạy video.

1. Trong `MusicPlayerController.kt`, gần khai báo `motionExoPlayer` (~dòng 131), thêm:

```kotlin
private var isMotionPlaybackAllowed = false

fun setMotionPlaybackAllowed(allowed: Boolean) {
    isMotionPlaybackAllowed = allowed
    motionExoPlayer?.playWhenReady = allowed
}
```

2. Thay **cả hai** chỗ `playWhenReady = true` trong `setupMotionPlayer` (dòng ~199) và `updateMotionPlayerMedia` (dòng ~250) bằng `playWhenReady = isMotionPlaybackAllowed`.
   ⚠️ **Không** sửa dòng `playWhenReady = true` trong `onPlaybackStateChanged` (STATE_ENDED, dòng ~207) thành `true` cứng — đổi nó thành `playWhenReady = isMotionPlaybackAllowed` luôn cho nhất quán.

3. Trong `MainActivity.kt`, ngay **trước** dòng `if (isPlayerExpanded) {` (~dòng 682), thêm:

```kotlin
LaunchedEffect(isPlayerExpanded) {
    playerController.setMotionPlaybackAllowed(isPlayerExpanded)
}
```

**Kiểm tra:**
- [ ] Mở Now Playing ở bài có bìa động → video chạy bình thường.
- [ ] Đóng sheet → trong Android Studio mở **Profiler → CPU**, CPU phải giảm rõ so với trước khi sửa.
- [ ] Đóng sheet, bấm Next ở mini-player sang bài có bìa động, rồi mở lại sheet → video chạy.

---

## Mục 5 – Lời bài hát: highlight sai & giữ vị trí cuộn cũ

**Triệu chứng:**
- (a) Trong đoạn nhạc dạo đầu bài (trước câu hát đầu tiên), dòng lời đầu tiên đã sáng.
- (b) Đang xem lời, chuyển bài → danh sách lời bài mới hiện ở giữa chừng (vị trí cuộn của bài cũ).
- (c) Tự cuộn đọc trước rồi hất mạnh (fling) → lời hát bị kéo về dòng đang hát quá sớm.

**Vị trí:** `NowPlayingSheet.kt`, khoảng dòng 813–858.

### 5a. Không highlight trước câu đầu tiên

Trong khối `activeLyricIndex` (~dòng 813):

```kotlin
// Trước
if (idx >= 0) idx else 0
// Sau
idx   // indexOfLast trả về -1 khi chưa tới câu nào → không dòng nào sáng
```

Và dòng `if (track == null || track.lyrics.isEmpty()) 0` đổi `0` thành `-1`. Các chỗ dùng `activeLyricIndex` đều đã kiểm tra `activeLyricIndex in track.lyrics.indices` nên an toàn với -1.

### 5b. Về đầu danh sách khi đổi bài

Thêm sau khối `LaunchedEffect(activeLyricIndex) { ... }` (~dòng 858):

```kotlin
LaunchedEffect(track?.id) {
    lyricsListState.scrollToItem(0)
    lastLyricsUserScrollTimeMs = 0L
}
```

### 5c. Tính "người dùng đang đọc" từ lúc cuộn **dừng hẳn**

Thay khối `LaunchedEffect(isLyricsDragged) { ... }` (~dòng 829–833) bằng:

```kotlin
var isUserScrollingLyrics by remember { mutableStateOf(false) }

LaunchedEffect(isLyricsDragged) {
    if (isLyricsDragged) {
        isUserScrollingLyrics = true
        lastLyricsUserScrollTimeMs = System.currentTimeMillis()
    }
}

LaunchedEffect(lyricsListState.isScrollInProgress) {
    if (!lyricsListState.isScrollInProgress && isUserScrollingLyrics) {
        isUserScrollingLyrics = false
        lastLyricsUserScrollTimeMs = System.currentTimeMillis()
    }
}
```

**Kiểm tra:**
- [ ] Bài có intro dài: chưa dòng nào sáng cho tới câu đầu tiên.
- [ ] Đang xem lời, bấm Next → lời bài mới hiện từ đầu.
- [ ] Hất mạnh danh sách lời → sau khi dừng khoảng 3,5 giây mới tự cuộn về dòng đang hát.

---

## Mục 6 – Ảnh bìa nhảy kích thước khi thoát Hàng đợi

**Triệu chứng:** Từ tab Hàng đợi bấm về ảnh bìa → ảnh bìa bị giật/đổi kích thước đột ngột.

**Vị trí:** `NowPlayingSheet.kt`, `.onSizeChanged { ... }` ở khoảng dòng 1849–1853.

**Vì sao lỗi:** Chiều cao cụm nút điều khiển được đo liên tục và dùng làm khoảng trống dưới ảnh bìa (`Spacer` dòng ~1220). Ở tab Hàng đợi, hàng tên bài bị ẩn → cụm nút thấp đi → ảnh bìa (đang mờ phía sau) giãn ra; khi quay lại thì co về đột ngột.

**Cách sửa:** Chỉ đo khi đang ở chế độ ảnh bìa:

```kotlin
.onSizeChanged { size ->
    if (size.height > 0 && centerView == NowPlayingCenterView.ARTWORK) {
        controlsDeckHeightPx = size.height
    }
}
```

**Kiểm tra:**
- [ ] Chuyển qua lại Ảnh bìa ↔ Hàng đợi ↔ Lời nhiều lần: ảnh bìa không nhảy.

---

## Mục 7 – Các lỗi nhỏ về cảm giác sử dụng

### 7a. Kéo đóng sheet bị rung

**Vị trí:** hàm `Modifier.sheetDragToDismiss` (~dòng 869–945).

**Vì sao:** Mỗi lần ngón tay di chuyển lại mở một coroutine `sheetOffsetY.snapTo(...)` và đọc `sheetOffsetY.value` **trước khi** coroutine trước kịp chạy → giá trị cộng dồn lệch.

**Cách sửa:** Giữ vị trí trong một biến cục bộ của cử chỉ:

```kotlin
// Ngay sau "var totalDy = 0f"
var localOffset = sheetOffsetY.value
```

```kotlin
// Thay khối tính newOffset
if (delta.y > 0 || localOffset > 0f) {
    localOffset = (localOffset + delta.y).coerceAtLeast(0f)
    val newOffset = localOffset
    scope.launch { sheetOffsetY.snapTo(newOffset) }
    // ... phần haptic giữ nguyên, dùng newOffset
}
```

Và ở phần tính `shouldDismiss` sau vòng lặp, dùng `localOffset` thay cho `sheetOffsetY.value`.

### 7b. Kéo sắp xếp hàng đợi không bám tay

**Vị trí:** `val itemStepPx = with(density) { 44.dp.toPx() }` (~dòng 1753).

**Vì sao:** Mỗi hàng cao khoảng 56dp (ảnh 42dp + padding 12dp + khoảng cách 2dp) nhưng bước kéo là 44dp → hàng chạy nhanh hơn ngón tay.

**Cách sửa:** Đo chiều cao thật của hàng:

```kotlin
var rowHeightPx by remember { mutableIntStateOf(0) }
val spacingPx = with(density) { 2.dp.toPx() }
val itemStepPx = if (rowHeightPx > 0) rowHeightPx + spacingPx else with(density) { 56.dp.toPx() }
```

và thêm `modifier = Modifier.onSizeChanged { rowHeightPx = it.height }` cho `QueueFlatTrackRow(...)` bên trong.

### 7c. Thông báo "Đã ưa thích" biến mất sớm

**Vị trí:** hàm `triggerFavoriteToast` (~dòng 487).

**Vì sao:** Bấm yêu thích 2 lần liên tiếp → hẹn giờ của lần 1 vẫn chạy và ẩn luôn thông báo lần 2.

**Cách sửa:**

```kotlin
var favoriteToastJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

fun triggerFavoriteToast(message: String) {
    favoriteToastMessage = message
    favoriteToastJob?.cancel()
    favoriteToastJob = scope.launch {
        delay(2500L)
        favoriteToastMessage = null
    }
}
```

### 7d. Tắt nhầm "giữ màn hình sáng"

**Vị trí:** `DisposableEffect(appSettings?.isKeepScreenOnEnabled)` (~dòng 446).

**Cách sửa:** Chỉ xóa cờ nếu chính mình đã bật:

```kotlin
androidx.compose.runtime.DisposableEffect(appSettings?.isKeepScreenOnEnabled) {
    val shouldKeepOn = appSettings?.isKeepScreenOnEnabled == true
    if (shouldKeepOn) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
        if (shouldKeepOn) {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
```

**Kiểm tra mục 7:**
- [ ] Kéo sheet xuống chậm rồi nhanh: sheet bám theo tay mượt, không rung.
- [ ] Kéo sắp xếp một bài xuống 3 hàng: bài dừng đúng dưới ngón tay.
- [ ] Bấm yêu thích 2 lần liên tục: thông báo thứ 2 hiện đủ ~2,5 giây.

---

## Mục 8 – Hiệu năng: màn hình vẽ lại 25 lần/giây

> ⭐⭐⭐ Mục này sửa 3 file và thay đổi cách truyền dữ liệu. Nên làm sau khi xong mục 1–7 và nhờ người có kinh nghiệm review.

**Triệu chứng:** Now Playing hơi giật khi vuốt ảnh bìa/cuộn hàng đợi, máy ấm khi mở lâu, đặc biệt với hàng đợi dài.

**Vì sao:** Trong `MusicPlayerController.startProgressTracker` (~dòng 1250–1295), mỗi **40ms** controller ghi `currentPositionMs` mới vào `_playbackState`. `MainActivity` đọc toàn bộ `playbackState` (dòng 260) → **cả `MainActivity` lẫn `NowPlayingSheet` (3.700 dòng)** vẽ lại 25 lần/giây, dù chỉ có thanh tua và lời bài hát cần vị trí mới.

Ghi chú: `NowBar` có nhận tham số `progress` (MainActivity dòng 611) nhưng **không dùng tới** bên trong `NowBar.kt` — vậy nên MainActivity đang vẽ lại vô ích.

### Bước 8.1 – Tách vị trí phát ra luồng riêng (`MusicPlayerController.kt`)

1. Cạnh `_playbackState` (~dòng 122) thêm:

```kotlin
private val _positionMs = MutableStateFlow(0L)
val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
```

2. Trong `startProgressTracker`, đổi phần cập nhật:

```kotlin
// Trước
_playbackState.value = _playbackState.value.copy(
    currentPositionMs = pos,
    durationMs = duration
)
// Sau
_positionMs.value = pos
if (_playbackState.value.durationMs != duration) {
    _playbackState.value = _playbackState.value.copy(durationMs = duration)
}
```

3. Mọi chỗ đang đặt `currentPositionMs = ...` trong controller (dùng Ctrl+F: dòng ~412, 742, 811, 831, 995, 1185, 1202) — thêm ngay sau đó `_positionMs.value = <cùng giá trị>`. Ví dụ trong `seekTo`: thêm `_positionMs.value = safePos`.

4. Ở `pause()` (~dòng 1116) và `release()` (~dòng 1304), đổi `_playbackState.value.currentPositionMs` thành `_positionMs.value`.

> Có thể giữ trường `currentPositionMs` trong `PlaybackState` để không làm vỡ code khác, nhưng **không** cập nhật nó mỗi 40ms nữa. Trước khi xóa hẳn, dùng **Find in Files** (Ctrl+Shift+F) tìm `currentPositionMs` để chắc không còn chỗ nào phụ thuộc.

### Bước 8.2 – Truyền vị trí vào Now Playing (`MainActivity.kt`)

1. Bỏ dòng `progress = ...` khi gọi `NowBar` (dòng 611) và bỏ tham số `progress` trong `NowBar.kt` (dòng 91) vì không dùng.
2. Khi gọi `NowPlayingSheet(...)` thêm tham số:

```kotlin
positionState = playerController.positionMs.collectAsState(),
```

   (`collectAsState()` trả về `State<Long>`. Truyền **cả object State**, không truyền `.value` — nếu đọc `.value` ở MainActivity thì MainActivity lại bị vẽ lại.)

### Bước 8.3 – Chỉ đọc vị trí ở nơi cần (`NowPlayingSheet.kt`)

1. Thêm tham số cho `NowPlayingSheet`: `positionState: androidx.compose.runtime.State<Long>`.
2. Tách toàn bộ phần **thanh tua + 2 nhãn thời gian + badge Lossless** (~dòng 1963–2127) ra một composable riêng, ví dụ `NowPlayingProgressSection(positionState, durationMs, ...)`. Bên trong nó mới đọc `positionState.value`. Nhờ vậy mỗi 40ms chỉ phần này vẽ lại.
3. Đổi `activeLyricIndex` sang `derivedStateOf` để chỉ đổi khi **sang câu mới**:

```kotlin
val lyrics = track?.lyrics.orEmpty()
val activeLyricIndex by remember(lyrics) {
    derivedStateOf {
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { it.timestampMs <= positionState.value + 60L }
    }
}
```

4. Trong `WordByWordLyricItem`, tham số `currentPositionMs` chỉ cần cho **dòng đang hát**. Đổi thành truyền `positionState` và chỉ đọc `.value` khi `isActive` để các dòng khác không vẽ lại.

### Bước 8.4 – Các chỗ tốn kém khác trong `NowPlayingSheet.kt`

| Vị trí | Vấn đề | Cách sửa |
|---|---|---|
| Key của hàng đợi (~dòng 1692–1698) | Mỗi key lặp lại cả danh sách → O(n²), hàng đợi 1.000 bài = ~500.000 phép so sánh mỗi lần vẽ | Tính trước một lần (code bên dưới) |
| `isFlacTrack` (~dòng 2018) | `contentResolver.getType` là lệnh gọi hệ thống chạy trên luồng giao diện | Chuyển sang `produceState` + `Dispatchers.IO`, hoặc bỏ nếu `audioUrl`/`bitRate` đã đủ thông tin |
| `MotionArtworkPlayer` (~dòng 2965–2994) | `File.exists()` và `MediaMetadataRetriever` chạy trên luồng giao diện | Chuyển sang `produceState` + `Dispatchers.IO` |

Code tính trước key hàng đợi:

```kotlin
val upNextKeys = remember(upNextItems) {
    val seen = HashMap<String, Int>()
    upNextItems.map { t ->
        val occ = seen.getOrDefault(t.id, 0)
        seen[t.id] = occ + 1
        "next_${t.id}_occ$occ"
    }
}
// ...
itemsIndexed(
    items = upNextItems,
    key = { index, _ -> upNextKeys[index] }
) { ... }
```

Mẫu `produceState` cho việc chạy nền:

```kotlin
val videoDimensions by produceState(initialValue = Pair(0, 0), motionVideoPath) {
    value = withContext(Dispatchers.IO) {
        // ... đưa nguyên khối runCatching { MediaMetadataRetriever ... } cũ vào đây
    }
}
```

**Kiểm tra mục 8:**
- [ ] Android Studio → **Layout Inspector** → bật "Show Recomposition Counts". Mở Now Playing, để phát 10 giây: `NowPlayingSheet` gần như không tăng, chỉ `NowPlayingProgressSection` tăng.
- [ ] Thanh tua, thời gian, lời bài hát karaoke vẫn chạy mượt.
- [ ] Tua, chuyển bài, tạm dừng, thoát app rồi mở lại → vị trí phát được khôi phục đúng.

---

## Mục 9 – Shuffle hiển thị sai thứ tự phát

> ⭐⭐⭐⭐ Thay đổi logic lõi của hàng đợi. **Phải thống nhất thiết kế với trưởng nhóm trước khi code.**

**Triệu chứng:** Bật Trộn bài:
- Danh sách "Tiếp tục phát" vẫn theo thứ tự gốc, không phải thứ tự sẽ phát.
- Vuốt ảnh bìa sang phải phát bài **kế tiếp theo thứ tự gốc**, còn nút Next phát **bài ngẫu nhiên**.
- Mục "Lịch sử" hiển thị sai.

**Vị trí:** `MusicPlayerController.kt`: `toggleShuffle()` (~dòng 1067), `skipToNext()` (~dòng 870), `skipToPrevious()` (~dòng 886).

**Vì sao:** Khi bật shuffle, controller chỉ bật `exoPlayer.shuffleModeEnabled`. ExoPlayer tự xáo trộn **bên trong**, nhưng `state.queue` (thứ tự giao diện đang hiển thị) vẫn theo thứ tự gốc.

**Hướng sửa đề xuất (kiểu Apple Music):**
1. Thêm biến `originalQueue: List<Track>?` trong controller.
2. Khi **bật** shuffle: lưu `originalQueue = queue`; giữ nguyên phần lịch sử và bài đang phát, xáo trộn phần phía sau (`subList(currentIndex + 1, size).shuffled()`); cập nhật `state.queue` và danh sách media của ExoPlayer theo thứ tự mới. **Không** bật `exoPlayer.shuffleModeEnabled`.
3. Khi **tắt** shuffle: khôi phục `originalQueue`, tìm lại vị trí bài đang phát theo `id`, cập nhật `currentIndex`.
4. `skipToNext()` bỏ nhánh `if (state.isShuffle)`, luôn phát `currentIndex + 1`.
5. Các hàm `addToQueue`, `playNext`, `removeQueueItem`, `moveQueueItem` cần cập nhật cả `originalQueue` nếu đang shuffle.

**Câu hỏi cần trưởng nhóm trả lời trước khi làm:**
- Cập nhật danh sách media của ExoPlayer bằng `moveMediaItem` từng bài (mượt, không ngắt nhạc) hay `setMediaItems` một lần (đơn giản, có thể ngắt ~100ms)?
- Tắt shuffle thì các bài đã thêm/xóa trong lúc shuffle được xử lý ra sao?

**Kiểm tra:**
- [ ] Bật shuffle: "Tiếp tục phát" hiển thị đúng thứ tự sẽ phát; bấm Next 3 lần đúng 3 bài đầu danh sách đó.
- [ ] Vuốt ảnh bìa và bấm Next cho cùng kết quả.
- [ ] Tắt shuffle: quay về thứ tự gốc, bài đang phát không bị ngắt.

---

## Mục 10 – Dọn dẹp

1. Xóa file backup `app/src/main/java/com/example/onemusic/ui/screens/player/NowPlayingSheet.kt.bak` (git đã lưu lịch sử, không cần file .bak).
2. Mở `NowPlayingSheet.kt` → **Code → Optimize Imports** (Ctrl+Alt+O) để xóa các import không dùng.
3. (Tùy chọn, bàn với nhóm) Hiệu ứng làm mờ toàn nền khi mở Lời/Hàng đợi (`blurRadiusAnimated`, ~dòng 974) khá nặng GPU và **không có tác dụng trên Android 11 trở xuống** (`Modifier.blur` chỉ chạy từ Android 12). Có thể cân nhắc chỉ làm mờ lớp nền màu thay vì cả ảnh bìa + video.

---

## Checklist tổng khi hoàn tất

- [ ] Build `assembleDebug` không lỗi, không có cảnh báo mới.
- [ ] Chạy thử toàn bộ các mục "Kiểm tra" ở trên trên **ít nhất 1 máy thật** (Android 12+) và 1 giả lập Android 10/11.
- [ ] Thử với hàng đợi lớn (≥ 500 bài): cuộn Hàng đợi mượt.
- [ ] Thử với bài có bìa động và bài không có.
- [ ] Mỗi mục là một commit riêng, message ghi rõ số mục (ví dụ `fix(now-playing): mục 2 - vuốt xóa đúng bài`).
