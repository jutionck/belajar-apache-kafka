package com.enigma.upskilling;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaConsumerExample {

    public static void main(String[] args) {
        AtomicBoolean running = new AtomicBoolean(true);

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(KafkaExampleConfig.consumerProperties())) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                consumer.wakeup();
            }));

            consumer.subscribe(Collections.singletonList(KafkaExampleConfig.TOPIC));
            System.out.printf(
                    "Menunggu event: topic=%s group=%s%n",
                    KafkaExampleConfig.TOPIC,
                    KafkaExampleConfig.CONSUMER_GROUP
            );

            try {
                while (running.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                    for (ConsumerRecord<String, String> record : records) {
                        System.out.printf(
                                "Diterima: key=%s partition=%d offset=%d value=%s%n",
                                record.key(),
                                record.partition(),
                                record.offset(),
                                record.value()
                        );
                        // Jalankan proses bisnis sebelum commit offset.
                    }

                    if (!records.isEmpty()) {
                        consumer.commitSync();
                    }
                }
            } catch (WakeupException exception) {
                if (running.get()) {
                    throw exception;
                }
            }
        }
    }
}
