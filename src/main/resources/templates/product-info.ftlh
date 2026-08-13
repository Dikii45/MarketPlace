<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle=product.title/>
</head>
<body>

<@c.topbar currentUser=currentUser/>

<div class="detail">
    <div class="gallery">
        <div class="gallery__main">
            <#if product.images?has_content>
                <img src="/images/${product.images[0].id}" alt="${product.title}">
            <#else>
                <svg class="icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2" style="width:64px;height:64px;">
                    <rect x="7" y="10" width="34" height="28" rx="3"/><circle cx="17" cy="20" r="3"/><path d="M11 33l10-9 8 7 6-6 8 7"/>
                </svg>
            </#if>
        </div>
        <#if (product.images?size > 1)>
        <div class="gallery__strip">
            <#list product.images as image>
                <#if image?index gt 0>
                    <img src="/images/${image.id}" alt="${product.title}">
                </#if>
            </#list>
        </div>
        </#if>
    </div>

    <div class="detail__info">
        <h1 class="detail__title display">${product.title}</h1>
        <div class="detail__price mono">${product.price?string("#,##0")} ₽</div>
        <p class="detail__desc">${product.description}</p>

        <dl class="meta-grid">
            <dt>Город</dt>
            <dd>${product.city}</dd>
            <dt>Автор</dt>
            <dd><a href="/user/${product.user.id}">${product.user.name}</a></dd>
        </dl>

        <form action="/product/delete/${product.id}" method="post">
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <button class="btn danger" type="submit">Удалить товар</button>
        </form>
    </div>
</div>

<@c.footer/>

</body>
</html>
