## Kafka

**[NEW]** Baca dokumentasi terbaru disini ![Kafka Raft](https://github.com/jutionck/belajar-apache-kafka/blob/main/KRAFT.md)

---

> Aplikasi yang biasa digunakan untuk publish (sebagai pengirim data) dan subscribe (yang menerima data). Ada sebuah pertanyaan, terus bedanya apa sama database ? Database jika kita simpan ya akan tersimpan, jika kita ambil kita akan get data di database, untuk kafka alurnya adalah akan mengirim dimana aplikasi tersedia yang melakukan subscribe akan menerima pesan yang dikirimkan.
> 
> Artikel dari amazon web services tentang publisher dan subscribe messaging https://aws.amazon.com/pub-sub-messaging/
> 
> Jika kita simulasikan ada sebuah Tabel di database, di Kafka ada sebuah Topic. Selain kafka sebenarnya banyak ya, tapi yang populer sampai saat ini adalah Kafka.
> 
> Apache kafka dibuat oleh developer LinkedIn, kemudian di donasikan ke Apache Software sehingga namanya dikenal sebagai Apache Kafka, kafka di tulis menggunakan bahasa Scala dan Java.
> 
> Contohkan aplikasi yang sebelum menggunakan publish subscribe di java
> 
> ![alt text](https://miro.medium.com/max/1400/1*OkSrDW2s5_fBPFlDLXmjpw.png)
> 
> Terlihat pada gambar diatas, komunikasi yang terjadi adalah 1 arah dimana bagian atas sebagai pengirim sebuah data. Sedangkan jika kita menggunakan apache kafka berikut gambaran nya :
> 
>  ![alt text](https://miro.medium.com/max/1400/1*kMDmSXR6PecQVjwQhwNU-g.png) 
> 
> Ada peran message broker, Message broker merupakan layanan antarmuka pengantar pesan yang berada sebagai penengah antara program yang terhubung.
> 
> Kafka kita mengenal namanya produce dan consumer, apa itu ?
Producer itu pihak yang mengirim data ke message broker, consumer adalah pihak yang mengambil/menerima data dari message broker. Aplikasi bisa bertindak sekaligus baik sebagai producer maupun consumer.
Setelah kita mengetahu apa itu kafka dan bagaimana simulasi sebuah aplikasi yang menggunakan Kafka, berikut arsitektur Apache Kafka yang bisa di gambarkan :
> 
> ![alt text](https://miro.medium.com/max/1400/1*p4vwymBjcNhkhiVyrrm1eg.png)
> 
> Di lihat dari gambar di atas bisa kita simpulkan apache kafka memiliki sebuah kafka ecosystem yang di dalam nya terdapat kafka cluster tempat broker berada dan ada nya sebuah zookeeper.
> 
> Zookeeper adalah layanan terpusat untuk memelihara informasi konfigurasi, penamaan, menyediakan sinkronisasi terdistribusi, dan menyediakan layanan grup.
> 
> Selengkapnya mengenai zookeeper https://zookeeper.apache.org
> 
> Selanjutnya adalah penulis akan membahas hal penting dari sebuah kafka itu sendiri yaitu Topic.
Data di kafka disimpan dalam Topic. Analogi yang deket untuk Topic, jika di database adalah Table, tetapi data di Topic tidak bisa di ubah
> 
> ![alt text](https://miro.medium.com/max/1400/1*-UQKy4HJAsTsJjnHcqrPmg.png)
> 
> Jika kita membuat sebuah topic kita bisa menentukan berapa jumlah partisi. Partisi adalah sebuah bagian-bagian, kenapa butuh partisi ? Misalkan aplikasi consumer ada 3, partisi yang dibuat hanya 1, otomatis yang menerima hanya 1 consumer saja, jadi sebelum membuat sebuah topic agar lebih dipersiapkan berapa jumlah partisi yang dibutuhkan dan biasanya berjumlah ganjil.
> 
> Ingat topic tidak bisa di ubah ya
> 
> Bagaimana kita menentukan partisi mana yang akan digunakan ? Ada teknik adalah dengan melakukan hashing lalu dilakukan modulo. Jika bingung boleh tidak ditentukan (secara default akan ditentukan oleh Kafka itu sendiri). Ketika consumer membaca data terakhir disebut dengan offset, dst untuk consumer lainnya akan read di urutan selanjutnya.
> 
> Replication
> 
> ![alt text](https://miro.medium.com/max/1400/1*_fka73fEuoCktFNYa2De5Q.png)
> 
> Replication atau replikasi adalah Menduplikasi topic yang sudah ada.
> 
> Di atas contoh ada 3 server kafka, terlihat bahwa terdapat 2 partisi, p1 dan p2, keduanya sama sama di replikasi.
> 
> R2 terpisah, itu semua sudah diatur oleh kafka itu sendiri, tapi ingat di satu server pasti beda replikasi, kenapa ? Ketika di production kejadian server mati, otomatis partisi tsb akan hilang, dengan terpisah apabila terjadi hal demikian maka ada backup.
> 
> Consumer Group
> 
> ![alt text](https://miro.medium.com/max/948/1*J-0xbraSo0fbyrrPCXedlg.png)
> 
> Mari kita bahas apa itu consumer group, jika penulis dapat simulasikan adalah sebagai berikut:
> 
> ![alt text](https://miro.medium.com/max/1136/1*4-5NWgu-Q1cft6sHwWdDhA.png)
> 
> Diatas sebuah partisi terbagi menjadi duaTerdapat sebuah aplikasi consumer. Maka otomatis kedua partisi tsb akan terkirim, tetapi lihat jika ada aplikasi consumer ke2, tetapi dia menerima dari kedua partisi yang sama seperti aplikasi consumer, jika seperti maka ketika ada case di pembayaran ketika ada order maka dia harus membayar dua kali, nah dengan adanya case tsb maka dibuatkan lah namanya consumer group (2 aplikasinya nya ya) lihat gambar di bawah ini :
> 
> ![alt text](https://miro.medium.com/max/1400/1*8Ecdc15ZrFZpOrtvdwYrVw.png)
> 
> Ketika ada aplikasi consumer baru (contohnya merchant) maka harus dibuat consumer grup lagi.
> 
> ![alt text](https://miro.medium.com/max/1400/1*3ezHN6-0fT8TuggtSch2EA.png)
> 
> Pasti akan timbul pertanyaan, kenapa aplikasi yang ketiga tidak mendapat consumer, ingat 1 partisi 1 consumer ya, maka ketika kita membuat partisi usahakan sama dengan aplikasi nya. Kegunaan lain adalah untuk kecepatan dalam pemrosesan data, karena makin banyak partisi makin banyak yang consum dan tentu cepat.
> 
> Retention Policy
> - Log retention time
> - Log retention bytes 
> - Offset retention time
> 
> Di kafka sayangnya topic tidak bisa di hapus seperti di database. Maka mau tidak mau jika kita akan menghapus pasti akan terhapus semua isi dalam topic. Sama halnya jika di database apabila ada id sama yang masuk maka akan terjadi update, jika di kafka akan terus masuk datanya. Nah muncul lah topic ini, apa itu ? Retention berfungsi untuk menghapus data yang tidak digunakan di kafka.
> 
> **Log retention time** (berapa lama data akan tersimpan ? By default adalah 7 hari) tetapi apakah bisa dirubah ? Ya pengaturan tsb dapat diubah misal 1 hari atau 30 hari. Tapi terkadang dengan cara ini ketika men set 30 hari misalnya, tetapi ketika belum 30 hari data sudah penuh maka hal itu tidak berguna, untuk mengatasi hal tersebut adalah menggunakan cara,
> 
> **Log retention byte** (pengaturan berdasarkan ukuran data yang disimpan)
> 
> **Offset retention time** (lokasi terakhir mengkonsumsi data) nah fungsi ini adalah seberapa lama data offset ini tersimpan di kafka.
> 
> 

### Instalasi Kafka
> 
> Download disini https://kafka.apache.org/downloads
> 
> Langkah selanjutnya adalah kita akan melakukan konfigurasi beberapa hal pada server kafka dan zookeeper. Untuk melakukan nya adalah buka folder config yang ada dalam folder kafka yang sudah di download tadi, lalu buka file zookeeper.properties dan server.properties.
> 
> ![alt text](https://miro.medium.com/max/1400/1*_gm_7luN1AIGCchRCOQg2Q.png)
> 
> Silahkan ubah pada dataDir, tempatkan direktori di lokasi yang aman, kenapa ? karena jika seperti saja ketika pc/laptop kita shutdown atau restart maka akan hilang.
> 
> ![alt text](https://miro.medium.com/max/1400/1*tSSpaunLh4laPDDkhYwAXA.png)
> 
> Sama hal nya dengan zookeeper.properties, pada server.properties ubah pada bagian log.dirs juga.
> 
> Hal terpenting lainnya adalah pada bagian ini :
> ![alt text](https://miro.medium.com/max/1400/1*XDOB-NhYr3OUEWAVpivfGg.png)
> 
> Selanjutnya kita akan menjalakan server kafka nya, sedikit ada perbedaan untuk pengguna sistem operasi windows dan linux/mac, tetapi tenang penulis akan memberikan kedua nya baik itu windows maupun linux/mac.
> 
Buka terminal (jangan lupa buka sebagai administrator ya jika windows), dan pastikan sudah berada pada direktori kafka, kemudian lakukan hal di bawah ini :

Untuk sistem operasi windows :

Terminal 1 :
```
./bin/windows/zookeeper-server-start.bat config/zookeeper.properties
```
Terminal 2 :
```
./bin/windows/kafka-server-start.bat config/server.properties
```

Untuk sistem operasi linux dan Mac :

Terminal 1 :
```
bin/zookeeper-server-start.sh config/zookeeper.properties
```
Terminal 2 :
```
bin/kafka-server-start.sh config/server.properties
```

> Pastikan keduanya berjalan dan tidak ada error ya. 
> 
> Sebelum lanjut, disini penulis akan memberikan beberapa contoh mengenai :
> 
> - Topic, bagaimana membuat dan melihat list topic yang tersedia 
> - Melakukan pengiriman pesan (produces)
> - Menerima sebuah pesan (consumer)
> - Simulasi topic partition dan consumer group
> - Membuat producer dan consumer pada pemrograman Java
> - Membuat sebuah topic

Untuk sistem operasi windows :

Terminal 3:
```
./bin/windows/kafka-topics.bat --create --topic example-topic replication-factor 1 --partitions 1 --bootstrap-server localhost:9092
```
Untuk sistem operasi linux dan Mac :

Terminal 3:
```
bin/kafka-topics.sh --create --topic example-topic replication-factor 1 --partitions 1 --bootstrap-server localhost:9092
```
> Penjelasan :
> - --create --topic untuk membuat sebuah topic
> - example-topic adalah sebuah nama topic yang akan di buat
> - replication factor 1 adalah jumlah server yang di buat
> - --partition 1 jumlah topic partisi yang di buat
> - --bootstrap-server localhost:9092 adalah lokasi server dan port default server kafka

Melihat sebuah topic

Untuk sistem operasi windows :

Terminal 3:
```
./bin/windows/kafka-topics.bat --list --topic --bootstrap-server localhost:9092
```
Untuk sistem operasi linux dan Mac :

Terminal 3:
```
bin/kafka-topics.sh --list --topic --bootstrap-server localhost:9092
```
> Penjelasan :
> - --list --topic untuk melihat sebuah topic
> - --bootstrap-server localhost:9092 adalah lokasi server dan port default server kafka

Mengirim sebuah pesan (producer)

Untuk sistem operasi windows :

Terminal 4:
```
./bin/windows/kafka-console-producer.bat --topic example-topic --bootstrap-server localhost:9092

Create your message, example Hello World [enter] dst
```
Untuk sistem operasi linux dan windows :

Terminal 4:
```
bin/kafka-console-producer.sh --topic example-topic --bootstrap-server localhost:9092

Create your message, example Hello World [enter] dst
```

> Penjelasan :
> - kafka-console-producer sebagai producer untuk pengiriman sebuah pesan

Menerima sebuah pesan (consumer)

Untuk sistem operasi windows :

Terminal 5 :
```
./bin/windows/kafka-console-consumer.bat --topic example-topic --from-beginning --bootstrap-server localhost:9092
```
Untuk sistem operasi linux dan Mac:

Terminal 5 :
```
bin/kafka-console-consumer.sh --topic example-topic --from-beginning --bootstrap-server localhost:9092
```
> Penjelasan :
> - kafka-console-consumer untuk consumer sebagai penerima pesan
> - --from beginning untuk melihat sebuah pesan dari awal di buat/dikirim, jika tidak menggunakannya, maka pesan yang diterima adalah yang terakhir atau saat consumer aktif
> - Lakukan pengiriman pesan pada tab producer, lalu perhatikan pada tab consumer, maka pesan akan diterima secara bersamaan, kenapa demikian ? karena kita belum menerapkan consumer group. Consumer group memastikan data yang diterima oleh consumer tidak akan sama.

### Praktik Topic Partition dan Consumer Group
Buat sebuah topic baru, untuk sistem operasi windows :

Terminal 3:
```
./bin/windows/kafka-topics.bat --bootstrap-server localhost:9092 replication-factor 1 --partitions 3  --create --topic example-topic-2
```
Buat sebuah topic baru, untuk sistem operasi linux dan mac :

Terminal 3:
```
bin/kafka-topics.sh --bootstrap-server localhost:9092 replication-factor 1 --partitions 3  --create --topic example-topic-2
```
> Jika kita perhatikan, hal yang mebedakan dengan membuat sebuah topic yang pertama adalah pada bagian --partition nya, untuk membuat sebuah partition lebih dari 1 maka cukup tuliskan angka nya berapa pun yang diinginkan, tetapi pastikan jumlahnya ganjil ya.

Lakukan pengiriman pesan dengan topic yang baru.

Melihat pesan dengan menerapkan consumer group, untuk sistem operasi windows :

Terminal 5 :
```
./bin/windows/kafka-console-consumer.bat --topic example-topic-2 --bootstrap-server localhost:9092 --group consum
```
```
./bin/windows/kafka-console-consumer.bat --topic example-topic-2  --from-beginning --bootstrap-server localhost:9092 --group consum
```
Melihat pesan dengan menerapkan consumer group, untuk sistem operasi linux dan mac :

Terminal 5 :
```
bin/kafka-console-consumer.sh --topic example-topic-2 --bootstrap-server localhost:9092 --group consum
```
```
bin/kafka-console-consumer.sh --topic example-topic-2  --from-beginning --bootstrap-server localhost:9092 --group consum
```
> Lihat apa yang terjadi, maka kedua consumer tidak akan menerima data yang duplikasi atau sama.

### Buat Producer menggunakan Java
1. Buat sebuah project maven, kemudian tambahkan ini pada `pom.xml`
```xml
<dependency>
<groupId>org.apache.kafka</groupId>
<artifactId>kafka-clients</artifactId>
<version>2.8.0</version>
</dependency>
```
> Dokumentasi untuk membuat juga bisa dilihat disini https://kafka.apache.org/28/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html

2. Buat class dengan nama `KafkaProducerExample`, lalu tuliskan script di bawah ini:
```java
public class KafkaProducerExample { 
    public static void main(String[] args) { 
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer<String,String>(properties);
        for (int i = 0; i < 10; i++) { 
            ProducerRecord<String, String> record = new ProducerRecord<>("topic-java", "Data ke " + i);
            producer.send(record); 
        }
        producer.close(); 
    }
}
```
> Kemudian panggil consumer dengan `topic-java` yang telah dibuat dan lihat apa yang terjadi.

### Buat Consumer menggunakan Java
Buat class `KafkaConsumerExample`, lalu tuliskan script di bawah ini:
```java
public class KafkaConsumerExample { 
    public static void main(String[] args) { 
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("topic-java"));
        //noinspection InfiniteLoopStatement
        while (true) { 
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for(ConsumerRecord<String, String> record: records) { 
                System.out.println("Receive data : " + record.value()); 
            } 
        } 
    }
}
```
> Jalankan `KafkaConsumerExample`, pasti akan mengeluarkan log yang tidak akan berhenti, Kemudian jalankan KafkaProducerExample, maka ketika kembali di KafkaConsumerExample akan melihat data yang diterima.

Untuk source code bisa dilihat disini ya https://github.com/jutionck/belajar-apache-kafka/tree/main/src/main/java/com/enigma/upskilling
