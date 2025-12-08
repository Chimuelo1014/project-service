package com.sentinel.project_service.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener para eventos de pago exitoso desde Billing-Service.
 * 
 * Evento: billing.payment_succeeded
 * Acción: Auditoría y sincronización de información (SIN modificar el flujo original de limites)
 * 
 * NOTA: Los límites de proyecto se actualizan a través de TenantEntity en tenant-service
 * Este listener es SOLO para auditoría, NO modifica directamente los proyectos
 */
@Component
public class BillingPaymentSucceededListener {

    private static final Logger log = LoggerFactory.getLogger(BillingPaymentSucceededListener.class);

    // Mapa de límites por plan
    private static final Map<String, PlanLimits> PLAN_LIMITS = new HashMap<>();

    static {
        PLAN_LIMITS.put("BASIC", new PlanLimits(1, 0, false));
        PLAN_LIMITS.put("STANDARD", new PlanLimits(3, 2, false));
        PLAN_LIMITS.put("PRO", new PlanLimits(5, 5, false));
        PLAN_LIMITS.put("ENTERPRISE", new PlanLimits(10, 10, true));
    }

    // No necesitamos inyectar ProjectRepository - es solo para auditoría

    @RabbitListener(queues = "project-billing-payment-queue")
    public void handlePaymentSucceeded(@Payload Map<String, Object> payload) {
        log.info("💳 Evento recibido: billing.payment_succeeded en Project-Service -> Payload: {}", payload);

        String tenantId = (String) payload.get("tenantId");
        String planId = (String) payload.get("planId");

        if (tenantId == null || planId == null) {
            log.warn("⚠️ Payload incompleto: tenantId={}, planId={}", tenantId, planId);
            return;
        }

        PlanLimits limits = PLAN_LIMITS.get(planId);
        if (limits == null) {
            log.warn("⚠️ Plan desconocido: {}", planId);
            return;
        }

        // AUDITORÍA: Los límites se actualizan en tenant-service
        // Este listener es SOLO para sincronización de información y logs
        // NO modificamos directamente los proyectos aquí para mantener cohesión y evitar duplicación
        log.info("✅ Plan actualizado para tenant {}: Plan={}, maxDomains={}, maxRepos={}, blockchain={}",
                tenantId, planId, limits.maxDomains, limits.maxRepos, limits.includesBlockchain);
    }

    record PlanLimits(int maxDomains, int maxRepos, boolean includesBlockchain) {
    }
}
