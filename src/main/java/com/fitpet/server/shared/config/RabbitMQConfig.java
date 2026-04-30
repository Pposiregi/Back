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
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * RabbitMQ 인프라 설정.
 *
 * <h2>토폴로지</h2>
 * <pre>
 * Producer
 *   → [ranking.overtake.exchange] (Direct)
 *       --routing-key: ranking.overtake--> [ranking.overtake.queue]
 *                                                    ↓ (소비)
 *                                             Consumer (FCM 발송)
 *                                                    ↓ (실패 3회)
 *                                      [ranking.overtake.dlx] (Dead Letter Exchange)
 *                                                    ↓
 *                                          [ranking.overtake.dlq]  (수동 확인/알림)
 * </pre>
 *
 * <h2>백오프(Backoff)</h2>
 * <ul>
 *   <li>초기 대기: 1s</li>
 *   <li>배수: 2.0 (1s → 2s → 4s)</li>
 *   <li>최대 대기: 10s</li>
 *   <li>최대 재시도: 3회 후 DLQ로 이동</li>
 * </ul>
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ── 메인 큐 ──────────────────────────────────────────────
    public static final String EXCHANGE    = "ranking.overtake.exchange";
    public static final String QUEUE       = "ranking.overtake.queue";
    public static final String ROUTING_KEY = "ranking.overtake";

    // ── Dead Letter ───────────────────────────────────────────
    public static final String DLX             = "ranking.overtake.dlx";
    public static final String DLQ             = "ranking.overtake.dlq";
    public static final String DLQ_ROUTING_KEY = "ranking.overtake.dead";

    // ── Exchange ──────────────────────────────────────────────

    @Bean
    DirectExchange rankingOvertakeExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange rankingOvertakeDlx() {
        return new DirectExchange(DLX, true, false);
    }

    // ── Queue ─────────────────────────────────────────────────

    /**
     * 메인 큐: 처리 실패 시 DLX 로 라우팅.
     *
     * <p>{@code x-dead-letter-exchange} 와 {@code x-dead-letter-routing-key} 를 설정하면
     * consumer 가 NACK(requeue=false) 를 전달하거나 TTL 이 만료될 때 DLQ 로 이동한다.</p>
     */
    @Bean
    Queue rankingOvertakeQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    /** Dead Letter Queue: 수동 검토 / 재처리 대상 */
    @Bean
    Queue rankingOvertakeDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    // ── Binding ───────────────────────────────────────────────

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

    // ── Converter & Template ──────────────────────────────────

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

    // ── Listener Container ────────────────────────────────────

    /**
     * 재시도 인터셉터: 지수 백오프(1s→2s→4s), 3회 초과 시 DLQ.
     *
     * <p>{@link RejectAndDontRequeueRecoverer} 는 최종 실패 시 NACK + requeue=false 를
     * 전송한다. 메인 큐에 {@code x-dead-letter-exchange} 가 설정되어 있으면 DLQ로 이동한다.</p>
     */
    @Bean
    RetryOperationsInterceptor rankingOvertakeRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1_000, 2.0, 10_000) // initial, multiplier, max (ms)
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
