package contracts.messaging

/*
 * Real wire payload for order-events / DISPATCH_CANDIDATE_FOUND, from
 * MapsIntegration DispatchEventConsumer. Unusually for this codebase it carries the event type BOTH
 * in the body and as a Kafka header (set via MessageBuilder), so both are asserted here.
 */
org.springframework.cloud.contract.spec.Contract.make {
    description("Should publish DISPATCH_CANDIDATE_FOUND to order-events")
    label("order_events_dispatch")
    input {
        triggeredBy('fireDispatchCandidateFound()')
    }
    outputMessage {
        sentTo('order-events')
        headers {
            header('eventType', 'DISPATCH_CANDIDATE_FOUND')
        }
        body([
            orderId: $(producer(regex('[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}'))),
            driverIds: ["4f4a4e37-6ca5-5598-94f1-43ef1628f631"],
            eventType: "DISPATCH_CANDIDATE_FOUND",
            deliveryLat: 12.935242,
            deliveryLng: 77.624400,
            deliveryAddress: "221B Baker Street, Bangalore"
        ])
    }
}
