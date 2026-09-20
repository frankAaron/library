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
                alert((r && r.msg) || '操作失败，请稍后重试');
            }
        },
        error: function () {
            alert('网络异常，请稍后重试');
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
