# Поисковый движок для веб-сайтов на Java Spring Boot.
Spring-приложение для индексации страниц и полнотекстового поиска по ним с поддержкой морфологии русского языка, работающее с локально установленной базой данных MySQL.

# Функциональность
- Полная индексация сайтов
- Индексация отдельных страниц
- Поиск по проиндексированным данным
- Статистика индексации
- Веб-интерфейс для управления

# Технологии
- Java 17
- Spring Boot 2.7.1
- Spring Data JPA
- MySQL 8.0
- Jsoup (парсинг HTML)
- Maven 4.0

# Особенности
- Для работы с русским текстом из-за отсутствия оригинальных инструментов из ТЗ был использован стеммер Портера.

# Работа с интерфейсом
- Dashboard: выводит информацию о наличии и статусе сайтов в конфигурации
  <img width="1033" height="506" alt="image" src="https://github.com/user-attachments/assets/8bd65235-caef-482a-b768-27af534ebf3d" />
  
- Management: интерфейс начала и остановки индексации а так же добавление и обновления страниц
  <img width="1033" height="353" alt="image" src="https://github.com/user-attachments/assets/4dee37d5-94af-46d4-ade3-c5283e599ac9" />
  
- Search: поиск информации по индексированным сайтам
  <img width="1049" height="360" alt="image" src="https://github.com/user-attachments/assets/eb1efd89-034f-47ae-9515-5a3e775d5ed1" />

# Структура программы
-  search-engine/
   -  src/main/java/searchengine
   -   config/ # Конфигурация
   -   controllers/ # REST контроллеры
   -   dto/ # DTO классы
   -   model/ # JPA сущности
   -   repository/ # Spring Data репозитории
   -   services/ # Бизнес-логика
   -   stemmer/ # Реализация стеммера
   -   Application.java
 - src/main/resources/
   - static/ # Статические файлы
   - templates/ # HTML шаблоны
   - application.yaml/ # Файл конфигурации
 - pom.xml

# Структура Api
- /api/statistics - статистика индексации
- /api/startIndexing - запуск индексации
- /api/stopIndexing - остановка индексации
- /api/indexPage - индексация одной страницы
- /api/search - поиск по запросу
