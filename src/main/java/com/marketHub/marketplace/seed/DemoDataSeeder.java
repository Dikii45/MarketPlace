package com.marketHub.marketplace.seed;

import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Category;
import com.marketHub.marketplace.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

// На пустой базе (например, самый первый docker compose up на свежей машине) автоматически
// наполняет каталог демо-товарами с реальными фото, чтобы сайт не выглядел пустым сразу после клонирования.
// На уже заполненной базе — no-op. Отключается в тестах через app.seed.enabled=false.
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final SeedTx seedTx;

    // loremflickr.com отвечает 302-редиректом на реальный файл фото — без followRedirects(NORMAL)
    // HttpClient по умолчанию НЕ следует за ним и молча вернул бы "нет фото"
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // выставляется один раз в начале run() пробным запросом — избавляет от долгих повторных таймаутов
    private volatile boolean photoServiceAvailable = false;

    private static final List<String> CITIES = List.of("Москва", "Казань", "Екатеринбург", "Новосибирск", "Владивосток");

    // keyword — англоязычный тег для подбора реального фото через loremflickr.com (публичный сервис placeholder-фото для тестовых данных)
    private record ProductDef(String title, String description, Category category, int price, int quantity, String keyword) {}

    private static final List<ProductDef> PRODUCT_DEFS = List.of(
            // ELECTRONICS
            new ProductDef("Ноутбук ASUS VivoBook 15", "15.6\" IPS, Ryzen 5, 16 ГБ ОЗУ, SSD 512 ГБ.", Category.ELECTRONICS, 54990, 5, "laptop"),
            new ProductDef("Наушники Sony WH-1000XM4", "Активное шумоподавление, до 30 часов работы.", Category.ELECTRONICS, 24990, 12, "headphones"),
            new ProductDef("Умные часы Xiaomi Mi Band 8", "Пульсометр, до 16 дней автономности.", Category.ELECTRONICS, 3490, 20, "smartwatch"),
            new ProductDef("Телевизор LG 43\" 4K", "Smart TV, webOS, HDR10.", Category.ELECTRONICS, 34990, 6, "television"),
            new ProductDef("Игровая консоль PlayStation 5", "1 ТБ SSD, геймпад DualSense в комплекте.", Category.ELECTRONICS, 54990, 4, "playstation"),
            new ProductDef("Фотоаппарат Canon EOS 200D", "Зеркальная камера, объектив 18-55 мм.", Category.ELECTRONICS, 42990, 3, "camera"),
            new ProductDef("Колонка JBL Charge 5", "Влагозащита, до 20 часов работы.", Category.ELECTRONICS, 12990, 15, "speaker"),
            new ProductDef("Робот-пылесос Xiaomi", "Лазерная навигация, влажная уборка.", Category.ELECTRONICS, 19990, 7, "vacuumcleaner"),
            // CLOTHES
            new ProductDef("Платье летнее женское", "Лёгкий хлопок, свободный крой.", Category.CLOTHES, 2490, 18, "dress"),
            new ProductDef("Джинсы Levi's 501", "Классический прямой крой, синий деним.", Category.CLOTHES, 5490, 10, "jeans"),
            new ProductDef("Кроссовки Nike Air Max", "Амортизация Air, дышащая сетка.", Category.CLOTHES, 8990, 9, "sneakers"),
            new ProductDef("Свитер вязаный", "Тёплая шерсть, унисекс.", Category.CLOTHES, 2990, 16, "sweater"),
            new ProductDef("Пуховик женский", "Съёмный капюшон, натуральный пух.", Category.CLOTHES, 7990, 8, "coat"),
            new ProductDef("Рубашка классическая", "Хлопок, приталенный силуэт.", Category.CLOTHES, 2190, 20, "shirt"),
            new ProductDef("Футболка хлопковая", "Базовая модель, разные цвета.", Category.CLOTHES, 990, 30, "tshirt"),
            new ProductDef("Пальто демисезонное", "Шерстяная смесь, длина миди.", Category.CLOTHES, 9990, 6, "coat"),
            new ProductDef("Кепка бейсболка", "Регулируемый ремешок, хлопок.", Category.CLOTHES, 890, 25, "baseballcap"),
            // HOME
            new ProductDef("Диван угловой", "Раскладной механизм, ящик для белья.", Category.HOME, 39990, 3, "sofa"),
            new ProductDef("Обеденный стол деревянный", "Массив дуба, на 6 персон.", Category.HOME, 24990, 4, "furniture"),
            new ProductDef("Кресло офисное", "Регулировка высоты и наклона спинки.", Category.HOME, 8990, 10, "chair"),
            new ProductDef("Кофеварка Delonghi", "Капсульная система, 1450 Вт.", Category.HOME, 6990, 12, "coffee"),
            new ProductDef("Микроволновая печь Samsung", "20 л, гриль, 800 Вт.", Category.HOME, 7990, 9, "kitchen"),
            new ProductDef("Настольная лампа", "Регулируемая яркость, LED.", Category.HOME, 1490, 22, "tablelamp"),
            new ProductDef("Комплект постельного белья", "Сатин, двуспальный размер.", Category.HOME, 2990, 17, "bedding"),
            new ProductDef("Набор кастрюль", "Нержавеющая сталь, 5 предметов.", Category.HOME, 5990, 11, "kitchenware"),
            new ProductDef("Книжный шкаф", "5 полок, ЛДСП дуб сонома.", Category.HOME, 8990, 5, "bookshelf"),
            new ProductDef("Ковёр шерстяной", "Ручная работа, 200x300 см.", Category.HOME, 14990, 4, "interior"),
            // AUTO
            new ProductDef("Летние шины Michelin", "Комплект 4 шт., R16.", Category.AUTO, 24990, 6, "tire"),
            new ProductDef("Аккумулятор автомобильный", "60 А·ч, полярность прямая.", Category.AUTO, 7990, 13, "carbattery"),
            new ProductDef("Автомагнитола Pioneer", "Bluetooth, USB, поддержка Android Auto.", Category.AUTO, 8990, 9, "car"),
            new ProductDef("Комплект ковриков", "Резиновые, по форме салона.", Category.AUTO, 2990, 20, "carinterior"),
            new ProductDef("Видеорегистратор 70mai", "Full HD, ночной режим.", Category.AUTO, 4990, 15, "dashcam"),
            new ProductDef("Домкрат гидравлический", "Грузоподъёмность 2 тонны.", Category.AUTO, 2490, 18, "car"),
            new ProductDef("Чехлы на сиденья", "Экокожа, универсальный размер.", Category.AUTO, 5990, 10, "carseat"),
            new ProductDef("Автомобильный компрессор", "Питание от прикуривателя, манометр.", Category.AUTO, 2190, 16, "tools"),
            new ProductDef("Зимние шины Nokian", "Комплект 4 шт., шипованные.", Category.AUTO, 34990, 5, "tire"),
            new ProductDef("Багажник на крышу", "Универсальное крепление, сталь.", Category.AUTO, 6990, 7, "car"),
            // OTHER
            new ProductDef("Велосипед горный Stels", "21 скорость, рама алюминий.", Category.OTHER, 18990, 6, "mountainbike"),
            new ProductDef("Палатка туристическая 4-местная", "Водонепроницаемый тент, 2 входа.", Category.OTHER, 8990, 8, "camping"),
            new ProductDef("Гитара акустическая Yamaha", "Дредноут, чехол в комплекте.", Category.OTHER, 12990, 5, "guitar"),
            new ProductDef("Набор гантелей 20 кг", "Разборные, виниловое покрытие.", Category.OTHER, 4990, 11, "dumbbells"),
            new ProductDef("Скейтборд", "Кленовая дека, ABEC-7 подшипники.", Category.OTHER, 3990, 14, "skateboard"),
            new ProductDef("Настольная игра \"Монополия\"", "Классическое издание для всей семьи.", Category.OTHER, 1990, 20, "boardgame"),
            new ProductDef("Рюкзак туристический", "60 л, влагозащитный чехол.", Category.OTHER, 5990, 12, "backpack"),
            new ProductDef("Спальный мешок", "До -10°C, компактная упаковка.", Category.OTHER, 3490, 15, "camping"),
            new ProductDef("Электросамокат Xiaomi", "До 30 км на одном заряде.", Category.OTHER, 27990, 6, "scooter")
    );

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            return;
        }

        log.info("Seed: каталог пуст, наполняю демо-товарами...");
        // один быстрый пробный запрос вместо повторных таймаутов на каждое из ~230 фото —
        // если сеть/сервис недоступны (например, у Docker-контейнера нет доступа в интернет),
        // сразу переходим на сгенерированные заглушки, а не ждём по 10-20 секунд на каждую попытку
        photoServiceAvailable = probePhotoService();
        if (!photoServiceAvailable) {
            log.warn("Seed: loremflickr.com недоступен — товары получат сгенерированные заглушки вместо фото");
        }

        List<User> sellers = seedTx.ensureSellers();

        int created = 0;
        for (int i = 0; i < PRODUCT_DEFS.size(); i++) {
            ProductDef def = PRODUCT_DEFS.get(i);
            List<byte[]> photos = fetchPhotos(def);
            Long sellerId = sellers.get(i % sellers.size()).getId();
            String city = CITIES.get(i % CITIES.size());
            seedTx.createProduct(def.title(), def.description(), def.category(), def.price(), def.quantity(),
                    city, sellerId, photos);
            created++;
        }
        log.info("Seed: создано {} демо-товаров, по 5 фото на каждый", created);
    }

    // 5 реальных фото по теме товара (loremflickr отдаёт случайное фото с Flickr по тегу),
    // при сетевой ошибке — сгенерированная заглушка, чтобы сидирование не падало из-за недоступности сервиса
    private List<byte[]> fetchPhotos(ProductDef def) {
        List<byte[]> photos = new ArrayList<>(5);
        for (int i = 1; i <= 5; i++) {
            byte[] bytes = fetchOnePhoto(def.keyword());
            photos.add(bytes != null ? bytes : generatePhoto(def.title(), def.category(), i));
        }
        return photos;
    }

    private boolean probePhotoService() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://loremflickr.com/64/64"))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "buysell-demo-seeder")
                    .GET()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] fetchOnePhoto(String keyword) {
        if (!photoServiceAvailable) {
            return null;
        }
        String url = "https://loremflickr.com/640/480/" + keyword;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "buysell-demo-seeder")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200 && response.body().length > 0) {
                    return response.body();
                }
                log.warn("Seed: фото для '{}' вернуло HTTP {} (попытка {})", keyword, response.statusCode(), attempt);
            } catch (Exception e) {
                log.warn("Seed: не удалось получить фото для '{}' (попытка {}): {}", keyword, attempt, e.getMessage());
            }
        }
        return null;
    }

    // цвет фона зависит от категории — используется только как запасной вариант, если фото не удалось скачать
    private Color categoryColor(Category category) {
        return switch (category) {
            case ELECTRONICS -> new Color(0x1f, 0x6f, 0x8a);
            case CLOTHES -> new Color(0x8a, 0x4f, 0x6f);
            case HOME -> new Color(0x5a, 0x7a, 0x3a);
            case AUTO -> new Color(0x6a, 0x5a, 0x2a);
            case OTHER -> new Color(0x4a, 0x4a, 0x7a);
        };
    }

    private byte[] generatePhoto(String title, Category category, int photoIdx) {
        int width = 640;
        int height = 480;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color base = categoryColor(category);
        int shade = (photoIdx - 1) * 12;
        Color background = new Color(
                Math.min(255, base.getRed() + shade),
                Math.min(255, base.getGreen() + shade),
                Math.min(255, base.getBlue() + shade)
        );
        g.setColor(background);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(255, 255, 255, 60));
        g.fillOval(width / 2 - 140, height / 2 - 140, 280, 280);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        drawCentered(g, title, width, height / 2 - 10);

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(g, "Фото " + photoIdx + " из 5", width, height / 2 + 24);

        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void drawCentered(Graphics2D g, String text, int width, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        if (textWidth <= width - 40) {
            g.drawString(text, (width - textWidth) / 2, y);
            return;
        }
        String[] words = text.split(" ");
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        for (String word : words) {
            if (fm.stringWidth(line1 + word) <= width - 40) {
                line1.append(word).append(" ");
            } else {
                line2.append(word).append(" ");
            }
        }
        int w1 = fm.stringWidth(line1.toString().trim());
        int w2 = fm.stringWidth(line2.toString().trim());
        g.drawString(line1.toString().trim(), (width - w1) / 2, y - 14);
        g.drawString(line2.toString().trim(), (width - w2) / 2, y + 14);
    }
}
