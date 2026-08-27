<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Заказы"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="page-head">
    <h1 class="display">Заказы</h1>
    <p>Ваши покупки и продажи.</p>
</div>

<#if errorMessage??>
    <div class="alert" style="margin:0 28px;">${errorMessage}</div>
</#if>
<#if successMessage??>
    <div class="alert alert--ok" style="margin:0 28px;">${successMessage}</div>
</#if>

<#function isTerminal status>
    <#return status.name() == "RECEIVED" || status.name() == "CANCELLED">
</#function>

<#-- одна карточка заказа (покупки) -->
<#macro orderCard order variant>
<#assign hasCancelled = false>
<#list order.items as item>
    <#if item.status.name() == "CANCELLED"><#assign hasCancelled = true></#if>
</#list>
<div class="order-card">
    <div class="order-card__head">
        <span class="order-card__id">
            <#if variant == "current">
                <span class="order-status-icon order-status-icon--pending" title="В обработке">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.5 2"/></svg>
                </span>
            <#elseif hasCancelled>
                <span class="order-status-icon order-status-icon--cancelled" title="Отменён">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M12 8v5"/><circle cx="12" cy="16" r="0.5" fill="currentColor"/></svg>
                </span>
            <#else>
                <span class="order-status-icon order-status-icon--done" title="Завершён">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.5 2.5L16 9.5"/></svg>
                </span>
            </#if>
            <span class="mono">Заказ #${order.id}</span>
        </span>
        <span class="mono">${order.dateOfCreated?substring(0,16)?replace("T"," ")}</span>
    </div>
    <div class="order-card__meta">
        <span><b>Адрес:</b> ${order.deliveryAddress}</span>
        <span><b>Оплата:</b> ${order.paymentMethod} · <span class="status-dot ${order.paymentStatus?lower_case}">${order.paymentStatus}</span></span>
    </div>
    <div class="table-scroll">
        <table class="data">
            <thead>
            <tr>
                <th>Товар</th>
                <th>Кол-во</th>
                <th>Продавец</th>
                <th>Статус</th>
            </tr>
            </thead>
            <tbody>
            <#list order.items as item>
                <tr>
                    <td><a class="btn-sm ghost" href="/product/${item.product.id}">${item.product.title}</a></td>
                    <td class="mono">${item.quantity}</td>
                    <td>${item.product.user.name}</td>
                    <td><span class="status-dot ${item.status?lower_case}">${item.status}</span></td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>
</#macro>

<#-- одна строка продажи -->
<#macro saleRow item statuses>
<tr>
    <td><a class="btn-sm ghost" href="/product/${item.product.id}">${item.product.title}</a></td>
    <td class="mono">${item.quantity}</td>
    <td>${item.order.buyer.name}</td>
    <td class="addr-cell">${item.order.deliveryAddress}</td>
    <td>
        <#if item.status.name() == "CANCELLED">
            <span class="status-dot ${item.status?lower_case}">${item.status}</span>
        <#else>
        <form class="order-status-form" action="/orders/items/${item.id}/status" method="post">
            <select name="status">
                <#list statuses as s>
                    <#assign isCurrent = (s == item.status)>
                    <#assign canMoveTo = false>
                    <#-- допустимые переходы статуса — должны совпадать с OrderService.updateStatus -->
                    <#switch item.status.name()>
                        <#case "NEW">
                            <#assign canMoveTo = (s.name() != "NEW")>
                            <#break>
                        <#case "CONFIRMED">
                            <#assign canMoveTo = (s.name() != "CONFIRMED" && s.name() != "NEW")>
                            <#break>
                        <#case "SENT">
                            <#assign canMoveTo = (s.name() == "RECEIVED" || s.name() == "CANCELLED")>
                            <#break>
                        <#case "RECEIVED">
                            <#assign canMoveTo = (s.name() == "CANCELLED")>
                            <#break>
                    </#switch>
                    <option value="${s}" <#if isCurrent>selected</#if> <#if !isCurrent && !canMoveTo>disabled</#if>>${s}</option>
                </#list>
            </select>
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <button class="btn-sm accent" type="submit">Сохранить</button>
        </form>
        </#if>
    </td>
    <td class="mono">${item.order.dateOfCreated?substring(0,16)?replace("T"," ")}</td>
</tr>
</#macro>

<#-- покупки: заказ считается завершённым, только если завершены ВСЕ его позиции -->
<#assign currentPurchases = []>
<#assign completedPurchases = []>
<#list myPurchases as order>
    <#assign allDone = true>
    <#list order.items as item>
        <#if !isTerminal(item.status)>
            <#assign allDone = false>
        </#if>
    </#list>
    <#if allDone>
        <#assign completedPurchases = completedPurchases + [order]>
    <#else>
        <#assign currentPurchases = currentPurchases + [order]>
    </#if>
</#list>

<#-- продажи: по статусу самой позиции -->
<#assign currentSales = []>
<#assign completedSales = []>
<#list mySales as item>
    <#if isTerminal(item.status)>
        <#assign completedSales = completedSales + [item]>
    <#else>
        <#assign currentSales = currentSales + [item]>
    </#if>
</#list>

<input type="radio" name="ordersTab" id="tab-purchases" class="tab-input" checked>
<input type="radio" name="ordersTab" id="tab-sales" class="tab-input">

<div class="tabs">
    <label class="tab-label" for="tab-purchases">
        Покупки
        <#if myPurchases?has_content><span class="count-badge mono">${myPurchases?size}</span></#if>
    </label>
    <label class="tab-label" for="tab-sales">
        Продажи
        <#if mySales?has_content><span class="count-badge mono">${mySales?size}</span></#if>
    </label>
</div>

<div class="tab-panel" id="panel-purchases">
    <#if myPurchases?has_content>
        <#if currentPurchases?has_content>
        <h3 class="sub-heading">Текущие</h3>
        <div class="order-list">
            <#list currentPurchases as order>
                <@orderCard order=order variant="current"/>
            </#list>
        </div>
        </#if>
        <#if completedPurchases?has_content>
        <h3 class="sub-heading">Завершённые</h3>
        <div class="order-list">
            <#list completedPurchases as order>
                <@orderCard order=order variant="completed"/>
            </#list>
        </div>
        </#if>
    <#else>
    <div class="empty-state">Вы пока ничего не заказали</div>
    </#if>
</div>

<div class="tab-panel" id="panel-sales">
    <#if mySales?has_content>
        <p class="tab-panel__hint">Меняйте статус, чтобы покупатель видел прогресс: подтвердили → отправили → доставлено.</p>
        <#if currentSales?has_content>
        <h3 class="sub-heading">Текущие</h3>
        <div class="table-card">
            <div class="table-scroll">
                <table class="data">
                    <thead>
                    <tr>
                        <th>Товар</th>
                        <th>Кол-во</th>
                        <th>Покупатель</th>
                        <th>Адрес доставки</th>
                        <th>Статус</th>
                        <th>Дата</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list currentSales as item>
                        <@saleRow item=item statuses=statuses/>
                    </#list>
                    </tbody>
                </table>
            </div>
        </div>
        </#if>
        <#if completedSales?has_content>
        <h3 class="sub-heading">Завершённые</h3>
        <div class="table-card">
            <div class="table-scroll">
                <table class="data">
                    <thead>
                    <tr>
                        <th>Товар</th>
                        <th>Кол-во</th>
                        <th>Покупатель</th>
                        <th>Адрес доставки</th>
                        <th>Статус</th>
                        <th>Дата</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list completedSales as item>
                        <@saleRow item=item statuses=statuses/>
                    </#list>
                    </tbody>
                </table>
            </div>
        </div>
        </#if>
    <#else>
    <div class="empty-state">У вас пока нет продаж</div>
    </#if>
</div>

<@c.footer/>

</body>
</html>
