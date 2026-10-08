<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>罚款对账 - 后台管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <h2 class="page-title">超期罚款对账</h2>

    <%-- ==================== 筛选栏 ==================== --%>
    <div class="card">
        <div class="filter-bar">
            <div class="form-item">
                <label>缴纳状态</label>
                <select id="qStatus">
                    <option value="">全部</option>
                    <option value="0">未缴纳</option>
                    <option value="1">已缴纳</option>
                </select>
            </div>
            <button class="btn btn-primary" onclick="loadPage(1)">查询</button>
        </div>
    </div>

    <div class="card">
        <div class="text-gray" style="font-size:12px;margin-bottom:10px;">
            说明：读者线上缴费由系统自动核销；线下收款场景可由管理员人工「标记已缴」完成对账。
        </div>
        <table class="table">
            <thead>
            <tr>
                <th>ID</th><th>图书</th><th>借阅人</th><th>账号</th><th>罚款金额(元)</th>
                <th>状态</th><th>缴纳时间</th><th>操作</th>
            </tr>
            </thead>
            <tbody id="fineTbody">
            <tr><td colspan="8"><div class="empty-tip">加载中...</div></td></tr>
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

    /** 格式化时间 */
    function fmtTime(ts) {
        if (!ts) return '-';
        var d = new Date(ts);
        var m = d.getMonth() + 1, day = d.getDate(), h = d.getHours(), mi = d.getMinutes();
        return d.getFullYear() + '-' + (m < 10 ? '0' + m : m) + '-' + (day < 10 ? '0' + day : day)
            + ' ' + (h < 10 ? '0' + h : h) + ':' + (mi < 10 ? '0' + mi : mi);
    }

    /** 分页加载罚款记录 */
    function loadPage(p) {
        pageNum = p;
        var url = buildUrl(ctx + '/admin/fine/list', {
            status: $('#qStatus').val(),
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

    /** 渲染罚款表格 */
    function renderRows(list) {
        var html = '';
        if (!list || list.length === 0) {
            html = '<tr><td colspan="8"><div class="empty-tip">暂无数据</div></td></tr>';
        }
        $.each(list, function (i, f) {
            html += '<tr>'
                + '<td>' + f.id + '</td>'
                + '<td>' + (f.bookName || '-') + '</td>'
                + '<td>' + (f.realName || '-') + '</td>'
                + '<td>' + (f.username || '-') + '</td>'
                + '<td class="text-danger">' + f.amount + '</td>'
                + '<td>' + (f.status === 0
                    ? '<span class="badge badge-red">未缴纳</span>'
                    : '<span class="badge badge-green">已缴纳</span>') + '</td>'
                + '<td>' + fmtTime(f.payTime) + '</td>'
                + '<td class="ops">'
                + (f.status === 0
                    ? '<button class="btn btn-primary btn-sm" onclick="markPaid(' + f.id + ')">标记已缴</button>'
                    : '<button class="btn btn-outline btn-sm" style="color:#e74c3c;border-color:#e74c3c;" onclick="revokeFine(' + f.id + ', \'' + (f.amount || 0) + '\')">撤销</button>')
                + '</td></tr>';
        });
        $('#fineTbody').html(html);
    }

    /** 人工核销罚款（对账场景） */
    function markPaid(id) {
        if (!confirm('确认该笔罚款已线下收取并标记为已缴？')) return;
        ajaxPost(ctx + '/admin/fine/markPaid', {id: id}, function (r) {
            alert(r.msg);
            loadPage(pageNum);
        });
    }

    /** 撤销罚款核销（管理员误判回滚，内部退款） */
    function revokeFine(fineId, amount) {
        var reason = prompt('撤销罚款核销将触发内部退款（' + amount + ' 元押金返还读者），请输入撤销原因：', '读者申诉，误判');
        if (!reason) return;
        ajaxPost(ctx + '/admin/fine/revoke', {fineId: fineId, reason: reason}, function (r) {
            if (r.code === 200) {
                alert('撤销成功：' + r.msg);
            } else {
                alert('撤销失败：' + r.msg);
            }
            loadPage(pageNum);
        });
    }

    loadPage(1);
</script>
</body>
</html>