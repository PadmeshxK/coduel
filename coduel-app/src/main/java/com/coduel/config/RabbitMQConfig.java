package com.coduel.config;

import com.coduel.rabbitmq.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue submissionQueue() {
        // Failed messages (after retries) are routed to the dead-letter exchange, not requeued/lost.
        return QueueBuilder.durable(RabbitMQConstants.SUBMISSION_QUEUE)
                .deadLetterExchange(RabbitMQConstants.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitMQConstants.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public TopicExchange submissionExchange() {
        return new TopicExchange(RabbitMQConstants.CODUEL_EXCHANGE);
    }

    @Bean
    public Binding submissionBinding(Queue submissionQueue, TopicExchange submissionExchange) {
        return BindingBuilder.bind(submissionQueue).to(submissionExchange).with("#");
    }

    // --- Run topology: ephemeral, results pushed over WebSocket. A run not picked up within 30s is
    //     stale (the user has moved on), so messages self-expire; no DLQ. ---

    @Bean
    public Queue runQueue() {
        return QueueBuilder.durable(RabbitMQConstants.RUN_QUEUE)
                .ttl(30_000)
                .build();
    }

    @Bean
    public TopicExchange runExchange() {
        return new TopicExchange(RabbitMQConstants.RUN_EXCHANGE);
    }

    @Bean
    public Binding runBinding(Queue runQueue, TopicExchange runExchange) {
        return BindingBuilder.bind(runQueue).to(runExchange).with("#");
    }

    // --- Dead-letter topology: terminal failures land here for inspection / manual replay ---

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstants.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.DEAD_LETTER_ROUTING_KEY);
    }
}
