<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head/>
</head>
<body>

<@c.topbar currentUser=currentUser/>

<div class="shell" style="padding-left:0;padding-right:0;">

    <div class="route">
        <h1 class="display">Товары со всей России</h1>
        <p>Покупайте у соседей и продавцов из других городов — с доставкой в любую точку страны.</p>
        <div class="route-line mono">
            <span class="stop">Москва</span><span class="seg"></span>
            <span class="stop">Казань</span><span class="seg"></span>
            <span class="stop">Екатеринбург</span><span class="seg"></span>
            <span class="stop">Новосибирск</span><span class="seg"></span>
            <span class="stop">Владивосток</span>
        </div>
    </div>

    <div class="filters">
        <h2>Свежие объявления</h2>
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
</div>
</body>
</html>
