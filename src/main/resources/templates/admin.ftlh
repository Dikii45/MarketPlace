<!DOCTYPE html>
<html>
<head>
    <title>BUYSELL</title>
</head>
<body>
<h1>BUYSELL</h1>
<h4>Привет админ</h4>

<style>
    table, th, td {
        border: 1px solid black;
    }
</style>

<table>

    <tr>
        <th>Email</th>
        <th>Номер телефона</th>
        <th>Активность</th>
        <th>Роли</th>
        <th>Бан</th>
        <th>Редоктирование</th>
        <th>Подробная информация</th>
    </tr>
    <#list users as user>
    <tr>
        <th>${user.email}</th>
        <th>${user.phoneNumber!"Не указан"}</th>
        <th>${user.active?c}</th>
        <td>
            <#list user.roles as role>
                ${role}
            </#list>
        </td>
        <th>
            <form action="/admin/user/ban/${user.id}" method="post">
                <input type="hidden" name="_csrf" value="${_csrf.token}">
                <input type="submit" value="Бан">
            </form>
        </th>
        <th><a href="/admin/user/edit/${user.id}">Редактирование</a></th>
        <th><a href="/user/${user.id}">Подробная инфомация</a></th>
        </#list>
</table>
</body>
</html>