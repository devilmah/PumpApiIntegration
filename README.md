# Kafka + ПУМП: прототип модуля интеграции

Учебный прототип, демонстрирующий полный сценарий получения U-файлов:

1. Чтение уведомлений из Kafka.
2. Запрос метаданных по периоду у Mock ПУМП.
3. Подтверждение пользователем.
4. Скачивание ZIP-файлов и сохранение на диск.

Проект состоит из двух частей:

- **Java-приложение (Swing)** — оконный интерфейс, чтение Kafka, вызовы ПУМП, скачивание файлов.
- **Mock ПУМП (Python / FastAPI)** — эмулятор внешней подсистемы: выдаёт токен, метаданные по периоду и ZIP-файлы.

> ⚠️ Проект **не для продакшена**. Токен статический, файлы генерируются в памяти, JSON парсится вручную. Цель — отработать архитектуру и сценарий.

---

## 📁 Структура репозитория

```
kafka-pump-prototype/
├── README.md
├── .gitignore
├── mock_pump/                     # Mock ПУМП (Python)
│   ├── main.py                    # FastAPI-приложение
│   └── requirements.txt
└── java-app/                      # Java-приложение (Swing)
    ├── pom.xml                    # Maven-конфигурация
    └── src/
        └── main/
            └── java/
                └── com/example/kafka_test/
                    ├── MainApp.java
                    ├── KafkaNotificationReader.java
                    └── integration/
                        ├── AuthClient.java
                        ├── PumpApiClient.java
                        ├── StorageService.java
                        ├── IntegrationOrchestrator.java
                        └── model/
                            ├── Notification.java
                            ├── PeriodInfo.java
                            └── DownloadResult.java
```

### Назначение ключевых классов

| Класс | Ответственность |
|---|---|
| `MainApp` | Окно Swing, меню, диалоги, запуск фоновых потоков |
| `KafkaNotificationReader` | Чтение накопленных сообщений из топика `file-notifications` |
| `AuthClient` | Получение токена доступа (эмуляция Keycloak) |
| `PumpApiClient` | Запрос метаданных по периоду и скачивание файлов |
| `StorageService` | Сохранение ZIP-файлов в папку `downloaded_files/` |
| `IntegrationOrchestrator` | Связывает шаги: метаданные → подтверждение → скачивание |
| `Notification` | Сообщение из Kafka (хранит сырой текст) |
| `PeriodInfo` | Ответ ПУМП: `period_id`, `summ`, `count`, `file_ids` |
| `DownloadResult` | Итог скачивания: список путей и ошибок |

---

## 🚀 Требования

- **Java 17+** и **Maven 3.8+**
- **Python 3.10+**
- **Apache Kafka** локально на `localhost:9092`
- **Windows / Linux / macOS**

---

## ⚙️ Запуск

### 1. Kafka

Запустите локальный брокер и создайте топик с одной партицией:

```bash
kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic file-notifications --partitions 1 --replication-factor 1
```

Отправьте тестовое уведомление:

```bash
echo 'file is ready to download' | kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic file-notifications
```

### 2. Mock ПУМП

```bash
cd mock_pump
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001
```

Проверка:

```bash
curl -X POST http://127.0.0.1:8001/auth/token
# {"access_token":"test-token-123","token_type":"Bearer"}
```

> На Windows используйте `127.0.0.1`, а не `localhost` — иначе возможен `ConnectException` из-за IPv6-резолвинга.

### 3. Java-приложение

```bash
cd java-app
mvn clean package
java -jar target/kafka-test-1.0-SNAPSHOT.jar
```

Или откройте проект в IntelliJ IDEA и запустите `MainApp`.

---

## 🖥 Использование

В меню **«Файл»** доступны пункты:

1. **Проверить уведомления (Kafka)** — читает накопленные сообщения из топика и показывает диалог: сколько всего сообщений и последние 10.
2. **Выбрать период и получить информацию** — по шагам:
   - ввод периода в формате `YYYY-MM-DD` (например, `2026-01-01`);
   - запрос метаданных у Mock ПУМП (сумма, количество счетов, список файлов);
   - диалог подтверждения;
   - скачивание ZIP-файлов в папку `downloaded_files/`.
3. **Выход** — закрыть приложение.

---

## 🔌 API Mock ПУМП

| Метод | Путь | Назначение |
|---|---|---|
| `POST` | `/auth/token` | Выдаёт статический токен |
| `GET` | `/pump/period/{period_id}` | Метаданные по периоду + список `file_ids` |
| `GET` | `/filestore/download/{file_id}` | Скачивание ZIP по ID |

Все защищённые эндпоинты требуют заголовок:

```
Authorization: Bearer test-token-123
```

---
