# Голосовалка с публичной верификацией на immudb — план проекта

## Идея одной фразой

Обычная система голосования говорит "доверьтесь нам, мы всё правильно посчитали".
Эта система вместо этого даёт каждому голосующему **математическое доказательство**,
что его голос учтён и не изменён — без необходимости доверять серверу.

## Архитектура

```
Frontend (React/vanilla JS)
    │
    ▼
Backend API (Kotlin + Spring Boot, immudb4j SDK)
    │
    ▼
immudb (Docker)
```

### Схема ключей в immudb

| Ключ | Значение | Операция |
|---|---|---|
| `poll:{poll_id}` | `{question, options[], created_at, closes_at, status}` | VerifiedSet |
| `vote:{poll_id}:{vote_id}` | `{option, timestamp}` | VerifiedSet |
| `voter:{poll_id}:{voter_hash}` | `{vote_id}` (анти-дубль) | VerifiedSet |
| `digest:{poll_id}:closed` | `{root_hash, tx_id, closed_at}` | VerifiedSet |

`voter_hash` = `SHA256(one_time_code + salt)` — так голос анонимен, но повторное
голосование по тому же коду невозможно.

---

## Фаза 0 — окружение (1 вечер)

- [ ] Поднять immudb в Docker (`docker run -d -p 3322:3322 codenotary/immudb:latest`)
- [ ] Установить immudb CLI (`immuclient`) и потыкать руками: `login`, `set`, `verified-get`
- [ ] Создать проект Spring Boot (Kotlin, Gradle), подключить `io.codenotary:immudb4j` с Maven Central
- [ ] Написать health-check: `ImmuClient` логинится, делает один `verifiedSet`/`verifiedGet`

## Фаза 1 — модель данных и создание опроса (1-2 вечера)

- [ ] Структуры `Poll`, `Vote`, `VoterRecord` (Go structs + JSON)
- [ ] Эндпоинт `POST /admin/polls` — создаёт опрос, генерирует `poll_id` (UUID)
- [ ] `VerifiedSet(poll:{poll_id}, ...)` при создании
- [ ] Эндпоинт `GET /polls/{poll_id}` — отдаёт вопрос и варианты (без результатов, пока опрос открыт)
- [ ] Генерация one-time кодов для голосующих (просто список UUID на N человек, для пет-проекта достаточно)

## Фаза 2 — голосование (2-3 вечера, самое важное)

- [ ] Эндпоинт `POST /polls/{poll_id}/vote` — принимает `{option, one_time_code}`
- [ ] Проверка: код не использован (`Get(voter:{poll_id}:{hash})` → должен быть NotFound)
- [ ] `VerifiedSet(vote:{poll_id}:{vote_id}, {option, timestamp})`
- [ ] `VerifiedSet(voter:{poll_id}:{hash}, {vote_id})` — помечаем код использованным
- [ ] Собрать **receipt** для пользователя: `{vote_id, tx_id, option}` — это его "квиток"
- [ ] Написать unit-тест: два голоса с одним кодом → второй должен упасть

⚠️ Важный нюанс: `vote_id` должен быть непредсказуемым (криптографически случайным),
иначе кто-то сможет перебором проверять чужие голоса по угаданным id.

## Фаза 3 — верификация и подсчёт (2 вечера)

- [ ] Эндпоинт `GET /polls/{poll_id}/verify/{vote_id}` — делает `VerifiedGet`,
      возвращает `{option, verified: true/false, tx_id}`
- [ ] Эндпоинт `GET /polls/{poll_id}/tally` — сканирует все `vote:{poll_id}:*`
      (immudb `Scan` по префиксу), агрегирует по вариантам
- [ ] На фронте: страница "проверить свой голос" — вводишь `vote_id` из квитка,
      видишь подтверждение
- [ ] На фронте: страница результатов с общим тэлли + списком отдельных
      голосов (без привязки к личности) и кнопкой "verify" у каждого

## Фаза 4 — закрытие опроса и публичный digest (1-2 вечера)

- [ ] Эндпоинт `POST /admin/polls/{poll_id}/close` — меняет статус, фиксирует текущий root hash
- [ ] `VerifiedSet(digest:{poll_id}:closed, {root_hash, tx_id, closed_at})`
- [ ] Опционально: публикация digest во внешнее место (публичный gist / pastebin) —
      это защита от сценария "админ immudb-сервера подменил и голоса, и сам digest"
- [ ] Страница "аудит" — показывает digest, кнопка "скачать все голоса как JSON"
      для независимой перепроверки кем угодно

## Фаза 5 — защита и полировка (по желанию, 2+ вечера)

- [ ] Rate limiting на голосование (простой in-memory throttle по IP)
- [ ] Красивая обработка ошибок ("код уже использован", "опрос закрыт")
- [ ] Docker-compose, объединяющий immudb + backend + frontend в один `docker compose up`
- [ ] README с объяснением механики (можно взять кусок из объяснения хеш-цепочек выше)

---

## Возможные "вау"-фичи, если останется запал

- **Визуализация inclusion proof**: на странице верификации показывать не просто
  "✓ verified", а сам путь хешей (упрощённо — как на диаграмме, которую я показал раньше)
- **Публичный API для сторонних верификаторов**: любой скрипт может дёрнуть твой
  эндпоинт и независимо пересчитать тэлли
- **Сравнение с "обычной" БД**: сделать вторую версию на Postgres без ledger и
  показать side-by-side, как легко там подделать голос без обнаружения

## Стек

- Backend: **Kotlin + Spring Boot**, immudb4j SDK
- immudb: Docker, локально
- Frontend: обычный React или даже просто vanilla JS + fetch — фронтенд тут не главное

---

# Технический дизайн (Kotlin + Spring Boot)

## Слои приложения

```
Controller  → принимает HTTP, валидирует DTO
Service     → бизнес-логика (анти-дубль, генерация id, тэлли)
LedgerService → тонкая обёртка над ImmuClient (единственное место, где вызывается immudb4j)
immudb      → внешнее хранилище
```

Ключевое архитектурное решение: **вся работа с immudb4j изолирована в одном классе**
`LedgerService`. Это даёт две вещи: (1) остальной код не знает про специфику
immudb-протокола и (2) `VerificationException` (сигнал подделки данных) ловится
и обрабатывается централизованно, а не размазана по контроллерам.

## Зависимости (build.gradle.kts)

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.codenotary:immudb4j:LATEST") // проверить актуальную версию на Maven Central
}
```

## Конфигурация клиента

```kotlin
@Configuration
class ImmudbConfig(
    @Value("\${immudb.host}") private val host: String,
    @Value("\${immudb.port}") private val port: Int,
    @Value("\${immudb.user}") private val user: String,
    @Value("\${immudb.password}") private val password: String,
) {
    @Bean(destroyMethod = "close")
    fun immuClient(): ImmuClient {
        val client = ImmuClient.newBuilder()
            .withServerUrl(host)
            .withServerPort(port)
            .build()
        client.login(user, password)
        client.useDatabase("defaultdb")
        return client
    }
}
```

## Модели данных

```kotlin
enum class PollStatus { OPEN, CLOSED }

data class Poll(
    val id: String,
    val question: String,
    val options: List<String>,
    val createdAt: Instant,
    val closesAt: Instant?,
    val status: PollStatus,
)

data class VoteRecord(val option: String, val timestamp: Instant)
data class VoteReceipt(val voteId: String, val txId: Long, val option: String)

data class CastVoteRequest(
    @field:NotBlank val option: String,
    @field:NotBlank val oneTimeCode: String,
)
```

## LedgerService — единственная точка входа в immudb

```kotlin
@Service
class LedgerService(
    private val immuClient: ImmuClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <T> put(key: String, value: T): Long {
        val bytes = objectMapper.writeValueAsBytes(value)
        val txHeader = immuClient.verifiedSet(key.toByteArray(), bytes)
        return txHeader.id
    }

    fun <T> getOrNull(key: String, clazz: Class<T>): T? =
        try {
            val entry = immuClient.verifiedGet(key.toByteArray())
            objectMapper.readValue(entry.value, clazz)
        } catch (e: KeyNotFoundException) {
            null
        } catch (e: VerificationException) {
            log.error("tamper_detected key={}", key, e)
            throw e // пробрасываем наверх — это не обычная ошибка, а сигнал подделки
        }

    // Уточнить актуальную сигнатуru scan-метода в текущей версии immudb4j —
    // концептуально: получить все записи с префиксом "vote:{pollId}:"
    fun scanByPrefix(prefix: String): List<ByteArray> {
        val request = ScanRequest.newBuilder().withPrefix(prefix.toByteArray()).build()
        return immuClient.scan(request).entriesList.map { it.value.toByteArray() }
    }
}
```

## VoteService — бизнес-логика голосования

```kotlin
class AlreadyVotedException : RuntimeException("Этот код уже использован")

@Service
class VoteService(private val ledger: LedgerService) {

    @Transactional
    fun castVote(pollId: String, req: CastVoteRequest): VoteReceipt {
        val voterHash = sha256(req.oneTimeCode + SALT)
        val voterKey = "voter:$pollId:$voterHash"

        if (ledger.getOrNull(voterKey, Map::class.java) != null) {
            throw AlreadyVotedException()
        }

        val voteId = UUID.randomUUID().toString() // криптографически случайный
        val voteKey = "vote:$pollId:$voteId"
        val txId = ledger.put(voteKey, VoteRecord(req.option, Instant.now()))
        ledger.put(voterKey, mapOf("voteId" to voteId))

        return VoteReceipt(voteId, txId, req.option)
    }

    fun verifyVote(pollId: String, voteId: String): VoteRecord? =
        ledger.getOrNull("vote:$pollId:$voteId", VoteRecord::class.java)
}
```

## Контроллер

```kotlin
@RestController
@RequestMapping("/api/polls/{pollId}")
class VoteController(private val voteService: VoteService) {

    @PostMapping("/vote")
    fun vote(@PathVariable pollId: String, @Valid @RequestBody req: CastVoteRequest): VoteReceipt =
        voteService.castVote(pollId, req)

    @GetMapping("/verify/{voteId}")
    fun verify(@PathVariable pollId: String, @PathVariable voteId: String): ResponseEntity<VoteRecord> =
        voteService.verifyVote(pollId, voteId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
}
```

## Централизованная обработка ошибок

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AlreadyVotedException::class)
    fun handleAlreadyVoted(e: AlreadyVotedException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(e.message ?: "conflict"))

    @ExceptionHandler(VerificationException::class)
    fun handleTamperDetected() =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError("Обнаружено нарушение целостности данных — обратитесь к администратору"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException) =
        ResponseEntity.badRequest().body(ApiError(e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" }))
}

data class ApiError(val message: String)
```

## API-контракт

| Метод | Путь | Тело/параметры | Ответ |
|---|---|---|---|
| POST | `/admin/polls` | `{question, options[]}` | `Poll` |
| POST | `/admin/polls/{id}/close` | — | `{rootHash, txId}` |
| GET | `/polls/{id}` | — | `Poll` (без результатов, пока открыт) |
| POST | `/polls/{id}/vote` | `{option, oneTimeCode}` | `VoteReceipt` (409, если код использован) |
| GET | `/polls/{id}/verify/{voteId}` | — | `VoteRecord` или 404 |
| GET | `/polls/{id}/tally` | — | `{option: count}` |

## Важные технические нюансы

- **`voteId` — обязательно `UUID.randomUUID()`**, никогда не инкрементный счётчик:
  предсказуемый id позволил бы перебором дёргать `verify/{voteId}` и деанонимизировать голоса.
- **`voterHash` не хранит и не логирует сырой `oneTimeCode`** — только его хеш с солью,
  иначе связка "код → голос" легко восстанавливается из логов.
- **`@Transactional` на `castVote` не защищает от гонки** между проверкой
  `voterKey` и записью — при высокой конкуррентности нужен либо `synchronized`
  по `voterHash`, либо compare-and-swap на уровне immudb (если SDK его поддерживает
  для конкретной версии — стоит перепроверить в документации).
- immudb4j API у разных версий слегка отличается (особенно scan/prefix-запросы) —
  перед тем как копировать код `LedgerService` один в один, свериться с
  README текущей версии на GitHub (`codenotary/immudb4j`).
