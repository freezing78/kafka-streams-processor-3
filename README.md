## Архитектура и логика работы
Компоненты системы  
1. Основные классы  
MessageProcessor.java - главный класс приложения:  
Создает Kafka Streams топологию  
Обрабатывает входящие сообщения из топика messages  
Применяет фильтрацию заблокированных пользователей  
Выполняет цензуру запрещенных слов  
Отправляет обработанные сообщения в топик filtered_messages  

UserFilterTransformer.java - трансформер для фильтрации сообщений:  
Проверяет, не заблокирован ли отправитель получателем  
Использует State Store для хранения списков блокировок  
Возвращает null для заблокированных сообщений  

BlockedUsersProcessor.java - процессор для обновления блокировок:  
Обрабатывает сообщения из топика blocked_users  
Обновляет State Store с информацией о заблокированных пользователях  
Каждый пользователь имеет свой список заблокированных контактов  

JsonSetSerde.java - сериализатор/десериализатор для хранения Set<String> в State Store  

2. Модели данных  
Message.java - модель сообщения:  

json  
{
  "messageId": "string",
  "sender": "string", 
  "receiver": "string",
  "content": "string",
  "timestamp": "long"
}  
BlockedUser.java - модель блокировки:  

json  
{
  "userId": "string",
  "blockedUserId": "string",
  "timestamp": "long"
}  
3. Топики Kafka  
messages - входящие сообщения для обработки  
filtered_messages - обработанные сообщения после фильтрации и цензуры  
blocked_users - команды блокировки пользователей  
banned_words_control - управление списком запрещенных слов  

Логика работы  
Получение сообщения → Из топика messages  
Парсинг JSON → В объект Message  
Проверка блокировки → UserFilterTransformer проверяет State Store  
Применение цензуры → Замена запрещенных слов на ****  
Сериализация → Обратно в JSON  
Отправка → В топик filtered_messages  

# Развертывание кластера

 Через Portainer docker-compose.yml (Web-интерфейс для управления Docker Compose) Развертывание через Portainer на машине с IP 10.127.1.2
 через kafka UI или docker создаются топики
 
docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic messages --partitions 3 --replication-factor 2

docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic filtered_messages --partitions 3 --replication-factor 2

docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic blocked_users --partitions 3 --replication-factor 2

docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic banned_words_control --partitions 1 --replication-factor 1

 
# Приложение запускается с машины IP 10.127.1.4
Запуск приложений  
Запустите в указанном порядке:  
MessageProcessor (основное приложение обработки)  
TestProducer (тестовый клиент)  

# Инструкция по тестированию
Команды TestProducer  
После запуска TestProducer доступны команды:  

text
Kafka Test Producer
Available commands: message, block, ban, exit
=================================
Команда message - отправка сообщения  
text  
Enter command: message  
Enter message ID: [уникальный_ид]  
Enter sender: [отправитель]  
Enter receiver: [получатель]   
Enter content: [текст_сообщения]  

Команда block - блокировка пользователя  
text  
Enter command: block  
Enter user ID (who is blocking): [кто_блокирует]  
Enter user ID to block: [кого_блокируют]  

Команда ban - управление запрещенными словами  
text  
Enter command: ban  
Enter action (add/remove): [add/remove]  
Enter word to add to banned list: [слово]  

Команда exit - выход  
text  
Enter command: exit  

# Тестовые данные для проверки  
Тест 1: Базовая отправка сообщения  
Команда:  

text  
Enter command: message  
Enter message ID: test1  
Enter sender: alice  
Enter receiver: bob  
Enter content: Hello Bob, this is a test message  
Ожидаемый результат в MessageProcessor:  

text  
Processing message from alice to bob  
ALLOWED: Message from alice to bob passed through  
Applied censorship to message: Hello Bob, this is a test message → Hello Bob, this is a test message  
Тест 2: Проверка цензуры запрещенных слов  
Команда:  

text  
Enter command: message  
Enter message ID: test2  
Enter sender: charlie  
Enter receiver: diana  
Enter content: This message contains spam and fraud words  
Ожидаемый результат:  

text  
Applied censorship to message: This message contains spam and fraud words → This message contains **** and ***** words  
Тест 3: Добавление нового запрещенного слова  
Команда:  

text  
Enter command: ban  
Enter action (add/remove): add  
Enter word to add to banned list: badword  
Ожидаемый результат:  

text  
✓ Banned words control sent successfully  
Тест 4: Проверка нового запрещенного слова  
Команда:  

text  
Enter command: message  
Enter message ID: test3  
Enter sender: eve  
Enter receiver: frank  
Enter content: This contains badword that should be censored  
Ожидаемый результат:  

text  
Applied censorship to message: This contains badword that should be censored → This contains ******* that should be censored  
Тест 5: Блокировка пользователя  
Команда:  

text  
Enter command: block  
Enter user ID (who is blocking): bob  
Enter user ID to block: alice  
Ожидаемый результат:  

text  
✓ User block command sent successfully  
Тест 6: Проверка блокировки  
Команда:  

text  
Enter command: message  
Enter message ID: test4  
Enter sender: alice  
Enter receiver: bob  
Enter content: This message should be blocked  
Ожидаемый результат:  

text  
BLOCKED: Message from alice to bob was blocked  
Тест 7: Удаление запрещенного слова  
Команда:  

text  
Enter command: ban  
Enter action (add/remove): remove  
Enter word to remove from banned list: spam  
Ожидаемый результат:  

text  
✓ Banned words control sent successfully  
Тест 8: Проверка удаления слова  
Команда:  

text  
Enter command: message  
Enter message ID: test5  
Enter sender: user1  
Enter receiver: user2  
Enter content: This spam should not be censored now  
Ожидаемый результат:  

text  
Applied censorship to message: This spam should not be censored now → This spam should not be censored now  
Полный сценарий тестирования  
Для полной проверки выполните последовательно:  

message - отправка обычного сообщения  
message - отправка сообщения с запрещенными словами (spam, fraud)  
ban - добавление нового запрещенного слова  
message - проверка нового запрещенного слова  
block - блокировка пользователя  
message - проверка блокировки  
ban - удаление запрещенного слова  
message - проверка удаления слова  
