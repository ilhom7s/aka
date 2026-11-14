# AKA — Admin service (REST) + фронтенд

Доработка админ-сервиса: у каждого товара теперь галерея из **3–5 фотографий**,
плюс новые поля `plotnost` (плотность, г/м²) и `material`. Добавлен фронтенд-админ
на чистом JS в фирменных цветах (вино `#6B0F19` + молочно-бежевый).

## Что изменилось на бэкенде

**Новые файлы**
- `entity/ProductImage.java` — сущность фото (`product_images`), `@ManyToOne` → `ProductVariant`.
- `dto/ProductImageDto.java`
- `services/ImageStorageService.java` — сохраняет файлы на диск, отдаёт URL `/images/{файл}`.
- `config/WebConfig.java` — раздача `/images/**` и CORS.

**Изменённые файлы**
- `entity/ProductVariant.java` — добавлена коллекция `List<ProductImage> images` (cascade + orphanRemoval); `imageUrl` оставлен как обложка (первое фото), чтобы витрина не сломалась.
- `dto/ProductVariantDto.java` — добавлено поле `images`.
- `dto/ProductRequestDto.java` — убран `imageUrl` (фото теперь приходят файлами).
- `services/AdminService.java` — `createProduct` / `editProduct` с валидацией 3–5 фото; удаление файлов при замене/удалении.
- `controllers/AdminController.java` — multipart-эндпоинты.
- `repository/UserMapper.java` — маппинг `ProductImage` → `ProductImageDto`.
- `repository/ProductVariantRepository.java` — grouped-запрос теперь возвращает `plotnost` и `material`.
- `Exceptions/GlobalExceptionHandler.java` — `400` на нарушение правил и большой файл.
- `resources/application.yaml` — лимиты multipart и каталог `app.upload.dir`.

## Эндпоинты

| Метод  | Путь                    | Тело / параметры                                   |
|--------|-------------------------|----------------------------------------------------|
| GET    | `/admin/allgoods`       | список товаров (с `images`)                         |
| POST   | `/admin/products`       | multipart: `product` (JSON) + 3–5 файлов `images`  |
| PUT    | `/admin/products/{id}`  | multipart: `product` (JSON) + опц. 3–5 `images`     |
| DELETE | `/admin/delete/{id}`    | —                                                  |
| PATCH  | `/admin/update/{id}`    | `?quantity=`                                        |
| GET    | `/admin/all` `/orders` `/delivered` | заказы                                 |
| GET    | `/admin/users`          | пользователи                                       |

> Старые `POST /admin/save` и `POST /admin/edit` заменены на multipart `/products`.

## Фронтенд

Лежит в `src/main/resources/static/` (`index.html`, `styles.css`, `app.js`).
Раздаётся самим Spring: после запуска открой **http://localhost:8081/**.
API-адрес определяется автоматически (тот же origin; при открытии через
Live Server/файл — стучится в `http://localhost:8081`).

## Запуск

```bash
./mvnw spring-boot:run
```

Каталог для картинок создаётся автоматически (`./uploads`). Добавь его в `.gitignore`:

```
uploads/
```

## Пример запроса curl (создание товара)

```bash
curl -X POST http://localhost:8081/admin/products \
  -F 'product={"productId":"K7","productName":"Кимоно дзюдо","color":"Белый","size":"170","material":"100% хлопок","plotnost":450,"price":5990,"quantity":10};type=application/json' \
  -F 'images=@1.jpg' -F 'images=@2.jpg' -F 'images=@3.jpg'
```
