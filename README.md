# Belajar Apache Kafka

Materi ini sekarang direferensikan dari dokumentasi resmi Apache Kafka:
- Introduction (Kafka 4.3): https://kafka.apache.org/43/getting-started/introduction/
- Getting Started: https://kafka.apache.org/43/getting-started/
- Quick Start: https://kafka.apache.org/43/getting-started/quickstart/

> Catatan: pembahasan khusus file `KRAFT.md` akan dilakukan terpisah.

---

## Ringkasan Konsep Inti (berdasarkan Introduction Kafka 4.3)

Apache Kafka adalah **platform event streaming terdistribusi** untuk:
1. Publish dan subscribe event stream
2. Menyimpan event stream secara andal
3. Memproses event stream secara real-time maupun retrospektif

Konsep utama:
- **Producer**: aplikasi yang mengirim event ke Kafka
- **Consumer**: aplikasi yang membaca event dari Kafka
- **Broker**: server Kafka
- **Topic**: kategori event
- **Partition**: pembagian topic untuk paralelisme
- **Offset**: penanda urutan event dalam partition
- **Replication**: salinan data antar broker untuk ketersediaan tinggi

---

## Mengapa Kafka Dipakai

Contoh use case umum:
- Integrasi data antar layanan dan sistem
- Event-driven architecture dan microservices
- Monitoring dan analytics real-time
- Pemrosesan transaksi dan notifikasi real-time

---

## Praktik Dasar CLI Kafka

### 1) Buat Topic
Linux/macOS:
```bash
bin/kafka-topics.sh --create \
  --topic example-topic \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-topics.bat --create --topic example-topic --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
```

### 2) Lihat Daftar Topic
Linux/macOS:
```bash
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

### 3) Kirim Pesan (Producer)
Linux/macOS:
```bash
bin/kafka-console-producer.sh --topic example-topic --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-console-producer.bat --topic example-topic --bootstrap-server localhost:9092
```

### 4) Terima Pesan (Consumer)
Linux/macOS:
```bash
bin/kafka-console-consumer.sh --topic example-topic --from-beginning --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-console-consumer.bat --topic example-topic --from-beginning --bootstrap-server localhost:9092
```

---

## Consumer Group (Dasar)

Consumer group dipakai agar pembacaan pesan dalam satu group terbagi antar consumer (load balancing).

Contoh:
```bash
bin/kafka-console-consumer.sh --topic example-topic --bootstrap-server localhost:9092 --group belajar-group
```

---

## Contoh Java di Repository Ini

Source code contoh:
- `/tmp/workspace/jutionck/belajar-apache-kafka/src/main/java/com/enigma/upskilling/KafkaProducerExample.java`
- `/tmp/workspace/jutionck/belajar-apache-kafka/src/main/java/com/enigma/upskilling/KafkaConsumerExample.java`

Dependensi Kafka client pada project saat ini:
```xml
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>2.8.0</version>
</dependency>
```

---

## Referensi Resmi

- Kafka 4.3 Introduction: https://kafka.apache.org/43/getting-started/introduction/
- Kafka 4.3 Getting Started: https://kafka.apache.org/43/getting-started/
- Kafka 4.3 Quick Start: https://kafka.apache.org/43/getting-started/quickstart/

