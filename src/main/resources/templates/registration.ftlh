<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Регистрация"/>
</head>
<body>

<@c.topbar showSearch=false/>

<div class="auth-shell">
    <div class="auth-card">
        <a class="auth-card__logo display" href="/"><span class="postmark"></span>BUYSELL</a>
        <h1>Регистрация</h1>

        <#if errorMessage??>
            <div class="alert">${errorMessage}</div>
        </#if>

        <form action="/registration" method="post">
            <div class="form-field">
                <label for="name">Имя пользователя</label>
                <input type="text" id="name" name="name">
            </div>
            <div class="form-field">
                <label for="email">Эл. почта</label>
                <input type="email" id="email" name="email">
            </div>
            <div class="form-field">
                <label for="phoneNumber">Номер телефона</label>
                <input type="text" id="phoneNumber" name="phoneNumber">
            </div>
            <div class="form-field">
                <label for="password">Пароль</label>
                <input type="password" id="password" name="password">
            </div>
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <button class="btn accent" type="submit">Зарегистрироваться</button>
        </form>
        <div class="auth-card__foot">
            Уже есть аккаунт? <a href="/login">Войти</a>
        </div>
    </div>
</div>

</body>
</html>
