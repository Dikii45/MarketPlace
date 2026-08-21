<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Панель администратора"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="page-head">
    <h1 class="display">Панель администратора</h1>
    <p>Пользователи, роли и модерация аккаунтов.</p>
</div>

<#if errorMessage??>
    <div class="alert">${errorMessage}</div>
</#if>
<#if successMessage??>
    <div class="alert alert--ok">${successMessage}</div>
</#if>

<div class="table-card">
    <div class="table-scroll">
        <table class="data">
            <thead>
            <tr>
                <th>Email</th>
                <th>Номер телефона</th>
                <th>Активность</th>
                <th>Роли</th>
                <th>Редактирование активности</th>
                <th>Редактирование Роли</th>
                <th>Подробная информация(Перейти на строничку)</th>
                <th>Удаление</th>
            </tr>
            </thead>
            <tbody>
            <#list users as user>
                <tr>
                    <td>${user.email}</td>
                    <td>${user.phoneNumber!"Не указан"}</td>
                    <td>
                        <#if user.active>
                            <span class="status-dot active">Активен</span>
                        <#else>
                            <span class="status-dot banned">Забанен</span>
                        </#if>
                    </td>
                    <td>
                        <#list user.roles as role>
                            <span class="role-badge">${role}</span>
                        </#list>
                    </td>
                    <td>
                        <form action="/admin/user/ban/${user.id}" method="post">
                            <input type="hidden" name="_csrf" value="${_csrf.token}">
                            <button class="btn-sm danger" type="submit">Изменить активность</button>
                        </form>
                    </td>
                    <td><a class="btn-sm ghost" href="/admin/user/edit/${user.id}">Редактирование</a></td>
                    <td><a class="btn-sm ghost" href="/user/${user.id}">Подробная информация</a></td>
                    <td>
                        <form action="/admin/user/delete/${user.id}" method="post" data-confirm="Удалить аккаунт ${user.email}? Это действие необратимо.">
                            <input type="hidden" name="_csrf" value="${_csrf.token}">
                            <button class="btn-sm danger" type="submit">Удалить</button>
                        </form>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>

<@c.footer/>

</body>
</html>
