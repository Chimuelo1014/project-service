package com.sentinel.project_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${project.events.exchange}")
    private String projectExchange;

    // ========================================
    // EXCHANGES
    // ========================================
    
    @Bean
    public TopicExchange projectExchange() {
        return new TopicExchange(projectExchange, true, false);
    }

    @Bean
    public TopicExchange tenantExchange() {
        return new TopicExchange("tenant-exchange", true, false);
    }

    @Bean
    public TopicExchange domainExchange() {
        return new TopicExchange("domain-exchange", true, false);
    }

    @Bean
    public TopicExchange billingExchange() {
        return new TopicExchange("billing-exchange", true, false);
    }

    // ========================================
    // QUEUES (Para publicar eventos)
    // ========================================
    
    @Bean
    public Queue projectCreatedQueue() {
        return QueueBuilder
                .durable("project.created.queue")
                .build();
    }

    @Bean
    public Queue projectDeletedQueue() {
        return QueueBuilder
                .durable("project.deleted.queue")
                .build();
    }

    @Bean
    public Queue domainAddedQueue() {
        return QueueBuilder
                .durable("project.domain.added.queue")
                .build();
    }

    @Bean
    public Queue repoAddedQueue() {
        return QueueBuilder
                .durable("project.repo.added.queue")
                .build();
    }

    // ========================================
    // QUEUES (Para consumir eventos)
    // ========================================
    
    // Queue para consumir tenant.plan.upgraded
    @Bean
    public Queue tenantUpgradedQueue() {
        return QueueBuilder
                .durable("project.tenant.upgraded.queue")
                .build();
    }

    // Queue para consumir domain.verified (desde C#)
    @Bean
    public Queue domainVerifiedQueue() {
        return QueueBuilder
                .durable("project.domain.verified.queue")
                .build();
    }

    // Queue para consumir billing.payment_succeeded
    @Bean
    public Queue projectBillingPaymentQueue() {
        return QueueBuilder
                .durable("project-billing-payment-queue")
                .build();
    }

    // ========================================
    // BINDINGS
    // ========================================
    
    @Bean
    public Binding projectCreatedBinding() {
        return BindingBuilder
                .bind(projectCreatedQueue())
                .to(projectExchange())
                .with("project.created");
    }

    @Bean
    public Binding projectDeletedBinding() {
        return BindingBuilder
                .bind(projectDeletedQueue())
                .to(projectExchange())
                .with("project.deleted");
    }

    @Bean
    public Binding domainAddedBinding() {
        return BindingBuilder
                .bind(domainAddedQueue())
                .to(projectExchange())
                .with("domain.added");
    }

    @Bean
    public Binding repoAddedBinding() {
        return BindingBuilder
                .bind(repoAddedQueue())
                .to(projectExchange())
                .with("repository.added");
    }

    @Bean
    public Binding tenantUpgradedBinding() {
        return BindingBuilder
                .bind(tenantUpgradedQueue())
                .to(tenantExchange())
                .with("tenant.plan.upgraded");
    }

    @Bean
    public Binding domainVerifiedBinding() {
        return BindingBuilder
                .bind(domainVerifiedQueue())
                .to(domainExchange())
                .with("domain.verified");
    }

    @Bean
    public Binding projectBillingPaymentBinding() {
        return BindingBuilder
                .bind(projectBillingPaymentQueue())
                .to(billingExchange())
                .with("billing.payment_succeeded");
    }

    // ========================================
    // MESSAGE CONVERTER
    // ========================================
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}