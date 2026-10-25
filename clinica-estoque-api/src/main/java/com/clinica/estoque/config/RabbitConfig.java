package com.clinica.estoque.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "estoque.exchange";
    public static final String FILA_MOVIMENTACAO = "estoque.movimentacao.queue";
    public static final String FILA_ALERTA = "estoque.alerta.queue";
    public static final String FILA_TRANSFERENCIA = "estoque.transferencia.queue";
    public static final String RK_MOVIMENTACAO = "movimentacao.registrada";
    public static final String RK_ALERTA = "alerta.gerado";
    public static final String RK_TRANSFERENCIA = "transferencia.solicitada";

    @Bean
    public TopicExchange estoqueExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue filaMovimentacao() {
        return QueueBuilder.durable(FILA_MOVIMENTACAO).build();
    }

    @Bean
    public Queue filaAlerta() {
        return QueueBuilder.durable(FILA_ALERTA).build();
    }

    @Bean
    public Queue filaTransferencia() {
        return QueueBuilder.durable(FILA_TRANSFERENCIA).build();
    }

    @Bean
    public Binding bindingMovimentacao(Queue filaMovimentacao, TopicExchange estoqueExchange) {
        return BindingBuilder.bind(filaMovimentacao).to(estoqueExchange).with("movimentacao.#");
    }

    @Bean
    public Binding bindingAlerta(Queue filaAlerta, TopicExchange estoqueExchange) {
        return BindingBuilder.bind(filaAlerta).to(estoqueExchange).with("alerta.#");
    }

    @Bean
    public Binding bindingTransferencia(Queue filaTransferencia, TopicExchange estoqueExchange) {
        return BindingBuilder.bind(filaTransferencia).to(estoqueExchange).with("transferencia.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(converter);
        template.setExchange(EXCHANGE);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory factory, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(factory);
        f.setMessageConverter(converter);
        return f;
    }
}
