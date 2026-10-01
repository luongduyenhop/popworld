/**
 * PopWorld - Bộ chọn Hành chính Việt Nam 3 Cấp (Tỉnh / Thành -> Quận / Huyện -> Phường / Xã)
 * Hỗ trợ nạp API tự động, lưu cache LocalStorage và fallback dự phòng mượt mà.
 */
(function() {
    'use strict';

    const CACHE_KEY = 'popworld_vn_provinces_cache_v3';
    const API_URL = 'https://provinces.open-api.vn/api/?depth=3';

    // Danh sách 63 tỉnh/thành phố chuẩn Việt Nam (Dự phòng offline tức thì)
    const FALLBACK_PROVINCES = [
        "Thành phố Hà Nội", "Thành phố Hồ Chí Minh", "Thành phố Hải Phòng", "Thành phố Đà Nẵng", "Thành phố Cần Thơ",
        "Tỉnh An Giang", "Tỉnh Bà Rịa - Vũng Tàu", "Tỉnh Bắc Giang", "Tỉnh Bắc Kạn", "Tỉnh Bạc Liêu", "Tỉnh Bắc Ninh",
        "Tỉnh Bến Tre", "Tỉnh Bình Định", "Tỉnh Bình Dương", "Tỉnh Bình Phước", "Tỉnh Bình Thuận", "Tỉnh Cà Mau",
        "Tỉnh Cao Bằng", "Tỉnh Đắk Lắk", "Tỉnh Đắk Nông", "Tỉnh Điện Biên", "Tỉnh Đồng Nai", "Tỉnh Đồng Tháp",
        "Tỉnh Gia Lai", "Tỉnh Hà Giang", "Tỉnh Hà Nam", "Tỉnh Hà Tĩnh", "Tỉnh Hải Dương", "Tỉnh Hậu Giang",
        "Tỉnh Hòa Bình", "Tỉnh Hưng Yên", "Tỉnh Khánh Hòa", "Tỉnh Kiên Giang", "Tỉnh Kon Tum", "Tỉnh Lai Châu",
        "Tỉnh Lâm Đồng", "Tỉnh Lạng Sơn", "Tỉnh Lào Cai", "Tỉnh Long An", "Tỉnh Nam Định", "Tỉnh Nghệ An",
        "Tỉnh Ninh Bình", "Tỉnh Ninh Thuận", "Tỉnh Phú Thọ", "Tỉnh Phú Yên", "Tỉnh Quảng Bình", "Tỉnh Quảng Nam",
        "Tỉnh Quảng Ngãi", "Tỉnh Quảng Ninh", "Tỉnh Quảng Trị", "Tỉnh Sóc Trăng", "Tỉnh Sơn La", "Tỉnh Tây Ninh",
        "Tỉnh Thái Bình", "Tỉnh Thái Nguyên", "Tỉnh Thanh Hóa", "Tỉnh Thừa Thiên Huế", "Tỉnh Tiền Giang",
        "Tỉnh Trà Vinh", "Tỉnh Tuyên Quang", "Tỉnh Vĩnh Long", "Tỉnh Vĩnh Phúc", "Tỉnh Yên Bái"
    ];

    let cachedData = null;
    let loadingPromise = null;

    async function loadAddressData() {
        if (cachedData && cachedData.length > 0) {
            return cachedData;
        }

        // 1. Kiểm tra cache trong localStorage
        try {
            const local = localStorage.getItem(CACHE_KEY);
            if (local) {
                const parsed = JSON.parse(local);
                if (Array.isArray(parsed) && parsed.length > 0) {
                    cachedData = parsed;
                    return cachedData;
                }
            }
        } catch (e) {
            console.warn('Lỗi đọc cache địa chỉ từ localStorage:', e);
        }

        // 2. Fetch API trực tuyến
        if (!loadingPromise) {
            loadingPromise = fetch(API_URL)
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
                            // LocalStorage full, bỏ qua
                        }
                        return data;
                    }
                    throw new Error('Dữ liệu rỗng');
                })
                .catch(err => {
                    console.warn('Không thể tải API hành chính trực tuyến, sử dụng danh sách dự phòng:', err);
                    cachedData = FALLBACK_PROVINCES.map((name, idx) => ({
                        code: idx + 1,
                        name: name,
                        districts: []
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
     * Khởi tạo Bộ chọn Hành chính Việt Nam
     * Chuẩn mô hình 2 cấp: Tỉnh / Thành phố -> Phường / Xã / Thị trấn
     */
    async function initSelector(options) {
        const {
            provinceElId,
            wardElId,
            districtElId, // Tùy chọn (input ẩn hoặc select cũ để tương thích ngược)
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

        // 1. Nạp danh sách Tỉnh / Thành phố
        provinceEl.innerHTML = '<option value="">-- Chọn Tỉnh / Thành phố --</option>';
        data.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.name;
            opt.textContent = p.name;
            opt.dataset.code = p.code;
            provinceEl.appendChild(opt);
        });

        // Kiểm tra xem districtEl có phải là thẻ SELECT 3 cấp truyền thống không
        const isLegacy3Tier = districtEl && districtEl.tagName === 'SELECT';

        if (isLegacy3Tier) {
            // === CHẾ ĐỘ 3 CẤP DỰ PHÒNG ===
            function populateDistrictsLegacy(selectedProvinceName, preselectedDistrict = '') {
                districtEl.innerHTML = '<option value="">-- Chọn Quận / Huyện --</option>';
                wardEl.innerHTML = '<option value="">-- Chọn Phường / Xã --</option>';
                wardEl.disabled = true;

                if (!selectedProvinceName) {
                    districtEl.disabled = true;
                    return;
                }
                districtEl.disabled = false;

                const provinceObj = data.find(p => p.name === selectedProvinceName);
                if (provinceObj && provinceObj.districts && provinceObj.districts.length > 0) {
                    provinceObj.districts.forEach(d => {
                        const opt = document.createElement('option');
                        opt.value = d.name;
                        opt.textContent = d.name;
                        opt.dataset.code = d.code;
                        if (preselectedDistrict && (d.name === preselectedDistrict || d.name.includes(preselectedDistrict) || preselectedDistrict.includes(d.name))) {
                            opt.selected = true;
                        }
                        districtEl.appendChild(opt);
                    });

                    const activeDistrict = districtEl.value || preselectedDistrict;
                    if (activeDistrict) {
                        populateWardsLegacy(selectedProvinceName, activeDistrict, initialWard);
                    }
                }
            }

            function populateWardsLegacy(selectedProvinceName, selectedDistrictName, preselectedWard = '') {
                wardEl.innerHTML = '<option value="">-- Chọn Phường / Xã --</option>';
                if (!selectedProvinceName || !selectedDistrictName) {
                    wardEl.disabled = true;
                    return;
                }
                wardEl.disabled = false;

                const provinceObj = data.find(p => p.name === selectedProvinceName);
                if (provinceObj && provinceObj.districts) {
                    const districtObj = provinceObj.districts.find(d => d.name === selectedDistrictName);
                    if (districtObj && districtObj.wards) {
                        districtObj.wards.forEach(w => {
                            const opt = document.createElement('option');
                            opt.value = w.name;
                            opt.textContent = w.name;
                            opt.dataset.code = w.code;
                            if (preselectedWard && (w.name === preselectedWard || w.name.includes(preselectedWard) || preselectedWard.includes(w.name))) {
                                opt.selected = true;
                            }
                            wardEl.appendChild(opt);
                        });
                    }
                }
            }

            provinceEl.onchange = function() {
                populateDistrictsLegacy(this.value);
            };

            districtEl.onchange = function() {
                populateWardsLegacy(provinceEl.value, this.value);
            };

            if (initialProvince) {
                const matchedP = data.find(p => p.name === initialProvince || p.name.includes(initialProvince) || initialProvince.includes(p.name));
                if (matchedP) {
                    provinceEl.value = matchedP.name;
                    populateDistrictsLegacy(matchedP.name, initialDistrict);
                }
            }
        } else {
            // === CHUẨN MÔ HÌNH 2 CẤP: TỈNH -> PHƯỜNG / XÃ ===
            function populateWards2Tier(selectedProvinceName, preselectedWard = '') {
                wardEl.innerHTML = '<option value="">-- Chọn Phường / Xã / Thị trấn --</option>';

                if (!selectedProvinceName) {
                    wardEl.disabled = true;
                    if (districtEl) districtEl.value = '';
                    return;
                }
                wardEl.disabled = false;

                const provinceObj = data.find(p => p.name === selectedProvinceName);
                const allWards = [];

                if (provinceObj && provinceObj.districts) {
                    provinceObj.districts.forEach(d => {
                        if (d.wards && d.wards.length > 0) {
                            d.wards.forEach(w => {
                                allWards.push({
                                    name: w.name,
                                    district: d.name,
                                    code: w.code
                                });
                            });
                        }
                    });
                }

                // Sắp xếp Xã/Phường theo thứ tự bảng chữ cái tiếng Việt
                allWards.sort((a, b) => a.name.localeCompare(b.name, 'vi'));

                allWards.forEach(w => {
                    const opt = document.createElement('option');
                    opt.value = w.name;
                    opt.textContent = `${w.name} (${w.district})`;
                    opt.dataset.district = w.district;
                    if (preselectedWard && (w.name === preselectedWard || w.name.includes(preselectedWard) || preselectedWard.includes(w.name))) {
                        opt.selected = true;
                        if (districtEl) districtEl.value = w.district;
                    }
                    wardEl.appendChild(opt);
                });

                // Nếu preselectedWard khớp, tự động gán district ngầm nếu có input
                if (wardEl.selectedIndex > 0) {
                    const selectedOpt = wardEl.options[wardEl.selectedIndex];
                    if (districtEl && selectedOpt) {
                        districtEl.value = selectedOpt.dataset.district || '';
                    }
                }
            }

            wardEl.onchange = function() {
                const selectedOpt = this.options[this.selectedIndex];
                if (districtEl && selectedOpt) {
                    districtEl.value = selectedOpt.dataset.district || '';
                }
            };

            provinceEl.onchange = function() {
                populateWards2Tier(this.value);
            };

            // Khởi tạo giá trị ban đầu nếu có
            if (initialProvince) {
                const matchedP = data.find(p => p.name === initialProvince || p.name.includes(initialProvince) || initialProvince.includes(p.name));
                if (matchedP) {
                    provinceEl.value = matchedP.name;
                    populateWards2Tier(matchedP.name, initialWard);
                } else {
                    provinceEl.value = initialProvince;
                    populateWards2Tier(initialProvince, initialWard);
                }
            }
        }
    }

    // Xuất ra global window
    window.VietnamAddressSelector = {
        loadData: loadAddressData,
        init: initSelector
    };

    // Tự động tải trước dữ liệu trong nền khi trang load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadAddressData);
    } else {
        loadAddressData();
    }
})();
