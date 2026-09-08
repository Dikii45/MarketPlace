# BUYSELL

[![CI/CD](https://github.com/Dikii45/MarketPlace/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Dikii45/MarketPlace/actions/workflows/ci-cd.yml)

Веб-маркетплейс на Spring Boot: пользователи публикуют объявления о товарах, покупатели собирают корзину и оформляют заказ, продавцы ведут заказы по статусам.

## Архитектура

Подробный разбор слоёв, модели данных (ER-диаграмма), карты эндпоинтов и известных инженерных решений — в [`docs/architecture.md`](docs/architecture.md).

## Стек

- Java 21, Spring Boot 4
- Spring Security — сессия и форма логина для сайта, отдельно JWT (JJWT) для REST API
- Spring Data JPA + Hibernate
- MySQL 8 (в проде/докере), H2 in-memory (в тестах)
- FreeMarker (серверный рендеринг шаблонов)
- springdoc-openapi (Swagger UI) — документация REST API
- JUnit 5, Mockito, AssertJ — тестовый набор
- Lombok
- Maven (`mvnw`)
- Docker / Docker Compose, GitHub Actions (CI/CD)

## Возможности

- Регистрация и вход, роли пользователей (обычный пользователь / админ)
- Публикация товара с загрузкой нескольких изображений, учёт остатка (quantity) и категорией
- Поиск товаров по названию, фильтры по категории и диапазону цен, сортировка по цене — на главной и в поиске
- Скрытие товаров с нулевым остатком
- Корзина: добавление, изменение количества, удаление
- Оформление заказа: адрес доставки, способ оплаты (наличными / картой)
- Заказы разбиваются по продавцам (`OrderItem`), у каждой позиции свой статус:
  `NEW → CONFIRMED → SENT → RECEIVED`, либо `CANCELLED`
- Страница «Заказы» отдельно показывает покупки и продажи, с делением на текущие/завершённые
- Пополнение остатка товара продавцом (restock)
- Мягкое удаление товара (`deleted`-флаг) — без потери истории заказов
- Админ-панель: управление пользователями и ролями
- Чат между покупателем и продавцом прямо на сайте (AJAX-опрос, без перезагрузки страницы)
- Регистрация: email только с популярных почтовых сервисов, телефон — только российский формат
- REST API для каталога товаров с авторизацией по JWT (см. ниже) — задел под будущее внешнее приложение
- На пустой базе (например, самый первый запуск после клонирования) каталог сам наполняется демо-товарами с фото, чтобы сайт не выглядел пустым

## Запуск через Docker (проще всего)

Нужен только установленный [Docker Desktop](https://www.docker.com/products/docker-desktop/) — Java, Maven и MySQL ставить не надо, всё поднимается в контейнерах.

```bash
git clone https://github.com/Dikii45/MarketPlace.git
cd MarketPlace
docker compose up --build
```

Первый запуск соберёт образ и поднимет MySQL — займёт пару минут. Дальше сайт на [http://localhost:8081](http://localhost:8081). На пустой базе приложение само создаёт ~45 демо-товаров с фото (см. «Возможности» выше), так что каталог сразу не пустой. Данные MySQL сохраняются между перезапусками в volume `db-data`; `docker compose down -v` — полностью снести и начать с чистой базы.

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
3. Запустить приложение (нужен JDK 21) — больше ничего ставить не надо, достаточно самого файла `mvnw` из репозитория:
   ```bash
   ./mvnw spring-boot:run       # Linux/macOS/Git Bash
   mvnw.cmd spring-boot:run     # Windows (cmd или PowerShell)
   ```
4. Открыть [http://localhost:8081](http://localhost:8081). На пустой базе каталог сам наполнится демо-товарами при первом старте.

## REST API

Отдельно от основного сайта — JSON API для каталога товаров, с авторизацией по JWT вместо cookie-сессии (задел на случай отдельного/мобильного клиента в будущем).

Интерактивная документация — **Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)** (спецификация — `/v3/api-docs`).

```bash
# получить токен
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@gmail.com","password":"пароль"}'

# использовать его в защищённых запросах
curl -X DELETE http://localhost:8081/api/products/123 \
  -H "Authorization: Bearer <token>"
```

| Метод | Путь | Доступ |
|---|---|---|
| POST | `/api/auth/login` | все |
| GET | `/api/products` | все (`?title=&category=&minPrice=&maxPrice=&sort=price_asc\|price_desc`) |
| GET | `/api/products/{id}` | все |
| POST | `/api/products` | по токену |
| DELETE | `/api/products/{id}` | по токену, владелец/админ |
| PATCH | `/api/products/{id}/restock?amount=N` | по токену, владелец/админ |

## Тестирование

```bash
./mvnw test
```

60+ тестов на четырёх уровнях:

- **Unit** (`ProductServiceTest`, `CartItemServiceTest`, `OrderServiceTest`, `UserServiceTest`) — бизнес-логика на моках (Mockito), без Spring-контекста и БД
- **Repository** (`@DataJpaTest`) — что `ProductRepository.search()` реально фильтрует по названию/категории/цене и сортирует так, как ожидается
- **Integration** (`CheckoutFlowIntegrationTest`) — полный путь товар → корзина → checkout → списание остатка → `Order`/`OrderItem`, на реальных бинах поверх H2
- **Controller** (`ProductControllerSecurityTest`) — проверка авторизации через настоящий HTTP-слой (MockMvc + Spring Security)

Тесты идут против H2 in-memory (`src/test/resources/application.properties`), а не реального MySQL — быстро и не трогает рабочую базу.

## CI/CD

На каждый push и pull request в `main` GitHub Actions (`.github/workflows/ci-cd.yml`) собирает проект и гоняет весь тестовый набор (юнит + repository + integration + controller, все на H2 — реальный MySQL в CI не нужен). При успешных тестах на push в `main` дополнительно собирается и публикуется Docker-образ в GitHub Container Registry:

```bash
docker pull ghcr.io/dikii45/marketplace:latest
```

## Структура проекта

```
controllers/       — HTTP-эндпоинты сайта (товары, корзина, заказы, пользователи, чат, админка)
controllers/rest/  — REST-контроллеры API (/api/**)
dto/                — плоские объекты для JSON-ответов API
security/          — JwtService и JwtAuthenticationFilter
seed/              — автонаполнение пустой БД демо-товарами при первом запуске
services/          — бизнес-логика
repositories/      — Spring Data JPA репозитории
models/            — сущности (Product, User, CartItem, Order, OrderItem, ChatMessage, Image...)
configurations/    — SecurityConfig (сессия для сайта + отдельная цепочка для JWT), OpenApiConfig
resources/templates/ — FreeMarker-шаблоны страниц
resources/static/    — CSS, JS
```

Подробное описание архитектуры корзины/заказов — в [`docs/cart-checkout-tz.md`](docs/cart-checkout-tz.md).
