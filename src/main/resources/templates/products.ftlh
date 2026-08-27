<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head/>
</head>
<body>

<script>
    (function () {
        var q = new URLSearchParams(location.search).get("title");
        if (q && q.trim() !== "") {
            document.documentElement.classList.add("search-active");
            document.addEventListener("DOMContentLoaded", function () {
                var input = document.querySelector('.search input[name="title"]');
                if (input) input.value = q;
            });
        }
    })();
</script>

<div class="hero">
    <@c.topbar currentUser=currentUser variant="hero"/>

    <div class="hero__bg" aria-hidden="true">
        <svg viewBox="0 0 1440 600" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <linearGradient id="heroSky" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="#0a2f42"/>
                    <stop offset="45%" stop-color="#0e5f6c"/>
                    <stop offset="70%" stop-color="#1f9089"/>
                    <stop offset="100%" stop-color="#cfe6ab"/>
                </linearGradient>
            </defs>
            <rect x="0" y="0" width="1440" height="600" fill="url(#heroSky)"/>
            <polygon points="0,340 100,280 220,320 340,260 480,300 620,240 760,290 900,250 1040,300 1180,260 1320,300 1440,270 1440,600 0,600" fill="#124f59" opacity="0.75"/>
            <polygon points="0,440 60,310 160,440" fill="#0f434c"/>
            <polygon points="120,430 260,190 262,430" fill="#123f47"/>
            <polygon points="262,430 260,190 430,430" fill="#2c8f89"/>
            <polygon points="560,440 740,90 744,440" fill="#0f333c"/>
            <polygon points="744,440 740,90 950,440" fill="#3aa79f"/>
            <polygon points="700,150 740,90 780,155" fill="#d7f3e4" opacity="0.85"/>
            <polygon points="1000,440 1150,210 1152,440" fill="#123f47"/>
            <polygon points="1152,440 1150,210 1300,440" fill="#2c8f89"/>
            <polygon points="1300,440 1380,300 1440,440" fill="#0f434c"/>
            <polygon points="0,460 40,430 70,455 110,420 150,450 190,415 230,448 270,410 310,445 350,418 390,450 430,412 470,447 510,420 550,452 590,415 630,448 670,420 710,450 750,418 790,447 830,412 870,450 910,420 950,448 990,415 1030,452 1070,418 1110,447 1150,412 1190,450 1230,420 1270,448 1310,415 1350,450 1390,420 1430,448 1440,440 1440,600 0,600" fill="#08222b"/>
        </svg>
    </div>

    <div class="hero__content">
        <h1 class="display hero__title">Добро пожаловать в BUYSELL</h1>
        <p class="hero__subtitle">Покупайте у соседей и продавцов из других городов — с доставкой в любую точку страны.</p>
        <a class="btn hero__cta" href="#products-list">Смотреть товары</a>
    </div>
</div>

<div class="shell" style="padding-left:0;padding-right:0;">

    <div class="route-strip">
        <div class="route-line mono">
            <span class="stop">Москва</span><span class="seg"></span>
            <span class="stop">Казань</span><span class="seg"></span>
            <span class="stop">Екатеринбург</span><span class="seg"></span>
            <span class="stop">Новосибирск</span><span class="seg"></span>
            <span class="stop">Владивосток</span>
        </div>
    </div>

    <div class="filters" id="products-list">
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
