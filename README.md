# Forze Storage Plugin

**Forze Storage** - це плагін для Minecraft серверів на базі Spigot/Paper, який надає гравцям можливість використовувати віртуальне сховище для зберігання предметів.

## 🌟 Особливості

- **Віртуальне сховище** - кожен гравець має власне сховище для зберігання предметів
- **Стабільна пагінація** - сховище поділено на сторінки по 45 предметів кожна з захистом від race conditions
- **Адміністративні функції** - адміністратори можуть переглядати та керувати сховищами інших гравців
- **Автоматичне збереження** - сховища автоматично зберігаються при закритті та виході з гри
- **Локалізація** - підтримка української мови
- **Безпека** - захист від дублювання предметів та несанкціонованого доступу
- **Діагностика** - вбудовані інструменти для відладки та моніторингу
- **Відновлення після помилок** - стійкість до зіпсованих даних та несподіваних збоїв

## 📋 Вимоги

- **Minecraft версія:** 1.21+
- **Java:** 17+
- **Платформа:** Spigot/Paper

## 🔧 Встановлення

1. Завантажте файл `ForzeStorage.jar`
2. Помістіть його в папку `plugins` вашого сервера
3. Перезапустіть сервер
4. Налаштуйте конфігураційні файли за потребою

## 📖 Команди

### Основні команди

| Команда | Опис | Дозвіл |
|---------|------|--------|
| `/storage` | Показати допомогу | `storage.use` |
| `/storage open` | Відкрити власне сховище | `storage.use` |

### Адміністративні команди

| Команда | Опис | Дозвіл |
|---------|------|--------|
| `/storage open <гравець>` | Відкрити сховище вказаного гравця | `storage.admin` |
| `/storage add <гравець> <предмет> <кількість>` | Додати предмет до сховища гравця | `storage.admin` |
| `/storage reload` | Перезавантажити конфігурацію плагіна | `storage.admin` |
| `/storage debug [гравець]` | Діагностика сховища (власного або вказаного гравця) | `storage.admin` |
| `/storage stats` | Показати статистику та очистити неактивні GUI | `storage.admin` |

### RCON команди

Всі адміністративні команди доступні через RCON/консоль:
```bash
storage add Steve DIAMOND 64
storage add Alex IRON_INGOT 32
storage debug Steve
storage stats
storage reload
```

## 🔑 Дозволи

| Дозвіл | Опис | За замовчуванням |
|--------|------|------------------|
| `storage.use` | Дозволяє використовувати власне сховище | `true` |
| `storage.admin` | Дозволяє керувати сховищами інших гравців | `op` |

## ⚙️ Конфігурація

### config.yml
```yaml
# Налаштування ForzeStorage
settings:
  # Максимальна кількість предметів на сторінку
  items-per-page: 45
  # Загальна кількість слотів у інвентарі
  inventory-size: 54

# Повідомлення
messages:
  no-permission: "&cУ вас немає прав для виконання цієї команди!"
  player-not-found: "&cГравця не знайдено!"
  storage-opened: "&aСховище відкрито!"
  storage-closed: "&aСховище закрито!"
  item-added: "&aПредмет додано до сховища!"
  invalid-amount: "&cНевірна кількість!"
  invalid-item: "&cНевірний предмет!"
  storage-full: "&cСховище переповнено!"
  page-changed: "&aСторінка змінена на %page%"
  next-page: "&aНаступна сторінка"
  previous-page: "&aПопередня сторінка"
  storage-title: "&8Сховище %player%"
```

### Кольорові коди
Плагін підтримує стандартні кольорові коди Minecraft з символом `&`:
- `&a` - зелений
- `&c` - червоний  
- `&8` - темно-сірий
- `&b` - блакитний
- тощо...

## 📁 Структура файлів

```
plugins/
└── ForzeStorage/
    ├── config.yml          # Основна конфігурація
    ├── messages.yml         # Повідомлення (не використовується в поточній версії)
    └── storages/           # Папка зі сховищами гравців
        ├── <UUID>.yml      # Файл сховища для кожного гравця
        └── ...
```

## 🎮 Як використовувати

### Для гравців
1. Використайте команду `/storage open` для відкриття власного сховища
2. Перетягуйте предмети між сховищем та своїм інвентарем (тільки забирання!)
3. Використовуйте стрілки для навігації між сторінками
4. Сховище автоматично зберігається при закритті

**Обмеження для звичайних гравців:**
- Можна тільки **забирати** предмети зі сховища
- Shift+Click заборонено
- Неможливо класти предмети у сховище

### Для адміністраторів
1. Використайте `/storage open <гравець>` для перегляду сховища гравця
2. Додавайте предмети командою `/storage add <гравець> <предмет> <кількість>`
3. Ви можете редагувати сховища інших гравців
4. Використовуйте `/storage debug` для діагностики проблем
5. Команда `/storage stats` показує статистику використання

## 🛡️ Безпека

- Гравці можуть тільки забирати предмети зі свого сховища
- Додавання предметів доступне тільки адміністраторам через команди
- Захист від дублювання предметів
- Автоматичне збереження при виході з гри
- Захист від race conditions при швидкій навігації
- Безпечна десеріалізація зіпсованих предметів

## 🔧 Технічні деталі

- **API версія:** 1.21
- **Мова програмування:** Java 17
- **Система збірки:** Maven
- **Залежності:** Spigot API 1.21.4
- **Thread-safe пагінація** з захистом від конфліктів
- **Автоматичне відновлення** після помилок десеріалізації

## 📝 Приклади використання

### Відкрити власне сховище
```
/storage open
```

### Адмін додає алмази гравцю
```
/storage add Steve DIAMOND 64
```

### Адмін переглядає сховище гравця
```
/storage open Steve
```

### Діагностика сховища
```
/storage debug Steve
```

### Перевірка статистики
```
/storage stats
```

## 🔍 Діагностика та відладка

### Команди діагностики
- `/storage debug` - діагностика власного сховища
- `/storage debug <гравець>` - діагностика сховища іншого гравця
- `/storage stats` - статистика всіх відкритих сховищ

### Що показує діагностика:
- Поточну сторінку та стан навігації
- Список предметів у відкритому інвентарі
- Аналіз файлу сховища
- Кількість предметів та сторінок
- Виявлення зіпсованих даних

### Логування
Плагін веде детальні логи всіх операцій:
- Відкриття/закриття сховищ
- Навігація між сторінками
- Збереження даних
- Помилки десеріалізації
- Діагностичну інформацію

## 🐛 Виправлені проблеми

### ✅ Версія 1.1+
- **Виправлено:** Підвисання пагінації при швидкому переключенні сторінок
- **Виправлено:** Змішування даних між сторінками
- **Виправлено:** Race conditions при одночасних операціях
- **Виправлено:** Втрата предметів при помилках десеріалізації
- **Додано:** Система діагностики та відладки
- **Додано:** Автоматичне відновлення після помилок
- **Покращено:** Стабільність при роботі з великими сховищами

### 🔄 Відомі обмеження
- Немає загального обмеження на розмір сховища (обмежено тільки кількістю сторінок)
- Відсутня система резервного копіювання (планується в наступних версіях)

## 🤝 Підтримка

### При виникненні проблем:
1. Перевірте конфігураційні файли
2. Переконайтесь, що у гравців є необхідні дозволи
3. Використайте `/storage debug` для діагностики
4. Перевірте логи сервера на наявність помилок
5. Використайте `/storage stats` для перевірки стану плагіна

### Типові помилки та рішення:
- **"GUI не знайдено"** - перезапустіть клієнт або використайте `/storage stats`
- **Предмети зникають** - перевірте логи, використайте діагностику
- **Пагінація не працює** - переконайтесь що не відбувається швидке переключення
- **Помилки десеріалізації** - плагін автоматично відновить базові предмети

## 📊 Моніторинг продуктивності

### Команди для адміністраторів:
```bash
# Статистика відкритих сховищ
/storage stats

# Діагностика конкретного гравця
/storage debug PlayerName

# Перезавантаження конфігурації без перезапуску
/storage reload
```

### Логи для моніторингу:
- `[ForzeStorage] Відкрито сховище` - відкриття сховищ
- `[ForzeStorage] Збережено сторінку` - успішні збереження
- `[ForzeStorage] Навігація завершена` - успішні переходи між сторінками
- `[ForzeStorage] WARNING` - попередження про проблеми
- `[ForzeStorage] SEVERE` - критичні помилки

## 📄 Ліцензія

Цей плагін розповсюджується як є, без жодних гарантій. Використовуйте на власний ризик.

---

**Версія:** 1.1-SNAPSHOT (з виправленнями пагінації)  
**Автор:** forze  
**Сумісність:** Minecraft 1.21+  
**Оновлено:** Червень 2024