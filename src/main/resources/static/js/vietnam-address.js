/**
 * PopWorld - Bộ chọn Hành chính Việt Nam 2 Cấp Chuẩn Quốc Gia
 * Nguồn dữ liệu: Bản đồ tra cứu Đơn vị Hành chính Việt Nam (sapnhap.bando.com.vn)
 * Căn cứ: Nghị quyết số 202/2025/QH15 & Nghị quyết số 60-NQ/TW
 * Cơ cấu: 34 Đơn vị hành chính cấp Tỉnh (09 TP trực thuộc TW, 25 Tỉnh) và 3.321 ĐVHC cấp Xã.
 */
(function() {
    'use strict';

    const CACHE_KEY = 'popworld_vn_provinces_cache_34_v1';
    const DATA_URL = '/data/vietnam_administrative_34.json';

    // Danh sách 34 Tỉnh/Thành phố chuẩn quốc gia sau sắp xếp (Dự phòng offline tức thì)
    const FALLBACK_34_PROVINCES = [
        { code: "01", name: "Thủ Đô Hà Nội", mergedFrom: "giữ nguyên" },
        { code: "04", name: "Tỉnh Cao Bằng", mergedFrom: "giữ nguyên" },
        { code: "08", name: "Tỉnh Tuyên Quang", mergedFrom: "tỉnh Hà Giang và tỉnh Tuyên Quang" },
        { code: "11", name: "Tỉnh Điện Biên", mergedFrom: "giữ nguyên" },
        { code: "12", name: "Tỉnh Lai Châu", mergedFrom: "giữ nguyên" },
        { code: "14", name: "Tỉnh Sơn La", mergedFrom: "giữ nguyên" },
        { code: "15", name: "Tỉnh Lào Cai", mergedFrom: "tỉnh Yên Bái và tỉnh Lào Cai" },
        { code: "19", name: "Tỉnh Thái Nguyên", mergedFrom: "tỉnh Bắc Kạn và tỉnh Thái Nguyên" },
        { code: "20", name: "Tỉnh Lạng Sơn", mergedFrom: "giữ nguyên" },
        { code: "22", name: "Thành phố Quảng Ninh", mergedFrom: "giữ nguyên" },
        { code: "24", name: "Thành phố Bắc Ninh", mergedFrom: "tỉnh Bắc Giang và tỉnh Bắc Ninh" },
        { code: "25", name: "Tỉnh Phú Thọ", mergedFrom: "tỉnh Vĩnh Phúc, tỉnh Hòa Bình và tỉnh Phú Thọ" },
        { code: "31", name: "Thành Phố Hải Phòng", mergedFrom: "thành phố Hải Phòng và tỉnh Hải Dương" },
        { code: "33", name: "Tỉnh Hưng Yên", mergedFrom: "tỉnh Thái Bình và tỉnh Hưng Yên" },
        { code: "37", name: "Tỉnh Ninh Bình", mergedFrom: "tỉnh Hà Nam, tỉnh Nam Định và tỉnh Ninh Bình" },
        { code: "38", name: "Tỉnh Thanh Hóa", mergedFrom: "giữ nguyên" },
        { code: "40", name: "Tỉnh Nghệ An", mergedFrom: "giữ nguyên" },
        { code: "42", name: "Tỉnh Hà Tĩnh", mergedFrom: "giữ nguyên" },
        { code: "44", name: "Tỉnh Quảng Trị", mergedFrom: "tỉnh Quảng Bình và tỉnh Quảng Trị" },
        { code: "46", name: "Thành Phố Huế", mergedFrom: "giữ nguyên" },
        { code: "48", name: "Thành Phố Đà Nẵng", mergedFrom: "thành phố Đà Nẵng và tỉnh Quảng Nam" },
        { code: "51", name: "Tỉnh Quảng Ngãi", mergedFrom: "tỉnh Kon Tum và tỉnh Quảng Ngãi" },
        { code: "52", name: "Tỉnh Gia Lai", mergedFrom: "tỉnh Bình Định và tỉnh Gia Lai" },
        { code: "56", name: "Tỉnh Khánh Hòa", mergedFrom: "tỉnh Ninh Thuận và tỉnh Khánh Hòa" },
        { code: "66", name: "Tỉnh Đắk Lắk", mergedFrom: "tỉnh Phú Yên và tỉnh Đắk Lắk" },
        { code: "68", name: "Tỉnh Lâm Đồng", mergedFrom: "tỉnh Đắk Nông, tỉnh Bình Thuận và tỉnh Lâm Đồng" },
        { code: "75", name: "Thành phố Đồng Nai", mergedFrom: "tỉnh Bình Phước và tỉnh Đồng Nai" },
        { code: "79", name: "Thành Phố Hồ Chí Minh", mergedFrom: "TPHCM, tỉnh Bà Rịa - Vũng Tàu và tỉnh Bình Dương" },
        { code: "80", name: "Tỉnh Tây Ninh", mergedFrom: "tỉnh Long An và tỉnh Tây Ninh" },
        { code: "82", name: "Tỉnh Đồng Tháp", mergedFrom: "tỉnh Tiền Giang và tỉnh Đồng Tháp" },
        { code: "86", name: "Tỉnh Vĩnh Long", mergedFrom: "tỉnh Bến Tre, tỉnh Trà Vinh và tỉnh Vĩnh Long" },
        { code: "91", name: "Tỉnh An Giang", mergedFrom: "tỉnh Kiên Giang và tỉnh An Giang" },
        { code: "92", name: "Thành Phố Cần Thơ", mergedFrom: "thành phố Cần Thơ, tỉnh Sóc Trăng và tỉnh Hậu Giang" },
        { code: "96", name: "Tỉnh Cà Mau", mergedFrom: "tỉnh Bạc Liêu và tỉnh Cà Mau" }
    ];

    let cachedData = null;
    let loadingPromise = null;

    /**
     * Tải dữ liệu 34 Tỉnh/Thành phố và 3.321 Xã/Phường
     */
    async function loadAddressData() {
        if (cachedData && cachedData.length > 0) {
            return cachedData;
        }

        // 1. Kiểm tra cache trong localStorage
        try {
            const local = localStorage.getItem(CACHE_KEY);
            if (local) {
                const parsed = JSON.parse(local);
                if (Array.isArray(parsed) && parsed.length === 34) {
                    cachedData = parsed;
                    return cachedData;
                }
            }
        } catch (e) {
            console.warn('Lỗi đọc cache địa chỉ từ localStorage:', e);
        }

        // 2. Fetch file JSON dữ liệu nội bộ
        if (!loadingPromise) {
            loadingPromise = fetch(DATA_URL)
                .then(res => {
                    if (!res.ok) throw new Error('HTTP ' + res.status);
                    return res.json();
                })
                .then(data => {
                    if (Array.isArray(data) && data.length > 0) {
                        cachedData = data;
                        try {
                            localStorage.setItem(CACHE_KEY, JSON.stringify(data));
                        } catch (e) {
                            // Bỏ qua nếu localStorage đầy
                        }
                        return data;
                    }
                    throw new Error('Dữ liệu rỗng');
                })
                .catch(err => {
                    console.warn('Không thể tải file dữ liệu 34 tỉnh thành nội bộ, kích hoạt fallback:', err);
                    cachedData = FALLBACK_34_PROVINCES.map(p => ({
                        code: p.code,
                        name: p.name,
                        mergedFrom: p.mergedFrom,
                        wards: []
                    }));
                    return cachedData;
                })
                .finally(() => {
                    loadingPromise = null;
                });
        }

        return loadingPromise;
    }

    /**
     * Khớp thông minh Tỉnh/Thành phố (hỗ trợ cả tên mới, tên cũ trước sáp nhập)
     */
    function findProvinceMatch(data, query) {
        if (!query) return null;
        const q = query.trim().toLowerCase();
        const qClean = q.replace(/^(tỉnh|thành phố|thủ đô)\s+/i, '').trim();

        // 1. Khớp chính xác tên mới
        let match = data.find(p => p.name.toLowerCase() === q);
        if (match) return match;

        // 2. Khớp tên cốt lõi (bỏ tiền tố)
        match = data.find(p => {
            const pClean = p.name.toLowerCase().replace(/^(tỉnh|thành phố|thủ đô)\s+/i, '').trim();
            return pClean === qClean || p.name.toLowerCase().includes(qClean) || q.includes(pClean);
        });
        if (match) return match;

        // 3. Khớp thông qua nguồn gốc sáp nhập (mergedFrom)
        // Ví dụ: người dùng chọn "Hải Dương" -> khớp "Thành Phố Hải Phòng"
        // "Bắc Giang" -> khớp "Thành phố Bắc Ninh"
        // "Bình Dương" -> khớp "Thành Phố Hồ Chí Minh"
        match = data.find(p => {
            if (!p.mergedFrom) return false;
            const mf = p.mergedFrom.toLowerCase();
            return mf.includes(q) || mf.includes(qClean);
        });
        return match || null;
    }

    /**
     * Khớp thông minh Phường/Xã
     */
    function findWardMatch(wards, query) {
        if (!query || !wards || wards.length === 0) return null;
        const q = query.trim().toLowerCase();
        const qClean = q.replace(/^(phường|xã|thị trấn|đặc khu)\s+/i, '').trim();

        // 1. Khớp chính xác tên
        let match = wards.find(w => w.name.toLowerCase() === q);
        if (match) return match;

        // 2. Khớp tên cốt lõi
        match = wards.find(w => {
            const wClean = w.name.toLowerCase().replace(/^(phường|xã|thị trấn|đặc khu)\s+/i, '').trim();
            return wClean === qClean || w.name.toLowerCase().includes(qClean) || q.includes(wClean);
        });
        if (match) return match;

        // 3. Khớp từ lịch sử sáp nhập của phường/xã
        match = wards.find(w => {
            if (!w.mergedFrom) return false;
            return w.mergedFrom.toLowerCase().includes(q) || w.mergedFrom.toLowerCase().includes(qClean);
        });
        return match || null;
    }

    /**
     * Khởi tạo Bộ chọn Hành chính Việt Nam 2 Cấp
     */
    async function initSelector(options) {
        const {
            provinceElId,
            wardElId,
            districtElId, // Thẻ input hidden tương thích ngược
            initialProvince = '',
            initialDistrict = '',
            initialWard = ''
        } = options;

        const provinceEl = document.getElementById(provinceElId);
        const wardEl = document.getElementById(wardElId);
        const districtEl = districtElId ? document.getElementById(districtElId) : null;

        if (!provinceEl || !wardEl) {
            return;
        }

        const data = await loadAddressData();

        // 1. Nạp danh sách 34 Tỉnh / Thành phố mới
        provinceEl.innerHTML = '<option value="">-- Chọn Tỉnh / Thành phố --</option>';
        data.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.name;
            opt.textContent = p.name;
            opt.dataset.code = p.code;
            if (p.mergedFrom && !p.mergedFrom.includes('giữ nguyên')) {
                opt.title = `Sáp nhập từ: ${p.mergedFrom}`;
            }
            provinceEl.appendChild(opt);
        });

        // Hàm nạp danh sách Phường / Xã trực thuộc
        function populateWards(selectedProvinceName, preselectedWard = '') {
            wardEl.innerHTML = '<option value="">-- Chọn Phường / Xã / Thị trấn --</option>';

            if (!selectedProvinceName) {
                wardEl.disabled = true;
                if (districtEl) districtEl.value = '';
                return;
            }

            const provinceObj = data.find(p => p.name === selectedProvinceName);
            if (!provinceObj || !provinceObj.wards || provinceObj.wards.length === 0) {
                wardEl.disabled = true;
                return;
            }

            wardEl.disabled = false;
            provinceObj.wards.forEach(w => {
                const opt = document.createElement('option');
                opt.value = w.name;
                opt.textContent = w.name;
                opt.dataset.code = w.code;
                if (w.mergedFrom && !w.mergedFrom.includes('giữ nguyên')) {
                    opt.title = `Sáp nhập từ: ${w.mergedFrom}`;
                }
                wardEl.appendChild(opt);
            });

            // Giữ district là chuỗi rỗng trong mô hình 2 cấp (Backend lưu "" tương thích MySQL NOT NULL)
            if (districtEl) {
                districtEl.value = '';
            }

            // Điền trước Phường/Xã nếu có
            if (preselectedWard) {
                const matchedWard = findWardMatch(provinceObj.wards, preselectedWard);
                if (matchedWard) {
                    wardEl.value = matchedWard.name;
                }
            }
        }

        // Lắng nghe sự kiện đổi Tỉnh / Thành phố
        provinceEl.onchange = function() {
            populateWards(this.value);
        };

        // Lắng nghe sự kiện đổi Phường / Xã
        wardEl.onchange = function() {
            if (districtEl) {
                districtEl.value = '';
            }
        };

        // 2. Điền trước dữ liệu ban đầu (nếu có)
        if (initialProvince) {
            const matchedP = findProvinceMatch(data, initialProvince);
            if (matchedP) {
                provinceEl.value = matchedP.name;
                populateWards(matchedP.name, initialWard);
            }
        }
    }

    // Xuất ra phạm vi toàn cục
    window.VietnamAddressSelector = {
        init: initSelector,
        loadData: loadAddressData
    };

})();
