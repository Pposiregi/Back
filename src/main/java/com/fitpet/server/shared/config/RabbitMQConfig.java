package com.fitpet.server.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "ranking.overtake.exchange";
    public static final String QUEUE = "ranking.overtake.queue";
    public static final String ROUTING_KEY = "ranking.overtake";

    public static final String DLX = "ranking.overtake.dlx";
    public static final String DLQ = "ranking.overtake.dlq";
    public static final String DLQ_ROUTING_KEY = "ranking.overtake.dead";


    @Bean
    DirectExchange rankingOvertakeExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange rankingOvertakeDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue rankingOvertakeQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue rankingOvertakeDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding rankingOvertakeBinding(Queue rankingOvertakeQueue,
                                   DirectExchange rankingOvertakeExchange) {
        return BindingBuilder.bind(rankingOvertakeQueue)
                .to(rankingOvertakeExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    Binding rankingOvertakeDlqBinding(Queue rankingOvertakeDlq,
                                      DirectExchange rankingOvertakeDlx) {
        return BindingBuilder.bind(rankingOvertakeDlq)
                .to(rankingOvertakeDlx)
                .with(DLQ_ROUTING_KEY);
    }


    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        // mandatory=true: 라우팅 불가 메시지를 ReturnsCallback으로 반환 (publisher-returns와 함께 동작)
        template.setMandatory(true);
        // 라우팅 실패 콜백: exchange에 바인딩된 큐가 없을 때 호출
        template.setReturnsCallback(returned ->
                log.error("[RabbitMQ] 메시지 라우팅 실패 – 큐 바인딩 확인 필요: exchange={}, routingKey={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(), returned.getReplyText())
        );
        return template;
    }

    @Bean
    RetryOperationsInterceptor rankingOvertakeRetryInterceptor() {
        ExponentialRandomBackOffPolicy backOff = new ExponentialRandomBackOffPolicy();
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000);

        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffPolicy(backOff)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rankingOvertakeListenerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter,
            RetryOperationsInterceptor rankingOvertakeRetryInterceptor) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // 재시도 정책은 인터셉터가 관리
        factory.setAdviceChain(rankingOvertakeRetryInterceptor);
        return factory;
    }
}
