<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Оформление заказа"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="page-head">
    <h1 class="display">Оформление заказа</h1>
    <p>Укажите адрес доставки и способ оплаты.</p>
</div>

<#if errorMessage??>
    <div class="alert" style="margin:0 28px;">${errorMessage}</div>
</#if>

<div class="checkout-grid">
    <div class="checkout-main">
        <form id="checkoutForm" action="/checkout" method="post">
            <input type="hidden" name="_csrf" value="${_csrf.token}">

            <div class="checkout-card">
                <h3 class="checkout-card__title">Адрес доставки</h3>
                <textarea id="address" name="address" required placeholder="Город, улица, дом, квартира"></textarea>
            </div>
        </form>

        <div class="checkout-card">
            <h3 class="checkout-card__title">Способ оплаты</h3>
            <div class="payment-options">
                <label class="payment-option">
                    <input type="radio" name="paymentMethod" value="CASH" form="checkoutForm" checked>
                    <span class="payment-option__title">Наличными</span>
                    <span class="payment-option__desc">При получении</span>
                </label>
                <label class="payment-option">
                    <input type="radio" name="paymentMethod" value="CARD" form="checkoutForm">
                    <span class="payment-option__title">Картой</span>
                    <span class="payment-option__desc">Онлайн-оплата</span>
                </label>
            </div>
        </div>

        <div class="checkout-card">
            <h3 class="checkout-card__title">Товары в заказе</h3>
            <div class="checkout-items">
                <#list cartItems as item>
                    <div class="checkout-item">
                        <div class="checkout-item__photo">
                            <#if item.product.previewImageId??>
                                <img src="/images/${item.product.previewImageId}" alt="${item.product.title}">
                            <#elseif item.product.images?has_content>
                                <img src="/images/${item.product.images[0].id}" alt="${item.product.title}">
                            <#else>
                                <svg class="icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="2">
                                    <rect x="7" y="10" width="34" height="28" rx="3"/><circle cx="17" cy="20" r="3"/><path d="M11 33l10-9 8 7 6-6 8 7"/>
                                </svg>
                            </#if>
                        </div>
                        <div class="checkout-item__body">
                            <div class="checkout-item__title">${item.product.title}</div>
                            <div class="checkout-item__meta mono">${item.quantity} шт.</div>
                        </div>
                        <div class="checkout-item__price mono">${(item.product.price * item.quantity)?string("#,##0")} ₽</div>
                    </div>
                </#list>
            </div>
        </div>
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
        <button class="btn accent btn-block" type="submit" form="checkoutForm">Подтвердить</button>
    </aside>
</div>

<@c.footer/>

</body>
</html>
