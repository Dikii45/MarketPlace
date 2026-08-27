# ТЗ: Корзина и оформление заказа

Статус: реализовано и проверено end-to-end (корзина → чекаут → заказы у покупателя и продавца).

## 1. Обзор и пользовательский сценарий

1. Пользователь просматривает товар и нажимает **«В корзину»** (на странице товара).
2. Открывает корзину (`/cart`) — видит список добавленных товаров, может убрать любой, видит итоговую сумму.
3. Нажимает **«Оформить заказ»** → переходит на `/checkout`.
4. Указывает адрес доставки и способ оплаты (**наличные** / **картой**), нажимает **«Подтвердить»**.
5. Создаётся один `Order` на всю корзину целиком (адрес и способ оплаты — общие на заказ), с одним `OrderItem` на каждый товар. Корзина очищается.
6. Заказ может включать товары **разных продавцов одновременно**. Каждый продавец видит и управляет статусом **только своих** товаров внутри заказа (`OrderItem.status`), не трогая чужие позиции того же заказа.

## 2. Модель данных (как реализовано)

### 2.1 `Order`
| Поле | Тип | Комментарий |
|---|---|---|
| `id` | Long | PK |
| `buyer` | User (`@ManyToOne`) | покупатель |
| `deliveryAddress` | String | адрес доставки, общий на весь заказ |
| `paymentMethod` | `PaymentMethod` (`@Enumerated(STRING)`) | `CASH` / `CARD` |
| `paymentStatus` | `PaymentStatus` (`@Enumerated(STRING)`) | по умолчанию `PENDING` при создании |
| `dateOfCreated` | LocalDateTime | через `@PrePersist` |
| `items` | `List<OrderItem>` | `@OneToMany(mappedBy = "order")` |

### 2.2 `OrderItem`
Одна строка = один товар одного продавца внутри заказа.

| Поле | Тип | Комментарий |
|---|---|---|
| `id` | Long | PK |
| `order` | Order (`@ManyToOne`) | |
| `product` | Product (`@ManyToOne`) | |
| `status` | `OrderStatus` (`@Enumerated(STRING)`) | `NEW` по умолчанию (см. §5 — правила переходов) |
| `quantity` | int | сколько единиц товара куплено в этой позиции (копия `CartItem.quantity` на момент оформления) |

### 2.3 `CartItem` (временное хранение корзины до оформления)
| Поле | Тип | Комментарий |
|---|---|---|
| `id` | Long | PK |
| `user` | User (`@ManyToOne`) | владелец корзины |
| `product` | Product (`@ManyToOne`) | |
| `quantity` | int | по умолчанию 1, зажато в диапазон `[1, product.getQuantity()]` |

### 2.5 `Product.quantity` (остаток на складе)

У `Product` есть поле `quantity` (int) — сколько единиц товара доступно. Заполняется продавцом в форме создания товара (`user-info.ftlh`). Списывается при оформлении заказа (см. `checkout` ниже). Пополняется продавцом (или админом) через `ProductService.restockProduct` / `POST /product/restock/{id}` (`amount: int`, прибавляется к текущему остатку, с проверкой владения). Когда доходит до 0 — товар считается закончившимся: скрывается из общего каталога (`ProductService.listProducts` фильтрует `quantity > 0`), но остаётся видимым на странице «Ваши товары» у самого продавца (`user.getProducts()`, без фильтра — иначе пополнить остаток было бы невозможно) и на своей странице `/product/{id}` (там кнопка «В корзину» скрывается и заменяется на «Товар закончился»).

### 2.4 Enum'ы

```java
public enum PaymentMethod { CASH, CARD }

// набор шире исходно задуманного UNPAID/PAID — оставили как есть, отражает реальный жизненный цикл оплаты
public enum PaymentStatus { PENDING, PAID, FAILED, REFUNDED }
```

`OrderStatus` (`NEW, CONFIRMED, SENT, RECEIVED, CANCELLED`) без изменений, живёт на `OrderItem`.

## 3. Репозитории

```java
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByUser(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    void deleteByUserAndProduct(User user, Product product);
    void deleteAllByUser(User user);
}

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(Long buyerId);
}

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByProduct_User_Id(Long sellerId);
}
```

## 4. Сервисы

### `CartItemService`
- `getCartItems(User user)`
- `getCartTotal(User user)` — сумма `product.price * item.quantity` по всем позициям
- `addProductToCart(User user, Product product)` — если `product.quantity <= 0`, ничего не делает; если товара ещё нет в корзине — добавляет с `quantity=1`; если уже есть — увеличивает на 1, не превышая `product.getQuantity()`
- `updateQuantity(User user, Product product, int quantity)` — выставляет точное количество, зажатое в `[1, product.getQuantity()]` (защита и на клиенте через `max` у `<input>`, и на сервере — на случай прямого POST в обход формы)
- `removeFromCart(User user, Product product)` — **`@Transactional`** (derived delete-запрос требует явной транзакции)
- `checkout(User user, String address, PaymentMethod method)` — **`@Transactional`**:
  1. Создать `Order` (`buyer`, `deliveryAddress`, `paymentMethod`, `paymentStatus=PENDING`)
  2. На каждый `CartItem` пользователя создать `OrderItem` (`order`, `product`, `status=NEW`, `quantity = min(cartItem.quantity, product.quantity)`), затем списать остаток: `product.quantity = max(0, product.quantity - quantity)`, сохранить `Product`
  3. `cartItemRepository.deleteAllByUser(user)`

  ⚠️ Без `@Transactional` здесь падает `TransactionRequiredException` на шаге очистки корзины (derived delete-методы Spring Data требуют активной транзакции) — сама вставка `Order`/`OrderItem` при этом успевает пройти, из-за чего корзина не очищается и заказ можно "продублировать". Столкнулись с этим на практике при первом прогоне — исправлено.

### `OrderService`
- `getMyPurchases(Principal principal)` → `List<Order>`
- `getMySales(Principal principal)` → `List<OrderItem>` (через `orderItemRepository.findByProduct_User_Id`)
- `updateStatus(Long orderItemId, OrderStatus status, Principal principal)` — работает с `OrderItem` (не `Order`). Проверка владения: `orderItem.getProduct().getUser().getId().equals(currentUser.getId())`.

## 5. Правила переходов `OrderItem.status`

| Текущий статус | Разрешённые следующие |
|---|---|
| `NEW` | любой, кроме `NEW` |
| `CONFIRMED` | `SENT`, `RECEIVED`, `CANCELLED` |
| `SENT` | `RECEIVED`, `CANCELLED` |
| `RECEIVED` | `CANCELLED` |
| `CANCELLED` | ничего (терминальный статус) |

Реализовано через `switch` + единый `boolean canMoveTo` флаг и один `if (!canMoveTo) return;` перед сохранением — специально без отдельного `save()` в каждой ветке, чтобы не повторить старый баг (был безусловный `setStatus`/`save` после `switch`, перезаписывавший статус независимо от проверки).

## 6. Контроллеры / эндпоинты

### `CartItemController`

| Метод | Путь | Параметры | Поведение |
|---|---|---|---|
| GET | `/cart` | — | модель: `currentUser`, `cartItems`, `cartTotal` → view `"cart"` |
| POST | `/cart/add/{productId}` | — | не залогинен → `redirect:/login`; свой товар → flash error, `redirect:/product/{id}`; иначе → `redirect:/cart` |
| POST | `/cart/remove/{productId}` | — | `redirect:/cart` |
| POST | `/cart/update/{productId}` | `quantity: int` | `updateQuantity(...)` → `redirect:/cart` |
| GET | `/checkout` | — | не залогинен → `redirect:/login`; корзина пуста → `redirect:/cart`; иначе модель `currentUser`, `cartItems`, `cartTotal` → view `"checkout"` |
| POST | `/checkout` | `address: String`, `paymentMethod: PaymentMethod` | `checkout(...)` → flash `successMessage` → `redirect:/orders` |

### `OrderController`

| Метод | Путь | Параметры | Поведение |
|---|---|---|---|
| GET | `/orders` | — | модель: `currentUser`, `myPurchases` (`List<Order>`), `mySales` (`List<OrderItem>`), `statuses` |
| POST | `/orders/items/{itemId}/status` | `status: OrderStatus` | `redirect:/orders` |

Старый `POST /product/order/{id}` (мгновенное оформление одного товара) удалён — покупка теперь только через корзину.

### `ProductController` (правки)

| Метод | Путь | Параметры | Поведение |
|---|---|---|---|
| POST | `/product/restock/{id}` | `amount: int` | владелец/админ пополняют остаток на `amount`; иначе flash error → `redirect:/product/{id}` |

### `GlobalModelAttributes` (`@ControllerAdvice`)

```java
@ModelAttribute("cartCount")
public int cartCount(Principal principal) { ... }
```
Кладёт число товаров в корзине в модель **каждого** запроса, чтобы бейдж на иконке корзины в шапке (`common.ftlh`) показывался на любой странице, а не только на `/cart`.

## 7. Фронтенд

- `common.ftlh` — иконка корзины в шапке (для залогиненных не-админов) с бейджем `cartCount`; `productCard` — бейдж «Товар закончился» (`product.quantity <= 0`) поверх фото.
- `user-info.ftlh` — в форме создания товара добавлено поле «Количество» (`name="quantity"`, `min=1`).
- `product-info.ftlh` — показывает «В наличии: N шт.»; если `quantity <= 0` — кнопка «В корзину» скрыта, вместо неё «Товар закончился»; владельцу/админу доступна форма «Пополнить остаток» (`amount` + кнопка → `POST /product/restock/{id}`).
- `cart.ftlh` — список товаров корзины: степпер количества (`<input type="number" min=1 max=product.quantity>` + «Обновить» → `POST /cart/update/{id}`), сумма по строке (`price × quantity`), «Убрать», общий итог.
- `checkout.ftlh` — адрес (textarea) + способ оплаты (радио CASH/CARD) + сводка заказа (с `× quantity` на каждой строке) + «Подтвердить».
- `orders.ftlh`:
  - «Мои покупки» — карточки `Order` (адрес, способ/статус оплаты, таблица `items` со статусами и колонкой «Кол-во»).
  - «Мои продажи» — плоская таблица `OrderItem` (товар, кол-во, покупатель, адрес доставки, статус со `<select>` и ограничением переходов из §5, дата).
- `site.css` — стили под всё перечисленное: точки статусов оплаты (`.status-dot.pending/.paid/.failed/.refunded`), `.card__sold-out` (оверлей на карточке), `.cart-row__qty` (степпер в корзине).

## 8. Грабли, на которые наступили при реализации (для памяти)

- **Забытый `@Entity`/`@Data`** на `CartItem`/`OrderItem` — без `@Entity` Hibernate не видит класс вообще; без `@Data` (Lombok) нет геттеров/сеттеров.
- **`@Enumerated(EnumType.STRING)`** обязателен на каждом enum-поле сущности — без него Hibernate по умолчанию хранит порядковый номer (`ORDINAL`), что хрупко при любом изменении порядка констант.
- **Стас constraint в MySQL после смены схемы**: таблица `orders` создавалась ещё на ранних итерациях (другой набор колонок/enum-значений), и при последующих `ddl-auto=update` в ней остался устаревший `CHECK` constraint, блокировавший вставку новых enum-значений. `ddl-auto=update` **не** пересоздаёт constraint'ы на существующих колонках. Починили через `DROP TABLE order_item, orders;` и перезапуск (Hibernate создал их заново с нуля).
- **`TransactionRequiredException` на derived delete-запросах** (`deleteAllByUser`, `deleteByUserAndProduct`) — такие методы Spring Data требуют явной `@Transactional` на вызывающем сервисном методе, иначе падают в рантайме, даже если код компилируется без единой ошибки.
