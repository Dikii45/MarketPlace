<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Вход"/>
</head>
<body>

<@c.topbar showSearch=false/>

<div class="auth-shell">
    <div class="auth-card">
        <a class="auth-card__logo display" href="/"><span class="postmark"></span>BUYSELL</a>
        <h1>Авторизация</h1>

        <#if errorMessage??>
            <div class="alert">${errorMessage}</div>
        </#if>

        <form action="/login" method="post">
            <div class="form-field">
                <label for="username">Эл. почта</label>
                <input type="email" id="username" name="username">
            </div>
            <div class="form-field">
                <label for="password">Пароль</label>
                <input type="password" id="password" name="password">
            </div>
            <input type="hidden" name="_csrf" value="${_csrf.token}">
            <button class="btn accent" type="submit">Вход</button>
        </form>
        <div class="auth-card__foot">
            Ещё нет аккаунта? <a href="/registration">Зарегистрироваться</a>
        </div>
    </div>
</div>

</body>
</html>
