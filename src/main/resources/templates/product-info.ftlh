 <#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle=product.title/>
</head>
<body>

<@c.topbar currentUser=currentUser/>

<#if errorMessage??>
    <div class="alert" style="margin:16px 28px 0;">${errorMessage}</div>
</#if>
<#if successMessage??>
    <div class="alert alert--ok" style="margin:16px 28px 0;">${successMessage}</div>
</#if>

<div class="detail">
    <div class="gallery">
        <div class="gallery__main">
            <#if product.images?has_content>
                <img id="galleryMain" src="/images/${product.images[0].id}" alt="${product.title}">
            <#else>
                <svg class="icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2" style="width:64px;height:64px;">
                    <rect x="7" y="10" width="34" height="28" rx="3"/><circle cx="17" cy="20" r="3"/><path d="M11 33l10-9 8 7 6-6 8 7"/>
                </svg>
            </#if>
        </div>
        <#if (product.images?size > 1)>
        <div class="gallery__strip">
            <#list product.images as image>
                <img src="/images/${image.id}" alt="${product.title}" data-gallery-thumb class="<#if image?index==0>is-active</#if>">
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
            <dt>В наличии</dt>
            <dd>${product.quantity} шт.</dd>
        </dl>

        <#if product.quantity <= 0>
            <div class="alert" style="margin:0;">Товар закончился</div>
        <#elseif currentUser?? && currentUser.id?? && currentUser.id != product.user.id>
        <form action="/cart/add/${product.id}" method="post">
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <button class="btn accent" type="submit">В корзину</button>
        </form>
        </#if>

        <#if currentUser?? && currentUser.id?? && (currentUser.isAdmin() || currentUser.id == product.user.id)>
        <form class="restock-form" action="/product/restock/${product.id}" method="post">
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <input type="number" name="amount" min="1" value="1" required>
            <button class="btn-sm accent" type="submit">Пополнить остаток</button>
        </form>

        <form action="/product/delete/${product.id}" method="post" data-confirm="Удалить товар «${product.title}»?">
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <button class="btn danger" type="submit">Удалить товар</button>
        </form>
        </#if>

    </div>
</div>

<@c.footer/>

</body>
</html>
