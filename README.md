# Belajar Apache Kafka

Materi di repository ini membahas dasar Apache Kafka untuk praktik lokal menggunakan Java.

Referensi resmi utama:
- Introduction (Kafka 4.3): https://kafka.apache.org/43/getting-started/introduction/
- Getting Started: https://kafka.apache.org/43/getting-started/
- Quick Start: https://kafka.apache.org/43/getting-started/quickstart/

Materi KRaft (tanpa ZooKeeper) dibahas lebih detail di:
- `KRAFT.md`

---

## Tujuan Belajar

Setelah mengikuti materi ini, kamu diharapkan memahami:
1. Peran **Producer**, **Consumer**, **Broker**, **Topic**, **Partition**, dan **Offset**.
2. Cara membuat topic dan mengirim/menerima pesan via CLI Kafka.
3. Cara menjalankan contoh Producer dan Consumer Java di repository ini.

---

## Prasyarat

- JDK 8+ (sesuai project saat ini)
- Apache Maven (`mvn`)
- Apache Kafka lokal (disarankan mode KRaft)

> Catatan: gunakan `mvn`, bukan `./mvnw`, karena file wrapper Maven belum tersedia di repository ini.

---

## Ringkasan Konsep Inti

Apache Kafka adalah **platform event streaming terdistribusi** untuk:
1. Publish/subscribe event stream
2. Menyimpan event stream secara andal
3. Memproses event stream secara real-time maupun retrospektif

Komponen utama:
- **Producer**: aplikasi pengirim event
- **Consumer**: aplikasi pembaca event
- **Broker**: server Kafka
- **Topic**: kategori event
- **Partition**: pembagian topic untuk paralelisme
- **Offset**: urutan event dalam partition
- **Replication**: salinan data antar broker

---

## Praktik Dasar CLI Kafka

### 1) Buat Topic

Linux/macOS:
```bash
bin/kafka-topics.sh --create \
  --topic topic-java \
  --partitions 3 \
  --replication-factor 1 \
  --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-topics.bat --create --topic topic-java --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092
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

### 3) Kirim Pesan (Console Producer)

Linux/macOS:
```bash
bin/kafka-console-producer.sh --topic topic-java --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-console-producer.bat --topic topic-java --bootstrap-server localhost:9092
```

### 4) Terima Pesan (Console Consumer)

Linux/macOS:
```bash
bin/kafka-console-consumer.sh --topic topic-java --from-beginning --bootstrap-server localhost:9092
```

Windows:
```bash
.\bin\windows\kafka-console-consumer.bat --topic topic-java --from-beginning --bootstrap-server localhost:9092
```

---

## Consumer Group (Dasar)

Consumer group dipakai agar pembacaan pesan dalam satu group terbagi antar consumer (load balancing).

Contoh:
```bash
bin/kafka-console-consumer.sh --topic topic-java --bootstrap-server localhost:9092 --group belajar-group
```

---

## Contoh Java di Repository Ini

Source code contoh:
- `src/main/java/com/enigma/upskilling/KafkaProducerExample.java`
- `src/main/java/com/enigma/upskilling/KafkaConsumerExample.java`

Dependensi Kafka client yang dipakai project:
```xml
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>2.8.0</version>
</dependency>
```

### Menjalankan contoh Java

1. Pastikan broker Kafka aktif di `localhost:9092` dan topic `topic-java` sudah dibuat.
2. Build project:
   ```bash
   mvn -q -DskipTests compile
   ```
3. Jalankan consumer (terminal 1):
   ```bash
   mvn -q -DskipTests org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.enigma.upskilling.KafkaConsumerExample
   ```
4. Jalankan producer (terminal 2):
   ```bash
   mvn -q -DskipTests org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.enigma.upskilling.KafkaProducerExample
   ```

---

## Materi Lanjutan

Untuk materi modern Kafka tanpa ZooKeeper (KRaft), lanjutkan ke:
- `KRAFT.md`

---

## Referensi Resmi

- Kafka 4.3 Introduction: https://kafka.apache.org/43/getting-started/introduction/
- Kafka 4.3 Getting Started: https://kafka.apache.org/43/getting-started/
- Kafka 4.3 Quick Start: https://kafka.apache.org/43/getting-started/quickstart/
