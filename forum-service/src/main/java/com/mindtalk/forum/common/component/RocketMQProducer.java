package com.mindtalk.forum.common.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMQProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    // ──────────────────── 同步发送 ────────────────────

    /**
     * 同步发送消息
     *
     * @param topic   主题
     * @param payload 消息体
     */
    public void sendSync(String topic, Object payload) {
        try {
            Message<String> message = buildMessage(payload);
            SendResult result = rocketMQTemplate.syncSend(topic, message);
            log.debug("[RocketMQ] 同步发送成功 topic={} msgId={}", topic, result.getMsgId());
        } catch (Exception e) {
            log.error("[RocketMQ] 同步发送失败 topic={}", topic, e);
        }
    }

    /**
     * 同步发送带 Tag 的消息
     */
    public void sendSync(String topic, String tag, Object payload) {
        String destination = topic + ":" + tag;
        sendSync(destination, payload);
    }

    /**
     * 同步发送延时消息
     *
     * @param topic       主题
     * @param payload     消息体
     * @param delayLevel  延时级别（1s/5s/10s/30s/1m/2m/3m/4m/5m/6m/7m/8m/9m/10m/20m/30m/1h/2h）
     */
    public void sendSyncDelay(String topic, Object payload, int delayLevel) {
        try {
            Message<String> message = buildMessage(payload);
            SendResult result = rocketMQTemplate.syncSend(topic, message,
                    rocketMQTemplate.getProducer().getSendMsgTimeout(), delayLevel);
            log.debug("[RocketMQ] 延时消息发送成功 topic={} delayLevel={} msgId={}",
                    topic, delayLevel, result.getMsgId());
        } catch (Exception e) {
            log.error("[RocketMQ] 延时消息发送失败 topic={}", topic, e);
        }
    }

    // ──────────────────── 异步发送 ────────────────────

    /**
     * 异步发送消息
     */
    public void sendAsync(String topic, Object payload) {
        try {
            Message<String> message = buildMessage(payload);
            rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    log.debug("[RocketMQ] 异步发送成功 topic={} msgId={}", topic, result.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("[RocketMQ] 异步发送失败 topic={}", topic, e);
                }
            });
        } catch (Exception e) {
            log.error("[RocketMQ] 异步消息构建失败 topic={}", topic, e);
        }
    }

    // ──────────────────── 单向发送（不关心结果） ────────────────────

    /**
     * 单向发送（不关心结果，性能最高）
     */
    public void sendOneWay(String topic, Object payload) {
        try {
            Message<String> message = buildMessage(payload);
            rocketMQTemplate.sendOneWay(topic, message);
        } catch (Exception e) {
            log.error("[RocketMQ] 单向发送失败 topic={}", topic, e);
        }
    }

    // ──────────────────── 内部工具方法 ────────────────────

    private Message<String> buildMessage(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return MessageBuilder.withPayload(json).build();
        } catch (Exception e) {
            throw new RuntimeException("消息序列化失败", e);
        }
    }
}
