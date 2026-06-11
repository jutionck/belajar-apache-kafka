# Belajar Apache Kafka

Repository ini berisi project Java pendamping untuk materi [Apache Kafka dengan KRaft](KRAFT.md). Project memperagakan producer dan consumer menggunakan Kafka client secara langsung agar trainee dapat memahami konsep dasarnya sebelum memakai framework.

## Yang Dipraktikkan

- Mengirim event JSON ke topic `orders`.
- Menggunakan `customerId` sebagai key agar event customer yang sama masuk ke partition yang sama.
- Melihat topic, partition, dan offset hasil pengiriman.
- Membaca event melalui consumer group `order-workers-java`.
- Melakukan commit offset setelah proses bisnis.
- Menggunakan producer idempotent dengan `acks=all`.

## Prasyarat

- Java 21
- Apache Maven
- Docker untuk jalur quick start, atau Apache Kafka 4.3 binary distribution

Periksa environment:

```bash
java -version
mvn -version
docker version
```

Perintah `docker version` tidak diperlukan jika memilih Kafka binary distribution.

## Quick Start Menjalankan Aplikasi

Jalankan seluruh perintah Maven dari root repository ini. Gunakan tiga terminal:

| Terminal | Proses |
|---|---|
| Terminal 1 | Broker Kafka |
| Terminal 2 | Java consumer |
| Terminal 3 | Java producer dan perintah observasi |

### 1. Jalankan Kafka dengan Docker

Jalur ini direkomendasikan untuk praktik paling cepat.

```bash
docker run \
  --name kafka-lab \
  --detach \
  --publish 9092:9092 \
  apache/kafka:4.3.0
```

Pastikan container aktif:

```bash
docker ps --filter name=kafka-lab
```

Periksa proses startup:

```bash
docker logs kafka-lab
```

Broker siap digunakan setelah log startup selesai dan container tetap berstatus `Up`.

### 2. Buat Topic

Jalankan Kafka CLI di dalam container:

```bash
docker exec kafka-lab \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic orders \
  --partitions 3 \
  --replication-factor 1
```

Periksa topic:

```bash
docker exec kafka-lab \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic orders
```

Output harus menampilkan topic `orders` dengan tiga partition. Replication factor `1` hanya digunakan untuk lab satu broker.

### 3. Build Project Java

```bash
mvn clean verify
```

Build berhasil jika output berakhir dengan:

```text
Tests run: 2, Failures: 0, Errors: 0
BUILD SUCCESS
```

### 4. Jalankan Consumer

Pada terminal 2:

```bash
mvn exec:java \
  -Dexec.mainClass=com.enigma.upskilling.KafkaConsumerExample
```

Consumer siap menerima event ketika menampilkan:

```text
Menunggu event: topic=orders group=order-workers-java
```

Biarkan proses ini tetap berjalan.

### 5. Jalankan Producer

Pada terminal 3:

```bash
mvn exec:java \
  -Dexec.mainClass=com.enigma.upskilling.KafkaProducerExample
```

Producer akan mengirim satu event menggunakan key default `customer-42`. Contoh output:

```text
Terkirim: key=customer-42 topic=orders partition=... offset=... value=...
```

Terminal consumer kemudian menampilkan event yang diterima:

```text
Diterima: key=customer-42 partition=... offset=... value=...
```

Kirim event dengan customer lain:

```bash
mvn exec:java \
  -Dexec.mainClass=com.enigma.upskilling.KafkaProducerExample \
  -Dexec.args=customer-99
```

Jalankan producer beberapa kali dengan key yang sama, lalu amati bahwa key tersebut masuk ke partition yang sama.

### 6. Periksa Consumer Group dan Lag

```bash
docker exec kafka-lab \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-workers-java
```

Perhatikan:

- `CURRENT-OFFSET`: posisi berikutnya yang akan dibaca consumer group;
- `LOG-END-OFFSET`: posisi terbaru pada partition;
- `LAG`: jumlah event yang belum diproses.

### 7. Hentikan Aplikasi

Hentikan consumer dengan `Ctrl+C`, lalu hentikan broker:

```bash
docker stop kafka-lab
```

Container dapat dijalankan kembali tanpa membuat ulang topic:

```bash
docker start kafka-lab
```

Hapus container dan seluruh data lab ketika sudah tidak diperlukan:

```bash
docker rm --force kafka-lab
```

### Menjalankan Kafka Tanpa Docker

Jika menggunakan Kafka binary distribution, ikuti bagian **Lab Lokal: Menjalankan Kafka KRaft** pada [KRAFT.md](KRAFT.md). Setelah broker aktif, buat topic dengan:

```bash
bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic orders \
  --partitions 3 \
  --replication-factor 1
```

Langkah build, consumer, dan producer tetap sama.

## Konfigurasi Runtime

Nilai default dapat diganti melalui environment variable:

| Environment variable | Default | Kegunaan |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Alamat awal cluster Kafka |
| `KAFKA_TOPIC` | `orders` | Topic yang digunakan |
| `KAFKA_CONSUMER_GROUP` | `order-workers-java` | Consumer group |

Contoh menjalankan consumer dengan group baru:

```bash
KAFKA_CONSUMER_GROUP=order-workers-java-2 \
  mvn exec:java \
  -Dexec.mainClass=com.enigma.upskilling.KafkaConsumerExample
```

Karena group tersebut belum memiliki committed offset dan konfigurasi `auto.offset.reset` adalah `earliest`, consumer akan membaca event dari awal.

## Mengamati Consumer Group

Jika menggunakan Kafka binary distribution:

```bash
bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-workers-java
```

Perhatikan `CURRENT-OFFSET`, `LOG-END-OFFSET`, dan `LAG`.

## Troubleshooting Menjalankan Aplikasi

### Producer atau consumer terus mencoba terhubung

Pastikan broker aktif:

```bash
docker ps --filter name=kafka-lab
docker logs kafka-lab
```

Periksa juga bahwa `KAFKA_BOOTSTRAP_SERVERS` tidak menunjuk ke alamat yang salah.

### Muncul `UnknownTopicOrPartitionException`

Buat topic `orders` terlebih dahulu dan pastikan producer serta consumer memakai nilai `KAFKA_TOPIC` yang sama.

### Consumer tidak menampilkan event lama

Consumer group menyimpan committed offset. Jalankan consumer dengan group baru:

```bash
KAFKA_CONSUMER_GROUP=order-workers-java-baru \
  mvn exec:java \
  -Dexec.mainClass=com.enigma.upskilling.KafkaConsumerExample
```

### Port `9092` sudah digunakan

Cari dan hentikan broker atau container Kafka lain yang menggunakan port tersebut sebelum menjalankan `kafka-lab`.

### Build gagal karena versi Java

Pastikan Maven benar-benar menggunakan Java 21:

```bash
mvn -version
```

Output harus menampilkan `Java version: 21`.

## Struktur Project

```text
src/main/java/com/enigma/upskilling/
├── KafkaExampleConfig.java
├── KafkaProducerExample.java
└── KafkaConsumerExample.java

src/test/java/com/enigma/upskilling/
└── KafkaExampleConfigTest.java
```

`KafkaExampleConfig` memusatkan konfigurasi yang digunakan producer dan consumer. Test memastikan konfigurasi reliability utama tidak berubah tanpa sengaja.

## Catatan Penting

- KRaft mengubah cara metadata cluster dikelola, tetapi API producer dan consumer tetap menggunakan broker melalui `bootstrap.servers`.
- Commit offset menandai progres consumer group; commit tidak menghapus event dari Kafka.
- Manual commit dalam contoh ini menunjukkan urutan `poll -> proses -> commit`. Sistem production tetap memerlukan idempotency, retry strategy, error handling, dan observability.
- Contoh menggunakan JSON tanpa schema registry agar fokus pada konsep dasar.

## Referensi

- [Materi KRaft repository ini](KRAFT.md)
- [Apache Kafka 4.3 Quick Start](https://kafka.apache.org/43/getting-started/quickstart/)
- [Apache Kafka Producer Configuration](https://kafka.apache.org/43/configuration/producer-configs/)
- [Apache Kafka Consumer Configuration](https://kafka.apache.org/43/configuration/consumer-configs/)
