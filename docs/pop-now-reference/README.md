# POP NOW UI Reference — POP MART

> **Source URL**: https://www.popmart.com/us/pop-now/list  
> **Series URL**: https://www.popmart.com/us/pop-now/set/1427  
> **Capture date**: 2026-09-27  
> **Viewport**: 1536×768 (desktop browser)  
> **Mobile viewport**: ❌ Không capture được (môi trường agent không hỗ trợ responsive resize)  
> **Status legend**: ✅ Quan sát thấy | ❌ Không có | 🔒 Yêu cầu login/purchase | ❓ Không xác định

---

## Flow tổng quan (quan sát thực tế)

```
[01] POP NOW List (/us/pop-now/list)
        │  click card series
        ▼
[02] Series Detail (/us/pop-now/set/{id})
        │  click "Pick One to Shake"
        ▼
[02.5] Login Gate Modal  ← PHÁT HIỆN MỚI (không có trong dự đoán ban đầu)
        │  đăng nhập thành công
        ▼
[03] Box Grid               🔒 (cần login thật)
        │
        ▼
[04] Box Hover/Focus        🔒
        │
        ▼
[05] Box Selected           🔒
        │
        ▼
[06] Reservation + Countdown 5 phút  🔒
        │
        ▼
[07] Checkout / Payment     🔒
        │
        ├── [08] Payment Success    🔒
        └── [08b] Payment Failed/Expired 🔒
        │
        ▼
[09] Ready to Unbox         🔒
        │
        ▼
[10] Pre-Unbox              🔒
        │
        ▼
[11] Shake & Guess / Hints  🔒 (series-specific)
        │
        ▼
[12] Unbox Animation        🔒
        │
        ▼
[13] Reveal                 🔒
        │
        ▼
[14] Result Detail          🔒
        │
        ▼
[15] Virtual Cabinet        🔒
        │
        ▼
[16] Ship Now               🔒
```

---

## 01 — POP NOW Listing (`/us/pop-now/list`)

**Status**: ✅ Captured

### Layout & Components
- **Grid**: 5 cột, product cards, không có sidebar
- **Filter bar** (top):
  - Checkbox "Local Express" (lọc giao nhanh)
  - Dropdown "Character" (lọc theo IP/nhân vật)
- **Banner** header mỏng phía trên grid

### Product Card Structure
```
┌──────────────────┐
│  [ảnh series]    │  chiếm ~70% height card, hình ảnh chụp stack box
│  [NEW] badge     │  góc trên trái, nền đen, text trắng, font nhỏ
├──────────────────┤
│  Tên series      │  2 dòng max, font regular
│  $19.99          │  giá, font nhỏ, không highlight
└──────────────────┘
```

### Typography & Color (quan sát)
| Thành phần | Style |
|------------|-------|
| Badge "NEW" | Nền đen `#000`, text trắng, uppercase, font ~10px |
| Tên series | `#1a1a1a`, regular weight, ~14px |
| Giá | `#1a1a1a`, regular, ~14px (KHÔNG đỏ ở listing) |
| Background card | Trắng hoặc xám nhạt `#f5f5f5` |

### Popworld Design Targets
- [ ] Grid 4–5 cột responsive
- [ ] Badge "MỚI" trên series mới
- [ ] Giá rõ ràng trên card
- [ ] Filter "Local Express" (có thể bỏ vì Popworld 1 kho)
- [ ] Filter theo nhân vật/IP

---

## 02 — Series Detail (`/us/pop-now/set/{id}`)

**Status**: ✅ Captured (VALORANT Classic Agents Series Figures — $19.99)

### Progress Stepper (quan trọng — luôn visible)
```
[🎁 Pick A Box] ──→ [🎲 Shake for Hints] ──→ [📦 Unbox Now]
    ACTIVE                 inactive                inactive
```
- Hiển thị cố định ngay dưới global navbar
- Step active: icon + text đậm hơn
- Mũi tên `──→` giữa các steps

### Layout (2 cột — desktop)
```
┌──────────────────────────┬──────────────────────────────┐
│  [Ảnh box stack]         │  Tên series (font lớn, đậm)  │
│  ← → (image carousel)   │  $19.99 (màu đỏ #e60012)     │
│                          │  Estimated Ship: Nov 05, 2026 │
│  "No.6kpsYR" (set code)  │  Free Shipping Over $49      │
│                          │  🚚 Local Express (3-5 days)  │
│                          │  ──────────────────────────── │
│                          │  [Pick One to Shake]  ← PRIMARY (nền đen, full-width) │
│                          │  [Buy Multiple Boxes] ← SECONDARY (border only) │
│                          │  "No duplicates if picking from SAME SET" │
│                          │  How to Play (text link)     │
└──────────────────────────┴──────────────────────────────┘
```

### Dưới fold (below the fold)
1. **Character strip** — scroll ngang, hình nhân vật trong series
2. **DETAIL INFORMATION** accordion:
   - Brand, Release Date, Size (7–9cm), Material (PVC/ABS), Age 15+
3. **POP NOW Purchase Notice** accordion
4. **Brand video** — full-width embed (thumbnail + play button)
5. **Marketing images** — high-res product shots

### Set Code
- Format: `No.` + 6 ký tự alphanumeric (e.g., `No.6kpsYR`)
- Hiển thị dưới ảnh chính của box
- Mỗi batch/restock có code khác nhau

### Typography & Color
| Thành phần | Style |
|------------|-------|
| Tên series | Font đậm, ~22px, `#1a1a1a` |
| Giá | `#e60012` (đỏ POP MART), ~24px, đậm |
| CTA Primary | Nền `#1a1a1a`, text trắng, full-width, ~48px height |
| CTA Secondary | Border `#1a1a1a`, nền trắng, full-width, ~48px height |
| Stepper active | Bold, icon sáng hơn |
| Shipping info | Text xanh lá `#00a651` (Local Express badge) |

### Popworld Design Targets
- [ ] Progress Stepper cố định (Pick → Shake → Unbox)
- [ ] Set Code / batch code hiển thị
- [ ] "No duplicates" badge/notice
- [ ] 2 CTA rõ ràng (primary + secondary)
- [ ] Character preview strip (horizontal scroll)
- [ ] "Hướng Dẫn Chơi" link
- [ ] Detail accordion (thông tin sản phẩm)
- [ ] POP NOW Notice accordion
- [ ] Video embed support (`series.videoUrl`)
- [ ] Giá màu đỏ trên series detail (khác với listing)

---

## 02.5 — Login Gate Modal (PHÁT HIỆN MỚI)

**Status**: ✅ Captured  
**Trigger**: Click "Pick One to Shake" khi chưa đăng nhập

### Modal Structure
```
┌────────────────────────────────┐
│  ×  (close button)             │
│                                │
│     SIGN IN OR REGISTER        │
│                                │
│  🇺🇸 United States ▼           │  ← Country picker
│  [👤 Enter email or phone]     │
│  [      CONTINUE      ]        │  ← nền đen
│                                │
│  ─────── Join With ───────     │
│      [G Google] [🍎 Apple]     │
│                                │
│  ☐ Platform's Terms & Privacy  │
│  ☐ Birth month confirmation    │
│                                │
│  [POP POINTS][BIRTHDAY][COUPON]│  ← Benefits row
└────────────────────────────────┘
```

### Popworld Design Targets
- [ ] Redirect to login page khi click "Chọn Hộp" chưa auth
- [ ] After login → redirect về series page, tự động tiếp tục flow
- [ ] Optional: hiển thị benefits của tài khoản trong login prompt

---

## 03–16 — States yêu cầu login/purchase

| # | State | Status | Ghi chú |
|---|-------|--------|---------|
| 03 | Box Grid | 🔒 | Cần login |
| 04 | Box Hover/Focus | 🔒 | Cần login |
| 05 | Box Selected | 🔒 | Cần login |
| 06 | Reservation + Countdown | 🔒 | 5 phút timer |
| 07 | Checkout | 🔒 | |
| 08 | Payment Success | 🔒 | |
| 08b | Payment Failed/Expired | 🔒 | |
| 09 | Ready to Unbox | 🔒 | |
| 10 | Pre-Unbox | 🔒 | |
| 11 | Shake & Hints | 🔒 | Series-specific |
| 12 | Unbox Animation | 🔒 | |
| 13 | Reveal | 🔒 | |
| 14 | Result Detail | 🔒 | |
| 15 | Virtual Cabinet | 🔒 | |
| 16 | Ship Now | 🔒 | |

> Toàn bộ các state từ 03 trở đi cần tài khoản POP MART thật + thanh toán thật.  
> Popworld tự thiết kế dựa trên **backend state machine đã implement**.

---

## Những phát hiện ngoài dự đoán

| # | Phát hiện | Ý nghĩa với Popworld |
|---|-----------|---------------------|
| 1 | Login Gate Modal (State 02.5) | POP NOW yêu cầu auth trước khi vào box grid |
| 2 | Progress Stepper cố định dưới navbar | UI luôn cho user biết họ đang ở step nào |
| 3 | Set Code (`No.6kpsYR`) | Mỗi batch/restock có identifier riêng |
| 4 | 2 luồng tách biệt | "Pick One" (POP NOW) vs "Buy Multiple" (thường) |
| 5 | "No duplicates" guarantee | Backend phải đảm bảo không trùng item trong set |
| 6 | Brand video section | Series có thể có video marketing embed |
| 7 | Country selector trong login | Không cần cho Popworld (1 quốc gia) |
| 8 | Benefits preview trong login | Khuyến khích đăng ký bằng cách show benefits |

---

## UX Lessons cho Popworld

### Nên học
- ✅ Progress Stepper luôn visible — user không bị lạc trong flow
- ✅ 2 CTA rõ ràng với visual hierarchy (primary vs secondary)
- ✅ "No duplicates" notice — tăng trust
- ✅ Character strip — tạo desire trước khi chọn hộp
- ✅ Set Code — transparency về batch cụ thể
- ✅ Login gate nhẹ nhàng (modal, không redirect trang)

### Không nên copy nguyên xi
- ❌ Brand assets, logo, màu sắc chính thức của POP MART
- ❌ Character images thuộc IP bản quyền
- ❌ Nội dung marketing copy verbatim
- ❌ Country selector (Popworld không cần)

---

## Screenshot Checklist (để capture thủ công nếu cần)

Những state chưa capture được, cần login thật:

```
[ ] 03-box-grid-available.png       — Grid các box, trạng thái AVAILABLE
[ ] 04-box-hover.png                — Hover/focus 1 box
[ ] 05-box-selected.png             — Box đã chọn, confirm CTA xuất hiện
[ ] 06-reservation-countdown.png    — Timer 5 phút đang chạy
[ ] 07-checkout.png                 — Trang thanh toán
[ ] 08-payment-success.png          — Sau khi thanh toán
[ ] 09-ready-to-unbox.png           — CTA "Unbox Now"
[ ] 10-pre-unbox.png                — Màn hình trước animation
[ ] 11-shake-hints.png              — Shake & Guess (nếu series hỗ trợ)
[ ] 12-unbox-animation.png          — Quá trình mở hộp
[ ] 13-reveal.png                   — Character được reveal
[ ] 14-result-detail.png            — Tên, rarity, thông tin item
[ ] 15-virtual-cabinet.png          — Item trong Cabinet
[ ] 16-ship-now.png                 — Ship Now flow
```

---

*Tài liệu này được tạo tự động từ browser capture — 2026-09-27. Không có code production nào bị thay đổi.*
