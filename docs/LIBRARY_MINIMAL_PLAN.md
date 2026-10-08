# Kế hoạch tối giản giao diện Thư viện

> Viết ngày 09/10/2026. Đường dẫn tính từ `app/src/main/java/com/example/onemusic/ui/screens/library/`. Số dòng có thể lệch — tìm theo đoạn code được nhắc.

## 1. Hiện trạng (tab Bài hát, từ trên xuống)

| # | Thành phần | File | Vấn đề |
|---|---|---|---|
| 1 | Tiêu đề "Thư viện" 32sp + phụ đề "N bài hát" + nút ⋮ | `components/LibraryCollapsibleHeader.kt` | Menu ⋮ chứa bộ chọn chế độ xem 3 ô (~150 dòng code lặp 3 lần) |
| 2 | Hàng 4 tab dạng pill có viền 1.5dp | `components/LibraryTabRow.kt` | Viền + nền đặc nặng mắt; nhãn "Tất cả bài hát" dài, phải cuộn ngang |
| 3 | Hàng 4 chip sắp xếp + chip đổi chế độ xem | `components/LibraryFilterBar.kt` | Chip chế độ xem **trùng** với menu ⋮; 2 hàng pill liên tiếp trông giống nhau |
| 4 | Hàng "Phát tất cả" (46dp) + nút trộn | `components/LibraryFilterBar.kt` | Chiếm cả một hàng riêng |
| 5 | Nhãn "TẤT CẢ BÀI HÁT (N)" | `tabs/SongsTabContent.kt` | **Trùng** số đếm với phụ đề ở #1 |
| 6 | Dòng bài: ảnh 70dp + nút tim | `items/SongListItem.kt` | Ảnh to, ít bài trên một màn hình |

Các tab khác cũng có nhãn trùng: "DANH SÁCH ALBUM (N)", "DANH SÁCH NGHỆ SĨ (N)", "DANH SÁCH PHÁT (N)". Tab Playlist có nút "Nhập .m3u8" **trùng** với mục trong menu ⋮.

Kết quả: trước khi thấy bài hát đầu tiên có 5 hàng điều khiển.

## 2. Mục tiêu

Còn **3 hàng** trước nội dung: Tiêu đề → Tab → Thanh công cụ gọn (1 hàng). Mỗi chức năng chỉ có **một** chỗ bấm. Không bỏ tính năng nào.

```
Thư viện                         ⋮
1.234 bài hát
Bài hát   Album   Nghệ sĩ   Playlist
[Tên A-Z ↑ ▾]          [▦]  [⤨]  [▶]
───────────────────────────────────
♪  Tên bài                          
   Nghệ sĩ · 3:42
```

## 3. Các bước (mỗi bước = 1 commit, build + chạy thử sau mỗi bước)

### Bước 1 — Xóa phần trùng lặp (rủi ro thấp)
- Xóa item nhãn mục ở cả 4 tab: `songs_section_label`, `albums_section_label`, `artists_section_label`, `playlists_section_label`. Số đếm đã có ở phụ đề header; số đã chọn khi chọn nhiều đã có ở `LibraryBatchActionBar` (`selectedCount`).
- Tab Playlist: bỏ nút tròn "Nhập .m3u8" (đã có trong menu ⋮).
- ⚠️ **Bắt buộc:** `FastAlphabetScroller` đang hard-code `headerOffsetCount = 4` (`LibraryScreen.kt`, gọi `FastAlphabetScroller(...)`). Khi bớt item phía trên phải sửa số này, nếu không bấm chữ cái sẽ nhảy lệch. Nên thay bằng hằng số đếm từ chính các `item(...)` trong `LibraryScreen` để lần sau không quên. (`SongsDragSelectRail` tìm theo key nên không bị ảnh hưởng.)

### Bước 2 — Gộp bộ lọc + phát thành một thanh công cụ
- Viết lại `LibraryFilterBar` thành 1 hàng cao ~40dp:
  - Trái: một nút văn bản "Tên A-Z ↑ ▾" mở `ApexDropdownMenu` liệt kê 4 kiểu sắp xếp (bấm lại kiểu đang chọn = đảo chiều, giữ đúng logic `setSortOption` hiện tại).
  - Phải: nút icon đổi chế độ xem (chu kỳ Danh sách → Lưới 2 → Lưới 3, giữ logic `nextViewMode`), nút trộn, nút phát (tròn, nền `PrimaryIvory`).
- Xóa khối "CHẾ ĐỘ HIỂN THỊ" khỏi menu ⋮ trong `LibraryCollapsibleHeader` (~150 dòng). Menu còn: Chọn nhiều · Quét lại · Nhập .m3u8 · Quản lý thư mục.
- Tab Album đang dùng `viewMode` nhưng không có thanh công cụ → khi xóa bộ chọn khỏi menu ⋮ sẽ **mất cách đổi chế độ xem ở tab Album**. Cách xử lý: hiện thanh công cụ rút gọn (chỉ nút chế độ xem) ở tab Album.
- Cập nhật lại `headerOffsetCount` nếu số item đổi.

### Bước 3 — Làm nhẹ hàng tab
- Đổi nhãn `LibraryTab.SONGS` từ "Tất cả bài hát" → "Bài hát" (`LibraryViewModel.kt`) để 4 tab vừa một màn hình, bỏ cuộn ngang.
- Bỏ viền 1.5dp; tab chưa chọn nền trong suốt chỉ chữ `TextSecondary`; tab đang chọn giữ nền `PrimaryIvory`.

### Bước 4 — Gọn dòng nội dung
- `SongListItem`: ảnh 70dp → 56dp, giảm padding dọc tương ứng.
- Tab Playlist: thay pill to "Tạo playlist mới" bằng một dòng đầu danh sách cùng kiểu với các dòng playlist (icon `+` + chữ "Tạo playlist mới").

### Bước 5 — Tùy chọn (cần bạn quyết, có đổi hành vi)
- **Nút tim trên mỗi dòng bài:** bỏ, chỉ hiện tim nhỏ không bấm được khi bài đã yêu thích; thêm/bỏ yêu thích qua nhấn giữ (`TrackActionMenu` đã có). Cùng hướng với commit `0c5d4dc` (bỏ nút Phát trên từng dòng).
- **Hiệu ứng đĩa vinyl** (`VinylDiscEffect` trong `items/AlbumGridCard.kt`): bỏ để ảnh album sạch hơn.
- **Lưới 3 cột:** cân nhắc chỉ giữ Danh sách + Lưới 2.

## 4. Checklist kiểm tra sau mỗi bước
- [ ] 4 tab đều hiển thị, số đếm ở phụ đề đúng.
- [ ] Sắp xếp: chọn từng kiểu, bấm lại để đảo chiều, mũi tên hiển thị đúng.
- [ ] Đổi chế độ xem ở tab Bài hát **và** tab Album; thoát app mở lại vẫn nhớ (`settingsPreferences`).
- [ ] Thanh chữ cái (sắp xếp Tên A-Z, ≥10 bài): bấm "M" nhảy đúng bài đầu tiên chữ M, không lệch.
- [ ] Chọn nhiều: kéo dải chọn bên trái, thanh thao tác đáy hiện đúng số đã chọn.
- [ ] Phát tất cả / Trộn bài hoạt động.
- [ ] Tab Playlist: tạo playlist, mở Yêu thích, nhập .m3u8 từ menu ⋮.
- [ ] Thư viện rỗng: thẻ "Chưa có bài hát nào" + nút quét vẫn hiện.
- [ ] Kiểm tra baseline kích thước file (các file được sửa đều nhỏ đi, không cần ngoại lệ mới).

## 5. Kết quả (09/10/2026)

Đã làm bước 1–4 và một phần bước 5, gộp trong một commit code vì nhiều bước sửa chồng lên cùng file.

- Bước 1: bỏ nhãn mục ở 4 tab và nút nhập .m3u8 trùng ở tab Playlist; `FastAlphabetScroller` nhận số hàng đầu trang qua hằng `SONGS_HEADER_ITEM_COUNT` (bắt buộc truyền, không còn mặc định).
- Bước 2: `LibraryFilterBar` còn 1 hàng (menu sắp xếp + nút chế độ xem / trộn / phát); bỏ bộ chọn chế độ xem khỏi menu ⋮; tab Album có thanh công cụ chỉ gồm nút chế độ xem (đặt trong `albumsTabContent` để `LibraryScreen` không vượt baseline kích thước).
- Bước 3: tab "Bài hát", bỏ viền, tab chưa chọn chỉ là chữ; vẫn giữ cuộn ngang làm dự phòng khi cỡ chữ lớn.
- Bước 4: ảnh bài 70 → 56dp; "Tạo playlist mới" là dòng đầu của khung playlist (`CreatePlaylistRow`).
- Bước 5: đĩa vinyl màu xám (`VinylGray`); bỏ `GRID_3` (giá trị đã lưu "GRID_3" tự đổi thành `GRID_2`), xóa `SongCompactGridItem` và tham số `isCompact`. **Giữ** nút tim trên từng dòng. Màn Trang chủ vẫn có lưới 3 cột riêng (`AlbumViewMode`), chưa đụng.

Chưa kiểm tra trên máy thật. `LrcParserTest.testEnhancedLrcPairTags` trượt sẵn từ trước, không liên quan.
