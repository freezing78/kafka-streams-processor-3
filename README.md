Развертывание кластера

 Через Portainer docker-compose.yml (Web-интерфейс для управления Docker Compose) Развертывание через Portainer на машине с IP 10.127.1.2
 через kafka UI или docker создаются топики
 
docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic messages --partitions 3 --replication-factor 2
docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic filtered_messages --partitions 3 --replication-factor 2
docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic blocked_users --partitions 3 --replication-factor 2
docker exec -it kafka-0 kafka-topics --bootstrap-server kafka-0:9092 --create --topic banned_words_control --partitions 1 --replication-factor 1
 
Приложение запускается с машины IP 10.127.1.4
