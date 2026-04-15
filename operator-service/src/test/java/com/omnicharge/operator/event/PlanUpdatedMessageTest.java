package com.omnicharge.operator.event;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PlanUpdatedMessageTest {

    @Test
    void testNoArgsConstructor() {
        PlanUpdatedMessage message = new PlanUpdatedMessage();
        assertThat(message).isNotNull();
        assertThat(message.getEventId()).isNull();
        assertThat(message.getOperatorId()).isNull();
        assertThat(message.getTimestamp()).isNull();
    }

    @Test
    void testAllArgsConstructor() {
        PlanUpdatedMessage message = new PlanUpdatedMessage(
                "event-123",
                10L,
                1234567890L
        );

        assertThat(message.getEventId()).isEqualTo("event-123");
        assertThat(message.getOperatorId()).isEqualTo(10L);
        assertThat(message.getTimestamp()).isEqualTo(1234567890L);
    }

    @Test
    void testBuilder() {
        PlanUpdatedMessage message = PlanUpdatedMessage.builder()
                .eventId("event-456")
                .operatorId(20L)
                .timestamp(9876543210L)
                .build();

        assertThat(message.getEventId()).isEqualTo("event-456");
        assertThat(message.getOperatorId()).isEqualTo(20L);
        assertThat(message.getTimestamp()).isEqualTo(9876543210L);
    }

    @Test
    void testSettersAndGetters() {
        PlanUpdatedMessage message = new PlanUpdatedMessage();
        
        message.setEventId("event-789");
        message.setOperatorId(30L);
        message.setTimestamp(1111111111L);

        assertThat(message.getEventId()).isEqualTo("event-789");
        assertThat(message.getOperatorId()).isEqualTo(30L);
        assertThat(message.getTimestamp()).isEqualTo(1111111111L);
    }

    @Test
    void testEqualsAndHashCode() {
        PlanUpdatedMessage message1 = new PlanUpdatedMessage("event-123", 10L, 1234567890L);
        PlanUpdatedMessage message2 = new PlanUpdatedMessage("event-123", 10L, 1234567890L);
        PlanUpdatedMessage message3 = new PlanUpdatedMessage("event-456", 20L, 9876543210L);

        assertThat(message1).isEqualTo(message2);
        assertThat(message1).isNotEqualTo(message3);
        assertThat(message1.hashCode()).isEqualTo(message2.hashCode());
    }

    @Test
    void testToString() {
        PlanUpdatedMessage message = new PlanUpdatedMessage("event-123", 10L, 1234567890L);
        String toString = message.toString();

        assertThat(toString).contains("eventId=event-123");
        assertThat(toString).contains("operatorId=10");
        assertThat(toString).contains("timestamp=1234567890");
    }

    @Test
    void testSerializable() throws Exception {
        PlanUpdatedMessage original = PlanUpdatedMessage.builder()
                .eventId("event-serializable")
                .operatorId(100L)
                .timestamp(System.currentTimeMillis())
                .build();

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        PlanUpdatedMessage deserialized = (PlanUpdatedMessage) ois.readObject();
        ois.close();

        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.getEventId()).isEqualTo(original.getEventId());
        assertThat(deserialized.getOperatorId()).isEqualTo(original.getOperatorId());
        assertThat(deserialized.getTimestamp()).isEqualTo(original.getTimestamp());
    }

    @Test
    void testWithNullValues() {
        PlanUpdatedMessage message = PlanUpdatedMessage.builder()
                .eventId(null)
                .operatorId(null)
                .timestamp(null)
                .build();

        assertThat(message.getEventId()).isNull();
        assertThat(message.getOperatorId()).isNull();
        assertThat(message.getTimestamp()).isNull();
    }

    @Test
    void testWithCurrentTimestamp() {
        long currentTime = System.currentTimeMillis();
        PlanUpdatedMessage message = PlanUpdatedMessage.builder()
                .eventId("event-current")
                .operatorId(50L)
                .timestamp(currentTime)
                .build();

        assertThat(message.getTimestamp()).isEqualTo(currentTime);
    }

    @Test
    void testWithUUIDEventId() {
        String uuid = java.util.UUID.randomUUID().toString();
        PlanUpdatedMessage message = PlanUpdatedMessage.builder()
                .eventId(uuid)
                .operatorId(60L)
                .timestamp(System.currentTimeMillis())
                .build();

        assertThat(message.getEventId()).isEqualTo(uuid);
        assertThat(message.getEventId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
