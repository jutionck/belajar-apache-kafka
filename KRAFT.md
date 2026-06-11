# Apache Kafka dengan KRaft

Dokumen ini adalah bahan ajar teknis untuk trainee dengan tingkat pengalaman campuran. Bagian **Wajib** ditujukan untuk seluruh trainee, sedangkan bagian **Pendalaman** dapat digunakan instruktur untuk menjelaskan cara kerja dan pertimbangan operasional Kafka.

Contoh dalam dokumen menggunakan:

- Apache Kafka 4.3 dalam mode KRaft
- Java 21 untuk menjalankan broker Kafka dan contoh aplikasi Java
- Java, Node.js, dan Go sebagai contoh aplikasi client

> **Catatan versi:** Kafka 4.x hanya menggunakan KRaft. ZooKeeper tetap dibahas secara singkat agar trainee dapat memahami dokumentasi atau sistem Kafka lama.

---

## Hasil Belajar

Setelah menyelesaikan materi dan praktik, trainee diharapkan mampu:

1. Menjelaskan peran broker, controller, producer, consumer, topic, partition, replica, dan offset.
2. Menjelaskan perbedaan metadata cluster dan data event.
3. Menjalankan Kafka KRaft satu node untuk development.
4. Membuat topic serta mengirim dan membaca event melalui CLI.
5. Menghubungkan aplikasi Java, Node.js, atau Go ke Kafka.
6. Menjelaskan pengaruh key terhadap partition dan ordering.
7. Menjelaskan hubungan partition, consumer group, offset, dan consumer lag.
8. Mengidentifikasi perbedaan konfigurasi development dan production.

## Rekomendasi Alur Pengajaran

| Sesi | Fokus | Durasi perkiraan |
|---|---|---:|
| 1 | Mental model Kafka dan KRaft | 45 menit |
| 2 | Lab CLI: topic, producer, consumer, group | 60 menit |
| 3 | Lab aplikasi: Java, Node.js, atau Go | 90 menit |
| 4 | Reliability, operasi, dan troubleshooting | 60 menit |

Untuk kelas dengan level campuran:

- Semua trainee mengerjakan lab CLI terlebih dahulu.
- Trainee memilih satu bahasa untuk lab aplikasi.
- Trainee berpengalaman mengerjakan eksperimen partition, consumer group, dan kegagalan broker sebagai latihan tambahan.

---

## 1. Mental Model Kafka (Wajib)

Apache Kafka adalah platform **distributed event streaming**. Aplikasi menulis event ke Kafka, Kafka menyimpan event secara berurutan, dan satu atau lebih aplikasi lain dapat membacanya.

Contoh event:

```json
{
  "eventId": "evt-1001",
  "customerId": "customer-42",
  "type": "ORDER_CREATED",
  "total": 175000
}
```

### Komponen utama

| Komponen | Tanggung jawab |
|---|---|
| **Producer** | Mengirim event ke topic. |
| **Consumer** | Membaca event dari topic. |
| **Broker** | Menerima, menyimpan, dan menyajikan data event. |
| **Topic** | Nama log atau kategori event, misalnya `orders`. |
| **Partition** | Pecahan topic yang menjadi unit penyimpanan dan paralelisme. |
| **Offset** | Posisi event yang meningkat secara berurutan dalam satu partition. |
| **Replica** | Salinan partition pada broker lain untuk fault tolerance. |
| **Controller** | Mengelola metadata cluster, bukan melayani aliran data aplikasi secara normal. |

### Topic bukan tabel database

Analogi topic sebagai tabel dapat membantu di awal, tetapi memiliki batas:

- Event di topic umumnya tidak diperbarui di tempat.
- Event baru ditambahkan ke ujung log.
- Offset adalah posisi dalam partition, bukan ID bisnis.
- Data lama dihapus berdasarkan kebijakan retention atau compaction.
- Consumer mengelola progres baca masing-masing.

### Partition, key, dan ordering

Kafka hanya menjamin urutan event **di dalam satu partition**, bukan di seluruh topic.

Producer biasanya menentukan partition berdasarkan key:

```text
key customer-42 -> partition 1
key customer-99 -> partition 0
```

Jika semua event milik `customer-42` memakai key yang sama, event tersebut biasanya masuk ke partition yang sama dan urutannya dapat dipertahankan.

> **Pertanyaan untuk trainee:** Jika sebuah topic memiliki tiga partition, apakah Kafka menjamin seluruh event dalam topic dapat dibaca sesuai urutan waktu global?

Jawaban: tidak. Ordering hanya dijamin per partition.

### Consumer group

Consumer yang menggunakan `group.id` yang sama bekerja sebagai satu kelompok:

- Satu partition hanya diberikan kepada satu consumer aktif dalam group yang sama.
- Satu consumer dapat menerima beberapa partition.
- Jika jumlah consumer lebih banyak daripada jumlah partition, sebagian consumer akan idle.
- Group yang berbeda dapat membaca event yang sama secara independen.

Contoh topic dengan tiga partition:

```text
orders
├── partition 0 -> consumer A
├── partition 1 -> consumer B
└── partition 2 -> consumer A
```

Offset yang telah diproses disimpan per **consumer group + topic + partition**.

### Delivery semantics

Istilah yang perlu dibedakan:

| Semantik | Arti praktis |
|---|---|
| **At-most-once** | Event mungkin hilang, tetapi tidak diproses ulang. |
| **At-least-once** | Event tidak sengaja hilang, tetapi mungkin diproses lebih dari sekali. |
| **Exactly-once** | Efek pemrosesan terjadi tepat sekali dalam batas sistem dan konfigurasi tertentu. |

Pendekatan umum aplikasi adalah **at-least-once** dengan handler yang idempotent. Contohnya, simpan `eventId` yang sudah diproses agar retry tidak membuat order kedua.

---

## 2. Apa Itu KRaft? (Wajib)

KRaft adalah arsitektur metadata Kafka yang menggunakan protokol konsensus berbasis Raft. KRaft menggantikan ZooKeeper sebagai tempat pengelolaan metadata cluster.

### Data event dan metadata adalah dua hal berbeda

```text
Client application
       |
       | produce / fetch event
       v
  Kafka broker(s)  ---------- menyimpan data topic dan partition
       |
       | membaca perubahan metadata
       v
 KRaft controller quorum ----- menyimpan metadata cluster
```

Contoh metadata cluster:

- broker yang aktif;
- daftar topic dan partition;
- lokasi replica dan partition leader;
- konfigurasi cluster;
- ACL dan informasi operasional lain.

KRaft menyimpan perubahan metadata sebagai log internal yang direplikasi oleh controller quorum. Salah satu controller menjadi **active controller**; controller lain mengikuti log metadata dan siap mengambil alih.

### Mengapa quorum memerlukan mayoritas?

Perubahan metadata hanya dapat dilanjutkan jika mayoritas controller voter tersedia.

| Jumlah controller | Mayoritas | Kegagalan yang dapat ditoleransi |
|---:|---:|---:|
| 1 | 1 | 0 |
| 3 | 2 | 1 |
| 5 | 3 | 2 |

Rumus toleransi kegagalan untuk jumlah voter ganjil:

```text
toleransi = (jumlah_controller - 1) / 2
```

Satu controller cukup untuk lab lokal, tetapi bukan untuk production.

### Peran node KRaft

Properti `process.roles` menentukan peran proses Kafka:

```properties
# Broker saja
process.roles=broker

# Controller saja
process.roles=controller

# Broker dan controller dalam satu proses
process.roles=broker,controller
```

Mode gabungan cocok untuk development. Untuk deployment production yang penting, gunakan controller dan broker terpisah agar beban broker tidak mengganggu quorum metadata.

### Perbandingan singkat

| Aspek | Kafka lama dengan ZooKeeper | Kafka dengan KRaft |
|---|---|---|
| Penyimpanan metadata | ZooKeeper | Metadata log internal Kafka |
| Controller election | Dikoordinasikan melalui ZooKeeper | Dipilih oleh controller quorum |
| Sistem yang dioperasikan | Kafka dan ZooKeeper | Kafka |
| Kafka 4.x | Tidak didukung | Satu-satunya mode |

---

## 3. Lab Lokal: Menjalankan Kafka KRaft (Wajib)

### Prasyarat

- Java 21
- Apache Kafka 4.3 binary distribution
- Terminal Linux, macOS, atau WSL

Periksa Java:

```bash
java -version
```

Masuk ke direktori hasil ekstraksi Kafka sebelum menjalankan perintah berikut.

### 3.1 Format storage dan jalankan broker

Generate cluster ID:

```bash
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
echo "$KAFKA_CLUSTER_ID"
```

Format storage untuk cluster standalone:

```bash
bin/kafka-storage.sh format \
  --standalone \
  --cluster-id "$KAFKA_CLUSTER_ID" \
  --config config/server.properties
```

Jalankan Kafka:

```bash
bin/kafka-server-start.sh config/server.properties
```

> `format` dilakukan saat membuat storage cluster baru, bukan setiap kali broker dinyalakan. Melakukan format ulang pada storage yang digunakan dapat menghilangkan metadata cluster.

Alternatif cepat dengan image resmi:

```bash
docker run --name kafka-lab --rm -p 9092:9092 apache/kafka:4.3.0
```

### 3.2 Verifikasi broker dan metadata quorum

Buka terminal baru:

```bash
bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

```bash
bin/kafka-metadata-quorum.sh \
  --bootstrap-server localhost:9092 \
  describe --status
```

Perhatikan nilai berikut:

- `ClusterId`: identitas cluster;
- `LeaderId`: active controller;
- `CurrentVoters`: anggota controller quorum;
- `HighWatermark`: metadata yang telah dikomit oleh quorum.

### 3.3 Buat dan periksa topic

```bash
bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders \
  --partitions 3 \
  --replication-factor 1
```

```bash
bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic orders
```

Replication factor `1` hanya sesuai untuk lab satu broker.

### 3.4 Kirim event dengan key

```bash
bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --property parse.key=true \
  --property key.separator=:
```

Masukkan beberapa baris:

```text
customer-42:{"eventId":"evt-1001","type":"ORDER_CREATED"}
customer-99:{"eventId":"evt-1002","type":"ORDER_CREATED"}
customer-42:{"eventId":"evt-1003","type":"ORDER_PAID"}
```

### 3.5 Baca event sebagai consumer group

```bash
bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --group order-workers \
  --from-beginning \
  --property print.key=true \
  --property print.partition=true \
  --property print.offset=true
```

Periksa posisi dan lag consumer group:

```bash
bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-workers
```

Kolom penting:

- `CURRENT-OFFSET`: offset berikutnya yang akan dibaca group;
- `LOG-END-OFFSET`: ujung log partition saat ini;
- `LAG`: selisih antara ujung log dan posisi group.

### 3.6 Eksperimen kelas

1. Jalankan dua consumer dengan `group.id` yang sama. Amati pembagian partition.
2. Jalankan consumer ketiga dan keempat. Amati consumer yang idle.
3. Jalankan consumer dengan group baru. Buktikan bahwa event yang sama dapat dibaca ulang.
4. Kirim beberapa event dengan key yang sama. Periksa apakah partition-nya sama.

---

## 4. Lab Aplikasi Multi-Bahasa

Semua contoh berikut memakai kontrak yang sama:

```text
bootstrap server : localhost:9092
topic            : orders
key              : customer-42
consumer group   : order-workers-<bahasa>
```

Gunakan key untuk menjaga affinity event bisnis. Gunakan JSON hanya untuk memudahkan lab; dalam sistem production, tentukan schema dan strategi evolusinya secara eksplisit.

Contoh kode berfokus pada penggunaan Kafka client. Lengkapi `import`, entrypoint, dan graceful shutdown sesuai struktur proyek masing-masing bahasa.

### 4.1 Java

Implementasi Java lengkap tersedia di repository ini:

- `src/main/java/com/enigma/upskilling/KafkaProducerExample.java`
- `src/main/java/com/enigma/upskilling/KafkaConsumerExample.java`
- Panduan menjalankan aplikasi: [README.md](README.md#quick-start-menjalankan-aplikasi)

Dependency Maven:

```xml
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>4.3.0</version>
</dependency>
```

Producer:

```java
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
props.put(ProducerConfig.ACKS_CONFIG, "all");

try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
    ProducerRecord<String, String> record = new ProducerRecord<>(
        "orders",
        "customer-42",
        "{\"eventId\":\"evt-java-1\",\"type\":\"ORDER_CREATED\"}"
    );

    RecordMetadata metadata = producer.send(record).get();
    System.out.printf(
        "partition=%d offset=%d%n",
        metadata.partition(),
        metadata.offset()
    );
}
```

Consumer:

```java
Properties props = new Properties();
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-workers-java");
props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
    consumer.subscribe(List.of("orders"));

    while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
        for (ConsumerRecord<String, String> record : records) {
            System.out.printf(
                "key=%s partition=%d offset=%d value=%s%n",
                record.key(), record.partition(), record.offset(), record.value()
            );
            // Jalankan proses bisnis sebelum commit.
        }
        consumer.commitSync();
    }
}
```

### 4.2 Node.js

Contoh menggunakan KafkaJS:

```bash
npm install kafkajs
```

Producer:

```javascript
const { Kafka } = require("kafkajs");

const kafka = new Kafka({
  clientId: "orders-node-producer",
  brokers: ["localhost:9092"],
});

const producer = kafka.producer();

async function main() {
  await producer.connect();
  await producer.send({
    topic: "orders",
    messages: [
      {
        key: "customer-42",
        value: JSON.stringify({
          eventId: "evt-node-1",
          type: "ORDER_CREATED",
        }),
      },
    ],
  });
  await producer.disconnect();
}

main().catch(console.error);
```

Consumer:

```javascript
const { Kafka } = require("kafkajs");

const kafka = new Kafka({
  clientId: "orders-node-consumer",
  brokers: ["localhost:9092"],
});

const consumer = kafka.consumer({ groupId: "order-workers-node" });

async function main() {
  await consumer.connect();
  await consumer.subscribe({ topic: "orders", fromBeginning: true });
  await consumer.run({
    eachMessage: async ({ partition, message }) => {
      console.log({
        key: message.key?.toString(),
        partition,
        offset: message.offset,
        value: message.value?.toString(),
      });
    },
  });
}

main().catch(console.error);
```

### 4.3 Go

Contoh menggunakan `kafka-go`:

```bash
go get github.com/segmentio/kafka-go
```

Producer:

```go
writer := &kafka.Writer{
	Addr:     kafka.TCP("localhost:9092"),
	Topic:    "orders",
	Balancer: &kafka.Hash{},
}
defer writer.Close()

err := writer.WriteMessages(context.Background(), kafka.Message{
	Key:   []byte("customer-42"),
	Value: []byte(`{"eventId":"evt-go-1","type":"ORDER_CREATED"}`),
})
if err != nil {
	log.Fatal(err)
}
```

Consumer:

```go
reader := kafka.NewReader(kafka.ReaderConfig{
	Brokers:     []string{"localhost:9092"},
	Topic:       "orders",
	GroupID:     "order-workers-go",
	StartOffset: kafka.FirstOffset,
})
defer reader.Close()

for {
	message, err := reader.FetchMessage(context.Background())
	if err != nil {
		log.Fatal(err)
	}

	log.Printf(
		"key=%s partition=%d offset=%d value=%s",
		message.Key,
		message.Partition,
		message.Offset,
		message.Value,
	)

	// Jalankan proses bisnis sebelum commit.
	if err := reader.CommitMessages(context.Background(), message); err != nil {
		log.Fatal(err)
	}
}
```

### Perbandingan istilah client

| Konsep | Java | Node.js/KafkaJS | Go/kafka-go |
|---|---|---|---|
| Alamat awal cluster | `bootstrap.servers` | `brokers` | `Brokers` / `Addr` |
| Consumer group | `group.id` | `groupId` | `GroupID` |
| Membaca event | `poll()` | `consumer.run()` | `FetchMessage()` |
| Commit eksplisit | `commitSync()` | konfigurasi commit di consumer | `CommitMessages()` |

API setiap library berbeda, tetapi konsep Kafka yang mendasarinya tetap sama.

---

## 5. Pendalaman Teknis untuk Instruktur

### 5.1 Alur produce

Secara ringkas:

1. Producer menggunakan `bootstrap.servers` untuk menemukan cluster.
2. Producer meminta metadata topic dan partition.
3. Partitioner memilih partition, biasanya berdasarkan key.
4. Producer mengirim record ke leader partition.
5. Leader menulis record dan replica follower mereplikasi data.
6. Broker mengirim acknowledgement sesuai konfigurasi `acks`.

`bootstrap.servers` tidak harus berisi semua broker. Client menggunakannya sebagai titik awal untuk menemukan metadata cluster.

### 5.2 Leader, replica, ISR, dan acknowledgement

- Setiap partition memiliki satu leader.
- Producer dan consumer berkomunikasi dengan leader partition.
- Replica follower menyalin log dari leader.
- ISR atau **in-sync replicas** adalah replica yang cukup mengikuti leader.
- `acks=all` meminta acknowledgement berdasarkan replica yang memenuhi aturan durability cluster.

Konfigurasi production yang lazim untuk data penting:

```properties
default.replication.factor=3
min.insync.replicas=2
```

Pada producer:

```properties
acks=all
enable.idempotence=true
```

Konfigurasi ini meningkatkan durability, tetapi tidak menggantikan desain retry, idempotency, observability, dan disaster recovery.

### 5.3 Offset dan commit

Membaca event dan melakukan commit offset adalah dua tindakan berbeda.

```text
fetch event -> proses bisnis -> commit offset
```

Jika consumer gagal setelah proses bisnis tetapi sebelum commit, event dapat dibaca ulang. Karena itu, handler at-least-once harus idempotent.

> Commit offset menandai progres baca. Commit bukan acknowledgement yang menghapus event dari Kafka.

### 5.4 Rebalance

Rebalance terjadi ketika pembagian partition dalam consumer group perlu dihitung ulang, misalnya saat:

- consumer bergabung atau keluar;
- consumer dianggap gagal;
- jumlah partition berubah;
- subscription berubah.

Selama rebalance, pemrosesan dapat tertunda. Monitor durasi proses, interval poll, error consumer, dan lag.

### 5.5 Retention dan compaction

Kafka memiliki dua pola kebijakan penyimpanan utama:

- **Delete retention**: event lama dihapus berdasarkan waktu atau ukuran.
- **Log compaction**: Kafka mempertahankan nilai terbaru untuk setiap key, tanpa menjamin hanya ada satu record per key setiap saat.

Retention tidak bergantung pada apakah event sudah dibaca consumer.

### 5.6 Static dan dynamic controller quorum

Kafka modern mendukung dynamic controller quorum. Pada dynamic quorum:

- `controller.quorum.bootstrap.servers` membantu node menemukan quorum;
- membership controller dapat diubah dengan tooling KRaft;
- `controller.quorum.voters` tidak digunakan.

Static quorum lama mendefinisikan seluruh voter melalui `controller.quorum.voters`. Jangan mencampur konfigurasi static dan dynamic quorum tanpa memahami proses format serta upgrade cluster.

Untuk lab standalone, perintah `kafka-storage.sh format --standalone` menyiapkan dynamic quorum satu controller.

---

## 6. Production Readiness

Konfigurasi lab sebelumnya **bukan** konfigurasi production. Sebelum production, putuskan dan uji hal berikut.

### Topologi dan availability

- Gunakan tiga atau lima dedicated controller sesuai target toleransi kegagalan.
- Gunakan beberapa broker dan replication factor yang sesuai.
- Sebarkan replica dan controller pada failure domain yang berbeda.
- Uji kehilangan broker dan controller secara terencana.
- Tetapkan kapasitas disk, network, dan retention berdasarkan beban.

### Reliability aplikasi

- Tentukan key berdasarkan kebutuhan ordering.
- Aktifkan idempotent producer untuk mengurangi duplikasi akibat retry.
- Tentukan retry, timeout, dan dead-letter strategy.
- Buat consumer handler idempotent.
- Tentukan kapan offset di-commit.
- Gunakan schema dan strategi kompatibilitas event.

### Security

- Gunakan TLS untuk enkripsi koneksi.
- Gunakan SASL atau mekanisme autentikasi yang sesuai.
- Terapkan ACL dengan prinsip least privilege.
- Pisahkan credential tiap aplikasi dan environment.
- Jangan menggunakan listener `PLAINTEXT` terbuka pada jaringan yang tidak tepercaya.

### Monitoring minimum

- broker dan controller availability;
- offline dan under-replicated partitions;
- produce/fetch request latency dan error rate;
- bytes in/out dan kapasitas disk;
- consumer lag per group;
- controller quorum health dan metadata lag;
- jumlah serta durasi consumer rebalance.

---

## 7. Troubleshooting

### `TimeoutException` atau client tidak terhubung

Periksa:

1. Broker benar-benar aktif.
2. Host dan port dapat dijangkau dari mesin client.
3. `advertised.listeners` berisi alamat yang dapat dijangkau client.
4. DNS, firewall, TLS, dan autentikasi sesuai.

`listeners` menentukan tempat broker menerima koneksi. `advertised.listeners` menentukan alamat yang diberitahukan broker kepada client.

### Consumer tidak menerima event

Periksa:

```bash
bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic orders
```

```bash
bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-workers
```

Kemungkinan penyebab:

- producer dan consumer menggunakan topic berbeda;
- consumer group sudah berada di akhir log;
- consumer lain dalam group menerima partition;
- deserialization gagal;
- consumer berhenti melakukan poll;
- ACL tidak mengizinkan read atau akses group.

### Cluster tidak memiliki quorum controller

Gejala umum adalah perubahan metadata tidak dapat dilakukan, misalnya pembuatan topic gagal.

Periksa:

```bash
bin/kafka-metadata-quorum.sh \
  --bootstrap-server localhost:9092 \
  describe --status
```

Pastikan mayoritas controller voter tersedia dan saling terhubung.

### Kesalahan umum saat lab

| Kesalahan | Dampak |
|---|---|
| Menjalankan `format` setiap startup | Berisiko membuat ulang metadata storage. |
| Menganggap offset sebagai ID event | ID tidak unik lintas partition. |
| Menambah consumer melebihi jumlah partition | Sebagian consumer idle. |
| Tidak memakai key untuk event yang harus berurutan | Event dapat tersebar ke partition berbeda. |
| Menganggap commit menghapus event | Event tetap mengikuti retention topic. |
| Menyamakan konfigurasi satu node dengan production | Tidak memiliki fault tolerance. |

---

## 8. Latihan dan Evaluasi

### Latihan dasar

1. Buat topic `payments` dengan empat partition.
2. Kirim lima event dengan dua customer key berbeda.
3. Buktikan bahwa event dengan key sama masuk ke partition yang sama.
4. Jalankan dua consumer dalam group `payment-workers`.
5. Jelaskan pembagian partition yang terjadi.

### Latihan menengah

1. Hentikan salah satu consumer dan amati rebalance.
2. Jalankan consumer dengan group baru dan baca event dari awal.
3. Tunda proses consumer selama beberapa detik dan amati lag.
4. Jelaskan risiko commit offset sebelum proses bisnis selesai.

### Tantangan lanjutan

Rancang pipeline:

```text
order-service -> orders -> payment-service -> payments -> notification-service
```

Dokumentasikan:

- key setiap topic;
- jumlah partition awal;
- consumer group;
- strategi retry dan dead-letter;
- bentuk idempotency;
- retention;
- metric dan alert utama.

### Pertanyaan evaluasi

1. Apa perbedaan broker dan controller?
2. Mengapa tiga controller dapat bertahan dari satu kegagalan, tetapi dua controller tidak memberi toleransi kegagalan yang sama?
3. Di level mana Kafka menjamin ordering?
4. Mengapa consumer group dengan enam consumer tidak dapat memproses enam event secara paralel jika topic hanya memiliki tiga partition?
5. Apa yang terjadi jika proses bisnis berhasil tetapi consumer gagal sebelum commit offset?
6. Mengapa replication factor `1` tidak sesuai untuk data production yang penting?

---

## Referensi Resmi

- [Apache Kafka 4.3 Quick Start](https://kafka.apache.org/43/getting-started/quickstart/)
- [Apache Kafka KRaft Operations](https://kafka.apache.org/43/operations/kraft/)
- [Apache Kafka Broker Configuration](https://kafka.apache.org/43/configuration/broker-configs/)
- [Apache Kafka Producer Configuration](https://kafka.apache.org/43/configuration/producer-configs/)
- [Apache Kafka Consumer Configuration](https://kafka.apache.org/43/configuration/consumer-configs/)
- [Apache Kafka Security](https://kafka.apache.org/43/security/security-overview/)

> Selalu cocokkan dokumentasi dengan versi Kafka yang benar-benar digunakan di environment pelatihan dan production.
