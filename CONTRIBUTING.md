# Hướng dẫn đóng góp – OneMusic

## Giới hạn kích thước code

Dự án từng có file 3.700 dòng (`NowPlayingSheet.kt`) và đã phải tách lại (xem
`docs/REFACTOR_SPLIT_FILES_PLAN.md`). Để không phình lại:

| Mức | File `.kt` | Hàm (kể cả `@Composable`, hàm dựng `LazyListScope`) | Cách áp dụng |
|---|---|---|---|
| **Chặn** | > 600 dòng | > 250 dòng | `./gradlew checkSizeLimits` báo lỗi (chạy cả trong `./gradlew :app:check`) |
| **Cần giải thích trong PR** | file mới > 400 dòng | composable > 200 dòng | người review kiểm tra |

- Chỉ tính trong `app/src/main`. Code test không bị giới hạn.
- Các vi phạm có từ trước nằm trong `config/size-limits-baseline.txt` kèm số dòng lúc lập. Con số đó
  **chỉ được giảm**: nếu code dài thêm, task báo lỗi. Khi bạn tách nhỏ được, task sẽ nhắc hạ số hoặc
  xóa dòng. Hãy cập nhật luôn trong PR đó.
- **Không** thêm dòng mới vào baseline hay tăng số để vượt qua kiểm tra. Hãy tách code theo mục dưới.

## Cách tách file / hàm (không đổi hành vi)

- Chỉ di chuyển code. Đổi `private` → `internal` khi sang file khác. Giữ nguyên tên và chữ ký public.
- Class/hàm public giữ ở package cũ (nơi gọi không phải sửa import). Phần phụ đặt cạnh nó hoặc vào
  package con, để `internal`.
- Compose:
  - State điều hướng, `BackHandler`, `LaunchedEffect`, launcher giữ ở composable cha. Con nhận giá trị
    và callback.
  - Giữ nguyên `key` / `contentType` của item `LazyColumn`.
  - State đổi liên tục (animation, vị trí cuộn, tiến độ phát) truyền dạng lambda `() -> T`, để chỉ
    composable con đọc nó bị recompose.
  - Khi phần con cần `Modifier.align(...)` (chỉ có trong `BoxScope`), truyền nó vào qua tham số
    `modifier` và giữ đúng thứ tự modifier.
- Class có khối `init`: mọi thuộc tính helper phải khai báo **trước** `init`.
- Mỗi bước tách là một commit: `refactor(<phạm vi>): <tách gì> (không đổi hành vi)`.

## Trước khi gửi PR

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew checkSizeLimits
```

Với thay đổi giao diện hoặc phát nhạc, chạy thêm smoke test ở mục 10 của
`docs/REFACTOR_SPLIT_FILES_PLAN.md`.
