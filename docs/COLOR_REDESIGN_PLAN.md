# Kế hoạch đổi bảng màu: Trắng – Xanh dương – Đen

> Viết ngày 08/10/2026. Đường dẫn tính từ `app/src/main/java/com/example/onemusic/` trừ khi ghi khác. Số liệu đếm lấy từ mã nguồn hiện tại; số dòng có thể lệch nên hãy tìm theo tên.

## 0. Quyết định đã chốt

| Câu hỏi | Chọn |
|---|---|
| Màu "xanh" | **Xanh dương** (thay xanh lá `Brand` #228B22) |
| Vai trò của trắng | **Thêm theme sáng** (nền trắng, chữ đen, nhấn xanh), giữ theme tối |
| Theme mặc định | **Sáng**. Người dùng chọn được 3 chế độ: Sáng (mặc định), Tối, Theo hệ thống |
| Now Playing | **Giữ nguyên hoàn toàn** (kể cả màu nền động theo ảnh bìa, nút điều khiển, kính mờ, hộp thoại mở trong đó) |
| Huy hiệu Hi-Res | **Giữ nguyên** (gradient vàng) |

Ý nghĩa: toàn app dùng bảng ba màu trắng, xanh, đen với 2 theme. **Riêng Now Playing là ngoại lệ có chủ đích**: luôn giữ giao diện tối hiện tại ở cả hai theme (giống cách Apple Music làm), vì màu nền của nó phụ thuộc ảnh bìa chứ không phụ thuộc theme.

## 1. Hiện trạng cần thay đổi

- Toàn bộ màu là hằng số cấp cao nhất (`val TextPrimary = …` trong `theme/Color.kt`), **không đổi được theo theme**. Muốn có theme sáng phải biến chúng thành màu theo theme, ví dụ `LocalAppColors` (xem mục 3).
- Mức độ phụ thuộc (số file dùng token, ngoài `Color.kt`): `TextPrimary` 61, `TextSecondary` 48, `PrimaryIvory` 40, `Brand` 39, `CharcoalBlack` 27, `SurfaceActiveIndicator` 26, `SurfaceDivider` 26, `SurfaceElevated` 23, `ObsidianBlack` 16, `SurfaceCard` 15, `SurfaceBorderStrong` 14, `SurfaceControl` 13, và các token `Ivory*` trong suốt ở 34 file.
- `Theme.kt` đã có tham số `darkTheme` nhưng mặc định `true` và `MainActivity` gọi `OneMusicTheme { }` không truyền gì. **Chưa có cài đặt theme nào** trong `data/local/SettingsPreferences.kt`.
- Màu XML (`res/values/colors.xml`, `themes.xml`, widget) chỉ có bản tối, cha theme là `Material.Light`.
- Màu động ở Now Playing: `ui/utils/PaletteHelper.kt` và `ui/screens/player/backdrop/NowPlayingBackdrop.kt`.
- Có 38 chỗ `Color.Black` và 15 mức alpha `Color.Black.copy(...)` khác nhau, 1 chỗ hex cứng (`AlbumGridCard.kt`), nhiều mức alpha `SurfaceElevated` tuỳ ý (xem báo cáo kiểm tra màu).

## 2. Bảng màu mới (đề xuất, chỉnh được)

Tất cả mã màu bên dưới đã tính tương phản WCAG; chữ thường cần ≥ 4.5, chữ lớn/icon ≥ 3.

### 2.1 Màu gốc

| Vai trò | Tối | Sáng |
|---|---|---|
| Nền chính | `#000000` | `#FFFFFF` |
| Bề mặt 1 (dock, sheet, dialog) | `#1C1C1E` | `#F2F2F7` |
| Bề mặt 2 (thẻ) | `#2C2C2E` | `#E5E5EA` |
| Bề mặt chọn / pill active | `#3A3A3C` | `#D1D1D6` |
| Viền, vạch kẻ | `#38383A` | `#C6C6C8` |
| Chữ chính | `#FFFFFF` | `#000000` |
| Chữ phụ | `#EBEBF5` @ 60% (≈ `#98989F`) | `#3C3C43` @ 60% (≈ `#6C6C70`) |
| Chữ mờ / vô hiệu | `#8E8E93` | `#8E8E93` |
| **Xanh nhấn (chữ, icon, thanh trượt)** | `#0A84FF` | `#0066D6` |
| **Xanh nền nút đặc** | `#0066D6` (chữ trắng) | `#0066D6` (chữ trắng) |
| Xanh nhấn nhạt (nền chip chọn) | xanh 18% | xanh 12% |
| Lỗi / yêu thích | `#FF453A` | `#D70015` |

Cảnh báo (`ApexAmber`) giữ vì là màu ngữ nghĩa, nhưng sẽ không còn là màu chủ đạo.

### 2.2 Tương phản đã kiểm

| Cặp | Tỉ lệ | Dùng cho |
|---|---|---|
| `#0A84FF` / `#000000` | 5.76 | chữ, icon xanh trên nền tối |
| `#0A84FF` / `#1C1C1E` | 4.66 | chữ xanh trên bề mặt tối |
| `#FFFFFF` / `#0066D6` | 5.42 | chữ trên nút xanh đặc (cả hai theme) |
| `#0066D6` / `#FFFFFF` | 5.42 | chữ, icon xanh trên nền sáng |
| `#0066D6` / `#F2F2F7` | 4.86 | chữ xanh trên bề mặt sáng |
| `#8E8E93` / `#000000` | 6.44 | chữ mờ trên nền tối |
| `#8E8E93` / `#FFFFFF` | ≈ 3.3 | chữ mờ trên nền sáng — chỉ dùng cho icon hoặc chữ lớn |
| `#3C3C43` / `#FFFFFF` | 10.94 | chữ phụ trên nền sáng |
| `#FF453A` / `#1C1C1E` | 4.99 | lỗi trên tối |
| `#D70015` / `#FFFFFF` | 5.38 | lỗi trên sáng |

Lưu ý quan trọng:
- **Không dùng chữ trắng trên `#0A84FF`** (3.65). Nút xanh đặc luôn dùng `#0066D6`.
- Bề mặt `#2C2C2E` với chữ xanh `#0A84FF` chỉ đạt 3.82: chỉ dùng cho icon, không dùng cho chữ thường.

### 2.3 Đặt tên token mới

Giữ tên cũ cho dễ đổi, nhưng đổi **ý nghĩa**:

| Tên cũ | Tên mới đề xuất |
|---|---|
| `PrimaryIvory`, `TextPrimary` | `onBackground` (chữ chính) |
| `TextSecondary`, `IvoryBody` | `onSurfaceVariant` |
| `MutedIvory`, `TextTertiary` | `onSurfaceMuted` |
| `ObsidianBlack` | `background` |
| `SurfaceElevated` / `SurfaceCard` / `SurfaceActive*` | `surface1` / `surface2` / `surface3` |
| `Brand`, `BrandLight`, `BrandDark` | `accent`, `accentOnDark`/`accentOnLight`, `accentPressed` |
| `ApexCyan` | đổi thành `accent` ở chữ cái đang chọn của `ApexAlphabetScroller`; **giữ nguyên** cho huy hiệu Lossless/Hi-Res |
| `ApexIndigo` | xoá cùng `DynamicMeshBackground` |
| `Ivory*` trong suốt | `onBackground` với alpha thang cố định (xem 3.1) |

## 3. Kiến trúc kỹ thuật

### 3.1 Màu theo theme

1. Tạo `theme/AppColors.kt` với `data class AppColors(...)`, hai instance `DarkAppColors` và `LightAppColors`, và `val LocalAppColors = staticCompositionLocalOf { DarkAppColors }`.
2. Thêm `object AppTheme { val colors @Composable get() = LocalAppColors.current }`.
3. Giữ `Color.kt` làm nơi duy nhất định nghĩa mã màu thô, nhưng các token UI đổi thành thuộc tính của `AppColors`.
4. Đưa `OneMusicTheme` về hai `ColorScheme` Material (`darkColorScheme` + `lightColorScheme`) lấy từ cùng `AppColors`, để TextField, Switch, ripple, Snackbar tự đổi.
5. Thang alpha cố định thay cho 15 mức `Color.Black.copy(...)`, `SurfaceElevated.copy(...)` rải rác: `Scrim` (đen 60%), `Veil` (40/60/78%), `GlassFill` (50%), `Hairline` (6%), `Subtle` (10%), `Stroke` (16%), `Selected` (24%).

Cách làm an toàn: **đổi tên theo từng nhóm token, mỗi nhóm một commit**, vì có ~40 file chạm vào. Có thể dùng IDE "Rename" hoặc `sed` theo cụm từ, rồi build.

### 3.2 Cài đặt theme

1. Thêm vào `SettingsPreferences.kt` một enum `ThemeMode { LIGHT, DARK, SYSTEM }`, **mặc định `LIGHT`**.
2. `MainActivity` đọc cài đặt, tính `darkTheme` (`DARK` → true, `LIGHT` → false, `SYSTEM` → `isSystemInDarkTheme()`) và truyền cho `OneMusicTheme`.
3. Thêm mục chọn 3 chế độ vào `ui/screens/settings/SettingsDisplaySection.kt`.
4. Trong `OneMusicTheme`, đặt `isAppearanceLightStatusBars` và `isAppearanceLightNavigationBars` = `!darkTheme` để biểu tượng thanh hệ thống đọc được.
5. Người dùng đã cài app từ trước sẽ chưa có giá trị lưu: lần đầu sau cập nhật họ sẽ thấy theme **sáng** (theo mặc định mới), khác với giao diện tối hiện tại. Nên ghi vào ghi chú phát hành.

### 3.3 Tài nguyên XML

- `res/values/colors.xml` giữ cho theme sáng; thêm `res/values-night/colors.xml` cho theme tối. Widget (`widget_onemusic_*.xml`) và drawable đã dùng `@color/...` nên tự đổi theo.
- `res/values/themes.xml`: đổi cha từ `Material.Light` thành `Theme.Material3.DayNight.NoActionBar` (hoặc tạo `values-night/themes.xml`), bỏ cứng `android:windowLightStatusBar=false`.
- `Theme.OneMusic.Starting` (splash): nền theo `@color/background`.
- Icon launcher (`ic_launcher_background.xml`): kiểm tra còn hợp với nền mới.

### 3.4 Now Playing (giữ nguyên, cô lập khỏi theme)

Không sửa `NowPlayingBackdrop.kt`, `PaletteHelper.kt`, nút điều khiển, thanh tua hay voan. Chỉ cần đảm bảo chúng **không bị theme sáng làm đổi màu**, vì các token `PrimaryIvory`, `TextPrimary`, `Ivory*`, `SurfaceElevated`… sẽ trở thành màu theo theme.

Cách làm: bọc toàn bộ `NowPlayingSheet` (và các hộp thoại mở trong đó, vì `Dialog` thừa hưởng CompositionLocal) bằng:

```kotlin
CompositionLocalProvider(LocalAppColors provides DarkAppColors) {
    MaterialTheme(colorScheme = DarkMaterialColorScheme, typography = Typography) {
        NowPlayingSheet(...)
    }
}
```

Đặt ở chỗ gọi `NowPlayingSheet` trong `MainActivity`/`AppOverlays.kt`. Khi đó mọi token bên trong nhận giá trị **tối** như hiện tại, kể cả `ApexSlider`, `ApexDialogContainer`, `ApexTrackActionSheet`, `TrackDetailsDialog`, `ApexEqualizerDialog`.

Việc cần làm thêm quanh Now Playing:
1. **Thanh hệ thống:** khi sheet mở ở theme sáng, đặt icon thanh trạng thái/điều hướng thành **sáng** (đọc được trên nền tối), và trả lại khi đóng sheet. Làm bằng `SideEffect`/`DisposableEffect` theo trạng thái mở sheet.
2. **`NowBar` (mini player)** nằm ngoài Now Playing nên **theo theme** như các thành phần khác.
3. **Accent xanh vs xanh lá:** trong Now Playing, `ApexSlider` mặc định dùng `Brand`. Vì `Brand` sẽ đổi sang xanh dương, các thanh trượt trong EQ/hộp thoại bên trong Now Playing sẽ đổi từ xanh lá sang xanh dương theo bảng màu mới (đây là chủ ý, đồng bộ màu thương hiệu). Thanh tua chính vẫn màu ngà như cũ.
4. **Kiểm tra lại các sửa đổi kính mờ** đã commit trước (alpha 0.50, `hazeState`, pager trong nguồn haze): chúng vẫn nằm trong vùng bọc ở trên nên không bị ảnh hưởng.

### 3.5 Kính mờ (Haze) và hộp thoại

- Nền kính `apexFrostedGlass` / `ApexDialogContainer` ở phần còn lại của app: dùng `surface1` với alpha 50% (đã đổi trong commit trước); tint theo theme (đen ở tối, trắng ở sáng). Trong Now Playing vẫn là bản tối nhờ vùng bọc ở mục 3.4.
- Thanh điều hướng dưới, NowBar, toast: cùng quy tắc.
- Bóng đổ (`ShadowColor`): ở theme sáng dùng đen alpha thấp (≈ 18%) thay vì 55%.

## 4. Các màn cần rà từng cái

Với mỗi màn: tìm hằng số màu cũ, đổi sang token mới, kiểm tra cả hai theme.

| Nhóm | File chính |
|---|---|
| Khung & điều hướng | `ui/main/*`, `ui/navigation/ApexBottomNavigation.kt`, `ui/components/NowBar.kt` |
| Trang chủ | `ui/screens/home/*` (19 file) |
| Thư viện | `ui/screens/library/*` |
| Tìm kiếm | `ui/screens/search/*` |
| Chi tiết album/nghệ sĩ/playlist | `ui/screens/detail/*` |
| Thư mục & dedup | `ui/screens/folder/*`, `ui/screens/dedup/*` |
| Cài đặt | `ui/screens/settings/*` |
| Now Playing | `ui/screens/player/**` |
| Thành phần dùng chung | `ui/components/*` (`ApexSlider`, `ApexToggle`, `ApexPill`, `ApexCard`, các Dialog…) |
| Widget và splash | `res/layout/widget_*`, `res/drawable/ic_widget_*`, `res/values*/` |

## 5. Thứ tự thực hiện

Mỗi bước là một commit, build được và xem được trước khi sang bước sau.

1. **Tạo hạ tầng theme** (`AppColors`, `LocalAppColors`, `ColorScheme` sáng/tối, cài đặt theme, `MainActivity`). Chưa đổi màu: bảng tối giữ nguyên, bảng sáng tạm giống tối. Kiểm tra app vẫn chạy y như cũ.
2. **Đổi tên token** theo nhóm (chữ, nền, nhấn, trong suốt). Không đổi mã màu. Build sạch sau mỗi nhóm.
3. **Áp bảng màu mới** cho theme tối (mục 2.1): nền đen, bề mặt xám trung tính, nhấn xanh dương.
4. **Áp bảng màu theme sáng** (mặc định) và kiểm tra từng màn ở hai theme.
5. **Cô lập Now Playing** (mục 3.4): bọc `DarkAppColors`, xử lý icon thanh hệ thống. Không đổi giao diện bên trong.
6. **Kính mờ & hộp thoại** (mục 3.5).
7. **XML, widget, splash, thanh hệ thống** (mục 3.3, 3.2 bước 4).
8. **Dọn dẹp**: xoá `DynamicMeshBackground` và `ApexIndigo` (code chết, không ai gọi), token chết, `VinylGray` hex cứng (chuyển vào `Color.kt`); **giữ** `PaletteHelper`, `ApexCyan` cho huy hiệu và `HiResGoldGradient`; cập nhật chú thích đầu `Color.kt` và `GEMINI.md`/`CONTRIBUTING.md` nếu còn nhắc "Warm Ivory".
9. **Quét kiểm**: chạy lại lệnh tìm `Color(0x`, `Color.White`, `Color.Black`, `colors.xml` và đối chiếu bảng màu; chụp từng màn ở cả hai theme.

## 6. Kiểm tra khi hoàn tất

- [ ] Build `assembleDebug` không lỗi, không cảnh báo mới; `checkSizeLimits` vẫn qua.
- [ ] Không còn `Color(0x…)` ngoài `theme/`, không còn `Color.White`/`Color.Black` làm nền chữ; `Color.Black` chỉ còn trong mặt nạ `DstIn` (hoặc đã thay bằng token).
- [ ] Mọi màn hình ở cả theme sáng và tối: chữ đọc được, tương phản đạt bảng 2.2.
- [ ] Thanh trạng thái và thanh điều hướng đúng màu ở cả hai theme.
- [ ] Nút xanh đặc luôn dùng chữ trắng và `#0066D6`.
- [ ] Huy hiệu Hi-Res vẫn vàng gradient như cũ ở mọi nơi hiển thị.
- [ ] Now Playing **không đổi** so với trước ở cả theme sáng và tối (so sánh ảnh chụp trước/sau), kể cả hộp thoại mở trong đó.
- [ ] Mở/đóng Now Playing ở theme sáng: icon thanh trạng thái đổi sáng khi mở và trả về tối khi đóng.
- [ ] Cài đặt theme: mặc định Sáng; chuyển Tối và Theo hệ thống có hiệu lực ngay, giữ sau khi khởi động lại.
- [ ] Widget 4x1 và 4x2 đổi đúng theo chế độ sáng/tối của hệ thống.
- [ ] Hộp thoại (EQ, tốc độ, hẹn giờ, thông tin bài) nhìn được trên cả hai theme.
- [ ] Chạy thử ít nhất 1 máy Android 12+ và 1 giả lập Android 10/11.

## 7. Rủi ro và điều cần lưu ý

- **Khối lượng lớn**: khoảng 40 file Compose cộng tài nguyên XML. Đổi tên token theo từng nhóm và build liên tục để tránh vỡ hàng loạt.
- **Now Playing tối trên app sáng**: khi mở sheet ở theme sáng, màn hình chuyển hẳn sang tối. Đó là chủ ý, nhưng cần làm mượt chuyển cảnh (sheet trượt lên che toàn màn hình nên thường chấp nhận được) và xử lý icon thanh hệ thống như mục 3.4.
- **Hộp thoại ở Now Playing bị bọc `DarkAppColors`**: hộp thoại mở từ ngoài Now Playing (ví dụ từ danh sách bài) theo theme; hộp thoại mở từ trong Now Playing luôn tối. Hai kiểu này sẽ khác nhau ở theme sáng; chủ ý.
- **Người dùng cũ**: theme mặc định đổi từ tối sang sáng, nên bản cập nhật sẽ làm giao diện thay đổi đột ngột với người đã quen. Cân nhắc giữ Tối cho người dùng đang có dữ liệu cài đặt cũ, chỉ áp Sáng cho cài mới (nếu bạn muốn).
- **Haze trong `Dialog`** (cửa sổ riêng) có thể không lấy mẫu đúng; nếu hộp thoại trong ra ngoài ý muốn thì dùng nền `surface1` đặc làm phương án dự phòng.
- **Ảnh bìa ở màn Chi tiết** (`DetailHero.kt` có logic đổi trắng/đen theo ảnh) cần kiểm tra lại với nền trắng của theme sáng.

## 8. Tiến độ

- **Bước 1 (xong):** hạ tầng theme, `ThemeMode`, `NowPlayingThemeScope`.
- **Bước 2 (xong, chưa build được trong môi trường viết code):** đổi các token **nền, chữ, viền, nhấn** sang `AppTheme.colors.*` ở 61 file ngoài `theme/` và `ui/screens/player/**`, 503 chỗ, không đổi mã màu. Ánh xạ: `TextPrimary`/`PrimaryIvory`→`textPrimary`, `TextSecondary`/`IvoryBody`→`textSecondary`, `TextTertiary`/`MutedIvory`→`textTertiary`, `TextDisabled`→`textDisabled`, `CharcoalBlack`→`onInverse`, `SurfaceElevated`→`surface1`, `SurfaceCard`→`surface2`, `SurfaceBase`/`SurfaceControl`/`SurfaceActive`/`SurfaceActiveIndicator`/`ActivePillBg`→tên tương ứng, `SurfaceDivider`→`divider`, `SurfaceBorderStrong`→`borderStrong`, `Brand`/`BrandLight`/`BrandDark`→`accent`/`accentLight`/`accentDark`.
  - Chỉ đổi những chỗ nằm trong thân hàm `@Composable` (hoặc lambda nội dung của Box/Column/Row/items…). Chỗ nằm trong `remember {}`, `drawBehind {}`, `Canvas {}`, giá trị mặc định tham số, hằng số cấp cao nhất… **giữ nguyên** vì `AppTheme.colors` chỉ gọi được trong ngữ cảnh composable.
  - **Chưa đổi:** `ObsidianBlack`, `ScrimColor`, `ShadowColor` và các token `Ivory*` trong suốt (cần xét từng chỗ vì có khi là lớp đen/trắng phủ lên ảnh bìa, không phải màu theo theme).
- **Việc cần xem lại ở bước 4 (cặp màu đảo):** `PrimaryIvory` đang đóng hai vai trò, vừa là **màu chữ** (→`textPrimary`) vừa là **nền pill đang chọn / nút ngà** đi với chữ `CharcoalBlack` (→`onInverse`). Ở theme sáng, `textPrimary` thành đen thì nền pill đen với chữ `onInverse` phải là trắng: `LightAppColors.onInverse` = trắng. Còn chữ trắng trên nền `accent` (nút xanh) hiện cũng đi qua `textPrimary`; cần tách thành token `onAccent` riêng khi làm bảng sáng.
- **Bước 3 (xong, chưa build được trong môi trường viết code):** áp bảng màu tối mới cho toàn app **trừ Now Playing**.
  - `DarkAppColors` đổi sang bảng mới (nền `#000000`, bề mặt `#1C1C1E`/`#2C2C2E`, chữ `#FFFFFF`/`#A1A1A6`/`#8E8E93`, viền `#38383A`, **nhấn xanh dương `#0A84FF`**, nút đặc `accentDark` `#0066D6`). Khác bảng ở mục 2.1 một chút: chữ phụ `#A1A1A6` (thay cho ≈`#98989F`) và chữ vô hiệu `#636366`.
  - `LegacyDarkAppColors` giữ nguyên bảng cũ (ngà ấm, xanh lá) và `NowPlayingThemeScope` dùng bảng này, nên Now Playing và hộp thoại mở trong đó **không đổi** (thanh trượt EQ trong Now Playing vẫn xanh lá).
  - Material `ColorScheme` và `Typography` dựng theo `AppColors` (`materialColorSchemeFor`, `typographyFor`); Now Playing dùng bản cũ.
  - Các tham số mặc định từng dùng token cũ (`ApexSlider`, `ApexCard`, `ApexDropdownMenuItem`, `ApexTrackActionSheet`, `ApplePlaybackIcons`, `ApexCircularGlassButton`, `SettingsActionRow`, `ApexDialogContainer`, `apexFrostedGlass`, `apexGlassCard`, `apexGroupedCardItem`) đổi sang `Color.Unspecified` và lấy màu theo theme tại chỗ dùng.
  - Chưa đổi (bước sau): `ObsidianBlack`, `Ivory*` trong suốt, `ApexReflectiveBorderBrush`/`ApexPillBorderBrush`, `PaletteHelper`, XML (`colors.xml`, `themes.xml`, widget, splash).
- **Bước 4 (xong, chưa build được trong môi trường viết code):** bảng màu **theme sáng** thật và các sửa theo cặp màu đảo.
  - `LightAppColors`: nền `#FFFFFF`, bề mặt `#F2F2F7`/`#E5E5EA`, chữ `#000000`/`#3C3C43`/`#737377`, nhấn `#0066D6`, lỗi `#D70015`, cảnh báo `#B26A00`, bóng đen 18%. `isDark = false` nên icon thanh hệ thống tự đổi. `materialColorSchemeFor` trả `lightColorScheme` khi `isDark = false`.
  - `AppColors` thêm `onAccent` (chữ trắng trên nền xanh/đỏ), `danger`, `warning` và thang trong suốt `hairline/subtle/stroke/muted/disabled/faint/medium/high` (thay `Ivory*`).
  - Đổi `ObsidianBlack`→`background`, `ApexRose`→`danger`, `ApexAmber`→`warning`, `ShadowColor`→`shadow`, `Ivory*`→thang trong suốt ở thêm 110 chỗ (ngữ cảnh composable).
  - `OnImageScope`: vùng nằm trên ảnh/gradient tối luôn dùng chữ trắng (banner nổi bật ở Trang chủ, ô chọn nhiều bài trên ảnh bìa lưới). Thanh trên màn Chi tiết dùng màu icon cố định theo độ sáng ảnh khi nằm trên ảnh. Huy hiệu Hi-Res giữ chữ tối trên nền vàng.
  - Đổi `ApexCyan`→`accent` ở thanh chữ cái. Hero màn Chi tiết mờ dần vào `background` của theme.
  - **Còn lại:** XML (`colors.xml`, `values-night`, `themes.xml`, widget, splash) thuộc bước 7; `DynamicMeshBackground` (mã chết) và `PaletteHelper` giữ nguyên.
- **Bước 5 (xong, chưa build được trong môi trường viết code):** cô lập Now Playing khỏi theme.
  - Phần lớn đã làm ở bước 1 và 3 (`NowPlayingThemeScope` bọc sheet với `LegacyDarkAppColors`, Material scheme và Typography bản cũ). Bước này bổ sung:
  - `ThemeOverrides.nowPlayingOpenCount`: `NowPlayingThemeScope` tăng/giảm bộ đếm; `OneMusicTheme` đọc nó trong composition và đặt `isAppearanceLightStatusBars/NavigationBars = false` khi > 0. Cách này giữ icon thanh hệ thống sáng cả khi theme hệ thống đổi lúc sheet đang mở (cách cũ chỉ đặt một lần lúc mở).
  - Hộp thoại "Thêm vào playlist" nằm ngoài sheet (trong `AppOverlays`): khi mở lúc `isPlayerExpanded` thì bọc `NowPlayingThemeScope` để giữ giao diện tối của Now Playing, không hiện hộp thoại trắng trên nền tối.
  - Đã rà `ui/screens/player/**`: không file nào thuộc nhóm đã chuyển sang `AppTheme.colors` ngoài phạm vi scope; mọi hằng số cũ ở đó giữ nguyên giá trị.
- **Bước 7 (xong, chưa build được trong môi trường viết code):** tài nguyên XML.
  - `res/values/colors.xml` là bảng **sáng**, thêm `res/values-night/colors.xml` là bảng **tối** (cùng tên màu nên layout/drawable widget không phải sửa). Thêm màu `background`. `apex_cyan` sáng đậm hơn (`#00728F`) để đọc được trên nền trắng; `apex_rose` sáng `#D70015`. `brand` thành xanh dương (`#0066D6` sáng, `#0A84FF` tối).
  - `themes.xml`: `Theme.OneMusic` sáng cha `Material.Light.NoActionBar`; `values-night/themes.xml` cha `Material.NoActionBar`; nền cửa sổ lấy `@color/background`, `windowLightStatusBar/NavigationBar` theo từng chế độ. Màn hình khởi động và `ic_launcher_background` giữ **nền đen** vì logo được thiết kế trên nền đen.
  - Widget và XML chỉ theo chế độ sáng/tối của **hệ thống**, không theo cài đặt "Chế độ giao diện" của app (RemoteViews không đọc được cài đặt này). `OneMusicTheme` đặt lại nền cửa sổ theo theme trong app khi chạy.
- **Bước 6 (xong, chưa build được trong môi trường viết code):** kính mờ và hộp thoại theo theme.
  - Nền kính (`apexFrostedGlass`, `ApexDialogContainer`, menu, NowBar, thanh điều hướng, bảng thao tác) đã lấy `surface1` theo theme từ bước 3–4; bước này bổ sung viền và lớp phủ.
  - Viền phản chiếu `ApexReflectiveBorderBrush` / `ApexPillBorderBrush` (trắng ngà cố định, mất hút trên nền trắng) thay bằng `AppTheme.colors.reflectiveBorderBrush` / `pillBorderBrush` (màu chữ chính trong suốt, theme sáng đậm hơn một chút) ở 7 chỗ. Trong Now Playing vẫn ra đúng giá trị cũ vì dùng bảng cũ.
  - Lớp phủ sau bảng thao tác bài (`ModalBottomSheet`) dùng `AppTheme.colors.scrim`: đen 40% ở theme sáng, giữ 60% ở theme tối và trong Now Playing.
  - Hộp thoại và `Popup` là cửa sổ riêng, nhưng nhận được `AppTheme.colors` vì CompositionLocal đi theo composition. Việc Haze lấy mẫu qua cửa sổ khác vẫn chưa được kiểm chứng trên máy thật.
- **Bước 8 (xong, chưa build được trong môi trường viết code):** dọn dẹp.
  - Xoá `ui/components/DynamicMeshBackground.kt` (không ai gọi) cùng token `ApexIndigo`; xoá token không còn dùng `ApexGlassSurfaceBg`, `ApexButtonGlassBg`.
  - `VinylGray` (hex cứng trong `AlbumGridCard.kt`) chuyển thành `VinylDiscGray` trong `theme/Color.kt`.
  - Viết lại chú thích đầu `Color.kt` (các hằng số ngà ấm chỉ còn là nguồn của `LegacyDarkAppColors`) và cập nhật `GEMINI.md`.
  - **Giữ lại có chủ ý:** `PaletteHelper.kt` và nền động của Now Playing, `ApexCyan` (dùng ở Material `tertiary`), `HiResGoldGradient`, các hằng số cũ trong `Color.kt` (nguồn của `LegacyDarkAppColors`).
  - **Không đụng:** `.agents/rules/oneui_design_guidelines.md` mô tả bảng màu Samsung Blue/Galaxy Violet từ trước, đã lỗi thời so với app; cần chủ dự án quyết định sửa hay xoá.
