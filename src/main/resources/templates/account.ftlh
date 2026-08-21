<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Аккаунт"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="shell-narrow" style="padding-top:24px;">
    <div class="page-head" style="padding-left:0;padding-right:0;">
        <h1 class="display">Аккаунт</h1>
        <p>Управляйте своими данными и настройками.</p>
    </div>

    <#if errorMessage??>
        <div class="alert">${errorMessage}</div>
    </#if>
    <#if successMessage??>
        <div class="alert alert--ok">${successMessage}</div>
    </#if>

    <section class="panel account-panel">
        <div class="panel__head">
            <h3>Профиль</h3>
            <p>Имя и телефон видят другие пользователи на странице ваших объявлений.</p>
        </div>
        <form action="/account" method="post">
            <div class="form-field">
                <label for="name">Имя</label>
                <input type="text" id="name" name="name" value="${currentUser.name}">
            </div>
            <div class="form-field">
                <label for="phoneNumber">Номер телефона</label>
                <input type="text" id="phoneNumber" name="phoneNumber" value="${currentUser.phoneNumber!""}">
            </div>
            <div class="form-field">
                <label for="email">Email</label>
                <input type="text" id="email" value="${currentUser.email}" disabled>
            </div>
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <div class="form-actions">
                <button class="btn accent" type="submit">Сохранить</button>
            </div>
        </form>
    </section>

    <section class="panel account-panel">
        <div class="panel__head">
            <h3>Аватар</h3>
            <p>Отображается рядом с вашим именем в шапке сайта.</p>
        </div>
        <form action="/account/avatar" method="post" enctype="multipart/form-data">
            <div class="avatar-upload">
                <#if currentUser.avatar??>
                    <span class="account-avatar account-avatar--lg" id="avatarPreview"><img src="/images/${currentUser.avatar.id}" alt=""></span>
                <#else>
                    <#assign initial = (currentUser.name?has_content)?then(currentUser.name?substring(0,1)?upper_case, "?")>
                    <span class="account-avatar account-avatar--lg" id="avatarPreview">${initial}</span>
                </#if>
                <div class="form-field">
                    <label for="avatar">Новое изображение</label>
                    <input type="file" id="avatar" name="avatar" accept="image/*" data-avatar-preview="avatarPreview">
                </div>
            </div>
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <div class="form-actions">
                <button class="btn accent" type="submit">Загрузить</button>
            </div>
        </form>
    </section>

    <section class="panel account-panel">
        <div class="panel__head">
            <h3>Пароль</h3>
            <p>Придумайте новый пароль, чтобы защитить аккаунт.</p>
        </div>
        <form action="/account/password" method="post">
            <div class="form-field">
                <label for="currentPassword">Текущий пароль</label>
                <input type="password" id="currentPassword" name="currentPassword" autocomplete="current-password" required>
            </div>
            <div class="form-field">
                <label for="newPassword">Новый пароль</label>
                <input type="password" id="newPassword" name="newPassword" autocomplete="new-password" minlength="6" required>
            </div>
            <div class="form-field">
                <label for="confirmPassword">Повторите новый пароль</label>
                <input type="password" id="confirmPassword" name="confirmPassword" autocomplete="new-password" minlength="6" required data-match="#newPassword">
            </div>
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <div class="form-actions">
                <button class="btn accent" type="submit">Сменить пароль</button>
            </div>
        </form>
    </section>

    <section class="panel account-panel account-panel--danger">
        <div class="panel__head">
            <h3>Опасная зона</h3>
            <p>Удаление аккаунта необратимо — вместе с ним пропадут все ваши объявления.</p>
        </div>
        <form action="/account/delete" method="post" data-confirm="Удалить аккаунт ${currentUser.email}? Это действие необратимо.">
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <div class="form-actions">
                <button class="btn danger" type="submit">Удалить аккаунт</button>
            </div>
        </form>
    </section>
</div>

<@c.footer/>

</body>
</html>
