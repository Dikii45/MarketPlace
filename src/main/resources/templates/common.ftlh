<#-- Shared layout macros: head / topbar / footer -->

<#macro head subtitle="">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>BUYSELL<#if subtitle?has_content> — ${subtitle}</#if></title>
    <link rel="stylesheet" href="/css/site.css">
    <script src="/js/site.js" defer></script>
</#macro>

<#macro topbar currentUser={} showSearch=true searchValue="" variant="">
<header class="top<#if variant?has_content> top--${variant}</#if>">
    <a class="wordmark display" href="/"><span class="postmark"></span>BUYSELL</a>
    <#if showSearch>
    <form class="search" role="search" action="/" method="get">
        <input type="text" name="title" placeholder="Поиск по названию объявления…" value="${searchValue}">
        <button type="submit">Найти</button>
    </form>
    </#if>
    <div class="account">
        <#if currentUser?? && currentUser.email?? && !currentUser.isAdmin()>
            <a class="cart-link" href="/cart" aria-label="Корзина">
                <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="9" cy="20" r="1.6"/><circle cx="18" cy="20" r="1.6"/>
                    <path d="M1.5 1.5h3l2.5 13.6a2 2 0 0 0 2 1.65h8.9a2 2 0 0 0 1.97-1.66L21.5 6.5H5.4"/>
                </svg>
                <#if cartCount?? && cartCount gt 0><span class="cart-badge mono">${cartCount}</span></#if>
            </a>
        </#if>
        <#if currentUser?? && currentUser.email??>
            <#assign initial = (currentUser.name?has_content)?then(currentUser.name?substring(0,1)?upper_case, "?")>
            <details class="account-menu">
                <summary class="account-trigger" aria-label="Аккаунт">
                    <#if currentUser.avatar??>
                        <span class="account-avatar"><img src="/images/${currentUser.avatar.id}" alt=""></span>
                    <#else>
                        <span class="account-avatar">${initial}</span>
                    </#if>
                </summary>
                <div class="account-menu__panel">
                    <div class="account-menu__head">
                        <#if currentUser.avatar??>
                            <span class="account-avatar account-avatar--lg"><img src="/images/${currentUser.avatar.id}" alt=""></span>
                        <#else>
                            <span class="account-avatar account-avatar--lg">${initial}</span>
                        </#if>
                        <div class="account-menu__who">
                            <div class="account-menu__name">${currentUser.name}</div>
                            <div class="account-menu__email mono">${currentUser.email}</div>
                        </div>
                    </div>
                    <#if currentUser.isAdmin()>
                        <a class="account-menu__item" href="/admin">Панель администратора</a>
                    </#if>
                    <a class="account-menu__item" href="/account">Аккаунт</a>
                    <a class="account-menu__item" href="/orders">Заказы</a>
                    <#if !currentUser.isAdmin()>
                        <a class="account-menu__item" href="/user/${currentUser.id}">Ваши товары</a>
                    </#if>
                    <form action="/logout" method="post">
                        <input type="hidden" name="_csrf" value="${_csrf.token}">
                        <button class="account-menu__item account-menu__item--danger" type="submit">Выйти</button>
                    </form>
                </div>
            </details>
        <#else>
            <a class="pill-link" href="/login">Войти</a>
        </#if>
    </div>
</header>
</#macro>

<#macro pageBanner title subtitle="">
<div class="page-banner">
    <div class="page-banner__bg" aria-hidden="true">
        <svg viewBox="0 0 1440 220" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <linearGradient id="bannerSky" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#0a2f42"/>
                    <stop offset="55%" stop-color="#12616d"/>
                    <stop offset="100%" stop-color="#1f9089"/>
                </linearGradient>
            </defs>
            <rect x="0" y="0" width="1440" height="220" fill="url(#bannerSky)"/>
            <polygon points="0,140 120,90 260,130 420,80 600,120 780,70 960,110 1140,80 1320,115 1440,90 1440,220 0,220" fill="#124f59" opacity="0.7"/>
            <polygon points="0,182 90,112 200,182" fill="#0f434c"/>
            <polygon points="520,192 650,92 660,192" fill="#123f47"/>
            <polygon points="660,192 650,92 800,192" fill="#2c8f89"/>
            <polygon points="1050,186 1160,112 1260,186" fill="#123f47"/>
            <polygon points="1260,186 1160,112 1360,186" fill="#2c8f89"/>
            <polygon points="0,200 40,185 80,198 120,180 160,196 200,178 240,195 280,177 320,194 360,176 400,195 440,178 480,196 520,180 560,197 600,179 640,196 680,178 720,195 760,177 800,194 840,178 880,196 920,180 960,197 1000,179 1040,195 1080,177 1120,194 1160,178 1200,196 1240,180 1280,197 1320,179 1360,196 1400,180 1440,195 1440,220 0,220" fill="#08222b"/>
        </svg>
    </div>
    <div class="page-banner__content">
        <h1 class="display">${title}</h1>
        <#if subtitle?has_content><p>${subtitle}</p></#if>
    </div>
</div>
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
        <#if product.quantity <= 0>
            <div class="card__sold-out">Товар закончился</div>
        </#if>
    </div>
    <div class="card__perf"></div>
    <div class="card__body">
        <div class="card__title">${product.title}</div>
        <div class="card__meta mono"><span>${product.city}</span><span>#${product.id}</span></div>
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
