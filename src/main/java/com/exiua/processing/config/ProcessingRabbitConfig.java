package com.exiua.processing.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for route-processing-service
 * Following publisher-only pattern like admin_users_api
 */
@Configuration
public class ProcessingRabbitConfig {
    
    public static final String EXCHANGE_NAME = "route_processing_exchange";
    public static final String QUEUE_NAME_STATUS = "queue_route_processing_status";
    public static final String QUEUE_NAME_RESULTS = "queue_route_processing_results";
    public static final String ROUTING_KEY_STATUS = "processing_status_key";
    public static final String ROUTING_KEY_RESULTS = "processing_results_key";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
    
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connection) {
        RabbitTemplate template = new RabbitTemplate(connection);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public Queue queueStatus() {
        return new Queue(QUEUE_NAME_STATUS, true);
    }

    @Bean
    public Queue queueResults() {
        return new Queue(QUEUE_NAME_RESULTS, true);
    }

    @Bean
    public Binding bindingStatus(Queue queueStatus, TopicExchange exchange) {
        return BindingBuilder.bind(queueStatus).to(exchange).with(ROUTING_KEY_STATUS);
    }

    @Bean
    public Binding bindingResults(Queue queueResults, TopicExchange exchange) {
        return BindingBuilder.bind(queueResults).to(exchange).with(ROUTING_KEY_RESULTS);
    }
}