# BUYSELL

Веб-маркетплейс на Spring Boot: пользователи публикуют объявления о товарах, покупатели собирают корзину и оформляют заказ, продавцы ведут заказы по статусам.

## Архитектура

Подробный разбор слоёв, модели данных (ER-диаграмма), карты эндпоинтов и известных инженерных решений — в [`docs/architecture.md`](docs/architecture.md).

## Стек

- Java 21, Spring Boot 4
- Spring Security (аутентификация по логину/паролю, роли)
- Spring Data JPA + Hibernate
- MySQL 8
- FreeMarker (серверный рендеринг шаблонов)
- Lombok
- Maven (`mvnw`)

## Возможности

- Регистрация и вход, роли пользователей (обычный пользователь / админ)
- Публикация товара с загрузкой нескольких изображений, учёт остатка (quantity)
- Поиск товаров по названию, скрытие товаров с нулевым остатком
- Корзина: добавление, изменение количества, удаление
- Оформление заказа: адрес доставки, способ оплаты (наличными / картой)
- Заказы разбиваются по продавцам (`OrderItem`), у каждой позиции свой статус:
  `NEW → CONFIRMED → SENT → RECEIVED`, либо `CANCELLED`
- Страница «Заказы» отдельно показывает покупки и продажи, с делением на текущие/завершённые
- Пополнение остатка товара продавцом (restock)
- Мягкое удаление товара (`deleted`-флаг) — без потери истории заказов
- Админ-панель: управление пользователями и ролями
- Общение продовец-закзчик происходит через чат, который встроен в сайт

## Запуск через Docker (проще всего)

Нужен только установленный [Docker Desktop](https://www.docker.com/products/docker-desktop/) — Java, Maven и MySQL ставить не надо, всё поднимается в контейнерах.

```bash
git clone https://github.com/Dikii45/MarketPlace.git
cd MarketPlace
docker compose up --build
```

Первый запуск соберёт образ и поднимет MySQL — займёт пару минут. Дальше сайт на [http://localhost:8081](http://localhost:8081). Данные MySQL сохраняются между перезапусками в volume `db-data`; `docker compose down -v` — полностью снести и начать с чистой базы.

## Запуск локально (без Docker)

1. Поднять MySQL и создать базу:
   ```sql
   CREATE DATABASE buysell;
   ```
2. Указать свои данные подключения в `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/buysell
   spring.datasource.username=root
   spring.datasource.password=<пароль>
   ```
   Схема создаётся/обновляется автоматически (`spring.jpa.hibernate.ddl-auto=update`).
3. Запустить приложение (нужен JDK 21):
   ```bash
   ./mvnw spring-boot:run
   ```
4. Открыть [http://localhost:8081](http://localhost:8081).

## Структура проекта

```
controllers/   — HTTP-эндпоинты (товары, корзина, заказы, пользователи, админка)
services/      — бизнес-логика
repositories/  — Spring Data JPA репозитории
models/        — сущности (Product, User, CartItem, Order, OrderItem, Image...)
configurations/ — SecurityConfig и прочая конфигурация
resources/templates/ — FreeMarker-шаблоны страниц
resources/static/    — CSS
```

Подробное описание архитектуры корзины/заказов — в [`docs/cart-checkout-tz.md`](docs/cart-checkout-tz.md).
