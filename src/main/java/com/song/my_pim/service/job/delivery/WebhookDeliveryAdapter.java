package com.song.my_pim.service.job.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.delivery.DeliveryTargetType;
import com.song.my_pim.entity.outbox.OutboxDeliveryEntity;
import com.song.my_pim.entity.outbox.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class WebhookDeliveryAdapter implements DeliveryAdapter{
    private final WebClient webhookWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public DeliveryTargetType type() { return DeliveryTargetType.WEBHOOK; }

    @Override
    public void deliver(OutboxDeliveryEntity outboxDelivery) throws Exception{
        DeliveryTargetEntity deliveryTarget = outboxDelivery.getTarget();
        OutboxEventEntity outboxEvent = outboxDelivery.getOutboxEvent();
        JsonNode jsonTree = objectMapper.readTree(deliveryTarget.getConfigJson()); // String -> JsonNode
        String url = jsonTree.path("url").asText();
        JsonNode payloadNode = outboxEvent.getPayloadJson();


        MDC.put("targetId", String.valueOf(deliveryTarget.getId()));
        MDC.put("targetType", String.valueOf(deliveryTarget.getType()));
        MDC.put("webhookUrl", url);

        if (url == null || url.isBlank()) {
            try {
                throw new IllegalArgumentException("delivery_target.config_json.url is missing (targetId=" + deliveryTarget.getId() + ")");
            } finally {
                MDC.remove("targetId");
                MDC.remove("targetType");
                MDC.remove("webhookUrl");
            }
        }

        try {
            webhookWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Event-Id", String.valueOf(outboxEvent.getEventUid()))
                    .header("X-Event-Type", String.valueOf(outboxEvent.getEventType()))
                    .bodyValue(payloadNode) //  String
                    .retrieve() // get ResponseSpec
                    .toBodilessEntity() //ResponseEntity
                    .block(); // synchronously waits for the reactive pipeline to complete and returns the result (or throws an error).
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Webhook failed. status=" + ex.getStatusCode()
                    + " targetId=" + deliveryTarget.getId()
                    + " body=" + ex.getResponseBodyAsString(), ex);
        } finally {
            MDC.remove("targetId");
            MDC.remove("targetType");
            MDC.remove("webhookUrl");
        }
    }
}
