## Hands On Kafka

### PART Introduction
> **Apache Kafka** merupakan salah satu aplikasi message service/broker atau publish subscribe yang paling banyak digunakan saat ini.
> 
> **What is Kafka?** 
> Apache Kafka adalah sebuah publish-subscribe messaging system. Messagging system adalah system yang bisa digunakan untuk mengirimkan message antar proses, aplikasi dan server.
> 
> Pada dasarnya kafka adalah message broker yang bertujuan untuk mengirimkan pesan dari sebuah producer yang akan di terima oleh sebuah consumer. Didalam kafka, setiap pesan yang masuk hanya akan ditambahkan saja (append only). Tidak akan dilakukan pengubahan terhadap data yang sebelumnya. Hal inilah yang menyebabkan proses messaging yang ada dalam kafka di claim sangat cepat.

![alt text](https://miro.medium.com/max/1246/0*PNtmqvwCkatFDxZo.png)

> **Producer**: Application that sends the messages. 
> 
> **Consumer**: Application that receives the messages. 
> 
> **Message**: Information that is sent from the producer to a consumer through Apache Kafka.
>
> **Connection**: A connection is a TCP connection between your application and the Kafka broker.
>
> **Topic**: A Topic is a category/feed name to which messages are stored and published.
> 
> **Topic partition**: Kafka topics are divided into a number of partitions, which allows you to split data across multiple brokers.
> 
> **Replicas** A replica of a partition is a “backup” of a partition. Replicas never read or write data. They are used to prevent data loss.
> 
> **Consumer** Group: A consumer group includes the set of consumer processes that are subscribing to a specific topic.
> 
> **Offset**: The offset is a unique identifier of a record within a partition. It denotes the position of the consumer in the partition.
> 
> **Node**: A node is a single computer in the Apache Kafka cluster.
> 
> **Cluster**: A cluster is a group of nodes i.e., a group of computers.
> 
> Read more : https://docs.google.com/document/d/1lG0z2lKBoJHAQtiu342qDzHJOGpZmg3-Z4Ly_4BFRSg/edit?usp=sharing

## Step

### PART Installation
1. Download kafka on https://kafka.apache.org/downloads
2. Unzip and move directory if you want

### PART Kafka Configuration
**Kafka**

Open the terminal go to your kafka folder:\
`cd kafka_2.13-2.8.0`

Tab 1
Run zookeeper: \
`bin/zookeeper-server-start.sh config/zookeeper.properties`

Tab 2
Run server: \
`bin/kafka-server-start.sh config/server.properties`

Tab 3
List topic :\
`bin/kafka-topics.sh --list --zookeeper localhost:2181`

Create topic:\
`bin/kafka-topics.sh --create --topic simple-notification --bootstrap-server localhost:9092`

Note:\
**simple-notification** -> is the name of topic\
**localhost:9092** -> is default port

Tab 4
Producer:\
`bin/kafka-console-producer.sh --topic simple-notification --bootstrap-server localhost:9092`\

*Create your message*

Tab 5
Consumer:\
`bin/kafka-console-consumer.sh --topic simple-notification --from-beginning --bootstrap-server localhost:9092`

Check your tab Kafka Server when you access from consumer
`Assignment received from leader for group console-consumer-64172 for generation 1. The group has 1 members, 0 of which are static. (kafka.coordinator.group.GroupCoordinator)`

