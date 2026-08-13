<#-- Shared layout macros: head / topbar / footer -->

<#macro head subtitle="">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>BUYSELL<#if subtitle?has_content> — ${subtitle}</#if></title>
    <link rel="stylesheet" href="/css/site.css">
</#macro>

<#macro topbar currentUser={} showSearch=true searchValue="">
<header class="top">
    <a class="wordmark display" href="/"><span class="postmark"></span>BUYSELL</a>
    <#if showSearch>
    <form class="search" role="search" action="/" method="get">
        <input type="text" name="title" placeholder="Поиск по названию объявления…" value="${searchValue}">
        <button type="submit">Найти</button>
    </form>
    </#if>
    <div class="account">
        <#if currentUser?? && currentUser.email??>
            <span class="name">Продавец: <b>${currentUser.name}</b></span>
            <#if currentUser.isAdmin()>
                <a class="pill-link admin" href="/admin">Панель администратора</a>
            </#if>
            <form action="/logout" method="post" style="display:inline;">
                <input type="hidden" name="_csrf" value="${_csrf.token}">
                <button class="logout-btn" type="submit">Выйти</button>
            </form>
        <#else>
            <a class="pill-link" href="/login">Войти</a>
        </#if>
    </div>
</header>
</#macro>

<#macro productCard product>
<a class="card" href="/product/${product.id}">
    <div class="card__photo">
        <#if product.previewImageId??>
            <img src="/images/${product.previewImageId}" alt="${product.title}">
        <#elseif product.images?has_content>
            <img src="/images/${product.images[0].id}" alt="${product.title}">
        <#else>
            <svg class="icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="7" y="10" width="34" height="28" rx="3"/><circle cx="17" cy="20" r="3"/><path d="M11 33l10-9 8 7 6-6 8 7"/>
            </svg>
        </#if>
        <div class="card__stamp mono">${product.price?string("#,##0")} ₽</div>
    </div>
    <div class="card__perf"></div>
    <div class="card__body">
        <div class="card__title">${product.title}</div>
        <div class="card__meta mono"><span>${product.city}</span><span>№ ${product.id}</span></div>
        <div class="card__go">Подробнее →</div>
    </div>
</a>
</#macro>

<#macro footer>
<footer class="site-footer">
    <span class="display">BUYSELL</span>
    <span>Товары со всей России</span>
</footer>
</#macro>
