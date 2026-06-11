package com.enigma.upskilling;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.ExecutionException;

public class KafkaProducerExample {

    public static void main(String[] args) {
        String customerId = args.length > 0 ? args[0] : "customer-42";
        String eventId = "evt-java-" + System.currentTimeMillis();
        String event = """
                {"eventId":"%s","customerId":"%s","type":"ORDER_CREATED"}
                """.formatted(eventId, customerId).trim();

        try (KafkaProducer<String, String> producer =
                     new KafkaProducer<>(KafkaExampleConfig.producerProperties())) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(KafkaExampleConfig.TOPIC, customerId, event);
            RecordMetadata metadata = producer.send(record).get();

            System.out.printf(
                    "Terkirim: key=%s topic=%s partition=%d offset=%d value=%s%n",
                    customerId,
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    event
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Pengiriman dibatalkan karena thread terinterupsi.");
        } catch (ExecutionException exception) {
            System.err.printf("Pengiriman gagal: %s%n", exception.getCause().getMessage());
        }
    }
}
