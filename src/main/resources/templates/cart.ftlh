<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Корзина"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="page-head">
    <h1 class="display">Корзина<#if cartItems?has_content><span class="count-badge mono">${cartItems?size}</span></#if></h1>
    <p>Товары, отобранные для заказа.</p>
</div>

<#if errorMessage??>
    <div class="alert" style="margin:0 28px;">${errorMessage}</div>
</#if>

<#if cartItems?has_content>
<div class="cart-layout">
    <div class="cart-items">
        <#list cartItems as item>
            <div class="cart-card">
                <a class="cart-card__photo" href="/product/${item.product.id}">
                    <#if item.product.previewImageId??>
                        <img src="/images/${item.product.previewImageId}" alt="${item.product.title}">
                    <#elseif item.product.images?has_content>
                        <img src="/images/${item.product.images[0].id}" alt="${item.product.title}">
                    <#else>
                        <svg class="icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="7" y="10" width="34" height="28" rx="3"/><circle cx="17" cy="20" r="3"/><path d="M11 33l10-9 8 7 6-6 8 7"/>
                        </svg>
                    </#if>
                </a>
                <div class="cart-card__body">
                    <a class="cart-card__title" href="/product/${item.product.id}">${item.product.title}</a>
                    <div class="cart-card__meta mono">${item.product.city} · ${item.product.user.name}</div>
                    <form action="/cart/remove/${item.product.id}" method="post">
                        <input type="hidden" name="_csrf" value="${_csrf.token}">
                        <button class="cart-card__remove" type="submit">Убрать</button>
                    </form>
                </div>

                <div class="qty-stepper">
                    <form action="/cart/update/${item.product.id}" method="post">
                        <input type="hidden" name="_csrf" value="${_csrf.token}">
                        <input type="hidden" name="quantity" value="${item.quantity - 1}">
                        <button class="qty-btn" type="submit" aria-label="Меньше" <#if (item.quantity <= 1)>disabled</#if>>−</button>
                    </form>
                    <span class="qty-value mono">${item.quantity}</span>
                    <form action="/cart/update/${item.product.id}" method="post">
                        <input type="hidden" name="_csrf" value="${_csrf.token}">
                        <input type="hidden" name="quantity" value="${item.quantity + 1}">
                        <button class="qty-btn" type="submit" aria-label="Больше" <#if (item.quantity >= item.product.quantity)>disabled</#if>>+</button>
                    </form>
                </div>

                <div class="cart-card__price mono">${(item.product.price * item.quantity)?string("#,##0")} ₽</div>
            </div>
        </#list>
    </div>

    <aside class="order-summary">
        <h3>Детали заказа</h3>
        <div class="order-summary__row">
            <span>${cartItems?size} шт.</span>
            <span class="mono">${cartTotal?string("#,##0")} ₽</span>
        </div>
        <div class="order-summary__total">
            <span>Итого</span>
            <span class="mono">${cartTotal?string("#,##0")} ₽</span>
        </div>
        <a class="btn accent btn-block" href="/checkout">Перейти к оформлению</a>
    </aside>
</div>

<#else>
<div class="empty-state">Корзина пуста</div>
</#if>

<@c.footer/>

</body>
</html>
