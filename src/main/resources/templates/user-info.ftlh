<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle=user.name/>
</head>
<body>

<@c.topbar currentUser=currentUser/>

<div class="page-head">
    <h1 class="display">${user.name}</h1>
    <p><b>Email:</b> ${user.email} · <b>Телефон:</b> ${user.phoneNumber!"Не указан"}</p>
</div>

<#if errorMessage??>
    <div class="alert" style="margin:0 28px;">${errorMessage}</div>
</#if>

<div class="filters">
    <h2>Товары пользователя ${user.name}</h2>
    <span class="count mono">${products?size} объявлений</span>
</div>

<#if products?has_content>
<div class="grid">
    <#list products as product>
        <@c.productCard product=product/>
    </#list>
</div>
<#else>
<div class="empty-state">Товаров нет</div>
</#if>

<@c.footer/>

</body>
</html>
