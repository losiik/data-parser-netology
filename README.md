# Company Search Service (2GIS + Spring WebFlux)

Сервис для поиска компаний через 2GIS API, сохранения результатов пользователей и управления историей запросов.

## 🔐 Переменные окружения
Для запуска необходимо создать файл `application.properties` по примеру с `application.example.properties`

Ключ от 2Gis - dgis.api.key

Документация 2Gis https://docs.2gis.com/api/search/places/examples

Соль для хеширования - hashKey

### `application.example.properties`
```properties
dgis.api.key=CHANGE_ME
dgis.base.api.url=https://catalog.api.2gis.com/3.0
hashKey=CHANGE_ME
spring.datasource.url=jdbc:postgresql://localhost:5432/company_search
spring.datasource.username=postgres
spring.datasource.password=postgres
```

## Пример использования API
### Поиск организаций в 2GIS
HTTP-запрос
```curl -X GET \
"http://localhost:8080/api/search?userId=1&city=Москва&text=кофейня" \
-H "accept: application/json"
```

### Пример сокращённого ответа
```{
"items": [
{
"id": "70000001000123456",
"name": "Кофейня №1",
"address": "Тверская улица, 10"
},
{
"id": "70000001000765432",
"name": "Coffee Point",
"address": "Никольская улица, 25"
},
{
"id": "70000001000987654",
"name": "Черный Бублик",
"address": "Арбат, 14"
}
]
}
```

