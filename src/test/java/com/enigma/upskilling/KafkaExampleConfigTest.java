package com.enigma.upskilling;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaExampleConfigTest {

    @Test
    void producerUsesDurableAndIdempotentConfiguration() {
        Properties properties = KafkaExampleConfig.producerProperties();

        assertEquals("all", properties.get(ProducerConfig.ACKS_CONFIG));
        assertEquals("true", properties.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
    }

    @Test
    void consumerUsesManualCommitAndReadsFromBeginningForNewGroup() {
        Properties properties = KafkaExampleConfig.consumerProperties();

        assertEquals("false", properties.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals("earliest", properties.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }
}
