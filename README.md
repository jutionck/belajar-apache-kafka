# Belajar Apache Kafka

Repository ini berisi project Java pendamping untuk materi [Apache Kafka dengan KRaft](KRAFT.md). Project memperagakan producer dan consumer menggunakan Kafka client secara langsung agar trainee dapat memahami konsep dasarnya sebelum memakai framework.

Mulai dari tahap **raw Kafka client** untuk memahami dasar, lalu lanjutkan ke implementasi real application Spring Boot pada repository [springboot-kafka-demo](https://github.com/jutionck/springboot-kafka-demo).

## Tujuan Repository

Repository ini bukan hanya contoh cara mengirim dan menerima message. Materi disusun untuk menjawab tiga pertanyaan utama:

1. **Why:** masalah apa yang diselesaikan Kafka?
2. **When:** kapan Kafka tepat atau tidak tepat digunakan?
3. **How:** bagaimana event dikirim, disimpan, dibaca, dan diproses dengan benar?

Setelah menyelesaikan materi, trainee diharapkan dapat:

- menjelaskan perbedaan komunikasi synchronous dan asynchronous;
- menentukan apakah sebuah kebutuhan benar-benar memerlukan Kafka;
- merancang topic, message key, partition, dan consumer group;
- memahami hubungan antara offset, retry, duplicate event, dan delivery semantics;
- menjalankan producer dan consumer Java;
- mengembangkan pola dasar tersebut menjadi aplikasi Spring Boot;
- mengenali kebutuhan tambahan sebelum Kafka digunakan di production.

## Why: Mengapa Menggunakan Kafka?

Dalam aplikasi sederhana, service dapat memanggil service lain secara langsung melalui HTTP:

```text
Order Service --HTTP--> Payment Service
```

Pendekatan ini mudah dipahami, tetapi pengirim dan penerima menjadi terhubung langsung. Jika `Payment Service` lambat atau tidak tersedia, request pada `Order Service` dapat ikut lambat atau gagal.

Kafka menyediakan pola komunikasi berbasis event:

```text
Order Service --publish--> Kafka topic orders --consume--> Payment Worker
                                               |
                                               +-----------> Analytics Worker
                                               |
                                               +-----------> Notification Worker
```

`Order Service` mempublikasikan fakta bahwa suatu kejadian telah terjadi. Consumer memproses event tersebut secara independen.

Kafka berguna karena menyediakan:

| Kebutuhan | Peran Kafka |
|---|---|
| Memisahkan lifecycle service | Producer tidak perlu mengetahui seluruh consumer |
| Menahan lonjakan traffic | Event disimpan sementara dan diproses sesuai kapasitas consumer |
| Memproses event secara asynchronous | Request utama tidak harus menunggu seluruh pekerjaan lanjutan |
| Mengirim satu event ke beberapa sistem | Consumer group berbeda dapat membaca event yang sama |
| Menjaga urutan untuk entitas tertentu | Event dengan key yang sama masuk ke partition yang sama |
| Mengulang pemrosesan | Consumer dapat membaca kembali event dari offset tertentu |
| Menyimpan jejak event untuk periode tertentu | Event mengikuti konfigurasi retention topic |

Kafka bukan sekadar message broker untuk memindahkan data. Kafka adalah **distributed event log**: event ditambahkan ke log, disimpan berdasarkan retention, dan dapat dibaca oleh banyak consumer.

### Contoh Masalah yang Diselesaikan

Ketika order dibuat, aplikasi perlu:

- menyimpan order;
- mengurangi stok;
- memulai pembayaran;
- mengirim notifikasi;
- memperbarui analytics.

Jika semuanya dipanggil langsung dalam satu request, kegagalan satu dependency dapat menggagalkan seluruh proses. Dengan Kafka, `Order Service` dapat mempublikasikan event `ORDER_CREATED`, kemudian setiap consumer menangani tugasnya sendiri.

Konsekuensinya, sistem menjadi **eventually consistent**. Response HTTP dapat selesai sebelum seluruh consumer menyelesaikan pekerjaannya. Aplikasi harus dirancang untuk menerima kondisi tersebut.

## When: Kapan Kafka Digunakan?

Kafka tepat digunakan ketika terdapat satu atau beberapa kondisi berikut:

- producer dan consumer perlu dikembangkan atau diskalakan secara independen;
- pekerjaan tidak harus selesai sebelum response utama dikembalikan;
- traffic datang dalam lonjakan dan perlu diserap sebagai buffer;
- satu event harus diproses oleh beberapa jenis consumer;
- event perlu disimpan dan dapat diproses ulang;
- throughput tinggi lebih penting daripada response per-message yang sangat sederhana;
- sistem memerlukan stream data berkelanjutan untuk integration, analytics, atau audit.

Contoh use case:

| Use case | Contoh event |
|---|---|
| E-commerce | `ORDER_CREATED`, `PAYMENT_COMPLETED`, `STOCK_RESERVED` |
| Perbankan | `TRANSACTION_POSTED`, `FRAUD_CHECK_REQUESTED` |
| Logistik | `PACKAGE_SCANNED`, `DELIVERY_STATUS_CHANGED` |
| Observability | application log, metric, audit event |
| Data platform | perubahan data operasional menuju data warehouse |

### Kapan Tidak Perlu Kafka?

Jangan memilih Kafka hanya karena ingin memakai arsitektur modern. Kafka menambah biaya operasional, model konsistensi asynchronous, dan kemungkinan duplicate processing.

Gunakan pendekatan yang lebih sederhana ketika:

- aplikasi hanya membutuhkan request-response langsung;
- pemanggil wajib menerima hasil akhir saat itu juga;
- volume data kecil dan hanya ada satu producer serta satu consumer sederhana;
- database queue atau scheduled job sudah mencukupi;
- tim belum siap mengoperasikan broker, monitoring lag, retry, DLT, dan schema evolution.

Perbandingan sederhana:

| Kebutuhan | Pilihan awal |
|---|---|
| Meminta saldo terbaru dan langsung menerima jawaban | HTTP/gRPC |
| Menjalankan pekerjaan background sederhana | Job queue |
| Menyimpan state transaksi utama | Database |
| Menyiarkan event ke banyak sistem dan dapat replay | Kafka |
| Memproses stream event ber-throughput tinggi | Kafka |

Kafka biasanya melengkapi database dan API, bukan menggantikan keduanya.

## How: Bagaimana Kafka Bekerja?

Alur dasar Kafka:

```text
Producer
   |
   | record(key, value)
   v
Topic: orders
   ├── Partition 0: offset 0, 1, 2, ...
   ├── Partition 1: offset 0, 1, 2, ...
   └── Partition 2: offset 0, 1, 2, ...
                     |
                     v
              Consumer Group
```

1. **Producer** membuat record yang berisi key, value, dan metadata.
2. Record dikirim ke sebuah **topic**.
3. Topic dibagi menjadi beberapa **partition** agar data dapat diproses secara paralel.
4. Kafka menambahkan record ke akhir partition dan memberikan **offset**.
5. **Consumer** membaca record dari partition.
6. **Consumer group** menyimpan progres baca melalui committed offset.

### Topic, Partition, Offset, dan Key

| Konsep | Fungsi |
|---|---|
| Topic | Nama stream event, misalnya `orders` |
| Partition | Unit penyimpanan dan paralelisme topic |
| Offset | Posisi record di dalam satu partition |
| Key | Menentukan pengelompokan record ke partition |
| Value | Payload event |

Contoh pada repository ini:

```text
topic = orders
key   = customer-42
value = {"eventType":"ORDER_CREATED", ...}
```

Semua event dengan key `customer-42` diarahkan secara konsisten ke partition yang sama selama jumlah partition dan strategi partitioning tetap sesuai. Kafka hanya menjamin ordering **di dalam satu partition**, bukan di seluruh topic.

### Consumer Group dan Skalabilitas

Consumer dengan group yang sama bekerja sebagai satu unit:

```text
orders: partition-0 --> consumer-A
orders: partition-1 --> consumer-B
orders: partition-2 --> consumer-C
```

Dalam satu consumer group, satu partition hanya diproses oleh satu consumer aktif pada satu waktu. Karena itu, jumlah partition menentukan batas atas paralelisme consumer group.

Consumer group berbeda dapat membaca event yang sama secara independen:

```text
orders --> group payment-workers
       --> group notification-workers
       --> group analytics-workers
```

### Delivery Semantics dan Duplicate Event

Pada pola at-least-once, consumer biasanya:

```text
poll event -> proses bisnis -> commit offset
```

Jika proses bisnis berhasil tetapi consumer mati sebelum commit offset, event dapat dibaca kembali. Karena itu, handler production harus **idempotent**: memproses event yang sama dua kali tidak boleh menghasilkan efek bisnis ganda.

Kafka tidak otomatis menyelesaikan seluruh consistency problem aplikasi. Developer tetap harus merancang:

- idempotency;
- retry dan backoff;
- dead-letter topic;
- schema evolution;
- observability dan consumer lag;
- strategi transaksi antara database dan publish event.

## How: Merancang Penggunaan Kafka di Aplikasi

Sebelum menulis producer atau consumer, jawab pertanyaan desain berikut.

### 1. Tentukan Event Bisnis

Event sebaiknya menyatakan fakta yang sudah terjadi, bukan instruksi teknis yang terlalu terikat pada implementasi consumer.

```text
Baik:   ORDER_CREATED
Baik:   PAYMENT_COMPLETED
Hindari: CALL_NOTIFICATION_SERVICE_NOW
```

Contoh kontrak event:

```json
{
  "eventId": "3833508d-5484-4de9-8b97-596ce9e0a96e",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-06-11T04:56:00Z",
  "order": {
    "customerId": "customer-42",
    "total": 175000
  }
}
```

Field penting:

| Field | Alasan |
|---|---|
| `eventId` | Digunakan untuk tracing dan idempotency |
| `eventType` | Menjelaskan jenis fakta bisnis |
| `occurredAt` | Menjelaskan waktu kejadian bisnis |
| data bisnis | Informasi minimum yang diperlukan consumer |

Event contract harus memiliki ownership dan strategi perubahan schema. Jangan mengubah atau menghapus field tanpa mempertimbangkan consumer lama.

### 2. Tentukan Topic

Topic merepresentasikan stream event yang memiliki tujuan, lifecycle, retention, dan aturan akses yang jelas.

Pertanyaan yang perlu dijawab:

- siapa pemilik topic?
- event apa yang boleh masuk?
- berapa lama event disimpan?
- apakah event perlu replay?
- siapa yang boleh produce dan consume?
- apakah data mengandung informasi sensitif?

Pada lab ini digunakan topic `orders`. Pada sistem besar, naming convention harus disepakati, misalnya:

```text
commerce.orders.created.v1
commerce.payments.completed.v1
```

### 3. Pilih Message Key

Key menentukan partition dan batas ordering.

| Kebutuhan ordering | Contoh key |
|---|---|
| Semua perubahan satu order harus berurutan | `orderId` |
| Semua event satu customer harus berurutan | `customerId` |
| Tidak memerlukan ordering per entitas | key dapat kosong, dengan konsekuensi distribusi tertentu |

Pada repository ini, `customerId` digunakan sebagai key agar event customer yang sama masuk ke partition yang sama.

Pemilihan key yang buruk dapat menyebabkan:

- ordering bisnis rusak;
- satu partition menerima traffic jauh lebih besar;
- consumer group sulit diskalakan.

### 4. Tentukan Consumer Group

Gunakan group yang sama untuk beberapa instance dengan tanggung jawab bisnis yang sama. Gunakan group berbeda untuk tanggung jawab bisnis yang berbeda.

```text
orders --> payment-workers       # beberapa instance, satu tanggung jawab
orders --> notification-workers  # tanggung jawab berbeda
orders --> analytics-workers     # tanggung jawab berbeda
```

Nama group harus stabil. Mengganti group berarti Kafka melihat consumer sebagai pembaca baru dengan offset sendiri.

### 5. Rancang Failure Handling

Consumer production harus membedakan error sementara dan error permanen.

| Jenis error | Contoh | Penanganan |
|---|---|---|
| Sementara | dependency timeout | retry dengan backoff |
| Permanen | payload tidak valid | kirim ke DLT dan investigasi |
| Duplicate | event pernah diproses | abaikan secara idempotent |
| Kapasitas kurang | lag terus meningkat | scale consumer atau evaluasi partition |

Retry tanpa batas dapat memblokir partition. DLT tanpa monitoring hanya memindahkan masalah. Keduanya harus disertai metric, alert, dan prosedur recovery.

### 6. Tentukan Transaction Boundary

Masalah umum terjadi ketika aplikasi harus menyimpan data ke database sekaligus mempublikasikan event:

```text
simpan order ke database -> publish ORDER_CREATED
```

Jika database berhasil tetapi publish gagal, state database dan Kafka menjadi tidak konsisten. Untuk sistem production, pertimbangkan pola seperti **transactional outbox**:

```text
Database transaction:
  1. simpan order
  2. simpan outbox event

Outbox publisher:
  3. publish event ke Kafka
  4. tandai outbox sudah dipublish
```

Project Spring Boot pada repository ini belum memakai database atau transactional outbox karena fokusnya adalah alur dasar producer, broker, consumer, retry, dan DLT.

### Checklist Keputusan

Sebelum menyetujui penggunaan Kafka, pastikan tim dapat menjawab:

- apa alasan asynchronous communication dibutuhkan?
- apa nama dan makna event?
- siapa owner event dan topic?
- key apa yang menjaga ordering bisnis?
- berapa partition yang diperlukan dan mengapa?
- consumer group apa saja yang membaca event?
- bagaimana consumer menangani duplicate event?
- bagaimana retry dan DLT dimonitor?
- bagaimana perubahan schema dilakukan?
- bagaimana konsistensi database dan publish event dijaga?

## Cara Menggunakan Repository Ini

Urutan belajar yang direkomendasikan:

| Tahap | Fokus | Dokumen/project |
|---|---|---|
| 1 | Memahami mental model Kafka dan KRaft | [KRAFT.md](KRAFT.md) |
| 2 | Menjalankan producer dan consumer tanpa framework | Quick start pada README ini |
| 3 | Mengamati partition, offset, consumer group, dan lag | Kafka CLI pada README ini |
| 4 | Melihat pola aplikasi yang lebih realistis | [springboot-kafka-demo](https://github.com/jutionck/springboot-kafka-demo) |
| 5 | Mendiskusikan production readiness | Bagian production pada [KRAFT.md](KRAFT.md) |

Project raw Kafka client sengaja dijalankan melalui command terpisah agar trainee melihat peran broker, producer, dan consumer secara eksplisit. Project Spring Boot menggabungkan pola tersebut ke bentuk aplikasi yang lebih dekat dengan penggunaan nyata: REST API, background consumer, retry, DLT, health check, dan Docker Compose.

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
- Jangan menganggap response `202 Accepted` berarti seluruh consumer telah selesai memproses event. Response tersebut hanya menyatakan request telah diterima untuk diproses secara asynchronous.
- Topic, partition count, retention, dan message key adalah keputusan desain. Nilai tersebut tidak seharusnya dipilih tanpa memahami kebutuhan ordering, throughput, replay, dan kapasitas consumer.

## Referensi

- [Materi KRaft repository ini](KRAFT.md)
- [Implementasi real application Spring Boot](https://github.com/jutionck/springboot-kafka-demo)
- [Apache Kafka 4.3 Quick Start](https://kafka.apache.org/43/getting-started/quickstart/)
- [Apache Kafka Producer Configuration](https://kafka.apache.org/43/configuration/producer-configs/)
- [Apache Kafka Consumer Configuration](https://kafka.apache.org/43/configuration/consumer-configs/)
