# Kế hoạch sửa lỗi trải nghiệm giao diện (UI/UX) toàn ứng dụng – dành cho người mới

Tài liệu này hướng dẫn **từng bước** cách sửa các vấn đề trải nghiệm người dùng trong OneMusic (ngoài màn Now Playing – phần đó có tài liệu riêng `NOW_PLAYING_FIX_PLAN.md`). Mỗi mục gồm: triệu chứng người dùng thấy, cách tái hiện, file và vị trí cần sửa, vì sao lỗi xảy ra, code sửa mẫu và checklist kiểm tra.

> Số dòng ghi trong tài liệu là số dòng tại thời điểm viết (30/09/2026). Nếu file đã thay đổi, hãy dùng **Ctrl+F** tìm theo đoạn code được trích, đừng tin tuyệt đối vào số dòng.

---

## 0. Chuẩn bị trước khi sửa

### 0.1 Bản đồ các file giao diện

Mọi đường dẫn bên dưới tính từ `app/src/main/java/com/example/onemusic/`.

| File | Vai trò |
|---|---|
| `MainActivity.kt` | Khung chính của app: 4 tab (Home, Library, Search, Settings), NowBar + thanh điều hướng nổi ở đáy, các màn phủ (Folder Manager, Duplicate Cleaner, Now Playing). **Mọi callback phát nhạc đều được nối ở đây** (dòng ~393–580). |
| `ui/screens/home/HomeScreen.kt` | Màn Trang chủ + các màn con: Playlist, Nghệ sĩ, Album, chi tiết (dùng `HomeSubView`). |
| `ui/screens/library/LibraryScreen.kt` | Màn Thư viện: danh sách bài, sắp xếp, chế độ lưới, chọn nhiều, thanh chữ cái. |
| `ui/screens/search/SearchScreen.kt` | Màn Tìm kiếm. |
| `ui/screens/detail/DetailScreen.kt` | Trang chi tiết Album/Nghệ sĩ/Playlist – **được dùng chung** bởi Home và Search. |
| `ui/screens/settings/SettingsScreen.kt` | Màn Cài đặt. |
| `ui/components/NowBar.kt` | Mini player nổi ở đáy. |
| `ui/navigation/ApexBottomNavigation.kt` | Thanh 4 tab nổi ở đáy. |
| `ui/components/ApexTrackActionSheet.kt` | Menu thao tác của một bài hát (hiện chỉ mở được từ Now Playing). |
| `ui/components/AddToPlaylistDialog.kt` | Dialog thêm 1 bài / nhiều bài vào playlist. |
| `ui/components/ApexConfirmDialog.kt` | Dialog xác nhận có sẵn – **dùng lại cho mọi thao tác xóa**. |
| `ui/utils/ApexTouchPhysics.kt` | `Modifier.apexBounceClick` – hiệu ứng nảy khi chạm, dùng ở gần như mọi nút. |
| `theme/Color.kt` | Bảng màu. Lưu ý: `PrismBlue` thực chất là **màu xanh lá** `ApexForest #228B22` (tên biến cũ). |

### 0.2 Sáu khái niệm cần nắm (đọc 15 phút trước khi bắt tay)

1. **Callback đi từ trên xuống:** các màn không tự phát nhạc. Chúng gọi callback (ví dụ `onTrackSelect`) và `MainActivity` mới là nơi gọi `playerController.setQueue(...)`. Muốn đổi hành vi phát nhạc → phải sửa **cả màn con lẫn chỗ nối trong MainActivity**.
2. **`remember` vs `rememberSaveable`:** `remember` mất giá trị khi composable bị gỡ khỏi màn hình (ví dụ chuyển tab). `rememberSaveable` giữ được nếu có `SaveableStateHolder` bao ngoài.
3. **`CompositionLocal`:** cách "phát" một giá trị xuống toàn bộ cây giao diện mà không phải truyền qua từng tham số. App đã dùng sẵn cho `LocalApexHazeState`.
4. **`LazyColumn` + `key`:** mỗi item có một khóa. Khóa trong app thường có dạng `"${track.id}_$index"` – lưu ý khi so sánh.
5. **Semantics (trợ năng):** thông tin cho TalkBack biết một phần tử là nút, công tắc hay tab. `Modifier.clickable`/`toggleable`/`semantics { }` là nơi khai báo.
6. **Snackbar vs Toast:** Toast là thông báo của hệ thống, không có nút bấm. Snackbar nằm trong giao diện app và có thể có nút "Hoàn tác".

### 0.3 Quy trình cho mỗi mục

1. Tạo branch riêng: `git checkout -b fix/ui-<số-mục>`.
2. Tái hiện lỗi trên máy/giả lập theo phần **"Cách tái hiện"**.
3. Sửa theo hướng dẫn.
4. Build lại và chạy phần **"Kiểm tra"**.
5. Commit, mỗi mục một commit: `fix(ui): mục 3 - ẩn các thao tác chưa có chức năng`.

### 0.4 Thứ tự làm đề xuất

| Đợt | Mục | Độ khó | Ghi chú |
|---|---|---|---|
| **1 – Lỗi rõ ràng, sửa ít** | [1](#mục-1--bấm-một-bài-nhưng-hàng-đợi-lại-là-cả-thư-viện), [14](#mục-14--ảnh-bìa-trống-chữ-bị-cắt-ảnh-ca-sĩ-không-hiện), [12](#mục-12--chọn-nhiều-bài-ở-thư-viện), [9](#mục-9--màn-tìm-kiếm) | ⭐–⭐⭐ | Người dùng thấy ngay |
| **2 – Tránh mất dữ liệu** | [2](#mục-2--xóa-không-hỏi-lại-không-hoàn-tác-được), [3](#mục-3--action-sheet-có-các-nút-giả), [4](#mục-4--nội-dung-cuối-danh-sách-bị-che), [5](#mục-5--lần-mở-app-đầu-tiên-trống-trơn) | ⭐⭐ | Mục 2 cần làm [bước 10.2 (Snackbar)](#bước-102--snackbar-dùng-chung) trước |
| **3 – Điều hướng** | [6](#mục-6--chuyển-tab-là-mất-trạng-thái), [7](#mục-7--cùng-thao-tác-nhưng-mỗi-màn-phản-ứng-khác-nhau), [8](#mục-8--danh-sách-không-có-menu-thao-tác-cho-từng-bài) | ⭐⭐⭐ | Nên làm liền nhau, nhờ người review |
| **4 – Phản hồi** | [10](#mục-10--phản-hồi-khi-quét-nhạc--toast), [11](#mục-11--trang-chủ-tự-nhảy-nội-dung), [13](#mục-13--album-bị-gộp-sai-không-sắp-xếp), [15](#mục-15--trang-chi-tiết-thiếu-nút-trộn-bài-và-tiêu-đề) | ⭐⭐ | |
| **5 – Nhất quán & trợ năng** | [16](#mục-16--hiệu-ứng-chạm-không-đồng-nhất), [17](#mục-17--màn-cài-đặt-khó-đọc), [18](#mục-18--trợ-năng-accessibility) | ⭐–⭐⭐ | |

> ⚠️ **Quyết định đã chốt trước khi làm đợt 3:** bấm vào một album/nghệ sĩ ở **mọi nơi** đều **mở trang chi tiết** (giống Home và Search hiện tại), không phát ngay. Nếu trưởng nhóm muốn khác, hỏi lại trước khi làm Mục 7.

---

# ĐỢT 1 – Lỗi rõ ràng, sửa ít

## Mục 1 – Bấm một bài nhưng hàng đợi lại là cả thư viện

**Triệu chứng:**
- Mở một album 10 bài, bấm bài số 3 → nghe hết bài 10 thì app phát tiếp một bài **không thuộc album**.
- Tương tự khi bấm bài trong playlist, trang nghệ sĩ, kết quả tìm kiếm, mục "Nghe gần đây".
- Ở Thư viện, chọn sắp xếp "Tên A-Z" rồi bấm "Phát tất cả" → nhạc vẫn phát theo thứ tự quét, không theo A-Z.

**Cách tái hiện:** Home → Album → mở một album → bấm bài gần cuối → mở Now Playing → tab Hàng đợi. Phần "Tiếp tục phát" chứa cả thư viện.

**Vị trí:**
- `MainActivity.kt` dòng ~428–431, ~508–511, ~544–548: cả 3 chỗ đều viết
  ```kotlin
  onTrackSelect = { track ->
      val idx = tracks.indexOf(track)
      playerController.setQueue(tracks, startIndex = idx, autoPlay = true)
  }
  ```
  `tracks` ở đây là **toàn bộ thư viện**.
- `LibraryScreen.kt` dòng ~720–723: nút "Phát tất cả" gọi `onTrackSelect(sortedTracks.first())` → lại rơi vào hàm trên.

**Vì sao lỗi:** callback chỉ nhận **một bài**, nên MainActivity không biết người dùng đang đứng trong danh sách nào.

**Cách sửa:** đổi callback thành "bài được bấm + danh sách đang hiển thị".

1. Trong `HomeScreen`, `LibraryScreen`, `SearchScreen`, `DetailScreen`, đổi kiểu tham số:
   ```kotlin
   // Trước
   onTrackSelect: (Track) -> Unit,
   // Sau
   onTrackSelect: (track: Track, context: List<Track>) -> Unit,
   ```
2. Sửa **từng chỗ gọi** cho truyền đúng danh sách đang thấy. Dùng Ctrl+F `onTrackSelect(` trong mỗi file:

   | File | Chỗ gọi | Danh sách truyền vào |
   |---|---|---|
   | `HomeScreen.kt` | Banner nổi bật (~dòng 441) | `tracks` |
   | `HomeScreen.kt` | Carousel "Nghe gần đây" (~596) | `recentlyPlayedTracks` |
   | `HomeScreen.kt` | Carousel "Có thể bạn sẽ thích" (~694) | `suggestedTracks` |
   | `DetailScreen.kt` | Bấm một hàng (~394) | `tracks` (danh sách của chính trang chi tiết) |
   | `LibraryScreen.kt` | List / Grid 2 / Grid 3 (~896, ~1025, ~1069) | `sortedTracks` |
   | `LibraryScreen.kt` | Nút "Phát tất cả" (~722) | `onTrackSelect(sortedTracks.first(), sortedTracks)` |
   | `LibraryScreen.kt` | Các chỗ `?: onTrackSelect(xxx.first())` (album/nghệ sĩ/chọn nhiều) | `onTrackSelect(xxx.first(), xxx)` |
   | `SearchScreen.kt` | Hàng bài hát (~654, ~982) | `displayTracks.map { it.track }` |

3. Trong `HomeScreen`, chỗ truyền xuống `DetailScreen` (~dòng 2023, 2041, 2058, 2076) vẫn là `onTrackSelect = onTrackSelect` – giữ nguyên vì kiểu đã khớp. Tương tự trong `SearchScreen` (~194, ~213).
4. Trong `MainActivity.kt`, thay cả 3 chỗ nối bằng một hàm chung. Đặt ngay dưới `var playlistToExport ...` (~dòng 269):
   ```kotlin
   val playFromContext: (Track, List<Track>) -> Unit = { track, context ->
       val list = context.ifEmpty { tracks }
       val idx = list.indexOf(track).coerceAtLeast(0)
       playerController.setQueue(list, startIndex = idx, autoPlay = true)
   }
   ```
   rồi dùng `onTrackSelect = playFromContext` cho Home và Library. Với Search, tạm thời giữ việc mở Now Playing (sẽ bàn ở Mục 7):
   ```kotlin
   onTrackSelect = { track, context ->
       playFromContext(track, context)
       isPlayerExpanded = true
   },
   ```

**Kiểm tra:**
- [ ] Bấm bài thứ 3 của một album → hàng đợi chỉ gồm các bài của album, bắt đầu từ bài 3.
- [ ] Thư viện, sắp xếp "Thời lượng ↓", bấm "Phát tất cả" → bài dài nhất phát đầu tiên.
- [ ] Tìm "a", bấm kết quả thứ 2 → hàng đợi là danh sách kết quả tìm kiếm.
- [ ] Bấm bài trong "Nghe gần đây" → hàng đợi là 8 bài gần đây.

---

## Mục 14 – Ảnh bìa trống, chữ bị cắt, ảnh ca sĩ không hiện

### 14a. Ảnh ca sĩ ở mục "Nghệ sĩ nổi bật" (Home) không bao giờ hiện

**Vị trí:** `HomeScreen.kt` ~dòng 942:
```kotlin
val customImg = artistImages[artistName]
```
**Vì sao:** kho ảnh `ArtistImageRepository` lưu khóa ở dạng **chữ thường, bỏ khoảng trắng hai đầu** (xem `ArtistImageRepository.kt` dòng ~237). Tra bằng tên gốc "Sơn Tùng M-TP" sẽ không khớp "sơn tùng m-tp".

**Cách sửa:** tra giống các màn khác:
```kotlin
val customImg = artistImages[artistName.trim().lowercase()]
    ?: artistImageRepository.getCachedImageUrl(artistName)
```

### 14b. Tab Nghệ sĩ ở Thư viện chỉ hiện chữ viết tắt

**Vị trí:** `LibraryScreen.kt` ~dòng 1290–1303 (ô tròn `avatarColor` + chữ cái).

**Cách sửa:** thêm ở đầu `LibraryScreen` (cạnh `val appSettings ...`):
```kotlin
val artistImageRepository = remember { ArtistImageRepository.getInstance(context) }
val artistImages by artistImageRepository.artistImagesFlow.collectAsState()
```
rồi trong hàng nghệ sĩ: nếu có ảnh (`artistImages[artistName.trim().lowercase()]`) thì hiện `AsyncImage` bo tròn 54dp, không có thì giữ ô chữ viết tắt như cũ. Copy mẫu từ `HomeScreen.kt` ~dòng 1502–1527.

### 14c. Bài không có ảnh bìa hiện ô trống (chế độ Danh sách ở Thư viện)

**Vị trí:** `LibraryScreen.kt` ~dòng 929–936 – `AsyncImage(model = track.artworkUrl ...)` không có ảnh thay thế.

**Cách sửa:** bọc giống `DetailScreen.kt` ~dòng 409–433:
```kotlin
Box(
    modifier = Modifier
        .size(70.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(SurfaceActiveIndicator),
    contentAlignment = Alignment.Center
) {
    if (track.artworkUrl.isNotBlank()) {
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(30.dp))
    }
}
```
Làm tương tự cho ảnh album ở tab Album của Thư viện (~dòng 1157).

### 14d. Tên bài bị cắt cụt, không có dấu "…"

**Vị trí:** `LibraryScreen.kt` ~dòng 941–949 và 956–963: có `maxLines = 1` nhưng **thiếu** `overflow = TextOverflow.Ellipsis`.

**Cách sửa:** thêm `overflow = TextOverflow.Ellipsis` vào cả hai `Text`. Dùng Ctrl+F `maxLines = 1\n` (bật regex) để soát các file khác.

**Kiểm tra Mục 14:**
- [ ] Bật "Tự động tải ảnh nghệ sĩ" trong Cài đặt → Home, mục "Nghệ sĩ nổi bật" có ảnh thật.
- [ ] Thư viện → tab Nghệ sĩ có ảnh.
- [ ] Bài không có bìa hiện biểu tượng nốt nhạc, không phải ô trống.
- [ ] Bài có tên rất dài hiện "…" ở cuối.

---

## Mục 12 – Chọn nhiều bài ở Thư viện

### 12a. Kéo ở mép trái để chọn nhiều bài không hoạt động

**Cách tái hiện:** Thư viện → ⋮ → "Chọn nhiều bài hát" → đặt ngón tay ở mép trái danh sách, kéo xuống. Không bài nào được chọn.

**Vị trí:** `LibraryScreen.kt` hàm `findTrackIndexAtY` (~dòng 322–328):
```kotlin
return sortedTracks.indexOfFirst { it.id == hitItem.key }
```
**Vì sao:** khóa của mỗi hàng là `"${track.id}_$index"` (~dòng 861), còn code so với `track.id` → **không bao giờ bằng nhau** → luôn trả về -1.

**Cách sửa:** phần sau dấu `_` cuối cùng chính là chỉ số trong `sortedTracks`:
```kotlin
fun findTrackIndexAtY(y: Float): Int {
    val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        y >= item.offset && y <= (item.offset + item.size)
    } ?: return -1
    val key = hitItem.key as? String ?: return -1
    val index = key.substringAfterLast('_').toIntOrNull() ?: return -1
    return if (index in sortedTracks.indices) index else -1
}
```
> Lưu ý: các hàng header (`"library_header"`, `"quick_actions_row"`...) cũng có dấu `_` nhưng phần cuối không phải số (`"header"`, `"row"`) nên `toIntOrNull()` trả `null` → an toàn.

### 12b. Nhấn giữ không vào chế độ chọn

Người dùng Android quen: **nhấn giữ một bài = bắt đầu chọn nhiều**. Hiện tại nhấn giữ lại mở dialog thông số kỹ thuật.

**Vị trí:** `onLongClick` ở ~dòng 883–887, ~1028–1032, ~1072–1076.

**Cách sửa:**
```kotlin
onLongClick = {
    if (!isMultiSelectMode) {
        isMultiSelectMode = true
        selectedTrackIds = setOf(track.id)
    }
},
```
Dialog thông số sẽ được mở từ menu thao tác của bài ở [Mục 8](#mục-8--danh-sách-không-có-menu-thao-tác-cho-từng-bài).

### 12c. Bấm "Hủy" trong dialog "Thêm vào playlist" làm mất các bài đã chọn

**Vị trí:** `LibraryScreen.kt` ~dòng 1650–1660 – `onDismiss` luôn tắt chế độ chọn.

**Cách sửa:** tách "đóng" và "thêm thành công":
1. Trong `AddToPlaylistDialog.kt`, hàm `AddToPlaylistMultipleDialog` (~dòng 294) thêm tham số `onAdded: () -> Unit = {}`. Ở 2 chỗ thêm thành công (~dòng 375–376 và ~443–444), gọi `onAdded()` **trước** `onDismiss()`.
2. Trong `LibraryScreen.kt`:
   ```kotlin
   AddToPlaylistMultipleDialog(
       tracks = selectedTracks,
       playlists = customPlaylists,
       onCreatePlaylist = onCreatePlaylist,
       onAddToPlaylist = onAddToPlaylist,
       onAdded = {
           isMultiSelectMode = false
           selectedTrackIds = emptySet()
       },
       onDismiss = { showBatchAddToPlaylistDialog = false }
   )
   ```

### 12d. Thanh chữ cái A–Z nhảy lệch một dòng và hiện sai lúc

**Vị trí:** `LibraryScreen.kt` ~dòng 1473–1492.

**Vì sao:**
- `scrollToItem(targetIndex + 4)` – nhưng trước danh sách bài có **5** item header (`library_header`, `library_main_filter_tabs`, `filter_tabs_row`, `quick_actions_row`, `songs_section_label`).
- Thanh vẫn hiện khi sắp xếp "Tất cả" (thứ tự quét, **không** theo ABC) → bấm chữ "M" nhảy đến bài chữ M đầu tiên nằm lung tung.

**Cách sửa:**
```kotlin
// Thêm hằng số ở đầu file (ngoài hàm)
private const val SONGS_HEADER_ITEM_COUNT = 5

// Điều kiện hiện thanh: chỉ khi sắp xếp theo tên
if (currentLibraryTab == LibraryTab.SONGS && !isMultiSelectMode &&
    libraryViewMode == LibraryViewMode.LIST && sortedTracks.size >= 10 &&
    currentSortOption == SongSortOption.TITLE_AZ
) { ... }

// Khi cuộn
listState.scrollToItem(targetIndex + SONGS_HEADER_ITEM_COUNT)
```
> Nếu sau này thêm/bớt item header trong tab Bài hát, nhớ sửa hằng số này.

**Kiểm tra Mục 12:**
- [ ] Chế độ chọn nhiều: kéo mép trái từ bài 2 đến bài 6 → 5 bài được chọn, có rung nhẹ mỗi bài.
- [ ] Nhấn giữ một bài → vào chế độ chọn, bài đó đã được tick.
- [ ] Chọn 3 bài → "Thêm vào playlist" → bấm Hủy → vẫn còn 3 bài được chọn.
- [ ] Sắp xếp A-Z → bấm chữ "M" → bài chữ M đầu tiên nằm ngay dưới mép trên, không bị lệch.
- [ ] Sắp xếp "Tất cả" → không thấy thanh chữ cái.

---

## Mục 9 – Màn Tìm kiếm

**Vị trí chung:** `SearchScreen.kt`.

### 9a. "Không tìm thấy kết quả" nháy lên mỗi lần gõ phím

**Vì sao:** trong lúc đang tìm (chạy nền), `searchResults` vẫn là `null` → `hasAnySearchResults = false` (~dòng 283) → màn "không tìm thấy" hiện ra rồi biến mất.

**Cách sửa:** thêm trạng thái "đang tìm" và chờ người dùng ngừng gõ 150ms (debounce):
```kotlin
var isSearching by remember { mutableStateOf(false) }

LaunchedEffect(searchQuery, searchIndex, tracks) {
    val q = searchQuery.trim()
    if (q.isBlank()) {
        searchResults = null
        isSearching = false
        return@LaunchedEffect
    }
    isSearching = true
    delay(150L) // gõ tiếp trong 150ms thì khối này tự hủy và chạy lại
    val idx = searchIndex ?: withContext(Dispatchers.Default) { MusicSearchIndex.build(tracks) }
    searchResults = withContext(Dispatchers.Default) { MusicSearchEngine.search(idx, q) }
    isSearching = false
}
```
Ở chỗ `if (!hasAnySearchResults)` (~dòng 703) đổi thành:
```kotlin
if (isSearching && searchResults == null) {
    item(key = "searching") {
        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrismBlue, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
} else if (!currentTabHasResults) {
    // ... khối "Không tìm thấy kết quả" giữ nguyên
}
```
(Cần import `kotlinx.coroutines.delay` và `androidx.compose.material3.CircularProgressIndicator`.)

### 9b. Chọn tab "Nghệ sĩ" nhưng chỉ có kết quả bài hát → màn trống trơn

**Vì sao:** điều kiện hiện "không tìm thấy" chỉ xét **tổng** kết quả, không xét tab đang chọn.

**Cách sửa:** thêm (ngay dưới `hasAnySearchResults`):
```kotlin
val currentTabHasResults = when (selectedFilter) {
    SearchFilterTab.ALL -> hasAnySearchResults
    SearchFilterTab.SONGS -> displayTracks.isNotEmpty()
    SearchFilterTab.ARTISTS -> displayArtists.isNotEmpty()
    SearchFilterTab.ALBUMS -> displayAlbums.isNotEmpty()
}
```
và dùng `currentTabHasResults` như code ở 9a.

### 9c. Nút "Tìm kiếm" trên bàn phím không làm gì, ô tìm kiếm không tự focus

**Vị trí:** `BasicTextField` ~dòng 357–371.

**Cách sửa:**
```kotlin
val focusRequester = remember { FocusRequester() }
val keyboardController = LocalSoftwareKeyboardController.current

LaunchedEffect(Unit) {
    if (searchQuery.isEmpty()) focusRequester.requestFocus()
}

BasicTextField(
    // ... giữ nguyên các tham số cũ
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = {
        keyboardController?.hide()
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            searchPrefs.addRecentSearch(q)
            recentSearches = searchPrefs.getRecentSearches()
        }
    }),
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .bringIntoViewResponder(noOpBringIntoViewResponder)
)
```
Import: `androidx.compose.ui.focus.FocusRequester`, `androidx.compose.ui.focus.focusRequester`, `androidx.compose.ui.platform.LocalSoftwareKeyboardController`, `androidx.compose.foundation.text.KeyboardActions`.

**Kiểm tra Mục 9:**
- [ ] Gõ nhanh "son tung" → không còn nháy chữ "Không tìm thấy".
- [ ] Gõ tên một bài (không trùng tên ca sĩ), chọn tab "Nghệ sĩ" → hiện "Không tìm thấy kết quả…", không trống trơn.
- [ ] Mở tab Tìm kiếm → bàn phím tự bật. Bấm nút Tìm trên bàn phím → bàn phím ẩn, từ khóa xuất hiện trong "Đã tìm kiếm gần đây".

---

# ĐỢT 2 – Tránh mất dữ liệu

## Mục 2 – Xóa không hỏi lại, không hoàn tác được

> Làm [bước 10.2 – Snackbar dùng chung](#bước-102--snackbar-dùng-chung) **trước** mục này.

**Nguyên tắc chung cho cả nhóm:**
- Thao tác **khó khôi phục** (xóa playlist, xóa thư mục quét, xóa file) → `ApexConfirmDialog` (đã có sẵn trong `ui/components/ApexConfirmDialog.kt`).
- Thao tác **nhẹ, khôi phục được** (bỏ một bài khỏi playlist) → làm ngay + Snackbar "Hoàn tác".

### 2a. Xóa playlist ngay trong danh sách Playlist (Home)

**Vị trí:** `HomeScreen.kt` ~dòng 1305–1316 – nút thùng rác gọi `onDeletePlaylist(pl.id)` luôn.

**Cách sửa:**
1. Thêm state ở đầu `HomeScreen` (cạnh `var showDeleteDialog ...`):
   ```kotlin
   var playlistPendingDelete by remember { mutableStateOf<CustomPlaylist?>(null) }
   ```
2. Nút thùng rác chỉ đặt state:
   ```kotlin
   onClick = { playlistPendingDelete = pl },
   ```
3. Cuối hàm, cạnh các dialog khác:
   ```kotlin
   playlistPendingDelete?.let { pl ->
       ApexConfirmDialog(
           title = "Xóa danh sách phát?",
           message = "Playlist \"${pl.name}\" sẽ bị xóa. Các bài hát gốc trên máy không bị ảnh hưởng.",
           confirmButtonText = "Xóa",
           isDestructive = true,
           onConfirm = {
               onDeletePlaylist(pl.id)
               playlistPendingDelete = null
           },
           onDismiss = { playlistPendingDelete = null }
       )
   }
   ```
4. **Dọn code chết:** khối `if (showDeleteDialog) { ... }` (~dòng 2165–2271) và biến `showDeleteDialog` **không bao giờ được bật** (không có chỗ nào gán `showDeleteDialog = true`). Xóa cả hai.

### 2b. Bỏ bài khỏi playlist (trang chi tiết)

**Vị trí:** `DetailScreen.kt` ~dòng 489–513. Nút xóa (36dp) đứng sát nút tim (36dp) → rất dễ bấm nhầm.

**Cách sửa:** dùng Snackbar có "Hoàn tác":
```kotlin
val showSnackbar = LocalAppSnackbar.current   // từ bước 10.2
// ...
.apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
    onRemoveTrackFromPlaylist(customPlaylist.id, track.id)
    showSnackbar(
        "Đã xóa \"${track.title}\" khỏi \"${customPlaylist.name}\"",
        "Hoàn tác"
    ) {
        onAddTrackToPlaylist?.invoke(customPlaylist.id, track.id)
    }
}
```
- Thêm tham số `onAddTrackToPlaylist: ((playlistId: String, trackId: String) -> Unit)? = null` cho `DetailScreen`, truyền từ `HomeScreen` → `MainActivity` (`musicRepository.addTrackToPlaylist(...)`).
- ⚠️ Hoàn tác sẽ đưa bài về **cuối** playlist (vì `addTrackToPlaylist` luôn thêm vào cuối). Ghi chú điều này trong PR; nếu cần giữ đúng vị trí thì phải thêm hàm `insertTrackAt` trong `PlaylistPreferences.kt` – để sau.
- Thêm `Spacer(Modifier.width(12.dp))` giữa nút tim và nút xóa để giảm bấm nhầm.

### 2c. Xóa thư mục quét nhạc

**Vị trí:** `FolderManagerScreen.kt` ~dòng 343.

**Cách sửa:** giống 2a – thêm state `folderPendingRemove`, bấm thùng rác chỉ đặt state, `ApexConfirmDialog` với message: *"Các bài hát trong thư mục này sẽ biến mất khỏi thư viện OneMusic (file gốc không bị xóa)."*

### 2d. "Xóa khỏi Thư viện" trong Action Sheet thực ra là xóa khỏi hàng đợi

**Vị trí:** `ApexTrackActionSheet.kt` ~dòng 266–278. Ở Now Playing, `onDeleteTrack` gọi `onRemoveQueueItem` (xóa khỏi **hàng đợi**).

**Cách sửa:** đổi nhãn cho đúng và bỏ màu đỏ (đây không phải thao tác phá hủy):
```kotlin
ActionSheetRow(
    icon = Icons.Rounded.RemoveCircleOutline,
    label = "Xóa khỏi hàng đợi",
    onClick = { ... giữ nguyên ... }
)
```
Chuyển mục này xuống **cuối** danh sách (người dùng hay bấm các mục đầu).

**Kiểm tra Mục 2:**
- [ ] Home → Playlist → bấm thùng rác → hiện hộp hỏi. Bấm Hủy → playlist còn nguyên.
- [ ] Trang chi tiết playlist → xóa một bài → Snackbar hiện → bấm "Hoàn tác" → bài quay lại.
- [ ] Folder Manager → xóa thư mục → hiện hộp hỏi.
- [ ] Now Playing → ⋮ → mục cuối là "Xóa khỏi hàng đợi", chữ trắng.

---

## Mục 3 – Action Sheet có các nút "giả"

**Triệu chứng:** Now Playing → ⋮:
- "Ghim bài hát" → toast "Đã ghim bài hát lên đầu danh sách" nhưng không có gì được ghim.
- "Tải về" → toast "Bài hát đã có sẵn trên thiết bị" (app nghe nhạc offline, nút vô nghĩa).
- "Chia sẻ lời bài hát..." → chia sẻ chỉ có tên bài, **không có lời**.
- "Chia sẻ Đài phát", "Tạo đài phát" → chỉ hiện toast.

**Vị trí:** `ApexTrackActionSheet.kt` ~dòng 238–370.

**Vì sao là vấn đề:** người dùng tin nút đã chạy, rồi mất niềm tin khi thấy không có gì xảy ra. Nút không làm gì còn tệ hơn không có nút.

**Cách sửa:**
1. **Xóa** các mục "Tải về", "Chia sẻ Đài phát", "Tạo đài phát".
2. "Ghim bài hát": chỉ hiện khi có chức năng thật:
   ```kotlin
   if (onPinTrack != null) {
       ActionSheetRow(icon = Icons.Rounded.PushPin, label = "Ghim bài hát", onClick = { ... onPinTrack(track) ... })
   }
   ```
   (bỏ nhánh `?: run { Toast ... }`).
3. "Chia sẻ lời bài hát": chỉ hiện khi `track.lyrics.isNotEmpty()`, và gửi lời thật:
   ```kotlin
   if (track.lyrics.isNotEmpty()) {
       ActionSheetRow(
           icon = Icons.Rounded.FormatQuote,
           label = "Chia sẻ lời bài hát",
           onClick = {
               scope.launch {
                   sheetState.hide()
                   onDismissRequest()
                   val lyricsText = track.lyrics.joinToString("\n") { it.text }.take(1500)
                   val shareIntent = Intent(Intent.ACTION_SEND).apply {
                       type = "text/plain"
                       putExtra(Intent.EXTRA_SUBJECT, "Lời bài hát: ${track.title}")
                       putExtra(Intent.EXTRA_TEXT, "${track.title} – ${track.artist}\n\n$lyricsText")
                   }
                   context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ lời bài hát"))
               }
           }
       )
   }
   ```
4. Sửa lại comment đầu hàm ("11 Mục Chuẩn 1:1 Apple Music") cho khớp số mục mới.

**Kiểm tra:**
- [ ] Mở ⋮ ở bài **có** lời → có mục "Chia sẻ lời bài hát", chia sẻ ra có lời thật.
- [ ] Mở ⋮ ở bài **không** lời → không có mục đó.
- [ ] Không còn mục nào chỉ hiện toast mà không làm gì.

---

## Mục 4 – Nội dung cuối danh sách bị che

**Triệu chứng:**
- Cài đặt → cuộn xuống cuối → thẻ "OneMusic – Phiên bản 1.0.0" bị NowBar + thanh tab che mất một nửa.
- Trên máy dùng **3 phím điều hướng** (không phải cử chỉ), vài dòng cuối của Thư viện/Trang chủ không bao giờ cuộn lên được hết.

**Vì sao:** khoảng đệm dưới được **gán cứng**:
- `HomeScreen.kt`, `LibraryScreen.kt`, `DetailScreen.kt`: `PaddingValues(bottom = 150.dp)`.
- `SettingsScreen.kt` ~dòng 146: `100.dp + navBottom`.

Trong khi cụm nổi thực tế = NowBar (~66dp) + thanh tab (~78dp) + thanh hệ thống (24–48dp) ≈ **170–192dp**.

**Cách sửa:** đo chiều cao thật một lần ở MainActivity rồi "phát" xuống mọi màn.

1. Tạo file mới `ui/utils/BottomOverlayPadding.kt`:
   ```kotlin
   package com.example.onemusic.ui.utils

   import androidx.compose.runtime.compositionLocalOf
   import androidx.compose.ui.unit.Dp
   import androidx.compose.ui.unit.dp

   /** Chiều cao cụm NowBar + thanh tab nổi ở đáy, đã cộng thanh điều hướng hệ thống. */
   val LocalBottomOverlayPadding = compositionLocalOf { 180.dp }
   ```
2. Trong `MainActivity.kt`, hàm `OneMusicApp`:
   ```kotlin
   val density = LocalDensity.current
   var bottomOverlayHeightPx by remember { mutableIntStateOf(0) }
   val bottomOverlayPadding = with(density) { bottomOverlayHeightPx.toDp() } + 16.dp
   ```
   - Thêm `LocalBottomOverlayPadding provides bottomOverlayPadding` vào `CompositionLocalProvider(...)` có sẵn (~dòng 376).
   - Gắn `.onSizeChanged { if (it.height > 0) bottomOverlayHeightPx = it.height }` vào `Column` chứa NowBar + thanh tab (~dòng 599).
3. Trong mọi màn, thay số cứng:
   ```kotlin
   contentPadding = PaddingValues(bottom = LocalBottomOverlayPadding.current)
   ```
   Danh sách chỗ cần thay (Ctrl+Shift+F `bottom = 150.dp` và `bottom = 100.dp + navBottom`):
   - `HomeScreen.kt` ~402, ~1116, ~1366, ~1585
   - `LibraryScreen.kt` ~347, và `padding(bottom = 150.dp ...)` của thanh chữ cái (~1491) và dock chọn nhiều (~1504)
   - `DetailScreen.kt` ~266
   - `SettingsScreen.kt` ~146
   - `SearchScreen.kt` ~442: `val bottomContentPadding = if (isKeyboardOpen) 24.dp else LocalBottomOverlayPadding.current`

> Các màn phủ **không** có NowBar (Folder Manager, Duplicate Cleaner) giữ nguyên `120.dp + navBottom`.

**Kiểm tra:**
- [ ] Máy/giả lập bật **3 phím điều hướng** (Cài đặt hệ thống → Điều hướng): cuộn hết Cài đặt → thẻ "OneMusic" hiện trọn, cách NowBar ~16dp.
- [ ] Chưa phát bài nào (NowBar ẩn): cuối danh sách không bị thừa khoảng trống quá lớn.
- [ ] Đổi sang điều hướng cử chỉ → vẫn đúng.

---

## Mục 5 – Lần mở app đầu tiên trống trơn

**Triệu chứng:** Cài app mới, mở lên → Trang chủ chỉ có lời chào, "Chưa có bài hát nào được phát gần đây", Playlist (1) với 0 bài. **Không có nút nào để quét nhạc.** Người dùng phải tự mò vào Thư viện → ⋮ → Quét lại, hoặc Cài đặt.

**Vì sao:**
- `MainActivity` chỉ xin quyền **thông báo** (~dòng 226–236), không xin quyền đọc nhạc.
- `HomeScreen.kt` có sẵn `triggerScanWithPermission()` (~dòng 333) nhưng **không chỗ nào gọi**.
- Nút "Quét lại toàn bộ thư viện" trong Cài đặt (~dòng 571–579) gọi `onRescan()` **không kiểm tra quyền** → không có quyền thì im lặng không làm gì.
- Logic xin quyền + quét bị **copy y hệt** ở `HomeScreen` và `LibraryScreen`.

**Cách sửa:**

1. Tạo file `ui/utils/ScanWithPermission.kt` gom logic về một chỗ:
   ```kotlin
   package com.example.onemusic.ui.utils

   import android.Manifest
   import android.content.pm.PackageManager
   import android.os.Build
   import android.widget.Toast
   import androidx.activity.compose.rememberLauncherForActivityResult
   import androidx.activity.result.contract.ActivityResultContracts
   import androidx.compose.runtime.Composable
   import androidx.compose.runtime.getValue
   import androidx.compose.runtime.remember
   import androidx.compose.runtime.rememberUpdatedState
   import androidx.compose.ui.platform.LocalContext
   import androidx.core.content.ContextCompat

   /** Trả về hàm: có quyền đọc nhạc thì quét luôn, chưa có thì xin quyền rồi quét. */
   @Composable
   fun rememberScanWithPermission(onRescan: () -> Unit): () -> Unit {
       val context = LocalContext.current
       val currentOnRescan by rememberUpdatedState(onRescan)
       val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
           Manifest.permission.READ_MEDIA_AUDIO
       } else {
           Manifest.permission.READ_EXTERNAL_STORAGE
       }
       val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
           if (granted) currentOnRescan()
           else Toast.makeText(context, "Cần quyền truy cập nhạc để quét bài hát", Toast.LENGTH_LONG).show()
       }
       return remember(launcher) {
           {
               if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                   currentOnRescan()
               } else {
                   launcher.launch(permission)
               }
           }
       }
   }
   ```
2. Trong `HomeScreen.kt` và `LibraryScreen.kt`: **xóa** `permissionLauncher` + `fun triggerScanWithPermission()`, thay bằng:
   ```kotlin
   val triggerScanWithPermission = rememberScanWithPermission(onRescan)
   ```
   Các chỗ gọi `triggerScanWithPermission()` giữ nguyên.
3. Trong `SettingsScreen.kt`, nút "Quét lại toàn bộ thư viện ngay" dùng `rememberScanWithPermission(onRescan)` tương tự.
4. **Màn trống ở Trang chủ:** trong `HomeSubView.MAIN`, ngay sau item `"home_header"`, thêm:
   ```kotlin
   if (tracks.isEmpty()) {
       item(key = "home_empty_library") {
           Column(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(horizontal = 20.dp, vertical = 12.dp)
                   .apexGlassCard(shape = RoundedCornerShape(26.dp))
                   .padding(28.dp),
               horizontalAlignment = Alignment.CenterHorizontally
           ) {
               Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = PrismBlue, modifier = Modifier.size(48.dp))
               Spacer(Modifier.height(12.dp))
               Text("Thư viện đang trống", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
               Spacer(Modifier.height(6.dp))
               Text(
                   "Quét bộ nhớ máy hoặc chọn thư mục chứa nhạc để bắt đầu.",
                   color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
               )
               Spacer(Modifier.height(18.dp))
               Box(
                   modifier = Modifier
                       .clip(PillShape)
                       .background(PrismBlue)
                       .apexBounceClick { triggerScanWithPermission() }
                       .padding(horizontal = 22.dp, vertical = 12.dp)
               ) { Text("Quét nhạc trên máy", color = Color.White, fontWeight = FontWeight.Bold) }
               Spacer(Modifier.height(10.dp))
               Text(
                   "Chọn thư mục",
                   color = PrismBlue, fontWeight = FontWeight.SemiBold,
                   modifier = Modifier.apexBounceClick { onOpenFolders() }.padding(8.dp)
               )
           }
       }
   }
   ```
   Và bọc các khối "Nghe gần đây", "Có thể bạn sẽ thích", "Nghệ sĩ nổi bật" trong `if (tracks.isNotEmpty())` để màn trống gọn gàng.

> Lưu ý hành vi hiện có: `onRescan` ở MainActivity **mở Folder Manager** nếu chưa lưu thư mục nào (~dòng 470–474). Giữ nguyên – nghĩa là lần đầu bấm "Quét nhạc" sẽ xin quyền rồi đưa người dùng tới màn chọn thư mục.

**Kiểm tra:**
- [ ] Gỡ app, cài lại → Trang chủ hiện thẻ "Thư viện đang trống" với 2 nút.
- [ ] Bấm "Quét nhạc trên máy" → hộp xin quyền của hệ thống hiện ra → Cho phép → tiếp tục quét (hoặc mở chọn thư mục).
- [ ] Từ chối quyền → toast giải thích, không crash.
- [ ] Cài đặt → "Quét lại toàn bộ thư viện" khi chưa có quyền → hộp xin quyền hiện ra.

---

# ĐỢT 3 – Điều hướng (nên nhờ người review)

## Mục 6 – Chuyển tab là mất trạng thái

**Triệu chứng:**
- Home → Album → mở album "X" → bấm tab Thư viện → bấm lại tab Home → quay về **màn chính** Home, không còn ở album "X".
- Thư viện: đang cuộn ở bài thứ 300, sắp xếp "Nghệ sĩ" → sang tab khác rồi quay lại → về đầu danh sách, sắp xếp về "Tất cả".
- Tìm kiếm: đang ở tab "Album" → quay lại thành "Tất cả".

**Vì sao:** `MainActivity.kt` ~dòng 393–582 dùng `AnimatedContent(targetState = currentScreen)`. Khi đổi tab, màn cũ bị **gỡ hẳn**, mọi `remember { }` bên trong bị xóa.

**Cách sửa:**

1. `MainActivity.kt`, trong `OneMusicApp` (cạnh `val hazeState = ...`):
   ```kotlin
   val tabStateHolder = rememberSaveableStateHolder()
   ```
   và bọc nội dung mỗi tab:
   ```kotlin
   ) { screen ->
       tabStateHolder.SaveableStateProvider(screen.name) {
           Box(
               modifier = Modifier
                   .fillMaxSize()
                   .hazeSource(state = hazeState, key = screen)
           ) {
               when (screen) { ... giữ nguyên ... }
           }
       }
   }
   ```
   Import: `androidx.compose.runtime.saveable.rememberSaveableStateHolder`.

   Chỉ bước này đã giữ được **vị trí cuộn** (vì `rememberLazyListState()` vốn dùng `rememberSaveable` bên trong).

2. Đổi các state điều hướng/lựa chọn sang `rememberSaveable`. Chỉ đổi những state **người dùng mong được giữ**:

   | File | Biến | Ghi chú |
   |---|---|---|
   | `HomeScreen.kt` | `currentSubView`, `selectedArtist`, `selectedAlbum`, `isSelectedFavorites`, `albumViewMode`, `artistGroupMode` | Enum/String/Boolean → dùng được luôn |
   | `HomeScreen.kt` | `selectedPlaylist: CustomPlaylist?` | **Không lưu được object** → đổi thành `selectedPlaylistId: String?` và tra lại: `customPlaylists.find { it.id == selectedPlaylistId }` |
   | `LibraryScreen.kt` | `currentLibraryTab`, `currentSortOption`, `sortAscending` | |
   | `SearchScreen.kt` | `selectedFilter`, `selectedArtist`, `selectedAlbum` | |

   ```kotlin
   // Trước
   var currentSubView by remember { mutableStateOf(HomeSubView.MAIN) }
   // Sau
   var currentSubView by rememberSaveable { mutableStateOf(HomeSubView.MAIN) }
   ```
   **Không** đổi các state dialog/menu (`showNewPlaylistDialog`, `isTopMenuOpen`, `trackForDetailsDialog`...) – mở lại tab thấy dialog tự bật lên là lạ.

3. (Tùy chọn) Lưu kiểu sắp xếp của Thư viện vào `SettingsPreferences` giống cách đã lưu `libraryViewMode` (~dòng 426), để giữ được cả khi tắt app.

**Kiểm tra:**
- [ ] Vào một album ở Home → sang tab khác → quay lại → vẫn ở album đó, đúng vị trí cuộn.
- [ ] Thư viện cuộn xuống giữa, sắp xếp "Nghệ sĩ" → sang tab khác → quay lại → giữ nguyên.
- [ ] Xoay màn hình (nếu app cho xoay) → không mất trạng thái.
- [ ] Nút Back của hệ thống vẫn hoạt động đúng như cũ.

---

## Mục 7 – Cùng thao tác nhưng mỗi màn phản ứng khác nhau

**Bảng hiện trạng:**

| Thao tác | Home | Thư viện | Tìm kiếm |
|---|---|---|---|
| Bấm album | Mở trang chi tiết | **Phát luôn** | Mở trang chi tiết |
| Bấm nghệ sĩ | Mở trang chi tiết | **Phát luôn** | Mở trang chi tiết |
| Bấm một bài | Phát, ở lại màn | Phát, ở lại màn | Phát **+ tự mở Now Playing** |

### 7a. Thư viện: bấm album/nghệ sĩ → mở trang chi tiết

**Vị trí:** `LibraryScreen.kt` ~dòng 1148–1152 (album) và ~1281–1285 (nghệ sĩ).

**Cách sửa** (làm giống `SearchScreen.kt` ~dòng 163–220):
1. Thêm state:
   ```kotlin
   var openedAlbum by rememberSaveable { mutableStateOf<String?>(null) }
   var openedArtist by rememberSaveable { mutableStateOf<String?>(null) }
   BackHandler(enabled = openedAlbum != null || openedArtist != null) {
       openedAlbum = null
       openedArtist = null
   }
   ```
2. Ngay sau các khai báo state (trước `Box(` chính), thêm:
   ```kotlin
   openedAlbum?.let { albumName ->
       val albumTracks = albums.firstOrNull { it.first == albumName }?.third.orEmpty()
       DetailScreen(
           title = albumName,
           subtitle = albumTracks.firstOrNull()?.artist ?: "Album",
           artworkUrl = albumTracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl,
           tracks = albumTracks,
           currentTrackId = currentTrackId,
           onTrackSelect = onTrackSelect,
           onPlayAll = { onPlayTracks?.invoke(it) },
           onShuffleAll = { onPlayTracks?.invoke(it.shuffled()) },
           onToggleFavorite = onToggleFavorite,
           onBack = { openedAlbum = null }
       )
       return
   }
   ```
   Làm tương tự cho `openedArtist` (danh sách bài lấy từ `artists.firstOrNull { it.first == name }?.second`).
3. Onclick của **hàng** album/nghệ sĩ → `openedAlbum = albumName` / `openedArtist = artistName`. **Giữ** nút ▶ tròn bên phải để phát nhanh.

### 7b. Tìm kiếm: bấm bài không tự mở Now Playing

**Vị trí:** `MainActivity.kt` ~dòng 544–552 (sau Mục 1 là khối `onTrackSelect = { track, context -> ... }`).

**Cách sửa:** bỏ dòng `isPlayerExpanded = true` ở cả `onTrackSelect` và `onPlayTracks` của Search. Người dùng muốn mở thì bấm NowBar như ở các màn khác.

### 7c. Nút Quay lại ở trang chi tiết về sai chỗ

**Triệu chứng:** Home → bấm nghệ sĩ trong "Nghệ sĩ nổi bật" → bấm ← → về **danh sách tất cả nghệ sĩ**, không về Trang chủ. Tương tự với playlist từ carousel.

**Vị trí:** `HomeScreen.kt` – `onBack = { currentSubView = HomeSubView.ARTISTS }` (~2027), `PLAYLISTS` (~2062, ~2086), `ALBUMS` (~2045); và `BackHandler` ~dòng 349–360.

**Cách sửa:** nhớ màn đã mở trang chi tiết.
```kotlin
var detailBackTarget by rememberSaveable { mutableStateOf(HomeSubView.MAIN) }

fun openDetail(view: HomeSubView) {
    detailBackTarget = currentSubView   // màn đang đứng trước khi mở chi tiết
    currentSubView = view
}
```
- Mọi chỗ đang viết `currentSubView = HomeSubView.ARTIST_DETAIL` / `ALBUM_DETAIL` / `PLAYLIST_DETAIL` → đổi thành `openDetail(HomeSubView.XXX_DETAIL)`.
- Mọi `onBack` của `DetailScreen` → `onBack = { currentSubView = detailBackTarget }`.
- Trong `BackHandler`, 3 nhánh `..._DETAIL -> ...` gộp thành:
  ```kotlin
  currentSubView in setOf(HomeSubView.ARTIST_DETAIL, HomeSubView.ALBUM_DETAIL, HomeSubView.PLAYLIST_DETAIL) ->
      currentSubView = detailBackTarget
  ```

### 7d. Pill "Thư mục" ở Thư viện trông như tab nhưng mở màn khác

**Vị trí:** `LibraryScreen.kt` ~dòng 585–615 và enum `LibraryTab` ~dòng 145–150.

**Cách sửa:** bỏ `FOLDERS` khỏi hàng pill (lối vào "Quản lý thư mục" đã có trong menu ⋮ ~dòng 562–569):
```kotlin
LibraryTab.entries.filter { it != LibraryTab.FOLDERS }.forEach { tab -> ... }
```
Khối `LibraryTab.FOLDERS -> { ... }` (~1354–1383) thành code chết → xóa cùng giá trị enum `FOLDERS`.

### 7e. Home và Thư viện gộp nghệ sĩ/album theo hai cách khác nhau

**Triệu chứng:** Home → Nghệ sĩ báo "120 nghệ sĩ", Thư viện → Nghệ sĩ báo "134 nghệ sĩ".

**Vì sao:** Home (~dòng 1333–1361) gộp **không phân biệt hoa thường** và có 2 chế độ; Thư viện (~dòng 219–230) gộp **phân biệt hoa thường**.

**Cách sửa (⭐⭐⭐, làm sau cùng trong đợt):** tạo `data/search/LibraryGrouping.kt` chứa 2 hàm dùng chung `groupArtists(tracks, mode)` và `groupAlbums(tracks)` (xem cách gộp đúng ở [Mục 13](#mục-13--album-bị-gộp-sai-không-sắp-xếp)), rồi cả Home, Thư viện, Tìm kiếm đều gọi hàm này. Copy logic của Home (bản đúng hơn) sang làm chuẩn.

**Kiểm tra Mục 7:**
- [ ] Thư viện → Album → bấm một hàng → mở trang chi tiết. Bấm nút ▶ bên phải → phát luôn.
- [ ] Thư viện → trang chi tiết → nút Back của hệ thống → về lại danh sách album.
- [ ] Tìm kiếm → bấm một bài → nhạc phát, **vẫn ở** màn Tìm kiếm.
- [ ] Home → bấm nghệ sĩ trong carousel → ← → về Trang chủ. Home → Nghệ sĩ → bấm một nghệ sĩ → ← → về danh sách nghệ sĩ.
- [ ] Hàng pill Thư viện chỉ còn 3 mục.
- [ ] Số nghệ sĩ/album ở Home và Thư viện bằng nhau.

---

## Mục 8 – Danh sách không có menu thao tác cho từng bài

**Triệu chứng:** Muốn thêm **một** bài vào playlist hoặc "Phát kế tiếp" từ Thư viện/Album/Tìm kiếm → không có cách nào, trừ khi phát bài đó rồi mở Now Playing → ⋮. Nhấn giữ chỉ mở dialog thông số kỹ thuật (bitrate, tần số...).

**Vị trí:** mọi chỗ `trackForDetailsDialog = track` (Ctrl+Shift+F), và `ApexTrackActionSheet.kt`.

**Cách sửa:**

1. **Mở rộng Action Sheet** (`ApexTrackActionSheet.kt`): thêm 2 tham số và 2 mục lên **đầu** danh sách:
   ```kotlin
   onPlayNext: ((Track) -> Unit)? = null,
   onAddToQueue: ((Track) -> Unit)? = null,
   ```
   ```kotlin
   if (onPlayNext != null) {
       ActionSheetRow(icon = Icons.AutoMirrored.Rounded.QueueMusic, label = "Phát kế tiếp", onClick = {
           scope.launch { sheetState.hide(); onDismissRequest(); onPlayNext(track) }
       })
   }
   if (onAddToQueue != null) {
       ActionSheetRow(icon = Icons.Rounded.AddToQueue, label = "Thêm vào cuối hàng đợi", onClick = {
           scope.launch { sheetState.hide(); onDismissRequest(); onAddToQueue(track) }
       })
   }
   ```
   Mục "Xem danh đề" (`onOpenCredits`) đổi nhãn thành **"Thông tin bài hát"** – đây chính là lối vào dialog thông số kỹ thuật.

2. **Đưa callback xuống các màn:** thêm vào `HomeScreen`, `LibraryScreen`, `SearchScreen`, `DetailScreen`:
   ```kotlin
   onAddToPlaylist: ((Track) -> Unit)? = null,
   onAddToQueue: ((Track) -> Unit)? = null,
   ```
   Nối ở `MainActivity.kt`:
   ```kotlin
   onAddToPlaylist = { track -> trackToAddToPlaylist = track },   // dialog đã có sẵn ~dòng 721
   onAddToQueue = { track -> playerController.addToQueue(track) },
   onPlayNext = { track -> playerController.playNext(track) },
   ```

3. **Thay dialog thông số bằng Action Sheet** ở mỗi màn:
   ```kotlin
   var trackForActions by remember { mutableStateOf<Track?>(null) }
   var trackForDetailsDialog by remember { mutableStateOf<Track?>(null) }

   trackForActions?.let { t ->
       ApexTrackActionSheet(
           track = t,
           onDismissRequest = { trackForActions = null },
           onToggleFavorite = onToggleFavorite,
           onAddToPlaylist = onAddToPlaylist,
           onPlayNext = onPlayNext,
           onAddToQueue = onAddToQueue,
           onOpenCredits = { trackForDetailsDialog = t }
           // KHÔNG truyền onDeleteTrack, onOpenSleepTimer ở ngoài Now Playing
       )
   }
   ```

4. **Cách mở menu:**
   - Home / Tìm kiếm / Trang chi tiết: **nhấn giữ** → `trackForActions = track`.
   - Thư viện: nhấn giữ đã dùng để chọn nhiều ([Mục 12b](#12b-nhấn-giữ-không-vào-chế-độ-chọn)) → thêm nút **⋮** nhỏ (`Icons.Rounded.MoreVert`, vùng chạm 48dp) ở cuối mỗi hàng, cạnh nút tim, bấm → `trackForActions = track`.

**Kiểm tra:**
- [ ] Thư viện → ⋮ ở một bài → "Thêm vào playlist" → chọn playlist → bài có trong playlist.
- [ ] Trang chi tiết album → nhấn giữ một bài → "Phát kế tiếp" → mở hàng đợi thấy bài đó ngay sau bài đang phát.
- [ ] Menu → "Thông tin bài hát" → dialog thông số hiện như cũ.
- [ ] Menu mở từ ngoài Now Playing **không** có "Xóa khỏi hàng đợi" và "Hẹn giờ tắt".

---

# ĐỢT 4 – Phản hồi và trạng thái

## Mục 10 – Phản hồi khi quét nhạc & Toast

### Bước 10.1 – Chỉ báo quét nhạc không chặn thao tác

**Triệu chứng:**
- Thư viện: khi quét, **cả màn bị phủ đen** kèm vòng xoay, không bấm được gì, không hủy được (`LibraryScreen.kt` ~dòng 1628–1645).
- Trang chủ, Tìm kiếm, Cài đặt: quét xong mới có một Toast, trong lúc quét không có dấu hiệu gì.

**Cách sửa:**
1. **Xóa** khối "Loading Scanning Indicator" ở `LibraryScreen.kt` (~1628–1645).
2. Trong `MainActivity.kt`, ngay **trên** `NowBar` trong `Column` cụm nổi (~dòng 602), thêm một pill nhỏ:
   ```kotlin
   AnimatedVisibility(visible = isScanning, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
       Row(
           modifier = Modifier
               .fillMaxWidth()
               .padding(horizontal = 20.dp, vertical = 4.dp)
               .clip(PillShape)
               .background(SurfaceElevated.copy(alpha = 0.92f))
               .padding(horizontal = 16.dp, vertical = 10.dp),
           verticalAlignment = Alignment.CenterVertically
       ) {
           CircularProgressIndicator(color = PrismBlue, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
           Spacer(Modifier.width(10.dp))
           Text("Đang quét thư viện nhạc…", color = Color.White, fontSize = 13.sp)
       }
   }
   ```
   (`isScanning` đã có sẵn ở dòng ~258.)
3. Xóa các Toast "Đang quét..." ở `HomeScreen`, `LibraryScreen`, `SettingsScreen`, `FolderManagerScreen` – pill này thay thế chúng.

### Bước 10.2 – Snackbar dùng chung

App đang có **43 Toast**. Toast không đi theo giao diện tối của app, không có nút "Hoàn tác", và nhiều cái chỉ thông báo điều hiển nhiên.

1. Tạo `ui/utils/AppSnackbar.kt`:
   ```kotlin
   package com.example.onemusic.ui.utils

   import androidx.compose.runtime.staticCompositionLocalOf

   /** showSnackbar(message, actionLabel?, onAction?) */
   typealias ShowSnackbar = (String, String?, (() -> Unit)?) -> Unit

   val LocalAppSnackbar = staticCompositionLocalOf<ShowSnackbar> { { _, _, _ -> } }
   ```
2. Trong `MainActivity.kt`:
   ```kotlin
   val snackbarHostState = remember { SnackbarHostState() }
   val showSnackbar: ShowSnackbar = remember {
       { message, actionLabel, onAction ->
           scope.launch {
               snackbarHostState.currentSnackbarData?.dismiss()
               val result = snackbarHostState.showSnackbar(
                   message = message,
                   actionLabel = actionLabel,
                   duration = SnackbarDuration.Short
               )
               if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
           }
       }
   }
   ```
   - Thêm `LocalAppSnackbar provides showSnackbar` vào `CompositionLocalProvider`.
   - Đặt `SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalBottomOverlayPadding.current))` **trong** `Box` gốc, sau cụm nổi.
3. **Thay dần** Toast bằng Snackbar theo quy tắc:

   | Loại thông báo | Làm gì |
   |---|---|
   | Xác nhận điều người dùng vừa thấy rõ (ví dụ "Đã đổi tên thành X" khi tên đã đổi trên màn) | **Xóa hẳn** |
   | Kết quả có thể hoàn tác (xóa bài khỏi playlist, bỏ yêu thích) | Snackbar + "Hoàn tác" |
   | Kết quả người dùng không tự thấy (quét xong N bài, xuất playlist xong) | Snackbar không nút |
   | Lỗi | Snackbar, nội dung nói rõ **cần làm gì tiếp** |

   Danh sách Toast cần soát: Ctrl+Shift+F `Toast.makeText`.

**Kiểm tra Mục 10:**
- [ ] Bấm quét lại ở Thư viện → vẫn cuộn, bấm bài được; pill "Đang quét…" hiện trên NowBar ở **mọi tab**.
- [ ] Quét xong → Snackbar "Đã quét xong: N bài hát", pill biến mất.
- [ ] Snackbar không bị NowBar/thanh tab che.

---

## Mục 11 – Trang chủ tự nhảy nội dung

**Triệu chứng:**
- Đang xem Trang chủ, bấm tim một bài bất kỳ → banner "NỔI BẬT HÔM NAY" **đổi sang bài khác**.
- Nghe hết một bài → mục "Có thể bạn sẽ thích" **xáo lại toàn bộ** ngay trước mắt.
- Chưa từng nghe bài nào nhưng "Nghe gần đây" vẫn hiện 8 bài đầu thư viện.

**Vị trí:** `HomeScreen.kt` ~dòng 264–297.

**Vì sao:**
- `featuredTrack` được `remember(tracks, favoriteTracks)`: bấm tim tạo list `tracks` mới → chọn ngẫu nhiên lại.
- `suggestedTracks` được `remember(tracks, recentlyPlayedTracks)`: mỗi lần nghe xong một bài, "gần đây" đổi → xáo lại.
- `recentlyPlayedTracks` khi rỗng thì lấy `tracks.take(8)`.

**Cách sửa:** chỉ chọn ngẫu nhiên lại khi **danh sách bài thực sự đổi** (thêm/bớt bài), còn đổi trạng thái tim thì không.
```kotlin
// Danh sách id: so sánh bằng nội dung, bấm tim không làm nó thay đổi
val trackIds = remember(tracks) { tracks.map { it.id } }
val trackById = remember(tracks) { tracks.associateBy { it.id } }

val featuredTrackId = remember(trackIds) {
    tracks.filter { it.artworkUrl.isNotBlank() }.randomOrNull()?.id ?: tracks.randomOrNull()?.id
}
// Tra lại object mới nhất để trạng thái tim luôn đúng
val featuredTrack = featuredTrackId?.let { trackById[it] }

val recentlyPlayedTracks = remember(trackById, appSettings.recentlyPlayedTrackIds) {
    appSettings.recentlyPlayedTrackIds.mapNotNull { trackById[it] }.take(8)   // KHÔNG fallback
}

val suggestedTrackIds = remember(trackIds) {
    tracks.filter { it.artworkUrl.isNotBlank() }.shuffled().take(8).map { it.id }
}
val suggestedTracks = suggestedTrackIds.mapNotNull { trackById[it] }
```
> `remember(trackIds)`: hai `List<String>` có cùng phần tử được coi là **bằng nhau**, nên dù `tracks` là object mới sau khi bấm tim, khối này không chạy lại.

Khi `recentlyPlayedTracks` rỗng, thẻ "Chưa có bài hát nào được phát gần đây" (~dòng 563–581) sẽ tự hiện – đúng ý đồ ban đầu.

**Kiểm tra:**
- [ ] Bấm tim nhiều bài ở Thư viện → quay về Home → banner nổi bật không đổi.
- [ ] Nghe hết một bài khi đang ở Home → "Có thể bạn sẽ thích" không xáo lại.
- [ ] Cài mới, quét nhạc, chưa nghe gì → "Nghe gần đây" hiện thẻ trống.
- [ ] Bấm tim bài đang ở banner → biểu tượng tim ở nơi khác cập nhật đúng.

---

## Mục 13 – Album bị gộp sai, không sắp xếp

**Triệu chứng:**
- Hai album cùng tên "Greatest Hits" của 2 ca sĩ khác nhau bị gộp thành **một** album, lẫn bài của nhau.
- Danh sách Album ở Home không theo thứ tự ABC (theo thứ tự quét).

**Vị trí:**
- `HomeScreen.kt` ~dòng 1567–1580 (`groupBy { it.album }`), ~2032–2034 (lọc bài theo tên album).
- `LibraryScreen.kt` ~dòng 211–217.
- `SearchScreen.kt` ~dòng 204–206, và `SearchAlbumCard` ~dòng 1021–1024.

**Cách sửa:**
1. Gộp theo **cặp (tên album, nghệ sĩ chính)**:
   ```kotlin
   private fun albumKey(track: Track): String {
       val album = track.album.ifBlank { "Album chưa rõ" }.trim().lowercase()
       val artist = extractArtistNames(track.artist).firstOrNull().orEmpty().trim().lowercase()
       return "$album|$artist"
   }
   ```
   `groupBy { albumKey(it) }` rồi `sortedBy { it.name.lowercase() }`.
2. Lưu `albumKey` (không phải tên) vào `selectedAlbum`, và trang chi tiết lọc bằng `tracks.filter { albumKey(it) == selectedAlbumKey }`.
3. Đưa hàm `albumKey` vào `data/search/LibraryGrouping.kt` ([Mục 7e](#7e-home-và-thư-viện-gộp-nghệ-sĩalbum-theo-hai-cách-khác-nhau)) để 3 màn dùng chung.

> **Chưa làm được:** sắp xếp bài trong album theo **số thứ tự track**. Model `Track` (`data/model/Track.kt`) chưa có trường `trackNumber`/`discNumber`; cần sửa bộ quét `data/scanner/LocalMusicScanner.kt` và database. Ghi thành việc riêng, cần người có kinh nghiệm.

**Kiểm tra:**
- [ ] Hai album trùng tên của 2 nghệ sĩ hiện thành 2 mục riêng, mỗi mục đúng bài.
- [ ] Danh sách album ở Home, Thư viện, Tìm kiếm đều theo ABC và có cùng số lượng.

---

## Mục 15 – Trang chi tiết thiếu nút Trộn bài và tiêu đề

**Vị trí:** `DetailScreen.kt`.

### 15a. Thiếu nút Trộn bài

Tham số `onShuffleAll` (~dòng 110) được truyền vào nhưng **không có nút nào dùng**.

**Cách sửa:** trong khối nút "Phát tất cả" (~dòng 336–365), bọc lại thành `Row`:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(0.82f),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(PillShape)
            .background(PrismBlue)
            .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) { ... giữ nguyên ... },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) { ... icon + "Phát tất cả" giữ nguyên ... }

    ApexCircularGlassButton(
        icon = Icons.Rounded.Shuffle,
        contentDescription = "Trộn bài",
        onClick = { if (tracks.isNotEmpty()) onShuffleAll(tracks) },
        size = 48.dp,
        iconSize = 22.dp
    )
}
```

### 15b. Cuộn xuống thì mất tiêu đề

Thanh trên cùng (~dòng 529–596) chỉ có nút ← và nút phải; khi cuộn qua phần ảnh bìa, người dùng không biết đang ở album nào.

**Cách sửa:**
```kotlin
val showTopTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```
Thay `Spacer(modifier = Modifier.weight(1f))` (~dòng 550) bằng:
```kotlin
Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
    androidx.compose.animation.AnimatedVisibility(visible = showTopTitle, enter = fadeIn(), exit = fadeOut()) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
```
Và thêm nền mờ cho cả thanh khi `showTopTitle` (ví dụ `.background(ObsidianBlack.copy(alpha = if (showTopTitle) 0.85f else 0f))` trên `Box` ngoài cùng của thanh).

**Kiểm tra:**
- [ ] Trang chi tiết có nút Trộn bài cạnh "Phát tất cả"; bấm → phát ngẫu nhiên trong album.
- [ ] Cuộn xuống qua ảnh bìa → tên album hiện giữa thanh trên cùng, có nền tối phía sau.

---

# ĐỢT 5 – Nhất quán & trợ năng

## Mục 16 – Hiệu ứng chạm không đồng nhất

**Triệu chứng:** Phần lớn nút có hiệu ứng **nảy** (`apexBounceClick`), nhưng một số chỗ có hiệu ứng **gợn sóng** (ripple) kiểu Material mặc định → cảm giác chắp vá.

**Vị trí** (Ctrl+Shift+F `.clickable {` trong `ui/screens`):
- `LibraryScreen.kt` ~832 (nút "Quét lại" ở màn trống), ~1528, ~1550, ~1571, ~1615 (dock chọn nhiều).
- `SettingsScreen.kt` ~777 (`SettingsToggleRow`) – sửa ở [Mục 18c](#18c-hàng-bậttắt-trong-cài-đặt-không-báo-trạng-thái).

**Cách sửa:** thay `.clickable { X }` bằng `.apexBounceClick(scaleDown = 0.92f, enableHaptic = true) { X }`.

**Kiểm tra:**
- [ ] Bấm từng nút trong dock chọn nhiều → đều nảy nhẹ + rung, không có gợn sóng.

---

## Mục 17 – Màn Cài đặt khó đọc

**Vị trí:** `SettingsScreen.kt`.

### 17a. Không có tiêu đề nhóm

~25 tùy chọn chia vào 5 thẻ nhưng **không có tiêu đề** (tiêu đề chỉ nằm trong comment code). Người dùng phải đọc từng dòng để tìm.

**Cách sửa:** thêm composable:
```kotlin
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp,
            fontSize = 12.sp
        ),
        modifier = Modifier.padding(start = 36.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}
```
và thay 4 item `spacer_ui`, `spacer_haptics`, `spacer_library`, `spacer_about` (cùng thêm một cái trước thẻ đầu) bằng:
`item { SettingsSectionHeader("Âm thanh & phát nhạc") }`, `"Giao diện"`, `"Rung phản hồi"`, `"Thư viện & bộ nhớ"`, `"Thông tin"`.

### 17b. Mô tả quá dài, trộn tiếng Anh

Ví dụ hiện tại: *"Hiển thị video hoạt họa chuyển động toàn màn hình khi nghe bài hát có hỗ trợ từ Apple Music. Tắt đi sẽ chỉ hiển thị ảnh bìa tĩnh"*.

**Quy tắc viết lại:**
- Tiêu đề: tiếng Việt trước, thuật ngữ tiếng Anh (nếu cần) để trong mô tả. Ví dụ "Phát liền mạch" thay cho "Phát nhạc liền mạch (Gapless Playback)".
- Mô tả: **một câu, dưới ~70 ký tự**, nói *điều gì xảy ra*, không quảng cáo ("Ultra HD", "1000x1000px", "tinh tế"...).

| Hiện tại | Đề xuất |
|---|---|
| Bìa đĩa chuyển động (Motion Artwork) – *Hiển thị video hoạt họa…* | **Bìa động** – *Phát video bìa cho bài hát có hỗ trợ* |
| Tự động tải ảnh nghệ sĩ chất lượng cao – *…1000x1000px Ultra HD từ Deezer/iTunes* | **Tải ảnh nghệ sĩ** – *Tự tải ảnh ca sĩ từ Internet* |
| Rung phản hồi cảm ứng (Tactile Haptics) – *Hiệu ứng rung xúc giác tinh tế…* | **Rung khi chạm** – *Rung nhẹ khi bấm nút và vuốt* |

Làm tương tự cho các mục còn lại.

### 17c. Thanh trượt Crossfade ghi bộ nhớ liên tục

**Vị trí:** ~dòng 273–282 – mỗi bước kéo gọi `settingsPreferences.updateSettings` (ghi file).

**Cách sửa:** giữ giá trị tạm trong lúc kéo, chỉ lưu khi thả tay:
1. `ApexSlider.kt` thêm tham số `onValueChangeFinished: (() -> Unit)? = null` và truyền vào `Slider(... onValueChangeFinished = onValueChangeFinished ...)`.
2. `SettingsScreen.kt`:
   ```kotlin
   var crossfadeDraft by remember(settings.crossfadeDurationSeconds) {
       mutableFloatStateOf(settings.crossfadeDurationSeconds.toFloat())
   }
   // Text hiển thị: "${crossfadeDraft.roundToInt()} giây"
   ApexSlider(
       value = crossfadeDraft,
       onValueChange = { crossfadeDraft = it },
       onValueChangeFinished = {
           settingsPreferences.updateSettings { it.copy(crossfadeDurationSeconds = crossfadeDraft.roundToInt()) }
       },
       valueRange = 1f..10f,
       steps = 8   // nếu ApexSlider hỗ trợ; nếu không thì thêm tham số steps tương tự
   )
   ```

**Kiểm tra Mục 17:**
- [ ] Cài đặt có 5 tiêu đề nhóm rõ ràng.
- [ ] Không mô tả nào dài quá 2 dòng trên máy 6 inch.
- [ ] Kéo thanh Crossfade mượt, con số cập nhật theo tay; thoát ra vào lại vẫn giữ giá trị.

---

## Mục 18 – Trợ năng (Accessibility)

> Cách kiểm tra chung: bật **TalkBack** (Cài đặt hệ thống → Hỗ trợ tiếp cận → TalkBack), vuốt qua từng phần tử và nghe máy đọc. Cài thêm app **Accessibility Scanner** của Google để quét tự động.

### 18a. Mọi nút `apexBounceClick` không được TalkBack đọc là "Nút"

**Vị trí:** `ui/utils/ApexTouchPhysics.kt` ~dòng 48–93.

**Cách sửa:** thêm tham số `role` và truyền vào `combinedClickable`:
```kotlin
fun Modifier.apexBounceClick(
    scaleDown: Float = 0.95f,
    enableHaptic: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    // ...
    .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        role = role,
        onClickLabel = onClickLabel,
        onClick = { ... },
        onLongClick = ...
    )
}
```
Import `androidx.compose.ui.semantics.Role`. Vì tham số mới có giá trị mặc định, **không cần sửa** hàng trăm chỗ gọi.

### 18b. Thanh tab ở đáy không bấm được bằng TalkBack

**Vị trí:** `ui/navigation/ApexBottomNavigation.kt` ~dòng 256–271. Cử chỉ được xử lý bằng `pointerInput` tự viết, nên TalkBack không biết các tab bấm được.

**Cách sửa:** thêm semantics cho từng tab (**không** dùng `clickable` vì sẽ xung đột với cử chỉ kéo):
```kotlin
Box(
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .semantics(mergeDescendants = true) {
            role = Role.Tab
            selected = isSelected
            contentDescription = screen.title
            onClick(label = screen.title) {
                onScreenSelectedState(screen)
                true
            }
        },
    contentAlignment = Alignment.Center
) {
    Icon(imageVector = screen.icon, contentDescription = null, ...)   // đã có mô tả ở Box
}
```

### 18c. Hàng bật/tắt trong Cài đặt không báo trạng thái

**Vị trí:** `SettingsScreen.kt` `SettingsToggleRow` ~dòng 774–778.

**Cách sửa:**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
        .padding(horizontal = 18.dp, vertical = 14.dp),
```
Công tắc `ApexToggle` bên trong hàng nên đặt `Modifier.clearAndSetSemantics { }` để TalkBack không đọc hai lần.

### 18d. Mô tả tiếng Anh cho trình đọc màn hình

| File | Dòng (~) | Hiện tại | Đổi thành |
|---|---|---|---|
| `NowBar.kt` | 325 | `"Pause"` / `"Play"` | `"Tạm dừng"` / `"Phát"` |
| `NowBar.kt` | 345 | `"Next"` | `"Bài tiếp theo"` |
| `LibraryScreen.kt` | 979, 1840 | `"Favorite"` | `if (track.isFavorite) "Bỏ yêu thích" else "Yêu thích"` |

Soát thêm: Ctrl+Shift+F `contentDescription = "[A-Z]` (bật regex).

### 18e. Vùng chạm quá nhỏ

Chuẩn Android: **tối thiểu 48×48dp**. Các chỗ đang nhỏ hơn:

| File | Nút | Kích thước |
|---|---|---|
| `NowPlayingSheet.kt` | Nút sao / ⋮ cạnh tên bài | 30dp |
| `SearchScreen.kt` | Nút ✕ xóa ô tìm kiếm | 32dp |
| `LibraryScreen.kt` | Nút tim (grid) | 32dp |
| `SearchScreen.kt` | Nút ✕ xóa lịch sử | 34dp |
| `LibraryScreen.kt`, `DetailScreen.kt` | Nút tim, nút xóa khỏi playlist | 36dp |
| `NowBar.kt` | Phát/Tạm dừng, Bài tiếp | 38dp |

**Cách sửa:** giữ nguyên **hình** nhỏ nhưng mở rộng **vùng chạm** – đặt `Modifier.minimumInteractiveComponentSize()` **trước** `.size(...)`:
```kotlin
Box(
    modifier = Modifier
        .minimumInteractiveComponentSize()   // vùng chạm ≥ 48dp
        .size(30.dp)                          // hình vẫn 30dp
        .clip(CircleShape)
        ...
```

### 18f. Độ tương phản chữ thấp

Chuẩn WCAG AA: chữ thường cần tỉ lệ tương phản **≥ 4.5:1**.

| Chỗ | Màu chữ / nền | Tỉ lệ ước tính | Cách sửa |
|---|---|---|---|
| Chữ gợi ý ô tìm kiếm (`SearchScreen.kt` ~350) | `TextTertiary #646C78` / `SurfaceElevated` | ~3:1 ❌ | Dùng `TextSecondary` |
| Tên ca sĩ trong Action Sheet (`ApexTrackActionSheet.kt` ~203) | `ApexForest #228B22` / `SurfaceElevated` | ~3.6:1 ❌ | Dùng `ApexForestLight #2EA02E` (~4.6:1) |
| Chữ trắng trên nút xanh lá (`PrismBlue`) | trắng / `#228B22` | ~4.4:1 ⚠️ | Nền nút dùng `ApexForestDark #1B6E1B` (~6.4:1), hoặc chữ ≥ 18sp đậm |

Kiểm tra lại bằng công cụ: [webaim.org/resources/contrastchecker](https://webaim.org/resources/contrastchecker/).

### 18g. Ngưỡng vuốt của NowBar tính bằng pixel

**Vị trí:** `NowBar.kt` ~dòng 163, 167, 171, 194–215 – các số `65f`, `35f`, `140f`, `100f` là **pixel**. Trên máy màn hình mật độ cao (3x), 65px chỉ ≈ 22dp → vuốt nhẹ cũng chuyển bài; trên máy mật độ thấp thì phải vuốt rất xa.

**Cách sửa:**
```kotlin
val density = LocalDensity.current
val skipThresholdPx = with(density) { 32.dp.toPx() }
val expandThresholdPx = with(density) { 16.dp.toPx() }
val maxDragXPx = with(density) { 64.dp.toPx() }
val maxDragYPx = with(density) { 44.dp.toPx() }
```
rồi thay các số cứng tương ứng (`65f` → `skipThresholdPx`, `50f` → `skipThresholdPx * 0.77f`, `35f` → `expandThresholdPx`, `25f` → `expandThresholdPx * 0.7f`, `140f` → `maxDragXPx`, `100f` → `maxDragYPx`).

**Kiểm tra Mục 18:**
- [ ] TalkBack: mọi nút đọc "…, Nút"; tab đáy đọc "Trang chủ, Tab, Đã chọn" và **nhấn đúp để chuyển tab được**.
- [ ] TalkBack: hàng cài đặt đọc "…, Công tắc, Đang bật".
- [ ] Không còn mô tả tiếng Anh.
- [ ] Accessibility Scanner không còn báo "Touch target" và "Text contrast" ở các chỗ trong bảng.
- [ ] Vuốt NowBar trên 2 máy có mật độ màn hình khác nhau → cảm giác giống nhau.

---

## Checklist tổng khi hoàn tất

- [ ] Build `assembleDebug` không lỗi, không cảnh báo mới.
- [ ] Chạy toàn bộ phần "Kiểm tra" ở trên trên **ít nhất 1 máy thật** và 1 giả lập **dùng 3 phím điều hướng**.
- [ ] Thử với thư viện **trống**, thư viện nhỏ (< 20 bài) và thư viện lớn (≥ 1.000 bài).
- [ ] Thử một lượt toàn app với **TalkBack bật**.
- [ ] Ctrl+Shift+F `Toast.makeText` – số lượng giảm rõ so với 43 ban đầu, những cái còn lại đều có lý do.
- [ ] Mỗi mục là một commit riêng, message ghi rõ số mục.
