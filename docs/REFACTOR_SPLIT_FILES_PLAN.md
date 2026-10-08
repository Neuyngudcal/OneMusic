# Kế hoạch tách các file code lớn thành file nhỏ (dễ bảo trì, dễ sửa)

Tài liệu này mô tả **từng bước** cách chia các file Kotlin quá lớn của OneMusic thành nhiều file nhỏ, mỗi file một trách nhiệm. Mục tiêu là **không thay đổi hành vi app** – chỉ sắp xếp lại code để lần sau sửa lỗi/thêm tính năng nhanh hơn, ít xung đột (merge conflict) hơn và review dễ hơn.

> Số dòng ghi trong tài liệu là số dòng tại thời điểm viết (07/10/2026). Nếu file đã thay đổi, hãy dùng **Ctrl+F** tìm theo tên hàm hoặc đoạn comment được trích, đừng tin tuyệt đối vào số dòng.
>
> Mọi đường dẫn bên dưới tính từ `app/src/main/java/com/example/onemusic/`.

---

## 0. Hiện trạng

### 0.1 Các file lớn nhất

Toàn bộ code Kotlin khoảng **27.000 dòng**; riêng 5 file đầu đã chiếm **~11.000 dòng (40%)**.

| # | File | Số dòng | Vấn đề chính |
|---|---|---|---|
| 1 | `ui/screens/player/NowPlayingSheet.kt` | 3.739 | Hàm `NowPlayingSheet()` dài **~2.170 dòng** (dòng 507–2676): gom cả cử chỉ kéo, pager ảnh bìa, lời bài hát, hàng đợi, bộ điều khiển, 5 dialog, toast. Thêm ~1.000 dòng composable phụ ở cuối file. |
| 2 | `ui/screens/home/HomeScreen.kt` | 2.236 | Hàm `HomeScreen()` dài **~2.030 dòng**: 7 màn con (`HomeSubView`) viết chung trong một `when`. |
| 3 | `ui/screens/library/LibraryScreen.kt` | 2.170 | Hàm `LibraryScreen()` dài **~1.700 dòng**: 3 tab × 3 chế độ xem + chọn nhiều + thanh chữ cái. |
| 4 | `playback/MusicPlayerController.kt` | 1.457 | Một class làm 8 việc: khởi tạo ExoPlayer, hàng đợi/shuffle, video bìa động, hẹn giờ tắt, fade âm lượng, theo dõi vị trí, tải lời, preload. |
| 5 | `ui/screens/search/SearchScreen.kt` | 1.432 | Hàm `SearchScreen()` dài **~930 dòng**. |
| 6 | `ui/screens/settings/SettingsScreen.kt` | 938 | Hàm `SettingsScreen()` dài ~630 dòng (5 nhóm cài đặt). |
| 7 | `data/scanner/AppleMusicMotionFetcher.kt` | 843 | Gom iTunes search + Apple AMP API + tải HLS + HTTP + so khớp chuỗi. |
| 8 | `MainActivity.kt` | 806 | Composable gốc `OneMusicApp()` dài ~540 dòng. |
| 9 | `ui/screens/detail/DetailScreen.kt` | 776 | 4 "layer" giao diện trong một hàm. |
| 10 | `ui/screens/dedup/DuplicateCleanerScreen.kt` | 729 | |
| 11 | `data/scanner/ReplayGainExtractor.kt` | 634 | 5 bộ đọc định dạng (FLAC, ID3, MP4, OGG…) trong một `object`. |
| 12 | `data/search/MusicSearchEngine.kt` | 572 | 4 `object`/class độc lập nằm chung file. |
| 13 | `ui/screens/folder/FolderManagerScreen.kt` | 565 | |
| 14 | `ui/components/AddToPlaylistDialog.kt` | 526 | 2 dialog độc lập chung file. |

Các file dưới ~450 dòng **không cần đụng** trong kế hoạch này.

### 0.2 Vì sao cần tách

- **Khó tìm:** muốn sửa thanh tua phải cuộn qua 2.000 dòng của `NowPlayingSheet()`.
- **Hay xung đột:** hai người sửa hai tính năng khác nhau của Now Playing đều đụng cùng một hàm → merge conflict.
- **Vẽ lại thừa (recomposition):** composable càng to, một `State` đổi càng kéo theo nhiều code chạy lại.
- **Không test được:** logic hàng đợi nằm lẫn trong `MusicPlayerController` (dính ExoPlayer) nên `PlaybackQueueTest` phải **chép lại thuật toán** thay vì gọi code thật – test xanh nhưng không bảo vệ gì.
- **Code lặp:** `formatDuration` được viết 4 lần ở 4 file, mỗi bản một kiểu định dạng khác nhau.

---

## 1. Nguyên tắc chung (đọc trước khi bắt tay)

1. **Chỉ di chuyển, không sửa hành vi.** Mỗi commit tách file chỉ cắt/dán + sửa import + đổi `private` → `internal`. Sửa lỗi thì làm ở commit/PR riêng. Như vậy reviewer kiểm tra được bằng `git diff --color-moved` (xem mục 2.3).
2. **Giữ nguyên tên hàm/class công khai và chữ ký (signature).** Nơi gọi (`MainActivity`, `OneMusicPlaybackService`…) không phải sửa gì ngoài import.
3. **Một file = một trách nhiệm.** Mục tiêu: file ≤ **400 dòng** (tối đa 600), hàm `@Composable` ≤ **~150–200 dòng** (không tính tham số).
4. **Cùng package khi có thể.** Tách trong cùng thư mục màn hình (ví dụ `ui/screens/home/`) thì không cần thêm import ở nơi gọi. Riêng Now Playing có ~18 file nên chia thư mục con (xem mục 3).
5. **`private` cấp file → `internal`.** Trong Kotlin, `private` ở cấp file nghĩa là "chỉ file này thấy". Khi chuyển hàm sang file khác, đổi thành `internal` (chỉ module `app` thấy) – **không** đổi thành `public`.
6. **Tách từ lá vào gốc.** Làm trước những phần đã là hàm riêng (chỉ cắt/dán), sau đó mới tách các khối nằm *bên trong* hàm lớn, cuối cùng mới tách state/logic.
7. **PR nhỏ.** Mỗi PR một file lớn (hoặc một bước của file lớn), dễ review, dễ revert.

---

## 2. Quy trình chuẩn cho MỖI lần tách

### 2.1 Checklist

1. Lấy code mới nhất, tạo branch: `git checkout -b refactor/split-<tên-file>-<bước>`.
2. **Trước khi sửa:** build + test phải xanh (lệnh ở 2.2). Mở app, chụp màn hình/quay video màn hình sắp tách để so sánh sau.
3. Tách code (ưu tiên dùng công cụ refactor của Android Studio – mục 2.4).
4. Build + chạy unit test + cài lên máy.
5. Chạy **smoke test** của màn hình đó (danh sách ở mục 10).
6. Commit, ghi rõ "không đổi hành vi", ví dụ:
   `refactor(player): tách CapsuleSlider ra file riêng (không đổi hành vi)`
7. Mở PR, ghi trong mô tả: file nguồn, file đích, khoảng dòng đã chuyển.

### 2.2 Lệnh build/test

```bash
./gradlew :app:compileDebugKotlin     # kiểm tra biên dịch nhanh
./gradlew :app:testDebugUnitTest      # unit test (PlaybackQueue, LrcParser, ReplayGain, Search...)
./gradlew :app:installDebug           # cài lên máy/giả lập để smoke test
```

### 2.3 Cách review một PR "chỉ di chuyển"

```bash
git diff --color-moved=dimmed-zebra --color-moved-ws=allow-indentation-change main...HEAD
```

Dòng bị **di chuyển nguyên vẹn** sẽ hiện màu mờ; chỉ những dòng **thật sự thay đổi** (import, `internal`, tham số mới) mới hiện màu đỏ/xanh đậm. Reviewer chỉ cần đọc phần màu đậm.

### 2.4 Công cụ Android Studio nên dùng

| Thao tác | Phím tắt | Dùng khi |
|---|---|---|
| **Move** (chuyển hàm/class sang file khác) | `F6` | Hàm đã là top-level (ví dụ `CapsuleSlider`). Tự cập nhật import ở mọi nơi gọi. |
| **Extract Function** | `Ctrl+Alt+M` | Biến một khối code trong hàm lớn thành hàm mới; IDE tự tìm biến cần truyền vào. |
| **Optimize Imports** | `Ctrl+Alt+O` | Sau mỗi lần cắt/dán – xóa import thừa ở file cũ, file mới có thể cần `Alt+Enter` để thêm import. |
| **Find Usages** | `Alt+F7` | Kiểm tra ai đang dùng một hàm trước khi đổi tầm nhìn (`private`/`internal`). |

---

## 3. Kỹ thuật tách trong Jetpack Compose (kèm ví dụ)

### 3.1 Tách một khối giao diện thành composable con

Truyền **giá trị + lambda**, không truyền cả `MutableState`. Composable con không tự sửa state của cha.

```kotlin
// TRƯỚC – nằm trong NowPlayingSheet(), khoảng dòng 2440–2569
if (showSleepTimerDialog) {
    /* ~130 dòng giao diện, đọc playbackState.sleepTimerMinutes, gọi onSetSleepTimer... */
}

// SAU – NowPlayingSheet.kt
if (showSleepTimerDialog) {
    SleepTimerDialog(
        sleepTimerMinutes = playbackState.sleepTimerMinutes,
        sleepTimerRemainingSeconds = playbackState.sleepTimerRemainingSeconds,
        hazeState = nowPlayingHazeState,
        onSetSleepTimer = onSetSleepTimer,
        onSetSleepTimerEndOfTrack = onSetSleepTimerEndOfTrack,
        onCancelSleepTimer = onCancelSleepTimer,
        onDismiss = { showSleepTimerDialog = false }
    )
}

// SAU – player/dialogs/SleepTimerDialog.kt
@Composable
internal fun SleepTimerDialog(
    sleepTimerMinutes: Int?,
    sleepTimerRemainingSeconds: Long?,
    hazeState: HazeState,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onDismiss: () -> Unit
) { /* dán nguyên khối giao diện cũ vào đây, `showSleepTimerDialog = false` → `onDismiss()` */ }
```

### 3.2 Tách nội dung `LazyColumn` → hàm mở rộng `LazyListScope`

`item { }` / `items { }` chỉ gọi được bên trong `LazyListScope`, nên **không** tách thành `@Composable`. Hãy viết hàm mở rộng (extension function):

```kotlin
// SAU – settings/SettingsAudioSection.kt
internal fun LazyListScope.settingsAudioSection(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    item(key = "header_audio") { SettingsSectionHeader("Âm thanh & phát nhạc") }
    item(key = "section_card_audio") {
        SettingsGroupCard {
            SettingsActionRow(
                icon = Icons.Rounded.Tune,
                title = "Bộ chỉnh âm",
                subtitle = "Chỉnh 9 dải tần, tăng bass và chọn cấu hình SoundAlive",
                onClick = onOpenEqualizer
            )
            // ... các hàng còn lại giữ nguyên
        }
    }
}

// SAU – SettingsScreen.kt
LazyColumn(/* ... */) {
    item(key = "settings_header") { /* header */ }
    settingsAudioSection(settings, settingsPreferences::updateSettings, onOpenEqualizer = { showEqualizerDialog = true })
    settingsDisplaySection(/* ... */)
    // ...
}
```

> ⚠️ Giữ nguyên các `key = "..."` của item. Khi tách ra nhiều hàm, kiểm tra không có hai item trùng key (LazyColumn sẽ crash).

### 3.3 Tách nhóm state + hàm lồng nhau → "state holder"

Trong `NowPlayingSheet()` có các hàm lồng (`collapseSheet`, `Modifier.sheetDragToDismiss`) dùng chung nhiều biến cục bộ (`sheetOffsetY`, `isDismissing`, `screenHeightPx`…). Gom chúng vào một class + hàm `remember…`:

```kotlin
// player/state/NowPlayingSheetState.kt
@Stable
internal class NowPlayingSheetState(
    val offsetY: Animatable<Float, AnimationVector1D>,
    private val screenHeightPx: Float,
    private val slideSpec: AnimationSpec<Float>,
    private val scope: CoroutineScope,
) {
    var isDismissing by mutableStateOf(false)
        private set

    val dismissProgress: Float
        get() = (offsetY.value / screenHeightPx).coerceIn(0f, 1f)

    fun collapse(initialVelocity: Float = 0f, onHaptic: () -> Unit, onCollapsed: () -> Unit) {
        if (isDismissing) return
        isDismissing = true
        onHaptic()
        scope.launch {
            offsetY.animateTo(screenHeightPx, slideSpec, initialVelocity.coerceAtLeast(0f))
            onCollapsed()
        }
    }
}

@Composable
internal fun rememberNowPlayingSheetState(): NowPlayingSheetState { /* remember { ... } */ }
```

Đây là bước **rủi ro nhất** (dễ làm sai thời điểm cử chỉ) → làm **cuối cùng**, sau khi các phần giao diện đã tách xong.

### 3.4 Giữ nguyên chỗ đặt `LaunchedEffect` / `remember`

Một `LaunchedEffect` đang ở cấp `NowPlayingSheet()` nếu bị chuyển vào nhánh `NowPlayingCenterView.LYRICS -> { ... }` sẽ **bị hủy và chạy lại mỗi lần đổi tab** → đổi hành vi. Khi tách:

- Effect đang ở hàm cha → **để nguyên ở hàm cha**, hoặc chuyển vào state holder (3.3), **không** chuyển vào composable con nằm trong `if`/`when`.
- `remember { }` chuyển sang composable con sẽ bị reset khi con đó rời khỏi màn hình. Nếu giá trị cần sống lâu hơn → để ở cha và truyền xuống.

### 3.5 Đọc `State` ở composable nhỏ nhất

App đã làm đúng với `positionState` (chỉ `NowPlayingProgressSection` đọc `.value` → mỗi 40ms chỉ thanh tua vẽ lại). Khi tách, **giữ nguyên cách truyền `State<Long>`**, đừng đổi thành truyền `Long` (sẽ làm cả sheet vẽ lại 25 lần/giây).

---

## 4. Thứ tự làm đề xuất

| Giai đoạn | Nội dung | Độ khó | Số PR (ước tính) | Ghi chú |
|---|---|---|---|---|
| **0** | Chuẩn bị: build/test xanh, chụp màn hình mốc | ⭐ | 0 | |
| **1** | Gom hàm tiện ích bị lặp (`formatDuration`…) | ⭐ | 1 | Khởi động, làm quen quy trình |
| **2** | `NowPlayingSheet.kt` | ⭐⭐⭐ | 5–6 | File lớn nhất, lợi ích nhiều nhất |
| **3** | `HomeScreen.kt` | ⭐⭐ | 3 | |
| **4** | `LibraryScreen.kt` | ⭐⭐ | 3 | |
| **5** | `SearchScreen.kt`, `SettingsScreen.kt` | ⭐⭐ | 2 | |
| **6** | `MusicPlayerController.kt` | ⭐⭐⭐⭐ | 4–5 | Logic phát nhạc – cần viết test trước, nhờ người review kỹ |
| **7** | `MainActivity.kt` + các file 500–850 dòng | ⭐⭐ | 4–5 | Làm dần, có thể song song |
| **8** | Rào chắn chống phình lại | ⭐ | 1 | |

> **Phối hợp với `NOW_PLAYING_FIX_PLAN.md` và `UI_UX_FIX_PLAN.md`:** hai tài liệu đó trỏ số dòng trong các file sắp bị tách. Nên **merge xong các mục sửa lỗi đang làm dở** trước khi bắt đầu giai đoạn tương ứng, rồi mới tách. Bước 2.1 (chỉ chuyển hàm đã top-level) an toàn làm song song. Sau khi tách, các số dòng trong 2 tài liệu đó sẽ lỗi thời – tìm theo tên hàm.

---

## Giai đoạn 1 – Gom hàm tiện ích bị lặp ✅ Đã xong (07/10/2026)

**Hiện trạng trước khi làm:** có 4 bản `formatDuration` với **định dạng khác nhau**, cộng một bản viết tay trong dialog hẹn giờ tắt:

| File | Kết quả cho 65 giây | Ghi chú |
|---|---|---|
| `ui/screens/player/NowPlayingSheet.kt:201` (public) | `1:05` | `ms <= 0` → `0:00`, không có `Locale` |
| `ui/screens/player/NowPlayingSheet.kt:2463` (viết tay) | `01:05` | Đồng hồ đếm ngược hẹn giờ tắt |
| `ui/screens/home/HomeScreen.kt:185` (private) | `1:05` | **Không được gọi ở đâu** (code chết) |
| `ui/screens/library/LibraryScreen.kt:1908` (private) | `01:05` | |
| `ui/screens/dedup/DuplicateCleanerScreen.kt:723` (private) | `01:05` | |

Ngoài ra `getAvatarColorForArtist()` trong `SearchScreen.kt:153` **cũng không được gọi ở đâu**.

**Đã làm:**

1. Tạo `ui/utils/DurationFormat.kt` gồm `formatDuration(ms, padMinutes = false)`, `formatRemaining(currentMs, totalMs)` và `formatTotalDuration(tracks)` (chuyển từ `DetailScreen.kt`).
2. Now Playing gọi `formatDuration(ms)`; Library, Dedup và đồng hồ hẹn giờ tắt gọi `formatDuration(ms, padMinutes = true)` → **giữ nguyên hiển thị** từng màn. (Thống nhất một kiểu cho cả app là quyết định thiết kế, làm ở PR khác nếu muốn.)
3. Xóa 2 hàm chết: `formatDuration` của Home và `getAvatarColorForArtist` của Search.
4. Thêm `app/src/test/.../ui/utils/DurationFormatTest.kt` (6 test).

**Khác biệt duy nhất có chủ đích:** hàm mới luôn dùng `Locale.US`. Bản cũ dùng ngôn ngữ của máy nên trên máy đặt ngôn ngữ dùng chữ số khác (ví dụ tiếng Ả Rập) sẽ hiện `١:٠٥` thay vì `1:05`. Với tiếng Việt/tiếng Anh kết quả giống hệt (đã so sánh tự động ~257.000 giá trị đầu vào giữa hàm mới và các bản cũ: 0 khác biệt).

**Kiểm tra trên máy:** thời lượng bài ở Now Playing (thanh tua, thời gian còn lại, hàng đợi), Thư viện (danh sách + lưới), Dọn trùng lặp, trang chi tiết album/nghệ sĩ/playlist ("x bài hát • y phút") và đồng hồ hẹn giờ tắt hiển thị y như trước.

---

## Giai đoạn 2 – `NowPlayingSheet.kt` (3.739 dòng → ~18 file)

### 2.0 Cấu trúc đích

Tạo thư mục con theo vai trò (mỗi thư mục là một package con của `ui.screens.player`):

```
ui/screens/player/
├── NowPlayingSheet.kt                 (~300)  Ghép các layer, giữ state chung, BackHandler
├── NowPlayingCenterView.kt            (~10)   enum ARTWORK / LYRICS / QUEUE        ← dòng 492–496
├── state/
│   ├── NowPlayingSheetState.kt        (~280)  Kéo-để-đóng, collapse, nested scroll  ← dòng 557–575, 818–1083
│   ├── ArtworkPagerSync.kt            (~100)  Đồng bộ pager ↔ bài đang phát         ← dòng 690–778
│   └── ControlsDeckVisibility.kt      (~90)   Ẩn/hiện cụm điều khiển khi cuộn       ← dòng 604–687
├── backdrop/
│   └── NowPlayingBackdrop.kt          (~270)  LAYER 0, 1B + màu từ ảnh bìa; LAYER 1A ở artwork/HeroArtworkPager.kt ← dòng 779–817, 1107–1382
├── artwork/
│   ├── StaticAlbumArtwork.kt          (~95)                                         ← dòng 2981–3073
│   ├── MotionArtworkPlayer.kt         (~300)                                        ← dòng 3074–3372
│   └── HeroArtworkPager.kt            (~150)  Pager ảnh bìa (LAYER 1A)               ← dòng 1260–1363
├── lyrics/
│   ├── NowPlayingLyricsPane.kt        (~250)  Nhánh LYRICS + tự cuộn tới câu đang hát ← dòng 920–1000, 1447–1610
│   └── LyricLineItems.kt              (~300)  isInstrumentalLine, InstrumentalDotsLyricItem,
│                                              WordByWordLyricItem, LyricWordChip     ← dòng 2677–2980
├── queue/
│   ├── NowPlayingQueuePane.kt         (~370)  Nhánh QUEUE (danh sách, vuốt xóa, kéo đổi chỗ) ← dòng 1611–1976
│   └── QueueComponents.kt             (~280)  QueueTopHeader, QueuePlaybackModesRow,
│                                              QueueFlatTrackRow                       ← dòng 3463–3739
├── controls/
│   ├── NowPlayingControlsDeck.kt      (~120)  Khung AnimatedVisibility của cụm điều khiển ← dòng 1977–2004, 2317–2339
│   ├── NowPlayingTrackHeader.kt       (~115)  Tên bài, nghệ sĩ, nút ⋯, nút tim       ← dòng 2005–2114
│   ├── AudioQualityBadge.kt           (~80)   Badge Lossless/Hi-Res                  ← dòng 2120–2193
│   ├── MasterPlaybackControls.kt      (~75)   Prev / Play-Pause / Next               ← dòng 2248–2316
│   ├── NowPlayingProgressSection.kt   (~95)                                         ← dòng 314–406
│   ├── CapsuleSlider.kt               (~95)                                         ← dòng 221–313
│   ├── NowPlayingActionDock.kt        (~85)                                         ← dòng 407–491
│   └── ApexDynamicEqualizerBars.kt    (~90)                                         ← dòng 3373–3462
└── dialogs/
    ├── PlaybackSpeedDialog.kt         (~100)                                        ← dòng 2340–2438
    ├── SleepTimerDialog.kt            (~130)                                        ← dòng 2439–2569
    └── FavoriteToastBanner.kt         (~60)                                         ← dòng 2625–2676
```

Các dialog đã là component dùng chung (`TrackDetailsDialog`, `ApexEqualizerDialog`, `ApexTrackActionSheet` – dòng 2570–2624) thì **giữ nguyên lời gọi** trong `NowPlayingSheet.kt`.

### 2.1 Bước 1 – Chuyển các hàm đã là top-level (PR 1, ⭐) ✅ Đã xong (07/10/2026)

Chỉ cắt/dán, **không** đổi một dòng logic nào:
`CapsuleSlider`, `NowPlayingProgressSection`, `NowPlayingActionDock`, `NowPlayingCenterView`, `isInstrumentalLine` + 3 composable lời bài hát, `StaticAlbumArtwork`, `MotionArtworkPlayer`, `ApexDynamicEqualizerBars`, 3 composable hàng đợi.

- Dùng `F6` (Move) cho từng hàm; đổi `private` → `internal`.
- Kết quả: `NowPlayingSheet.kt` còn ~2.300 dòng, **~1.400 dòng đã ra file riêng** mà rủi ro gần như bằng 0.

**Kết quả thực tế:** `NowPlayingSheet.kt` 3.722 → 2.357 dòng; 1.333 dòng code chuyển sang 9 file (`NowPlayingCenterView.kt`, `controls/` ×4, `lyrics/LyricLineItems.kt`, `artwork/` ×2, `queue/QueueComponents.kt`), đã đối chiếu tự động là giống hệt bản gốc. Chỉ đổi `private` → `internal` cho 5 hàm được gọi từ file khác (`NowPlayingProgressSection`, `isInstrumentalLine`, `WordByWordLyricItem`, `StaticAlbumArtwork`, `MotionArtworkPlayer`); `InstrumentalDotsLyricItem` và `LyricWordChip` vẫn `private` vì chỉ dùng trong `LyricLineItems.kt`. Xóa 30 import không còn dùng trong `NowPlayingSheet.kt`. `ApexDynamicEqualizerBars` đặt ở `controls/` nhưng hiện chỉ hàng đợi (`QueueTopHeader`) dùng.

### 2.2 Bước 2 – Tách dialog & toast trong thân hàm (PR 2, ⭐) ✅ Đã xong (07/10/2026)

`PlaybackSpeedDialog`, `SleepTimerDialog`, `FavoriteToastBanner` theo mẫu ở mục 3.1. Biến `isSpeedMenuOpen`, `showSleepTimerDialog`, `favoriteToastMessage` **vẫn ở `NowPlayingSheet()`** (vì `BackHandler` dòng 1084 và nút ở dock cũng dùng).

**Kết quả thực tế:** `NowPlayingSheet.kt` 2.357 → 2.102 dòng; 3 file mới trong `dialogs/`. Mỗi dialog nhận giá trị + lambda thay vì đọc biến của sheet: `isSpeedMenuOpen = false` / `showSleepTimerDialog = false` → `onDismiss()`, `playbackState.xxx` → tham số, `nowPlayingHazeState` → `hazeState`. `FavoriteToastBanner` nhận `Modifier.align(Alignment.BottomCenter)` từ Box cha qua tham số `modifier` (thứ tự modifier giữ nguyên). Đã kiểm tra ngược tự động: hoàn tác các phép đổi tên thì thân 3 hàm khớp tuyệt đối với khối gốc, và thân `NowPlayingSheet.kt` chỉ khác đúng 3 chỗ gọi.

### 2.3 Bước 3 – Tách nền (backdrop) (PR 3, ⭐⭐) ✅ Đã xong (07/10/2026)

`NowPlayingBackdrop(displayedTrack, pagerState, colors, blurRadius, frostedGlassAlpha, motionPlayer, ...)`. Lưu ý: pager ảnh bìa (LAYER 1A) nhận cử chỉ vuốt ngang → truyền `pagerState` từ cha xuống, **không** tạo `rememberPagerState` mới bên trong.

**Kết quả thực tế:** `NowPlayingSheet.kt` 2.102 → 1.805 dòng.
- `backdrop/NowPlayingBackdrop.kt` (266 dòng): màu động từ ảnh bìa, độ mờ nền, lớp voan (LAYER 1B) và LAYER 0. Các giá trị này **chỉ dùng trong phần nền** nên chuyển hẳn vào đây; khi màu đổi (600 ms) giờ chỉ phần nền vẽ lại thay vì cả sheet.
- `artwork/HeroArtworkPager.kt` (151 dòng): LAYER 1A tách riêng để mỗi hàm dưới 200 dòng. `pagerState` vẫn tạo ở `NowPlayingSheet()` và truyền xuống. Màu viền mờ (`scrimColor`) tính một lần ở `NowPlayingBackdrop` thay vì trong từng trang (cùng giá trị).
- Tham số: `isArtworkMode`, `displayedTrack`, `track`, `queue`, `pagerState`, `isDynamicMeshBackgroundEnabled`, `motionVideoPath`, `motionPlayer`, `hazeState`, `isSheetFullyVisible` (= `!isDismissing`), `controlsDeckHeight`, `onArtworkLongClick` (= mở "Thông tin bài hát").
- Khối LAYER 0 vốn thụt lề lệch trong bản gốc, đã chỉnh lại (chỉ đổi khoảng trắng). Kiểm tra ngược tự động: khớp nội dung với bản gốc; `NowPlayingSheet.kt` chỉ đổi đúng 3 chỗ.
- Dọn import thừa trong cả thư mục `player/` (vd `key`, `alpha`, `size` thực chất là tên tham số hoặc thuộc tính của DrawScope/View, không phải hàm được import).

### 2.4 Bước 4 – Tách khung Lời bài hát và Hàng đợi (PR 4, ⭐⭐⭐) ✅ Đã xong (07/10/2026)

- `NowPlayingLyricsPane(track, positionState, activeLyricIndex, listState, nestedScrollConnection, onSeek, ...)`.
- `NowPlayingQueuePane(queue, currentIndex, listState, nestedScrollConnection, onPlayQueueIndex, onMoveQueueItem, onRemoveQueueItem, ...)`.
- `lyricsListState`, `queueListState` và các `LaunchedEffect` tự cuộn (dòng 920–1000) **vẫn tạo ở `NowPlayingSheet()`** (xem 3.4) – vì `AnimatedContent` hủy nhánh LYRICS khi chuyển sang QUEUE.

**Kết quả thực tế:** `NowPlayingSheet.kt` 1.805 → 1.287 dòng.
- `lyrics/NowPlayingLyricsPane.kt` (223 dòng): `NowPlayingLyricsPane(track, listState, nestedScrollConnection, activeLyricIndex: () -> Int, positionState, onSeekToLine)`. `activeLyricIndex` truyền dạng lambda để giá trị vẫn chỉ được đọc trong từng dòng lời như trước (đổi câu không làm vẽ lại cả khung). Việc đặt lại `lastLyricsUserScrollTimeMs` + cuộn tới dòng khi bấm một từ vẫn chạy ở sheet qua `onSeekToLine`.
- `queue/NowPlayingQueuePane.kt` (~460 dòng): nhận `playbackState` + các callback; mở dialog (`isSpeedMenuOpen = true`…) đổi thành callback `onOpenSpeedMenu`, `onOpenSleepTimer`, `onOpenDetails`, `onOpenOptions`.
- `lyricsListState`, `queueListState`, 2 nested scroll connection và mọi `LaunchedEffect` tự cuộn **vẫn ở `NowPlayingSheet()`**. Kiểm tra ngược tự động: hoàn tác đổi tên thì thân 2 hàm khớp tuyệt đối với nhánh gốc.
- **Bước 2.4b ✅ Đã xong (07/10/2026):** tách hàng "Tiếp tục phát" (vuốt xóa + kéo đổi thứ tự) thành `queue/QueueReorderableRow.kt` (188 dòng): `LazyItemScope.QueueReorderableRow(track, actualIndex, queueLastIndex, minReorderIndex, onPlayQueueIndex, onMoveQueueItem, onRemoveQueueItem)` – là hàm mở rộng của `LazyItemScope` vì dùng `Modifier.animateItem`. `actualIndex` vẫn tính ở `NowPlayingQueuePane`; các `rememberUpdatedState` giữ nguyên trong hàng nên cử chỉ kéo không bị khởi động lại. `NowPlayingQueuePane.kt` còn 321 dòng (hàm ~230 dòng). Kiểm tra ngược: chỉ 5 dòng khác bản gốc, đúng là các dòng đổi tên (`nTrack` → `track`, 2 giới hạn kéo thành tham số).

### 2.5 Bước 5 – Tách cụm điều khiển (PR 5, ⭐⭐) ✅ Đã xong (07/10/2026)

`NowPlayingControlsDeck` chứa `NowPlayingTrackHeader`, `AudioQualityBadge`, `NowPlayingProgressSection`, `MasterPlaybackControls`, `NowPlayingActionDock`. Giữ `positionState` dạng `State<Long>` (3.5).

**Kết quả thực tế:** `NowPlayingSheet.kt` 1.287 → 1.004 dòng. Thêm 3 file trong `controls/`:
- `NowPlayingTrackHeader.kt` (156 dòng): `NowPlayingTrackHeader(track, onToggleFavorite, onOpenOptions)` – tên bài, nghệ sĩ, nút tim, nút ⋯.
- `AudioQualityBadge.kt` (179 dòng): `rememberAudioQualityInfo(track)` (nhận dạng FLAC/Hi-Res/định dạng, đọc MIME trên luồng IO) + `AudioQualityBadge(visible, info, onClick, modifier)`. Phần nhận dạng vẫn được gọi **đúng chỗ cũ** trong cụm điều khiển (không chuyển vào slot `centerBadge` vẽ lại 25 lần/giây); chỉ phần giao diện nằm trong slot.
- `MasterPlaybackControls.kt` (89 dòng): `MasterPlaybackControls(isPlaying, onPrevious, onPlayPause, onNext)`. Biến chống bấm liên tục (`lastButtonSkipTimeMs`) **vẫn ở sheet**, nằm trong lambda `onPrevious`/`onNext` – nếu chuyển vào hàm con sẽ bị reset mỗi lần cụm điều khiển ẩn/hiện.
- Khung `AnimatedVisibility` + `onSizeChanged` (đo chiều cao cụm) và `NowPlayingActionDock` vẫn ở sheet vì gắn chặt với `centerView`, `controlsDeckHeightPx` – sẽ xem lại ở bước 2.6. Báo cáo tự động: mọi dòng khác bản gốc đều là phép đổi tên (`displayedTrack` → `track`, mở dialog → callback…).

### 2.6 Bước 6 – Tách state holder (PR 6, ⭐⭐⭐⭐) ✅ Đã xong về code (07/10/2026) – ⚠️ còn chờ smoke test cử chỉ trên máy

`NowPlayingSheetState`, `ArtworkPagerSync`, `ControlsDeckVisibility` theo mẫu 3.3. Làm **cuối cùng**, test kỹ cử chỉ (mục 10.1). Nhờ người đã làm `NOW_PLAYING_FIX_PLAN.md` review.

**Kết quả thực tế:** `NowPlayingSheet.kt` 1.004 → 650 dòng. Thêm 3 file trong `state/`:
- `NowPlayingSheetState.kt` (279 dòng): class `NowPlayingSheetState` giữ `offsetY`, `isDismissing`, ngưỡng đóng + rung khi vượt ngưỡng, `collapse()`, `nestedScrollConnection` (chế độ ảnh bìa) và `dragToDismissModifier(enabled, isPagerScrolling)`; `rememberNowPlayingSheetState(onCollapse, isArtworkMode)` tạo state và chạy hoạt ảnh trượt lên lúc mở (`LaunchedEffect(Unit)` cũ). Trong sheet vẫn giữ hàm cục bộ `Modifier.sheetDragToDismiss(enabled)` (gọi sang state) nên 2 chỗ dùng không đổi.
- `ArtworkPagerSync.kt` (114 dòng): `rememberArtworkPagerState(queue, currentIndex, centerView, onPlayQueueIndex): PagerState` – tạo pager, đồng bộ bài đang phát → pager, vuốt pager → phát bài, chống kẹt giữa 2 trang, nạp trước ảnh bìa bài kế/trước. `displayedTrack` vẫn tính ở sheet.
- `ControlsDeckVisibility.kt` (125 dòng): class giữ `isLyricsDeckVisible`/`isQueueDeckVisible`, 2 `derivedStateOf` "đang ở đầu danh sách", 2 nested scroll connection và `isVisible(centerView)`; `rememberControlsDeckVisibility(...)` chạy 4 `LaunchedEffect` cũ (thứ tự giữ nguyên).
- Các `LaunchedEffect` tự cuộn lời/hàng đợi, `previousCenterView`, chống bấm liên tục và khung `AnimatedVisibility` + `onSizeChanged` **vẫn ở sheet** (xem 3.4).
- Gộp 2 đoạn code giống hệt nhau thành hàm riêng trong `NowPlayingSheetState`: rung khi vượt ngưỡng (`updateThresholdHaptic`, trước lặp 2 lần) và bật về vị trí mở (`settleOpen`, trước lặp 3 lần).
- **Khác biệt nhỏ có chủ đích:** trước đây `NestedScrollConnection` nằm trong `remember {}` nên chụp `onCollapse` của lần vẽ đầu tiên; giờ luôn gọi bản mới nhất qua `rememberUpdatedState` (giống `BackHandler`). `screenHeightPx` chụp một lần lúc tạo state – như bản gốc ở nhánh nested scroll; Activity không khai báo `configChanges` nên xoay màn hình vẫn tạo lại sheet.
- Kiểm tra: `git diff --color-moved` – mọi dòng không phải "di chuyển nguyên vẹn" đều là đổi tên (`sheetOffsetY` → `offsetY`, `collapseSheet` → `collapse`, `centerView == ARTWORK` → `isArtworkMode.value()`…). 3 file trong `state/` đã biên dịch thử được với Compose Desktop 1.7.3 + Kotlin 2.1.0 (stub phần Android). **Chưa build/cài được bản Android** trong môi trường cloud (không tải được Android SDK) → cần chạy `./gradlew :app:compileDebugKotlin` và smoke test 10.1 (kéo đóng, hất đóng, vuốt ngang ảnh bìa, cuộn lời/hàng đợi ẩn-hiện cụm điều khiển, nút Back) trước khi merge.

---

## Giai đoạn 3 – `HomeScreen.kt` (2.236 dòng → ~12 file) ✅ Đã xong (07/10/2026)

### 3.0 Cấu trúc đích

```
ui/screens/home/
├── HomeScreen.kt                (~250)  State, BackHandler, AnimatedContent chọn HomeSubView, dialog
├── HomeModels.kt                (~50)   HomeCategory, AlbumItemData, ArtistItemData, ArtistGroupMode,
│                                        AlbumViewMode, HomeSubView, extractArtistNames   ← dòng 159–200
├── main/                                  ── nhánh HomeSubView.MAIN (dòng 401–1167) ──
│   ├── HomeMainContent.kt       (~120)  LazyColumn gọi các section bên dưới, header, thẻ thư viện trống ← 407–487
│   ├── HomeHeroBanner.kt        (~115)  Bài nổi bật                                       ← 488–597
│   ├── HomeRecentSection.kt     (~220)  "Nghe gần đây" + "Có thể bạn sẽ thích"            ← 598–814
│   ├── HomePlaylistsCarousel.kt (~150)  "Playlist yêu thích"                              ← 815–959
│   ├── HomeTopArtistsSection.kt (~120)  "Nghệ sĩ nghe nhiều"                              ← 960–1074
│   └── HomeExploreSection.kt    (~95)   "Khám phá thêm"                                   ← 1075–1167
├── HomePlaylistsContent.kt      (~220)  HomeSubView.PLAYLISTS                             ← 1168–1384
├── HomeArtistsContent.kt        (~215)  HomeSubView.ARTISTS                               ← 1385–1597
├── HomeAlbumsContent.kt         (~430)  HomeSubView.ALBUMS (danh sách + lưới 2/3 cột)     ← 1598–2028
├── HomeDetailRoutes.kt          (~110)  3 nhánh ARTIST/ALBUM/PLAYLIST_DETAIL → DetailScreen ← 2029–2136
└── NewPlaylistDialog.kt         (~75)                                                     ← 2137–2209
```

> Nếu muốn ít thư mục, có thể để 6 file `main/` ngang hàng trong `home/` – quan trọng là mỗi section một file.

### 3.1 Các bước

1. **PR 1:** `HomeModels.kt` + `NewPlaylistDialog.kt` (chỉ cắt/dán).
2. **PR 2:** Tách 4 màn con `PLAYLISTS`, `ARTISTS`, `ALBUMS`, các nhánh `*_DETAIL`. Mỗi màn con là một `@Composable` nhận `tracks`, callback, và `onBack`/`onOpenDetail`. Các biến `albumViewMode`, `artistGroupMode` đang dùng `rememberSaveable` ở `HomeScreen()` → **giữ ở cha** để không mất khi chuyển màn con.
3. **PR 3:** Tách màn chính thành các `LazyListScope.homeXxxSection(...)` (mục 3.2).

**Kết quả thực tế (07/10/2026) ✅ Đã xong về code – ⚠️ còn chờ build Android + smoke test 10.2:** `HomeScreen.kt` 2.229 → 436 dòng; 12 file mới, để **ngang hàng trong `home/`** (cùng package, không cần thêm import ở nơi gọi):
- `HomeModels.kt` (42), `NewPlaylistDialog.kt` (116) – tên đang gõ (`newPlaylistName`) vẫn giữ ở `HomeScreen()` nên đóng/mở lại hộp thoại không mất chữ, như cũ.
- Màn chính: `HomeMainContent.kt` (180: `LazyColumn`, lời chào, thẻ thư viện trống) gọi 5 hàm `LazyListScope`: `homeHeroBannerSection` (`HomeHeroBanner.kt`, 160), `homeRecentSection` (`HomeRecentSection.kt`, 266), `homePlaylistsCarouselSection` (`HomePlaylistsCarousel.kt`, 187), `homeTopArtistsSection` (`HomeTopArtistsSection.kt`, 160), `homeExploreSection` (`HomeExploreSection.kt`, 128). Thứ tự và `key` của các item giữ nguyên.
- Màn con: `HomePlaylistsContent.kt` (278), `HomeArtistsContent.kt` (284), `HomeAlbumsContent.kt` (504), `HomeDetailRoutes.kt` (164: `HomeArtistDetailRoute`, `HomeAlbumDetailRoute`, `HomePlaylistDetailRoute`).
- Toàn bộ state điều hướng (`currentSubView`, `detailBackTarget`, `selected*`, `isSelectedFavorites`), chế độ xem (`albumViewMode`, `artistGroupMode`) và trạng thái menu đang mở **vẫn ở `HomeScreen()`** vì `BackHandler` dùng đến; màn con nhận giá trị + callback. Các callback mở chi tiết giữ đúng thân lệnh cũ của từng nơi (vd mở Yêu thích từ Trang chủ thì xóa cả `selectedAlbum`/`selectedArtist`, từ màn Playlist thì không – y như bản gốc).
- Kiểm tra: `git diff --color-moved` – mọi dòng không phải "di chuyển nguyên vẹn" đều là thay phép gán state bằng callback. Cả thư mục `home/` đã biên dịch thử được với Compose Desktop 1.7.3 + Kotlin 2.1.0 (stub phần Android/app theo chữ ký thật). Cần chạy `./gradlew :app:compileDebugKotlin` và smoke test Trang chủ trước khi phát hành.

**Lưu ý:** `HomeAlbumsContent` (~430 dòng) và tab Album của `LibraryScreen` có thể đang vẽ thẻ album giống nhau. Sau khi tách xong cả hai, so sánh – nếu trùng thì gộp thành `ui/components/AlbumGridCard.kt` (PR riêng, có đổi hành vi nhỏ → cần smoke test cả hai màn).

---

## Giai đoạn 4 – `LibraryScreen.kt` (2.170 dòng → ~9 file)

> ✅ **Đã tách (07/10/2026, commit 59d437a trên `main`)** theo cấu trúc hơi khác bên dưới: `LibraryScreen.kt` còn 416 dòng; thêm `LibraryViewModel.kt`, `components/` (FastAlphabetScroller, LibraryBatchActionBar, LibraryCollapsibleHeader, LibraryFilterBar, LibraryTabRow), `items/` (AlbumGridCard, SongGridItem, SongListItem), `tabs/` (Albums/Artists/Playlists/SongsTabContent). Khi gộp nhánh đã thay `LibraryViewModel.formatDuration` bằng `formatDuration(ms, padMinutes = true)` ở `ui/utils` (giữ nguyên hiển thị, xem Giai đoạn 1). Cấu trúc dự kiến dưới đây chỉ còn để tham khảo. Ghi chú: trong commit đó phần Now Playing cũng được tách theo kiểu file phẳng; khi gộp đã giữ cấu trúc thư mục con của Giai đoạn 2 và bỏ các file phẳng đó.

```
ui/screens/library/
├── LibraryScreen.kt             (~300)  State, sắp xếp, mở DetailScreen, Box layout, dialog, menu thao tác
├── LibraryModels.kt             (~40)   LibraryTab, SongSortOption, LibraryViewMode        ← dòng 170–200
├── LibraryHeader.kt             (~350)  Header + thanh tab lọc (LazyListScope)             ← 402–752
├── LibraryQuickActions.kt       (~80)   "Phát tất cả" / "Trộn bài" + nhãn section           ← 753–830
├── LibrarySongsSection.kt       (~340)  Thẻ trống + LIST / GRID_2 / GRID_3                  ← 831–1164
├── LibraryAlbumsSection.kt      (~320)  Tab Album                                           ← 1165–1484
├── LibraryArtistsSection.kt     (~150)  Tab Nghệ sĩ                                         ← 1485–1630
├── LibraryMultiSelect.kt        (~250)  Thanh kéo-để-chọn bên trái + dock thao tác hàng loạt ← 1632–1718, 1744–1895
└── LibraryTrackGridCards.kt     (~260)  LibraryTrackGridCard, LibraryTrackCompactGridCard    ← 1915–2170
```

**Các bước:** PR 1 – `LibraryModels.kt` + `LibraryTrackGridCards.kt` (cắt/dán). PR 2 – 4 section trong `LazyColumn` (mục 3.2). PR 3 – `LibraryMultiSelect.kt`.

**Lưu ý quan trọng:** `findTrackIndexAtY` (dòng 324) **đọc chỉ số bài từ key của item** dạng `"${track.id}_$index"`. Khi chuyển `itemsIndexed(...)` sang `LibrarySongsSection.kt`, **giữ nguyên chính xác định dạng key**, nếu không tính năng kéo-để-chọn nhiều bài sẽ chọn sai bài. Ghi một comment ở cả hai nơi trỏ tới nhau.

---

## Giai đoạn 5 – `SearchScreen.kt` và `SettingsScreen.kt` ✅ Đã xong (08/10/2026)

### 5.1 `SearchScreen.kt` (1.432 dòng → 5 file)

```
ui/screens/search/
├── SearchScreen.kt              (~250)  State, dựng index, tìm kiếm debounce, mở DetailScreen, layout
├── SearchBarHeader.kt           (~135)  Ô tìm kiếm + hàng tab lọc                          ← dòng 371–502
├── SearchBlankState.kt          (~250)  Lịch sử tìm kiếm khi ô trống (theo tab, LazyListScope) ← 515–760
├── SearchResultsSection.kt      (~320)  Spinner, "không tìm thấy", Nghệ sĩ/Album/Bài hát    ← 761–1082
└── SearchResultItems.kt         (~340)  SearchAlbumCard, ArtistRow, ArtistCard, TrackResultRow ← 1095–1432
```

### 5.2 `SettingsScreen.kt` (938 dòng → 7 file)

```
ui/screens/settings/
├── SettingsScreen.kt            (~160)  Header, LazyColumn gọi 5 section, 4 dialog xác nhận
├── SettingsAudioSection.kt      (~145)  Âm thanh & phát nhạc                               ← dòng 192–335
├── SettingsDisplaySection.kt    (~125)  Giao diện                                          ← 336–459
├── SettingsHapticsSection.kt    (~75)   Rung phản hồi                                      ← 460–533
├── SettingsLibrarySection.kt    (~100)  Thư viện & bộ nhớ                                  ← 534–630
├── SettingsAboutSection.kt      (~65)   Thông tin                                          ← 631–693
└── SettingsRows.kt              (~185)  SettingsSectionHeader, GroupCard, Divider, ToggleRow, ActionRow ← 757–938
```

**Kết quả thực tế (08/10/2026) – ⚠️ còn chờ build Android + smoke test 10.2:**

- **Settings:** `SettingsScreen.kt` 938 → 207 dòng. `SettingsAudioSection.kt` (191), `SettingsDisplaySection.kt` (145), `SettingsHapticsSection.kt` (122), `SettingsLibrarySection.kt` (123), `SettingsAboutSection.kt` (96), `SettingsRows.kt` (233, 5 hàm `private` → `internal`). Mỗi nhóm là một hàm `LazyListScope.settingsXxxSection(...)`, nhận `settings` + `settingsPreferences` và callback; `key` các item giữ nguyên. 4 dialog, các cờ mở dialog và `crossfadeDraft` **vẫn ở `SettingsScreen()`**; `crossfadeDraft` truyền dạng lambda `() -> Float` để lúc kéo thanh trượt chỉ hàng đó vẽ lại như cũ.
- **Search:** `SearchScreen.kt` 1.427 → 368 dòng. `SearchBarHeader.kt` (219: `SearchInputBar` + `SearchFilterTabsRow`), `SearchBlankState.kt` (316: `LazyListScope.searchBlankState`), `SearchResultsSection.kt` (376: `LazyListScope.searchResultsSection`), `SearchResultItems.kt` (400: `SearchAlbumCard`, `ArtistRow`, `ArtistCard`, `TrackResultRow`, `private` → `internal`). Dựng index, debounce tìm kiếm, mở `DetailScreen`, `BackHandler`, `focusRequester` + tự focus, lịch sử tìm kiếm (`recentSearches`) **vẫn ở `SearchScreen()`**. Việc lưu từ khóa khi mở kết quả gom vào một lambda `recordRecentSearch(query, label)`; nơi nào trước đây không lưu (vd bấm nghệ sĩ/bài ở tab trống) thì vẫn không lưu. `keyboardController` và `BringIntoViewResponder` chuyển vào `SearchInputBar` (luôn hiển thị nên vòng đời không đổi).
- Kiểm tra: `git diff --color-moved` – mọi dòng không phải "di chuyển nguyên vẹn" đều là thay phép gán state bằng callback. Hai thư mục `search/`, `settings/` đã biên dịch thử được với Compose Desktop 1.7.3 + Kotlin 2.1.0 (stub phần Android/app theo chữ ký thật).

Đây là file **dễ nhất để luyện kỹ thuật 3.2** – các section độc lập, chỉ đọc `settings` và gọi `updateSettings`. Có thể làm Settings **trước** Search nếu là người mới.

---

## Giai đoạn 6 – `MusicPlayerController.kt` (1.457 dòng → ~9 file) ✅ Đã xong về code (08/10/2026) – ⚠️ còn chờ build Android + smoke test 10.3

Đây là "bộ não" phát nhạc, lỗi ở đây ảnh hưởng toàn app → **viết test trước, tách sau**.

### 6.0 Cấu trúc đích

```
playback/
├── MusicPlayerController.kt       (~450)  Mặt tiền (facade): GIỮ NGUYÊN mọi hàm public, bên trong ủy quyền
├── PlaybackState.kt               (~30)   RepeatMode, PlaybackState                         ← dòng 41–65
├── player/
│   ├── ExoPlayerFactory.kt        (~60)   RenderersFactory + AudioSink + build ExoPlayer    ← dòng 345–397
│   └── PlayerEventListener.kt     (~115)  Player.Listener (onIsPlayingChanged, onMediaItemTransition, onPlayerError...) ← 398–507
├── queue/
│   └── QueueOperations.kt         (~200)  Thuật toán hàng đợi THUẦN KOTLIN (không ExoPlayer)
├── motion/
│   └── MotionArtworkController.kt (~270)  motionExoPlayer, cache, tải video bìa động        ← 146–269, 609–735
├── SleepTimer.kt                  (~70)                                                      ← 1257–1308
├── VolumeFader.kt                 (~50)   smoothFadeIn / smoothFadeOut                       ← 1340–1380
├── PositionTracker.kt             (~60)   startProgressTracker / stopProgressTracker        ← 1381–1433
└── TrackPreparation.kt            (~90)   loadLyricsIfMissing, preloadSurroundingTracks, ReplayGain ← 525–608
```

### 6.1 Ý tưởng tách hàng đợi

Hiện mỗi hàm hàng đợi làm 3 việc: tính danh sách mới → cập nhật `_playbackState` → gọi ExoPlayer. Tách **phần tính toán** ra hàm thuần:

```kotlin
// playback/queue/QueueOperations.kt
internal data class QueueResult(val queue: List<Track>, val currentIndex: Int)

internal object QueueOperations {
    fun insertNext(queue: List<Track>, currentIndex: Int, tracks: List<Track>): QueueResult {
        if (tracks.isEmpty()) return QueueResult(queue, currentIndex)
        if (queue.isEmpty()) return QueueResult(tracks, 0)
        val insertIndex = (currentIndex + 1).coerceIn(0, queue.size)
        return QueueResult(queue.toMutableList().apply { addAll(insertIndex, tracks) }, currentIndex)
    }

    fun move(queue: List<Track>, currentIndex: Int, from: Int, to: Int): QueueResult? { /* từ moveQueueItem */ }
    fun remove(queue: List<Track>, currentIndex: Int, index: Int): QueueResult? { /* từ removeQueueItem */ }
    fun shuffle(/* ... */) { /* từ toggleShuffle, nhận Random để test được */ }
}

// MusicPlayerController.kt – phần còn lại chỉ áp kết quả vào state + ExoPlayer
fun playNextTracks(tracks: List<Track>) {
    val state = _playbackState.value
    if (state.queue.isEmpty()) { setQueue(tracks, startIndex = 0, autoPlay = false); return }
    val insertIndex = (state.currentIndex + 1).coerceIn(0, state.queue.size)
    val result = QueueOperations.insertNext(state.queue, state.currentIndex, tracks)
    _playbackState.value = state.copy(queue = result.queue)
    exoPlayer?.addMediaItems(insertIndex, tracks.map(::trackToMediaItem))
    insertAfterCurrentInOriginal(tracks, state.currentTrack)
}
```

**Lợi ích lớn nhất:** `PlaybackQueueTest.kt` hiện tự chép lại thuật toán (comment: *"Helper implementing the exact queue algorithms from MusicPlayerController"*). Sau khi tách, test **gọi thẳng `QueueOperations`** → test thật sự bảo vệ code chạy trong app.

### 6.2 Các bước

1. **PR 1 (⭐):** `PlaybackState.kt`, `VolumeFader.kt`, `PositionTracker.kt`, `SleepTimer.kt` – phụ thuộc ít, chỉ cần truyền `scope`, `exoPlayer` getter và callback.
2. **PR 2 (⭐⭐⭐):** Viết test cho `QueueOperations` **dựa trên các case của `PlaybackQueueTest` hiện tại** (copy các case, đổi helper giả thành lời gọi thật) → tách `QueueOperations` → chạy test. Xóa helper chép tay trong `PlaybackQueueTest`.
3. **PR 3 (⭐⭐⭐):** `MotionArtworkController` – sở hữu `motionExoPlayer`, `_motionVideoPath`, `motionJob`. Controller vẫn expose `motionVideoPath` và `motionExoPlayer` như cũ (ủy quyền) để `MainActivity` không phải sửa.
4. **PR 4 (⭐⭐⭐⭐):** `ExoPlayerFactory` + `PlayerEventListener`. Listener cần gọi ngược vào controller (`handleTrackEnded`, cập nhật state) → truyền một interface nhỏ hoặc các lambda, **không** truyền nguyên controller.

**Kết quả thực tế (08/10/2026):** `MusicPlayerController.kt` 1.457 → 895 dòng, 4 commit:
1. `PlaybackState.kt` (29), `VolumeFader.kt` (70), `PositionTracker.kt` (82), `SleepTimer.kt` (65).
2. `queue/QueueOperations.kt` (137) – thuật toán hàng đợi thuần Kotlin. `PlaybackQueueTest` bỏ helper chép tay, **gọi thẳng `QueueOperations`**: 9 → 26 test (thêm shuffle, khôi phục thứ tự gốc, xóa lịch sử, chuỗi `moveMediaItem`). Khác biệt nhỏ: xáo dùng `kotlin.random.Random` (tham số để test cố định được) thay cho `Collections.shuffle` – vẫn ngẫu nhiên đều.
3. `motion/MotionArtworkController.kt` (333) – player video bìa động, `motionVideoPath`, bộ nhớ đệm RAM, job tải. Controller vẫn expose `motionVideoPath`, `motionExoPlayer`, `setMotionPlaybackAllowed`, `removeMotionArtworkForCurrentTrack`, `clearMotionArtworkCache`. Sửa kèm: bộ nhớ đệm RAM trước đây khai báo **sau** khối `init` nên coroutine nạp cache từ DB có thể chạy trước khi nó được gán (lỗi bị `runCatching` nuốt im lặng); giờ được khởi tạo trước.
4. `player/ExoPlayerFactory.kt` (73), `player/PlayerEventListener.kt` (101) và `TrackPreparation.kt` (94: ReplayGain, tải lời, nạp trước bài kế/trước). Listener gọi ngược controller qua interface `PlayerEventListener.Callbacks` (6 thành viên), không nhận nguyên controller.

**Ai giữ biến nào:** controller giữ `_playbackState`, `_positionMs`, `exoPlayer`, `mediaSession`, `userVolume`, `isReorderingQueue`, `originalQueue`, `lastUserSkipTimestamp`; `PlayerEventListener` giữ `consecutivePlaybackErrors`; `VolumeFader` giữ `fadeJob`; `PositionTracker` giữ `progressJob`; `SleepTimer` giữ `sleepTimerJob`; `TrackPreparation` giữ `lyricsJob`, `preloadJob`; `MotionArtworkController` giữ mọi thứ về bìa động.

**⚠️ Thứ tự khởi tạo:** mọi thuộc tính helper (`volumeFader`, `positionTracker`, `sleepTimer`, `motionArtwork`, `trackPreparation`, `playerEventCallbacks`) **phải khai báo trước khối `init`** – `init` gọi `setupPlayer()` dùng ngay `playerEventCallbacks`; khai báo sau `init` thì giá trị còn null → crash khi mở app.

**Kiểm tra (trong môi trường cloud, không có Android SDK):**
- API public của controller không đổi (so tự động danh sách `fun`/`val` public trước và sau).
- Biên dịch toàn bộ `playback/` với stub media3/Android theo chữ ký thật; bản gốc cũng biên dịch được với cùng stub.
- Chạy `PlaybackQueueTest` (26 test) bằng JUnit thật.
- **Test so sánh (không commit, chạy trên stub):** khởi tạo controller gốc và controller mới, chạy cùng 24 thao tác (setQueue, playNext, addToQueue, move, remove, playQueueIndex, next/prev, repeat, autoplay, tốc độ, yêu thích, xóa lịch sử, hẹn giờ, tua, xóa hết, bật/tắt shuffle…) – `PlaybackState` và vị trí giống hệt nhau sau mọi bước. Test này cũng bắt được lỗi thứ tự khởi tạo ở trên khi cố tình cài lại.
- Chưa kiểm tra được: sự kiện ExoPlayer thật (chuyển bài gapless, lỗi phát, hết bài), fade, bìa động → cần smoke test 10.3 trên máy.

**Lưu ý:**
- Các biến dùng chung giữa nhiều nhóm (`isReorderingQueue`, `originalQueue`, `consecutivePlaybackErrors`) phải có **một chủ sở hữu duy nhất**. Ghi rõ trong KDoc class nào giữ biến nào.
- `OneMusicPlaybackService` và `MainActivity` chỉ dùng API public của controller → kiểm tra bằng `Alt+F7` rằng không có chữ ký nào bị đổi.

---

## Giai đoạn 7 – `MainActivity.kt` và các file trung bình

Làm dần, ưu tiên file nào **sắp phải sửa** thì tách trước.

### 7.1 `MainActivity.kt` (806 dòng) ✅ Đã xong (08/10/2026)

```
MainActivity.kt                   (~250)  Activity, splash, nhận intent mở file nhạc, xin quyền thông báo
ui/main/OneMusicApp.kt            (~250)  Composable gốc: state, launcher, BackHandler, bố cục ← dòng 263–449
ui/main/MainTabsHost.kt           (~165)  Nội dung 4 tab (Home, Library, Search, Settings)  ← 450–614
ui/main/BottomControlsOverlay.kt  (~95)   NowBar + thanh điều hướng + snackbar              ← 615–711
ui/main/AppOverlays.kt            (~95)   Folder Manager, Duplicate Cleaner, Now Playing, Add to Playlist ← 712–806
ui/main/PlaylistM3uLaunchers.kt   (~45)   Launcher xuất/nhập M3U8                           ← 336–377
```

Package `ui/main` đã có sẵn bên thư mục test (`app/src/test/.../ui/main/`). Lưu ý: file test `MainScreenViewModelTest.kt` thực ra chứa class `MusicRepositoryTest` – nhân tiện đổi tên file cho khớp (PR riêng).

**Kết quả thực tế (08/10/2026) ✅ Đã xong về code – ⚠️ còn chờ build Android + smoke test:** `MainActivity.kt` 806 → 179 dòng (chỉ còn Activity: splash, mở tệp nhạc từ ngoài, quyền thông báo, theo dõi thẻ nhớ). `OneMusicApp` chuyển sang package `ui.main` (public như cũ, `MainActivity` thêm 1 import):
- `ui/main/OneMusicApp.kt` (247): state điều hướng (tab, Now Playing, các lớp phủ, ô tìm kiếm), Snackbar dùng chung, khôi phục hàng đợi lúc mở app, ẩn/hiện thanh hệ thống, `BackHandler`, `CompositionLocalProvider`.
- `ui/main/MainTabsHost.kt` (206): `AnimatedContent` 4 tab + `SaveableStateHolder`.
- `ui/main/BottomControlsOverlay.kt` (164): `BoxScope.BottomControlsOverlay` – chỉ báo quét, NowBar, thanh tab, Snackbar; báo chiều cao thật qua `onHeightMeasured`.
- `ui/main/AppOverlays.kt` (141): Thư mục nhạc, Dọn trùng lặp, Now Playing (kèm `LaunchedEffect` cho phép video bìa động chạy), hộp thoại thêm vào playlist.
- `ui/main/PlaylistM3uLaunchers.kt` (90): `rememberPlaylistM3uLaunchers(...)` trả về `exportPlaylist` / `importPlaylist`; giữ luôn `playlistToExport` (trước ở `OneMusicApp`, chỉ launcher dùng).
- Kiểm tra: `git diff --color-moved` – mọi dòng không phải "di chuyển nguyên vẹn" đều là thay phép gán state bằng callback; `ui/main/` biên dịch thử được với Compose Desktop (stub các màn hình theo chữ ký thật).
- Đã đổi tên `MainScreenViewModelTest.kt` → `MusicRepositoryTest.kt` (commit riêng, giữ package).

### 7.2 `AppleMusicMotionFetcher.kt` (843 dòng) ✅ Đã xong (08/10/2026)

```
data/scanner/motion/
├── AppleMusicMotionFetcher.kt   (~285)  API public + quy trình chính (cache, searchAndFetch) ← dòng 49–330
├── ItunesSearchClient.kt        (~95)   Tìm collectionId qua iTunes                       ← 331–420
├── AppleMusicAmpClient.kt       (~95)   editorialVideo + làm mới JWT                      ← 421–512
├── HlsVideoDownloader.kt        (~200)  Phân tích M3U8, tải MP4/HLS                       ← 513–713
├── MotionHttpClient.kt          (~70)   httpGet, httpGetWithAuth, httpGetBytes            ← 714–781
└── StringSimilarity.kt          (~35)   levenshteinSimilarity / Distance                   ← 815–843
```

`MusicSearchEngine.kt` cũng có `computeLevenshteinDistance` (có `maxLimit` để dừng sớm). Có thể đặt cả hai vào chung `util/StringSimilarity.kt`, **nhưng giữ 2 hàm riêng** vì hành vi khác nhau.

**Kết quả thực tế (08/10/2026) – ⚠️ còn chờ build Android + thử tải bìa động trên máy:** `AppleMusicMotionFetcher.kt` 843 → 370 dòng.
- **Khác cấu trúc dự kiến:** `AppleMusicMotionFetcher.kt` **giữ ở package `data.scanner`** (không chuyển vào `motion/`) để `MusicRepository` và `MotionArtworkController` không phải sửa import; chỉ các phần phụ vào package con `data/scanner/motion/` (đều `internal`).
- `motion/ItunesSearchClient.kt` (141): `findCollectionId(track)` + `pickBestCollectionId(results, track, onCandidate)` (chấm điểm, **thuần**) + `sanitizeQuery`.
- `motion/AppleMusicAmpClient.kt` (120): giữ JWT, `fetchEditorialVideo(collectionId)`, làm mới JWT từ web; `parseEditorialVideo(json)` **thuần**. `EditorialVideoResult` chuyển từ class lồng public sang `internal` (không nơi nào khác dùng).
- `motion/HlsVideoDownloader.kt` (163): `selectBestVariant(content, url)` và `findDirectMp4Url(content, url)` **thuần**, `downloadFile`, `downloadHlsVariant`.
- `motion/MotionHttpClient.kt` (95): `get`, `getWithAuth`, `getBytes`, `resolveUrl`, User-Agent và timeout.
- `motion/StringSimilarity.kt` (36): `levenshteinSimilarity` / `levenshteinDistance` – **chưa gộp** với `MusicSearchEngine.computeLevenshteinDistance` (hành vi khác, xem ghi chú dưới).
- Fetcher còn: API public, quy trình `searchAndFetch`, `downloadAndSave`, negative cache. Log giữ nguyên (log "Candidate match" đi qua tham số `onCandidate` để hàm chấm điểm không phụ thuộc `android.util.Log`).
- Bỏ hằng `LEVENSHTEIN_THRESHOLD` không được dùng ở đâu.
- **Test mới** `app/src/test/.../data/scanner/motion/MotionFetcherHelpersTest.kt` (13 test, trước đây fetcher không có test nào): Levenshtein, `resolveUrl`, chọn biến thể HLS (ưu tiên AVC ≤ 1080p), đọc `EXT-X-MAP`, đọc JSON editorialVideo (ưu tiên vuông, bỏ host không phải Apple), `sanitizeQuery`, chấm điểm kết quả iTunes.
- Kiểm tra: `git diff --color-moved`; bản gốc và bản mới cùng biên dịch được với một bộ stub Android; 13 test chạy đạt bằng JUnit + kotlinx-serialization-json thật.

### 7.3 `ReplayGainExtractor.kt` (634 dòng – đã có test) ✅ Đã xong (08/10/2026)

```
data/scanner/replaygain/
├── ReplayGainExtractor.kt   (~80)   object mặt tiền: extract(), nhận dạng định dạng, ReplayGainData ← dòng 15–77
├── FlacVorbisParser.kt      (~140)                                                         ← 78–213
├── Id3v2Parser.kt           (~285)  ID3v2.2/2.3/2.4, USLT, TXXX, COMM, RVA2               ← 214–495
├── Mp4AtomParser.kt         (~50)                                                          ← 496–541
├── OggOpusParser.kt         (~40)                                                          ← 542–578
└── GainValueParsers.kt      (~60)   parseGainString, parseR128Gain, parseItunNorm, đọc số nguyên ← 579–634
```

`LocalMusicScanner` gọi `ReplayGainExtractor.extract`; test gọi thêm `parseGainString`, `parseItunNorm`, `parseR128Gain` → **giữ các hàm public này trên `ReplayGainExtractor`** (bản mới chỉ gọi sang file tương ứng), nơi gọi và test không cần sửa. Các parser con đặt `internal`.

**Kết quả thực tế (08/10/2026):** `ReplayGainExtractor.kt` 634 → 98 dòng.
- Giống 7.2: `ReplayGainExtractor` và `ReplayGainData` **giữ ở package `data.scanner`** (nơi gọi và test không đổi); parser vào package con `data/scanner/replaygain/`, đều `internal object`: `FlacVorbisParser.kt` (144), `Id3v2Parser.kt` (292), `Mp4AtomParser.kt` (52), `OggOpusParser.kt` (42), `GainValueParsers.kt` (60).
- Thân hàm chép nguyên văn; gọi chéo giữa các parser bằng import thành viên object (vd `import ...GainValueParsers.parseGainString`) nên không phải sửa dòng nào. Các hàm public cũ (`parseGainString`, `parseR128Gain`, `parseItunNorm`, `parseVorbisCommentBlock`) giữ trên `ReplayGainExtractor`, chỉ gọi sang parser.
- Kiểm tra (JVM thuần, không cần Android): `ReplayGainExtractorTest` 5/5; **test so sánh** bản gốc với bản mới trên 40.000 đầu vào (FLAC/ID3v2.2–2.4/MP4/OGG/Opus dựng ngẫu nhiên, byte rác, tệp bị cắt, 10 phần mở rộng) + 15.000 chuỗi giá trị → kết quả giống hệt (≈14.000 đầu vào có dữ liệu thật). Test so sánh bắt được lỗi khi cố tình sửa 1 ký tự trong chuỗi `origin`.

### 7.4 `MusicSearchEngine.kt` (572 dòng – đã có test, dễ nhất) ✅ Đã xong (08/10/2026)

Đã chia sẵn thành nhiều `object`, chỉ cần mỗi cái một file trong `data/search/`: `SearchTextNormalizer.kt`, `ArtistExtractor.kt`, `SearchModels.kt` (SearchMatchReason, MatchedTrack, MatchedArtist, MatchedAlbum, SearchResults, SearchableTrack), `MusicSearchIndex.kt`, `MusicSearchEngine.kt`. Cùng package → test không phải sửa.

**Kết quả thực tế (08/10/2026):** `MusicSearchEngine.kt` 572 → 301 dòng, đúng cấu trúc dự kiến, tất cả trong package `data.search`:
- `SearchTextNormalizer.kt` (47), `ArtistExtractor.kt` (96), `SearchModels.kt` (59: SearchMatchReason, MatchedTrack, MatchedArtist, MatchedAlbum, SearchResults, SearchableTrack), `MusicSearchIndex.kt` (79), `MusicSearchEngine.kt` (301: `search` ×2, `isFuzzyMatch`, `computeLevenshteinDistance`).
- Chỉ cắt file: ghép thân 5 file mới (bỏ dòng package/import) **giống hệt từng byte** dòng 8–572 của bản gốc. Không đổi phạm vi truy cập nào (các `private` đều nằm trong object của chính nó). Bỏ import thừa `kotlin.math.min` (không dùng).
- Kiểm tra (JVM thuần): `MusicSearchEngineTest` 8/8 không sửa; **test so sánh** bản gốc (package `orig`) với bản mới trên 400 thư viện ngẫu nhiên × 10 truy vấn (tiếng Việt có dấu, feat./&/x, Remix/Live, gõ sai chính tả) – index, kết quả `search` (cả 2 overload), `extractIndividualArtists`, `tokenize`, `mergeCanonicalArtists` giống hệt (2.791/4.000 truy vấn có kết quả bài hát). Test bắt được lỗi khi cố tình đổi ngưỡng fuzzy hoặc đổi `đ`→`D`.

### 7.5 Các file 500–780 dòng còn lại

| File | Cách tách |
|---|---|
| `ui/screens/detail/DetailScreen.kt` (776) | Theo 4 layer có sẵn comment: `DetailHero.kt` (dòng 185–290), `DetailTrackList.kt` (291–589), `DetailStickyTopBar.kt` (590–681), dialog (682–776). |
| `ui/screens/dedup/DuplicateCleanerScreen.kt` (729) | `DuplicateGroupCard` (dòng 490–722) ra file riêng; dock thao tác (374–465) ra file riêng. |
| `ui/screens/folder/FolderManagerScreen.kt` (565) | Tách danh sách thư mục và dialog xác nhận (dòng 549+). |
| `ui/components/AddToPlaylistDialog.kt` (526) | `AddToPlaylistMultipleDialog` (dòng 300+) sang `AddToPlaylistMultipleDialog.kt`; phần giao diện chung của 2 dialog (nếu trùng) gom thành composable nội bộ. |

**Kết quả thực tế – `DetailScreen.kt` (08/10/2026) – ⚠️ còn chờ build Android + thử màn chi tiết Album/Nghệ sĩ/Yêu thích/Playlist:** 765 → 199 dòng. 4 file mới **ngang hàng trong `detail/`**, đều `internal`; `DetailScreen` giữ nguyên chữ ký public:
- `DetailHero.kt` (321): `DetailHeroBackground` (LAYER 1 – ảnh bìa/icon 520dp + parallax; nhận `listState`, đọc offset trong `graphicsLayer` như cũ; `bottomScrim` chuyển vào đây vì chỉ dùng ở đây) và `DetailHeroHeader` (nội dung item `hero_album_header`: tên, nghệ sĩ + Hi-Res, số bài & thời lượng, nút Phát tất cả / Trộn bài).
- `DetailTrackList.kt` (226): `DetailTrackRow` – nội dung một item bài hát. `itemsIndexed` (cùng `key` `"${track.id}_$index"` và `contentType`) và `isCurrent` giữ ở cha.
- `DetailStickyTopBar.kt` (137): LAYER 3. `topBarBackgroundAlpha` truyền dạng lambda (đọc trong thanh trên → khi animation chạy chỉ thanh trên recompose, không cả màn). Nút Chia sẻ tự lấy `LocalContext`.
- `DetailDialogs.kt` (104): `RenamePlaylistDialog`. Hộp thoại xóa playlist (chỉ là lời gọi `ApexConfirmDialog`) và `TrackActionMenu` để lại ở cha.
- State (`showRenameDialog`, `renameInput`, `showDeleteConfirmDialog`, `trackForActions`, `listState`, `showTopTitle`, cài đặt) giữ ở `DetailScreen`; con nhận giá trị + callback: `trackForActions = track` → `onTrackLongClick`, `appSettings.isHiResBadgeEnabled` → tham số, nút Đổi tên → `onRenameClick(customPlaylist)` (cha nạp `renameInput` rồi mở hộp thoại), `showRenameDialog = false` → `onDismiss()`, `onRenamePlaylist(id, tên.trim())` → `onRename(tên.trim())`. Ngoài các chỗ này, thân hàm chép nguyên văn.
- **Kiểm tra:** bản gốc và bản mới biên dịch với cùng bộ stub (Compose Desktop 1.7.3). **Test giao diện so sánh** gốc/mới bằng Compose Desktop **1.5.12 + Kotlin 1.9.22** (bản 1.6+ cần `androidx.collection` trên dl.google.com bị chặn; Material3 1.1 thiếu `HorizontalDivider`/`Icons.AutoMirrored` → thêm stub/đổi icon giống nhau cho cả 2 bản): 6 cấu hình (album có ảnh, Yêu thích, nghệ sĩ, danh sách rỗng, playlist có/không callback), mỗi cấu hình 7–8 ảnh chụp (ban đầu, menu nhấn giữ, mở/gõ/mở lại Đổi tên, hộp thoại xóa, giữa/cuối animation thanh trên khi cuộn, cuộn ngược, parallax) + cây semantics + nhật ký callback/snackbar → **giống hệt từng pixel**. Test bắt được cả 5 lỗi cố tình cài (vạch kẻ lệch 4dp, nền thanh trên luôn tối, parallax 0.40, bỏ `trim`, không nạp lại tên khi mở Đổi tên).

---

## Giai đoạn 8 – Rào chắn để file không phình lại

1. Thêm script `scripts/check-file-size.sh` báo các file `.kt` vượt **600 dòng**:
   ```bash
   #!/usr/bin/env bash
   # Liệt kê file Kotlin > 600 dòng trong app/src/main. Thoát mã 1 nếu có.
   LIMIT=${1:-600}
   over=$(find app/src/main -name '*.kt' -exec wc -l {} + | awk -v l="$LIMIT" '$2 != "total" && $1 > l')
   [ -z "$over" ] && echo "OK: không file nào vượt $LIMIT dòng" && exit 0
   echo "Các file vượt $LIMIT dòng:"; echo "$over" | sort -rn; exit 1
   ```
2. (Tùy chọn) Thêm [detekt](https://detekt.dev) với các luật `LongMethod` (ngưỡng ~200 cho composable), `LargeClass`, `LongParameterList` – chạy ở chế độ cảnh báo trước, chỉ bật chặn khi đã tách xong.
3. Thêm một mục vào hướng dẫn đóng góp/review: *"File mới > 400 dòng hoặc composable > 200 dòng cần giải thích trong PR."*

---

## 9. Tiêu chí hoàn thành (Definition of Done)

- [ ] Không còn file nào trong `app/src/main` vượt **600 dòng** (script ở Giai đoạn 8 trả `OK`).
- [ ] Không còn hàm `@Composable` nào dài quá **~250 dòng**.
- [ ] `./gradlew :app:testDebugUnitTest` xanh; `PlaybackQueueTest` gọi code thật (`QueueOperations`).
- [ ] Smoke test mục 10 đạt trên ít nhất 1 máy thật.
- [ ] Không còn hàm tiện ích bị định nghĩa lặp (`formatDuration`…).
- [ ] `NOW_PLAYING_FIX_PLAN.md` / `UI_UX_FIX_PLAN.md`: thêm ghi chú đầu tài liệu rằng code đã được tách, tìm theo tên hàm thay vì số dòng.

---

## 10. Smoke test sau mỗi lần tách

### 10.1 Now Playing
- [ ] Mở sheet từ NowBar: trượt lên mượt; vuốt xuống để đóng; nút Back đóng đúng thứ tự (dialog → về ảnh bìa → đóng sheet).
- [ ] Vuốt ngang ảnh bìa đổi bài; bấm Next/Prev thì pager chạy theo, **không** tự phát nhầm bài.
- [ ] Video bìa động chạy khi mở sheet, dừng khi đóng.
- [ ] Tab Lời: tự cuộn tới câu đang hát; tự cuộn tay thì không bị giật về; đổi bài thì về đầu.
- [ ] Tab Hàng đợi: bấm bài để phát, kéo đổi chỗ, vuốt xóa đúng bài; cuộn xuống ẩn cụm điều khiển, cuộn lên hiện lại.
- [ ] Thanh tua, Play/Pause, Shuffle, Repeat, tốc độ phát, hẹn giờ tắt, bộ chỉnh âm, thông tin bài, menu ⋯, toast tim.

### 10.2 Home / Library / Search / Settings
- [ ] Home: banner, nghe gần đây, playlist, nghệ sĩ, khám phá; vào/ra từng màn con, Back về đúng chỗ; tạo/đổi tên/xóa playlist.
- [ ] Library: 3 tab × 3 chế độ xem; sắp xếp; chọn nhiều bằng kéo dọc mép trái; thanh chữ cái; thêm hàng loạt vào playlist.
- [ ] Search: gõ có dấu/không dấu; 4 tab lọc; lịch sử tìm kiếm; mở nghệ sĩ/album.
- [ ] Settings: bật/tắt từng công tắc, mở lại app vẫn giữ; các nút xóa cache có hộp xác nhận.

### 10.3 Phát nhạc (sau Giai đoạn 6)
- [ ] Phát, chuyển bài, hết bài tự sang bài sau; Repeat 1/tất cả; Shuffle bật/tắt giữ đúng thứ tự gốc.
- [ ] "Phát tiếp", "Thêm vào hàng đợi" khi đang shuffle và không shuffle.
- [ ] Hẹn giờ tắt (theo phút và hết bài); rút tai nghe tự dừng; widget và thông báo media cập nhật.
- [ ] Mở lại app khôi phục bài và vị trí phát cuối.
