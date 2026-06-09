# Apache Kafka Raft

### Pendahuluan

Selamat datang di panduan Apache Kafka yang telah diperbarui! Sejak awal tahun 2020-an, Apache Kafka telah mengalami evolusi paling signifikan dalam sejarahnya. Ketergantungan pada Apache ZooKeeper, yang selama bertahun-tahun menjadi komponen wajib untuk manajemen klaster, kini telah dihilangkan.

Panduan ini akan berfokus pada cara modern dalam menggunakan Kafka, yaitu dengan mode **KRaft (Kafka Raft Metadata mode)**, yang kini menjadi standar utama. Kita akan membahas mengapa perubahan ini terjadi dan bagaimana Anda bisa memulai dengan arsitektur Kafka yang lebih sederhana, lebih cepat, dan lebih skalabel.

---

### 1. Konsep Inti yang Tetap Relevan

Meskipun arsitektur di balik layar telah berubah, fundamental cara Anda berinteraksi dengan Kafka sebagai pengembang sebagian besar tetap sama. Konsep-konsep ini masih menjadi pilar utama:

*   **Producer:** Aplikasi yang mengirimkan (mempublikasikan) aliran data/pesan.
*   **Consumer:** Aplikasi yang menerima (berlangganan) aliran data/pesan.
*   **Broker:** Server Kafka yang menyimpan data. Beberapa broker membentuk sebuah *cluster*.
*   **Topic:** Kategori atau nama *feed* tempat pesan disimpan dan dipublikasikan. Ibarat sebuah tabel di database.
*   **Partition:** Sebuah *topic* dipecah menjadi beberapa *partition* untuk memungkinkan skalabilitas dan pemrosesan paralel.
*   **Offset:** ID unik berurutan yang diberikan Kafka untuk setiap pesan dalam sebuah partisi.

**Intinya:** Logika bisnis aplikasi Anda yang menggunakan Producer dan Consumer API tidak banyak berubah. Perubahan terbesarnya ada di sisi operasional dan arsitektur.

---

### 2. Perubahan Terbesar: Selamat Tinggal ZooKeeper, Selamat Datang KRaft!

Ini adalah inti dari pembaruan Kafka.

**Dulu (Dengan ZooKeeper):**
Kafka menggunakan ZooKeeper untuk tugas-tugas kritis seperti:
*   Menyimpan metadata klaster (info broker, konfigurasi topic, ACL).
*   Memilih *controller* (broker yang bertanggung jawab mengelola state partisi).

Ini menciptakan arsitektur di mana Anda harus mengelola dan mengamankan dua sistem terdistribusi yang berbeda, yang menambah kerumitan operasional.

**Sekarang (Dengan KRaft):**
Kafka sekarang dapat mengelola metadatanya sendiri menggunakan protokol konsensus yang disebut **Raft**. Beberapa broker ditunjuk sebagai *controller* dan mereka bertanggung jawab untuk menjaga konsistensi metadata di seluruh klaster.

**Keuntungan Utama KRaft:**

1.  **Arsitektur yang Lebih Sederhana:** Anda hanya perlu menginstal, mengonfigurasi, dan memonitor satu sistem: Kafka. Tidak ada lagi klaster ZooKeeper yang terpisah.
2.  **Skalabilitas Masif:** Kafka dengan KRaft dapat mendukung jumlah partisi yang jauh lebih besar (hingga jutaan) dalam satu klaster, sesuatu yang sulit dicapai dengan ZooKeeper.
3.  **Startup dan Pemulihan Lebih Cepat:** Waktu yang dibutuhkan klaster untuk pulih dari kegagalan (misalnya, memilih controller baru) jauh lebih singkat dibandingkan dengan arsitektur berbasis ZooKeeper.

---

### 3. Instalasi Modern Kafka dengan KRaft (Lokal)

Sebelumnya menjalankan `zookeeper-server-start.sh`. Berikut cara saat ini menggunakan Kafka tanpa Zookeeper.

**Prasyarat:**
*   Java Development Kit (JDK) versi 11 atau 17 terinstal.

**Langkah-langkah:**

1.  **Unduh dan Ekstrak Apache Kafka**
    Unduh versi stabil terbaru (misalnya, 4.x) dari situs resmi Apache Kafka dan ekstrak arsipnya.

2.  **Buat ID untuk Klaster Anda**
    Setiap klaster Kafka baru dengan mode KRaft memerlukan ID unik. Jalankan perintah berikut dari direktori root Kafka Anda:
    ```bash
    bin/kafka-storage.sh random-uuid
    ```
    Salin output UUID yang muncul (contoh: `rA5hAWnyS_y-42A42aA4bQ`).

3.  **Format Direktori Penyimpanan (Log)**
    Ganti `YOUR_CLUSTER_ID` dengan UUID yang Anda dapatkan dari langkah sebelumnya. Perintah ini akan menginisialisasi direktori log Anda dengan ID klaster.
    ```bash
    bin/kafka-storage.sh format -t YOUR_CLUSTER_ID -c config/kraft/server.properties
    ```

4.  **Jalankan Server Kafka**
    Sekarang, Anda bisa langsung menjalankan server Kafka. Perhatikan bahwa kita menggunakan file konfigurasi dari direktori `config/kraft/`.
    ```bash
    bin/kafka-server-start.sh config/kraft/server.properties
    ```
    Selesai! Server Kafka Anda sekarang berjalan dalam mode KRaft, tanpa ZooKeeper.

---

### 4. Konfigurasi Dasar KRaft (`server.properties`)

Sebelum menjalankan broker, pastikan file `config/kraft/server.properties` memiliki properti inti berikut:

```properties
# ID unik broker/controller dalam cluster
node.id=1

# Untuk local dev: node ini berperan sebagai broker dan controller
process.roles=broker,controller

# Listener untuk client dan quorum controller
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
advertised.listeners=PLAINTEXT://localhost:9092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
inter.broker.listener.name=PLAINTEXT
controller.listener.names=CONTROLLER

# Daftar voter controller (format: nodeId@host:port)
controller.quorum.voters=1@localhost:9093

# Direktori penyimpanan log
log.dirs=/tmp/kraft-combined-logs

# Faktor replikasi metadata internal (1 untuk single-node dev)
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
```

Untuk local development satu node, konfigurasi di atas sudah cukup. Untuk produksi multi-node, lihat bagian **Setup Production KRaft Cluster**.

---

### 5. Menguji Topic, Producer, dan Consumer

Cara berinteraksi melalui *command-line* tetap sama.

1.  **Buat Topic (dengan jumlah partisi dan replika):**
    ```bash
    bin/kafka-topics.sh \
      --create \
      --topic belajar-kafka-raft \
      --partitions 3 \
      --replication-factor 1 \
      --bootstrap-server localhost:9092
    ```

2.  **Lihat daftar topic:**
    ```bash
    bin/kafka-topics.sh --list --bootstrap-server localhost:9092
    ```

3.  **Lihat detail topic:**
    ```bash
    bin/kafka-topics.sh --describe --topic belajar-kafka-raft --bootstrap-server localhost:9092
    ```

4.  **Jalankan Console Producer:**
    ```bash
    bin/kafka-console-producer.sh --topic belajar-kafka-raft --bootstrap-server localhost:9092
    ```
    > Ketik beberapa pesan di sini, lalu tekan Enter setelah setiap pesan.

5.  **Jalankan Console Consumer (di terminal terpisah):**
    ```bash
    bin/kafka-console-consumer.sh --topic belajar-kafka-raft --bootstrap-server localhost:9092 --from-beginning
    ```
    > Anda akan melihat pesan yang Anda ketik di producer muncul di sini.

---

## Contoh Program dengan Java (Maven)

### Struktur Proyek: `pom.xml`
Buat proyek Maven dan gunakan `pom.xml` berikut untuk mengelola dependensi.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.belajarkafka</groupId>
    <artifactId>kafka-raft</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Dependensi untuk Apache Kafka Clients -->
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>3.7.0</version> <!-- Gunakan versi client yang stabil dan modern -->
        </dependency>

        <!-- Dependensi untuk Logging (opsional tapi sangat direkomendasikan) -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.7</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.7</version>
        </dependency>
    </dependencies>
</project>
```
### 1. Program Kafka Producer (Java)

Buat file `KafkaProducerKraft.java` di dalam `src/main/java/com/belajarkafka/`.

```java
package com.belajarkafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaProducerKraft {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerKraft.class);

    public static void main(String[] args) {
        log.info("Membuat Kafka Producer...");

        // 1. Definisikan properti untuk Producer
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        String topic = "belajar-kafka-raft";
        String value = "Halo dari Java! Pesan ini dikirim ke Kafka modern (KRaft).";
        String key = "id_" + System.currentTimeMillis();

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);

            // Gunakan get() agar error pengiriman bisa ditangkap dengan jelas
            var metadata = producer.send(producerRecord).get();
            log.info("Pesan berhasil dikirim! \n" +
                    "Topic: " + metadata.topic() + "\n" +
                    "Key: " + producerRecord.key() + "\n" +
                    "Partition: " + metadata.partition() + "\n" +
                    "Offset: " + metadata.offset() + "\n" +
                    "Timestamp: " + metadata.timestamp());
        } catch (TimeoutException e) {
            log.error("Timeout saat mengirim pesan ke broker Kafka", e);
        } catch (SerializationException e) {
            log.error("Gagal serialisasi key/value pesan", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread terinterupsi saat menunggu hasil pengiriman", e);
        } catch (ExecutionException e) {
            log.error("Gagal mengirim pesan (execution error)", e.getCause());
        } catch (Exception e) {
            log.error("Terjadi error tak terduga pada Producer", e);
        }
    }
}
```
### 2. Program Kafka Consumer (Java)

Buat file `KafkaConsumerKraft.java` di dalam `src/main/java/com/belajarkafka/`.

```java
package com.belajarkafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaConsumerKraft {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerKraft.class);

    public static void main(String[] args) {
        log.info("Membuat Kafka Consumer...");

        String groupId = "kafka-raft";
        String topic = "belajar-kafka-raft";

        // 1. Definisikan properti untuk Consumer
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Mulai baca dari paling awal

        // 2. Buat instance KafkaConsumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        AtomicBoolean running = new AtomicBoolean(true);

        // 3. Graceful shutdown saat aplikasi dihentikan (Ctrl+C / SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook dipanggil, menghentikan consumer...");
            running.set(false);
            consumer.wakeup(); // Memaksa poll() keluar
        }));

        // 4. Berlangganan (subscribe) ke topic
        consumer.subscribe(Collections.singletonList(topic));

        try {
            // 5. Poll untuk data baru sampai shutdown diminta
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Pesan diterima! \n" +
                            "Key: " + record.key() + ", " +
                            "Value: " + record.value() + "\n" +
                            "Partition: " + record.partition() + ", " +
                            "Offset: " + record.offset());
                }
            }
        } catch (WakeupException e) {
            if (running.get()) {
                throw e; // WakeupException di luar alur shutdown normal
            }
        } catch (TimeoutException e) {
            log.error("Timeout saat polling data dari broker Kafka", e);
        } catch (TopicAuthorizationException | GroupAuthorizationException e) {
            log.error("Consumer tidak memiliki izin akses topic/group", e);
        } catch (UnknownTopicOrPartitionException e) {
            log.error("Topic tidak ditemukan atau partisi belum tersedia", e);
        } catch (Exception e) {
            log.error("Terjadi error tak terduga pada Consumer", e);
        } finally {
            consumer.close();
            log.info("Kafka Consumer ditutup dengan aman.");
        }
    }
}
```
### Cara Menjalankan Program Java

**Prasyarat:**

*   Pastikan Kafka Server Anda sudah berjalan dalam mode KRaft.
*   Pastikan topic `belajar-kafka-raft` sudah dibuat.

**Langkah-langkah:**

1.  **Kompilasi Proyek:**
    Buka terminal di direktori root proyek Anda dan jalankan:
    ```bash
    mvn clean package
    ```

2.  **Jalankan Consumer:**
    Buka terminal **pertama** dan jalankan:
    ```bash
    mvn exec:java -Dexec.mainClass="com.belajarkafka.KafkaConsumerKraft"
    ```

3.  **Jalankan Producer:**
    Buka terminal **kedua** dan jalankan:
    ```bash
    mvn exec:java -Dexec.mainClass="com.belajarkafka.KafkaProducerKraft"
    ```

Di terminal consumer, Anda akan melihat pesan yang dikirim oleh producer muncul.

---

## Setup Production KRaft Cluster (Multi-Broker)

Untuk production, jalankan minimal **3 node** agar quorum controller tetap tersedia saat satu node gagal.

Contoh ringkas `server.properties` per node:

```properties
# Node 1
node.id=1
process.roles=broker,controller
listeners=PLAINTEXT://kafka-1:9092,CONTROLLER://kafka-1:9093
advertised.listeners=PLAINTEXT://kafka-1:9092
controller.listener.names=CONTROLLER
inter.broker.listener.name=PLAINTEXT
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
controller.quorum.voters=1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093
log.dirs=/var/lib/kafka/data

# Node 2 (ubah node.id, listeners, advertised.listeners, log.dirs)
node.id=2
listeners=PLAINTEXT://kafka-2:9092,CONTROLLER://kafka-2:9093
advertised.listeners=PLAINTEXT://kafka-2:9092

# Node 3 (ubah node.id, listeners, advertised.listeners, log.dirs)
node.id=3
listeners=PLAINTEXT://kafka-3:9092,CONTROLLER://kafka-3:9093
advertised.listeners=PLAINTEXT://kafka-3:9092
```

Rekomendasi tambahan production:

* Gunakan `default.replication.factor=3` dan `min.insync.replicas=2`.
* Pisahkan node controller dan broker bila beban tinggi.
* Gunakan storage cepat (SSD/NVMe) dan monitoring aktif.

---

## Security Dasar (SSL/TLS, SASL, ACL)

### 1. SSL/TLS (enkripsi komunikasi)

```properties
listeners=SSL://:9092,CONTROLLER://:9093
advertised.listeners=SSL://kafka-1:9092
listener.security.protocol.map=CONTROLLER:PLAINTEXT,SSL:SSL
ssl.keystore.location=/etc/kafka/secrets/kafka.server.keystore.jks
ssl.keystore.type=PKCS12
ssl.client.auth=required
ssl.truststore.location=/etc/kafka/secrets/kafka.server.truststore.jks
ssl.truststore.type=PKCS12
```

### 2. SASL Authentication

Kafka mendukung SASL (mis. `SCRAM-SHA-256`, `SCRAM-SHA-512`, `PLAIN`, `OAUTHBEARER`) untuk autentikasi client dan antar broker. Untuk production, kombinasi umum adalah **SASL_SSL**.

### 3. ACL Authorization

Aktifkan authorizer dan buat ACL agar akses topic tidak terbuka untuk semua user:

```bash
bin/kafka-acls.sh --bootstrap-server kafka-1:9092 \
  --add --allow-principal User:app-producer \
  --operation Write --topic belajar-kafka-raft

bin/kafka-acls.sh --bootstrap-server kafka-1:9092 \
  --add --allow-principal User:app-consumer \
  --operation Read --topic belajar-kafka-raft --group kafka-raft
```

---

## Monitoring & Troubleshooting

### Health check cepat

```bash
# Cek status API broker
bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Cek metadata cluster dan node controller
bin/kafka-metadata-quorum.sh --bootstrap-server localhost:9092 describe --status

# Cek konfigurasi broker tertentu
bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type brokers --entity-name 1 --describe
```

### Metric penting yang perlu dimonitor

* **Broker availability:** broker up/down, controller active.
* **Request latency:** produce/fetch request latency.
* **Throughput:** bytes in/out per broker dan per topic.
* **Consumer lag:** keterlambatan consumer group terhadap latest offset.
* **Under-replicated partitions:** indikator replikasi tidak sehat.

### Skenario error umum dan solusi singkat

1. **`TimeoutException` saat producer send/poll consumer**  
   Periksa konektivitas host/port broker, listener, dan firewall.
2. **`TopicAuthorizationException` / `GroupAuthorizationException`**  
   Verifikasi ACL user/principal pada topic dan consumer group.
3. **Topic tidak ditemukan (`UnknownTopicOrPartitionException`)**  
   Buat topic terlebih dulu atau periksa salah ketik nama topic.
4. **Consumer tidak menerima data**  
   Cek `group.id`, offset (`--from-beginning`), dan pastikan producer benar-benar mengirim ke topic yang sama.
