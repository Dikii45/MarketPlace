<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Чат с ${otherUser.name}"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="chat-page">
    <div class="chat-window">
        <div class="chat-header">
            <#if otherUser.avatar??>
                <span class="account-avatar"><img src="/images/${otherUser.avatar.id}" alt=""></span>
            <#else>
                <span class="account-avatar">${otherUser.name?substring(0,1)?upper_case}</span>
            </#if>
            <div class="chat-header__who">
                <div class="chat-header__name">${otherUser.name}</div>
                <div class="chat-header__status<#if otherOnline> is-online</#if>">
                    <#if otherOnline>Онлайн<#else>Не в сети</#if>
                </div>
            </div>
            <a class="btn-sm ghost" href="/orders">← К заказам</a>
        </div>

        <div class="chat-messages" id="chatMessages"
             data-chat-url="/chat/${otherUser.id}"
             data-last-id="<#if messages?has_content>${messages?last.id}<#else>0</#if>">
            <#list messages as m>
                <div class="chat-bubble <#if m.sender.id == currentUser.id>chat-bubble--mine<#else>chat-bubble--theirs</#if>" data-id="${m.id}">
                    <div class="chat-bubble__text">${m.text}</div>
                    <div class="chat-bubble__time mono">${m.sentAt?substring(8,10)}.${m.sentAt?substring(5,7)} ${m.sentAt?substring(11,16)}</div>
                </div>
            </#list>
            <#if !messages?has_content>
                <div class="empty-state">Напишите первое сообщение</div>
            </#if>
        </div>

        <form class="chat-input" id="chatForm">
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <input type="text" name="text" placeholder="Напишите сообщение…" autocomplete="off" required>
            <button type="submit" class="btn accent">Отправить</button>
        </form>
    </div>
</div>

<@c.footer/>

</body>
</html>
