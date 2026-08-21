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

<div class="filters">
    <h2>Мои покупки</h2>
</div>

<#if myPurchases?has_content>
<div class="table-card">
    <div class="table-scroll">
        <table class="data">
            <thead>
            <tr>
                <th>Товар</th>
                <th>Продавец</th>
                <th>Статус</th>
                <th>Дата</th>
            </tr>
            </thead>
            <tbody>
            <#list myPurchases as order>
                <tr>
                    <td><a class="btn-sm ghost" href="/product/${order.product.id}">${order.product.title}</a></td>
                    <td>${order.product.user.name}</td>
                    <td><span class="status-dot ${order.status?lower_case}">${order.status}</span></td>
                    <td class="mono">${order.dateOfCreated?substring(0,16)?replace("T"," ")}</td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>
<#else>
<div class="empty-state">Вы пока ничего не заказали</div>
</#if>

<div class="filters">
    <h2>Мои продажи</h2>
</div>

<#if mySales?has_content>
<div class="table-card">
    <div class="table-scroll">
        <table class="data">
            <thead>
            <tr>
                <th>Товар</th>
                <th>Покупатель</th>
                <th>Статус</th>
                <th>Дата</th>
            </tr>
            </thead>
            <tbody>
            <#list mySales as order>
                <tr>
                    <td><a class="btn-sm ghost" href="/product/${order.product.id}">${order.product.title}</a></td>
                    <td>${order.buyer.name}</td>
                    <td>
                        <form class="order-status-form" action="/orders/${order.id}/status" method="post">
                            <select name="status">
                                <#list statuses as s>
                                    <option value="${s}" <#if order.status == s>selected</#if>>${s}</option>
                                </#list>
                            </select>
                            <input type="hidden" name="_csrf" value="${_csrf.token}">
                            <button class="btn-sm accent" type="submit">Сохранить</button>
                        </form>
                    </td>
                    <td class="mono">${order.dateOfCreated?substring(0,16)?replace("T"," ")}</td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>
<#else>
<div class="empty-state">У вас пока нет продаж</div>
</#if>

<@c.footer/>

</body>
</html>
