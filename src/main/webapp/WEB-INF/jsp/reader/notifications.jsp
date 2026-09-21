<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>消息中心 - 校园图书借阅管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
    <style>
        .notify-bell{position:relative;cursor:pointer;padding:8px 14px;}
        .notify-bell .bell-icon{font-size:18px;}
        .notify-bell .badge-dot{position:absolute;top:4px;right:4px;background:#e74c3c;color:#fff;font-size:11px;min-width:16px;height:16px;border-radius:8px;display:flex;align-items:center;justify-content:center;padding:0 4px;}
        .notify-item{padding:14px 18px;border-bottom:1px solid #f0f0f0;display:flex;gap:14px;transition:background .2s;}
        .notify-item:hover{background:#fafafa;}
        .notify-item.unread{background:#f0f7ff;border-left:3px solid #3498db;}
        .notify-item.unread:hover{background:#e6f0fa;}
        .notify-type{flex-shrink:0;width:40px;height:40px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:18px;}
        .type-1{background:#e8f5e9;color:#2e7d32;}
        .type-2{background:#fff3e0;color:#e65100;}
        .type-3{background:#ffebee;color:#c62828;}
        .type-4{background:#fff8e1;color:#f57f17;}
        .type-5{background:#e3f2fd;color:#1565c0;}
        .type-99{background:#f3e5f5;color:#6a1b9a;}
        .notify-body{flex:1;min-width:0;}
        .notify-title{font-weight:600;color:#2c3e50;margin-bottom:4px;font-size:14px;}
        .notify-content{color:#666;font-size:13px;line-height:1.6;white-space:pre-wrap;}
        .notify-time{color:#999;font-size:12px;margin-top:6px;}
        .empty-tip{text-align:center;padding:60px 20px;color:#999;font-size:14px;}
    </style>
</head>
<body>
<%@ include file="/WEB-INF/jsp/common/header.jsp" %>

<div class="container">
    <div class="card">
        <div class="card-title" style="display:flex;justify-content:space-between;align-items:center;">
            <span>📬 消息中心</span>
            <button class="btn btn-outline btn-sm" onclick="markAll()">全部标记已读</button>
        </div>
        <div id="notifyList">
            <div class="empty-tip">加载中...</div>
        </div>
    </div>
</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="${pageContext.request.contextPath}/static/js/common.js"></script>
<script>
    var CTX = '${pageContext.request.contextPath}';
    var TYPE_ICON = {1:'📚',2:'⏰',3:'🚫',4:'💸',5:'💰',99:'📢'};
    var TYPE_NAME = {1:'预订到书',2:'即将到期',3:'账号冻结',4:'罚款生成',5:'押金退款',99:'系统通知'};

    function loadList(){
        $.get(CTX + '/notify/list', function(r){
            if(!r.success){ $('#notifyList').html('<div class="empty-tip">加载失败</div>'); return; }
            var data = r.data;
            if(!data.list || data.list.length === 0){
                $('#notifyList').html('<div class="empty-tip">🎉 暂无消息，一切安好</div>');
                return;
            }
            var html = '';
            data.list.forEach(function(n){
                var cls = n.isRead === 0 ? 'unread' : '';
                var typeCls = 'type-' + n.type;
                var icon = TYPE_ICON[n.type] || '📢';
                var typeName = TYPE_NAME[n.type] || '通知';
                html += '<div class="notify-item ' + cls + '">'
                    + '<div class="notify-type ' + typeCls + '">' + icon + '</div>'
                    + '<div class="notify-body">'
                    + '<div class="notify-title">' + (n.isRead === 0 ? '<span style="color:#3498db;">[未读]</span> ' : '') + n.title + '</div>'
                    + '<div class="notify-content">' + n.content + '</div>'
                    + '<div class="notify-time">' + typeName + ' · ' + n.createTime + '</div>'
                    + '</div></div>';
            });
            $('#notifyList').html(html);
        });
    }
    function markAll(){
        ajaxPost(CTX + '/notify/markAllRead', {}, function(){ loadList(); });
    }
    $(function(){ loadList(); });
</script>
</body>
</html>
