package com.exiua.processing.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.exiua.processing.config.ProcessingRabbitConfig;

/**
 * Service for sending route processing messages to RabbitMQ
 * Following the pattern from admin_users_api - publisher only
 */
@Service
public class ProcessingMessageService {
    
    private final RabbitTemplate rabbitTemplate;

    public ProcessingMessageService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;

        try {
            System.out.println("=== VERIFICANDO CONEXIÓN A RABBITMQ ===");
            rabbitTemplate.execute(channel -> {
                System.out.println("Conexión a RabbitMQ establecida correctamente");
                return null;
            });
        } catch (Exception e) {
            System.err.println("Error al conectar con RabbitMQ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send processing status message
     */
    public void enviarMensajeEstado(String mensaje) {
        try {
            System.out.println("=== INICIO ENVÍO STATUS A RABBITMQ ===");
            System.out.println("Exchange: " + ProcessingRabbitConfig.EXCHANGE_NAME);
            System.out.println("Routing Key: " + ProcessingRabbitConfig.ROUTING_KEY_STATUS);
            System.out.println("Mensaje a enviar: " + mensaje);

            rabbitTemplate.convertAndSend(
                ProcessingRabbitConfig.EXCHANGE_NAME,
                ProcessingRabbitConfig.ROUTING_KEY_STATUS,
                mensaje
            );

            System.out.println("=== MENSAJE STATUS ENVIADO EXITOSAMENTE ===");
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje de estado: " + e.getMessage());
        }
    }

    /**
     * Send processing results message
     */
    public void enviarMensajeResultados(String mensaje) {
        try {
            System.out.println("=== INICIO ENVÍO RESULTS A RABBITMQ ===");
            System.out.println("Exchange: " + ProcessingRabbitConfig.EXCHANGE_NAME);
            System.out.println("Routing Key: " + ProcessingRabbitConfig.ROUTING_KEY_RESULTS);
            System.out.println("Mensaje a enviar: " + mensaje);

            rabbitTemplate.convertAndSend(
                ProcessingRabbitConfig.EXCHANGE_NAME,
                ProcessingRabbitConfig.ROUTING_KEY_RESULTS,
                mensaje
            );

            System.out.println("=== MENSAJE RESULTS ENVIADO EXITOSAMENTE ===");
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje de resultados: " + e.getMessage());
        }
    }
}