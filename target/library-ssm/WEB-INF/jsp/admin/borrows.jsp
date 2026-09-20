<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>借阅记录管理 - 后台管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">借阅记录管理</h2>

    <%-- ==================== 筛选栏 ==================== --%>
    <div class="card">
        <div class="filter-bar">
            <div class="form-item">
                <label>关键词</label>
                <input type="text" id="qKeyword" placeholder="书名/姓名/账号/学号">
            </div>
            <div class="form-item">
                <label>状态</label>
                <select id="qStatus">
                    <option value="">全部状态</option>
                    <option value="0">借阅中</option>
                    <option value="1">已归还</option>
                    <option value="2">超期未还</option>
                    <option value="3">超期已归还</option>
                </select>
            </div>
            <div class="form-item">
                <label>借出开始日期</label>
                <input type="date" id="qStart">
            </div>
            <div class="form-item">
                <label>借出结束日期</label>
                <input type="date" id="qEnd">
            </div>
            <button class="btn btn-primary" onclick="loadPage(1)">查询</button>
            <button class="btn btn-warning" onclick="markOverdue()">立即执行超期检查</button>
        </div>
    </div>

    <div class="card">
        <table class="table">
            <thead>
            <tr>
                <th>ID</th><th>图书</th><th>借阅人</th><th>账号</th><th>借出时间</th>
                <th>应还时间</th><th>归还时间</th><th>状态</th><th>续借</th><th>罚款(元)</th>
            </tr>
            </thead>
            <tbody id="borrowTbody">
            <tr><td colspan="10"><div class="empty-tip">加载中...</div></td></tr>
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
    var STATUS_BADGES = {
        0: ['借阅中', 'badge-blue'],
        1: ['已归还', 'badge-green'],
        2: ['超期未还', 'badge-red'],
        3: ['超期已归还', 'badge-gray']
    };

    /** 格式化时间字符串（后端返回时间戳） */
    function fmtTime(ts) {
        if (!ts) return '-';
        var d = new Date(ts);
        var m = d.getMonth() + 1, day = d.getDate();
        return d.getFullYear() + '-' + (m < 10 ? '0' + m : m) + '-' + (day < 10 ? '0' + day : day);
    }

    /** 分页加载借阅记录 */
    function loadPage(p) {
        pageNum = p;
        var url = buildUrl(ctx + '/admin/borrow/list', {
            keyword: $.trim($('#qKeyword').val()),
            status: $('#qStatus').val(),
            startDate: $('#qStart').val(),
            endDate: $('#qEnd').val(),
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

    /** 渲染借阅记录表格 */
    function renderRows(list) {
        var html = '';
        if (!list || list.length === 0) {
            html = '<tr><td colspan="10"><div class="empty-tip">暂无数据</div></td></tr>';
        }
        $.each(list, function (i, r) {
            var badge = STATUS_BADGES[r.status] || ['未知', 'badge-gray'];
            html += '<tr>'
                + '<td>' + r.id + '</td>'
                + '<td>' + (r.bookName || '-') + '</td>'
                + '<td>' + (r.realName || '-') + '</td>'
                + '<td>' + (r.username || '-') + '</td>'
                + '<td>' + fmtTime(r.borrowDate) + '</td>'
                + '<td>' + fmtTime(r.dueDate) + '</td>'
                + '<td>' + fmtTime(r.returnDate) + '</td>'
                + '<td><span class="badge ' + badge[1] + '">' + badge[0] + '</span></td>'
                + '<td>' + r.renewCount + '</td>'
                + '<td class="' + (r.fineAmount > 0 ? 'text-danger' : '') + '">' + r.fineAmount + '</td>'
                + '</tr>';
        });
        $('#borrowTbody').html(html);
    }

    /** 手动触发超期检查（正式环境由每日 00:30 定时任务自动执行） */
    function markOverdue() {
        if (!confirm('确认立即执行超期检查？')) return;
        ajaxPost(ctx + '/admin/borrow/markOverdue', {}, function (r) {
            alert(r.msg);
            loadPage(pageNum);
        });
    }

    loadPage(1);
</script>
</body>
</html>
