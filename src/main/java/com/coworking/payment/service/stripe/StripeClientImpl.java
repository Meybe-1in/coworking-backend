package com.coworking.payment.service.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StripeClientImpl implements StripeClient {

    @Value("${stripe.currency}")
    private String currency;

    @Override
    public String createPaymentIntent(BigDecimal price, Long reservationId) {

        try {

            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Monto inválido para pago");
            }

            long amount = price.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amount)
                            .setCurrency(currency)
                            .putMetadata("reservationId", reservationId.toString())
                            .build();

            PaymentIntent intent = PaymentIntent.create(params);

            return intent.getClientSecret();

        } catch (StripeException e) {
            throw new RuntimeException("Error creando PaymentIntent", e);
        }
    }
}