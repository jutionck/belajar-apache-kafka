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

### 4. Menguji Topic, Producer, dan Consumer

Cara berinteraksi melalui *command-line* tetap sama.

1.  **Buat Topic:**
    ```bash
    bin/kafka-topics.sh --create --topic belajar-kafka-raft --bootstrap-server localhost:9092
    ```

2.  **Jalankan Console Producer:**
    ```bash
    bin/kafka-console-producer.sh --topic belajar-kafka-raft --bootstrap-server localhost:9092
    ```
    > Ketik beberapa pesan di sini, lalu tekan Enter setelah setiap pesan.

3.  **Jalankan Console Consumer (di terminal terpisah):**
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
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaProducerKraft {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerKraft.class);

    public static void main(String[] args) {
        log.info("Membuat Kafka Producer...");

        // 1. Definisikan properti untuk Producer
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 2. Buat instance KafkaProducer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // 3. Buat record yang akan dikirim
        String topic = "belajar-kafka-v2";
        String value = "Halo dari Java! Pesan ini dikirim ke Kafka modern (KRaft).";
        String key = "id_" + System.currentTimeMillis(); // Key bersifat opsional

        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(topic, key, value);

        // 4. Kirim data (bersifat asynchronous)
        producer.send(producerRecord, (metadata, exception) -> {
            // Callback ini akan dieksekusi setiap kali record berhasil dikirim atau ada exception
            if (exception == null) {
                // Record berhasil dikirim
                log.info("Pesan berhasil dikirim! \n" +
                        "Topic: " + metadata.topic() + "\n" +
                        "Key: " + producerRecord.key() + "\n" +
                        "Partition: " + metadata.partition() + "\n" +
                        "Offset: " + metadata.offset() + "\n" +
                        "Timestamp: " + metadata.timestamp());
            } else {
                log.error("Gagal mengirim pesan", exception);
            }
        });

        // 5. Flush dan tutup Producer
        // Karena send() bersifat asynchronous, kita perlu flush untuk memastikan pesan terkirim sebelum aplikasi berakhir.
        producer.flush();
        producer.close();

        log.info("Producer selesai.");
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
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

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

        // 3. Berlangganan (subscribe) ke topic
        consumer.subscribe(Collections.singletonList(topic));

        // 4. Poll untuk data baru dalam sebuah loop tak terbatas
        while (true) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(1000)); // Timeout 1 detik

            for (ConsumerRecord<String, String> record : records) {
                log.info("Pesan diterima! \n" +
                        "Key: " + record.key() + ", " +
                        "Value: " + record.value() + "\n" +
                        "Partition: " + record.partition() + ", " +
                        "Offset: " + record.offset());
            }
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
