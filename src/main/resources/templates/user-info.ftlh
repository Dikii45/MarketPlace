<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle=user.name/>
</head>
<body>

<@c.topbar currentUser=currentUser/>

<div class="page-head">
    <h1 class="display">${user.name}</h1>
    <p><b>Email:</b> ${user.email} · <b>Телефон:</b> ${user.phoneNumber!"Не указан"}</p>
</div>

<#if errorMessage??>
    <div class="alert" style="margin:0 28px;">${errorMessage}</div>
</#if>

<div class="filters">
    <h2>Товары пользователя ${user.name}</h2>
    <span class="count mono">${products?size} объявлений</span>
</div>

<#if currentUser?? && currentUser.id == user.id && !currentUser.isAdmin()>
<details class="add-product">
    <summary class="btn accent">Добавить товар</summary>
    <form class="add-product__form" action="/product/create" method="post" enctype="multipart/form-data">
        <div class="form-grid">
            <div class="form-field span-2">
                <label for="title">Название</label>
                <input type="text" id="title" name="title" required>
            </div>
            <div class="form-field span-2">
                <label for="description">Описание</label>
                <textarea id="description" name="description" required></textarea>
            </div>
            <div class="form-field">
                <label for="price">Цена</label>
                <input type="number" id="price" name="price" min="0" required>
            </div>
            <div class="form-field">
                <label for="quantity">Количество</label>
                <input type="number" id="quantity" name="quantity" min="1" value="1" required>
            </div>
            <div class="form-field">
                <label for="city">Город</label>
                <input type="text" id="city" name="city" required>
            </div>
            <div class="form-field span-2">
                <label for="files">Фото (до 10 шт.)</label>
                <input type="file" id="files" name="files" accept="image/*" multiple required data-max-files="10" data-preview="filesPreview">
                <div class="file-preview" id="filesPreview"></div>
            </div>
        </div>
        <input type="hidden" name="_csrf" value="${_csrf.token}">
        <button class="btn accent" type="submit">Опубликовать</button>
    </form>
</details>
</#if>

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

</body>
</html>
