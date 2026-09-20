/**
 * 全站公共 JS 工具
 * 依赖：jQuery 3.x
 */

/**
 * 统一 POST 请求（JSON 提交，后端统一返回 Result{code,msg,data}）
 * @param url   请求地址
 * @param data  提交数据对象
 * @param okCb  成功回调（不传则默认刷新页面）
 */
function ajaxPost(url, data, okCb) {
    $.ajax({
        url: url,
        type: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify(data || {}),
        success: function (r) {
            if (r && r.code === 200) {
                if (okCb) {
                    okCb(r);
                } else {
                    location.reload();
                }
            } else {
                Toast.err((r && r.msg) || '操作失败，请稍后重试');
            }
        },
        error: function () {
            Toast.err('网络异常，请稍后重试');
        }
    });
}

/**
 * 获取 URL 查询参数
 */
function qs(name) {
    var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
    var r = window.location.search.substr(1).match(reg);
    return r === null ? '' : decodeURIComponent(r[2]);
}

/**
 * 拼接查询 URL（自动过滤空参数）
 */
function buildUrl(base, params) {
    var arr = [];
    $.each(params, function (k, v) {
        if (v !== '' && v !== null && v !== undefined) {
            arr.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
        }
    });
    return arr.length ? base + '?' + arr.join('&') : base;
}

/**
 * 通用分页跳转（保留当前查询参数）
 */
function goPage(pageNum) {
    var params = window.__pageParams || {};
    params.pageNum = pageNum;
    window.location.href = buildUrl(window.__pageUrl || window.location.pathname, params);
}

/* ==================== Modal 弹窗组件 ==================== */
var Modal = (function () {
    function mask() {
        var el = document.createElement('div');
        el.className = 'modal-mask';
        return el;
    }

    function confirm(title, msg, okCb, cancelCb) {
        var m = mask();
        m.innerHTML = '<div class="modal-box">' +
            '<div class="modal-head"><span class="modal-icon warn">!</span>' + title + '</div>' +
            '<div class="modal-body">' + msg + '</div>' +
            '<div class="modal-foot">' +
            '<button class="btn btn-cancel">取消</button>' +
            '<button class="btn btn-confirm">确定</button>' +
            '</div></div>';
        document.body.appendChild(m);
        m.querySelector('.btn-cancel').onclick = function () {
            document.body.removeChild(m);
            cancelCb && cancelCb();
        };
        m.querySelector('.btn-confirm').onclick = function () {
            document.body.removeChild(m);
            okCb && okCb();
        };
        m.onclick = function (e) { if (e.target === m) { document.body.removeChild(m); cancelCb && cancelCb(); } };
    }

    function prompt(title, placeholder, okCb, cancelCb) {
        var m = mask();
        m.innerHTML = '<div class="modal-box">' +
            '<div class="modal-head"><span class="modal-icon info">i</span>' + title + '</div>' +
            '<div class="modal-body"><input type="text" placeholder="' + (placeholder || '') + '"></div>' +
            '<div class="modal-foot">' +
            '<button class="btn btn-cancel">取消</button>' +
            '<button class="btn btn-confirm">确定</button>' +
            '</div></div>';
        document.body.appendChild(m);
        var input = m.querySelector('input');
        setTimeout(function () { input.focus(); }, 50);
        m.querySelector('.btn-cancel').onclick = function () {
            document.body.removeChild(m);
            cancelCb && cancelCb(null);
        };
        m.querySelector('.btn-confirm').onclick = function () {
            var v = input.value;
            document.body.removeChild(m);
            okCb && okCb(v);
        };
        input.onkeydown = function (e) {
            if (e.key === 'Enter') { m.querySelector('.btn-confirm').click(); }
            if (e.key === 'Escape') { m.querySelector('.btn-cancel').click(); }
        };
        m.onclick = function (e) { if (e.target === m) { document.body.removeChild(m); cancelCb && cancelCb(null); } };
    }

    return { confirm: confirm, prompt: prompt };
})();

/* ==================== Toast 轻提示 ==================== */
var Toast = (function () {
    function ensureWrap() {
        var w = document.querySelector('.toast-wrap');
        if (!w) { w = document.createElement('div'); w.className = 'toast-wrap'; document.body.appendChild(w); }
        return w;
    }
    function show(msg, type, dur) {
        var wrap = ensureWrap();
        var item = document.createElement('div');
        type = type || 'info';
        var iconMap = { ok: '\u2713', err: '\u2717', warn: '\u26A0', info: '\u2139' };
        item.className = 'toast-item ' + type;
        item.innerHTML = '<span class="ti-icon">' + (iconMap[type] || iconMap.info) + '</span><span>' + msg + '</span>';
        wrap.appendChild(item);
        dur = dur || 2400;
        setTimeout(function () {
            item.classList.add('fade-out');
            setTimeout(function () { if (item.parentNode) item.parentNode.removeChild(item); }, 300);
        }, dur);
    }
    return {
        ok: function (m, d) { show(m, 'ok', d); },
        err: function (m, d) { show(m, 'err', d); },
        warn: function (m, d) { show(m, 'warn', d); },
        info: function (m, d) { show(m, 'info', d); }
    };
})();
/* 全局替换 alert 为 Toast */
window.alert = function(msg){ Toast.warn(String(msg)); };