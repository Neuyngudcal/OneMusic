# MANDATORY DESIGN SYSTEM RULES: SAMSUNG ONE UI 8.5

Tất cả các thành phần giao diện và mã nguồn trong dự án này BẮT BUỘC phải tuân thủ nghiêm ngặt các nguyên tắc thiết kế của Samsung One UI (One UI 7 / 8.5 thế hệ mới). KHÔNG ĐƯỢC PHÉP LÀM KHÁC.

---

## 1. NGUYÊN TẮC BẤT DI BẤT DỊCH (CORE MANDATES)

### 1.1. Phân Vùng "Viewing Area" vs. "Interaction Area" (Reachability First)
- **Top 35-45% (Viewing Area):** Chỉ chứa tiêu đề lớn (`32sp ExtraBold`), lời chào ngữ cảnh, thống kê, hoặc ảnh bìa. Tuyệt đối KHÔNG đặt các nút bấm tương tác chính ở góc trên cùng ngoài tầm với ngón cái.
- **Khi cuộn trang (Scroll):** Bắt buộc phải có animation co lại (collapse) mượt mà từ Large Header thành Compact App Bar (`17sp Bold`).
- **Bottom 55-65% (Interaction Area):** Toàn bộ danh sách, nút điều khiển phát nhạc, nút bấm tương tác, thanh trượt Scrubber PHẢI nằm trọn trong "Thumb Zone" (vùng ngón cái thao tác một tay).

### 1.2. Hình Học Squircle & Đường Cong Liên Tục (Continuous Curvature)
- **CẤM:** Không dùng góc vuông nhọn (`0dp`), không dùng bo góc tròn đơn giản kiểu iOS thuần.
- **BẮT BUỘC:** Sử dụng chuẩn Squircle / Continuous Curve:
  - `SquircleSmall`: `12dp` (Tags, badges nhỏ)
  - `SquircleMedium`: `20dp` (Album thumbnail list, nút phụ)
  - `SquircleLarge`: `26dp` (Cards, Containers, Dialogs)
  - `SquircleExtraLarge`: `32dp` (Now Playing Artwork)
  - `PillShape`: Bo tròn viên thuốc `50%` (`999dp`) cho Now Bar, Filter Chips, Action Pills, Scrubber Track.

### 1.3. Bảng Màu & Vật Liệu (OLED Deep Black & Materiality)
- **Dark Mode:** Bắt buộc nền đen tuyệt đối `#000000` (Pure AMOLED Black).
- **Cards & Surfaces:** Màu xám than sâu `#101216` đến `#181B22` với đường viền siêu mảnh `1px solid rgba(255, 255, 255, 0.08)`.
- **Primary Accent:** Màu xanh Samsung Blue `#2F80ED`, điểm nhấn Galaxy Violet `#8A2BE2`.
- **Dynamic Glassmorphism:** Các thanh nổi (Now Bar, Bottom Navigation) phải có hiệu ứng kính mờ (Acrylic Blur) và phản chiếu màu sắc của bài hát đang phát.

### 1.4. Động Lực Học (Spring Physics & 120Hz Feel)
- Mọi tương tác chạm (Touch Down) phải có phản hồi nảy `scale(0.96)` bằng `spring(dampingRatio = 0.7f, stiffness = 400f)`.
- Chuyển cảnh giữa Mini Player và Full Player phải mượt mà dạng trượt nở không giật khựng.

### 1.5. One UI 8.5 "Now Bar"
- Trình phát thu nhỏ BẮT BUỘC phải là thanh viên thuốc nổi (Floating Pill) phía trên Bottom Navigation.
- Phải có cột sóng âm thanh mini (Mini Visualizer Bars), chữ chạy Marquee và thanh tiến trình mảnh ở viền đáy viên thuốc.

### 1.6. Samsung SoundAlive & Equalizer
- Bắt buộc tích hợp công tắc Dolby Atmos, UHQ Upscaler và Equalizer 9 dải tần chuẩn (`63Hz` đến `16kHz`).

---

## 2. CHECKLIST KIỂM DUYỆT TRƯỚC KHI CODE (PRE-CODE CHECKLIST)
Mỗi khi tạo mới hoặc sửa bất kỳ màn hình nào:
- [ ] Đã bọc trong `OneUIScaffold` với Large Title collapse chưa?
- [ ] Tất cả nút bấm có nằm trong tầm với ngón tay cái không?
- [ ] Đã dùng bo góc `SquircleMedium` / `SquircleLarge` / `PillShape` chưa?
- [ ] Đã dùng đúng bảng màu `OneUIBlack`, `OneUIDarkCard`, `SamsungBlue` chưa?
- [ ] Đã có phản hồi chạm Spring Physics chưa?
