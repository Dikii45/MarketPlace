<#import "common.ftlh" as c>
<!DOCTYPE html>
<html lang="ru">
<head>
    <@c.head subtitle="Редактирование пользователя"/>
</head>
<body>

<@c.topbar currentUser=currentUser showSearch=false/>

<div class="shell-narrow" style="padding-top:24px;">
    <div class="page-head" style="padding-left:0;padding-right:0;">
        <h1 class="display">Редактирование пользователя ${user.name}</h1>
        <p>Выберите роли, доступные этому аккаунту.</p>
    </div>

    <form action="/admin/user/edit" method="post">
        <div class="role-list">
            <#list roles as role>
                <label class="role-option">
                    <input type="checkbox" name="${role}" ${user.roles?seq_contains(role)?string("checked", "")}>
                    ${role}
                </label>
            </#list>
        </div>
        <input type="hidden" value="${user.id}" name="userId">
        <input type="hidden" value="${_csrf.token}" name="_csrf">
        <button class="btn accent" type="submit">Сохранить</button>
    </form>
</div>

<@c.footer/>

</body>
</html>
