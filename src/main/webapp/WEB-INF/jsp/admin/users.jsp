<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>读者管理 - 后台管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">读者账号管理（学生 / 教师 / 访客三级权限管控）</h2>

    <%-- ==================== 查询栏 ==================== --%>
    <div class="card">
        <div class="filter-bar">
            <div class="form-item">
                <label>关键词</label>
                <input type="text" id="qKeyword" placeholder="账号/姓名/学号工号">
            </div>
            <div class="form-item">
                <label>身份</label>
                <select id="qRole">
                    <option value="">全部身份</option>
                    <option value="1">学生</option>
                    <option value="2">教师</option>
                    <option value="3">访客</option>
                </select>
            </div>
            <button class="btn btn-primary" onclick="loadPage(1)">查询</button>
        </div>
    </div>

    <%-- ==================== 按角色批量调整权限 ==================== --%>
    <div class="card">
        <div class="card-title">按角色批量调整权限 <span class="text-gray" style="font-size:12px;font-weight:normal">一键批量更新某身份下所有读者的借阅参数，覆盖原值</span></div>
        <div class="filter-bar" style="flex-wrap:wrap;">
            <div class="form-item">
                <label>目标身份</label>
                <select id="bRole">
                    <option value="1">学生</option>
                    <option value="2">教师</option>
                    <option value="3">访客</option>
                </select>
            </div>
            <div class="form-item">
                <label>借阅额度(本)</label>
                <input type="number" id="bCount" value="5" min="0">
            </div>
            <div class="form-item">
                <label>借阅天数</label>
                <input type="number" id="bDays" value="30" min="0">
            </div>
            <div class="form-item">
                <label>可续借次数</label>
                <input type="number" id="bRenew" value="1" min="0">
            </div>
            <div class="form-item">
                <label>超期罚款(元/天)</label>
                <input type="number" id="bFine" value="0.50" min="0" step="0.01">
            </div>
            <button class="btn btn-primary" onclick="batchApply()">一键批量应用</button>
            <button class="btn btn-success" onclick="applyPreset('student')">恢复学生默认</button>
            <button class="btn btn-success" onclick="applyPreset('teacher')">恢复教师默认</button>
            <button class="btn btn-success" onclick="applyPreset('visitor')">恢复访客默认</button>
        </div>
    </div>

    <div class="card">
        <div class="text-gray" style="font-size:12px;margin-bottom:10px;">
            说明：可直接修改各读者的借阅额度 / 借阅天数 / 可续借次数 / 罚款标准，点击「保存权限」立即生效（Redis 权限缓存同步失效）。
        </div>
        <table class="table">
            <thead>
            <tr>
                <th>ID</th><th>账号</th><th>姓名</th><th>身份</th><th>学号/工号</th><th>手机号</th>
                <th>额度(本)</th><th>借期(天)</th><th>续借(次)</th><th>罚款(元/天)</th>
                <th>押金(元)</th><th>状态</th><th>操作</th>
            </tr>
            </thead>
            <tbody id="userTbody">
            <tr><td colspan="13"><div class="empty-tip">加载中...</div></td></tr>
            </tbody>
        </table>
        <div class="pagination">
            <button id="prevBtn" onclick="loadPage(pageNum - 1)">上一页</button>
            <span class="page-info" id="pageInfo"></span>
            <button id="nextBtn" onclick="loadPage(pageNum + 1)">下一页</button>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    var pageNum = 1;
    var ctx = '${pageContext.request.contextPath}';
    var ROLE_NAMES = {0: '管理员', 1: '学生', 2: '教师', 3: '访客'};
    var ROLE_BADGES = {0: 'badge-gray', 1: 'badge-blue', 2: 'badge-green', 3: 'badge-orange'};

    /** 分页加载读者列表 */
    function loadPage(p) {
        pageNum = p;
        var url = buildUrl(ctx + '/admin/user/list', {
            keyword: $.trim($('#qKeyword').val()),
            role: $('#qRole').val(),
            pageNum: pageNum,
            pageSize: 10
        });
        $.get(url, function (r) {
            if (!r || r.code !== 200) {
                alert((r && r.msg) || '加载失败');
                return;
            }
            var d = r.data;
            renderRows(d.list);
            $('#pageInfo').text('第 ' + d.pageNum + ' / ' + d.pages + ' 页　共 ' + d.total + ' 条');
            $('#prevBtn').prop('disabled', d.pageNum <= 1);
            $('#nextBtn').prop('disabled', d.pageNum >= d.pages);
        });
    }

    /** 渲染读者表格（权限参数行内编辑） */
    function renderRows(list) {
        var html = '';
        if (!list || list.length === 0) {
            html = '<tr><td colspan="13"><div class="empty-tip">暂无数据</div></td></tr>';
        }
        $.each(list, function (i, u) {
            html += '<tr>'
                + '<td>' + u.id + '</td>'
                + '<td>' + u.username + '</td>'
                + '<td>' + u.realName + '</td>'
                + '<td><span class="badge ' + (ROLE_BADGES[u.role] || 'badge-gray') + '">' + (ROLE_NAMES[u.role] || '未知') + '</span></td>'
                + '<td>' + (u.stuOrJobNo || '-') + '</td>'
                + '<td>' + (u.phone || '-') + '</td>'
                + '<td><input class="cell-input" id="c_' + u.id + '_count" value="' + u.maxBorrowCount + '"></td>'
                + '<td><input class="cell-input" id="c_' + u.id + '_days" value="' + u.maxBorrowDays + '"></td>'
                + '<td><input class="cell-input" id="c_' + u.id + '_renew" value="' + u.maxRenewCount + '"></td>'
                + '<td><input class="cell-input" id="c_' + u.id + '_fine" value="' + u.finePerDay + '"></td>'
                + '<td>' + u.deposit + '</td>'
                + '<td>' + (u.status === 0
                    ? '<span class="badge badge-green">正常</span>'
                    : '<span class="badge badge-red">停用</span>') + '</td>'
                + '<td class="ops">'
                + '<button class="btn btn-primary btn-sm" onclick="savePerm(' + u.id + ')">保存权限</button>'
                + (u.status === 0
                    ? '<button class="btn btn-danger btn-sm" onclick="toggleStatus(' + u.id + ', 1)">停用</button>'
                    : '<button class="btn btn-success btn-sm" onclick="toggleStatus(' + u.id + ', 0)">启用</button>')
                + '</td></tr>';
        });
        $('#userTbody').html(html);
    }

    /** 保存行内权限参数 */
    function savePerm(id) {
        ajaxPost(ctx + '/admin/user/updatePerm', {
            id: id,
            maxBorrowCount: parseInt($('#c_' + id + '_count').val()),
            maxBorrowDays: parseInt($('#c_' + id + '_days').val()),
            maxRenewCount: parseInt($('#c_' + id + '_renew').val()),
            finePerDay: parseFloat($('#c_' + id + '_fine').val())
        }, function (r) {
            alert(r.msg);
        });
    }

    /** 启用/停用账号 */
    function toggleStatus(id, status) {
        var tip = status === 1 ? '确认停用该账号？停用后无法登录与借阅' : '确认启用该账号？';
        if (!confirm(tip)) return;
        ajaxPost(ctx + '/admin/user/updateStatus', {id: id, status: status}, function (r) {
            alert(r.msg);
            loadPage(pageNum);
        });
    }

    /** 按角色批量应用权限 */
    function batchApply() {
        var role = parseInt($('#bRole').val());
        var roleName = ROLE_NAMES[role];
        if (!confirm('确认将所有' + roleName + '的权限批量更新为当前参数？此操作将覆盖原值')) return;
        ajaxPost(ctx + '/admin/user/batchUpdatePerm', {
            role: role,
            maxBorrowCount: parseInt($('#bCount').val()),
            maxBorrowDays: parseInt($('#bDays').val()),
            maxRenewCount: parseInt($('#bRenew').val()),
            finePerDay: parseFloat($('#bFine').val())
        }, function (r) {
            alert(r.msg);
            loadPage(pageNum);
        });
    }

    /** 恢复角色默认权限预设 */
    function applyPreset(role) {
        var presets = {
            student: {role: 1, count: 5, days: 30, renew: 1, fine: 0.50},
            teacher: {role: 2, count: 10, days: 60, renew: 2, fine: 0.30},
            visitor: {role: 3, count: 2,  days: 15, renew: 0, fine: 1.00}
        };
        var p = presets[role];
        $('#bRole').val(p.role);
        $('#bCount').val(p.count);
        $('#bDays').val(p.days);
        $('#bRenew').val(p.renew);
        $('#bFine').val(p.fine);
    }

    loadPage(1);
</script>
</body>
</html>