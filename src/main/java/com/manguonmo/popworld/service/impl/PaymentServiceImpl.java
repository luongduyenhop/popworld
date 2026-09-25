package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.service.PaymentService;
import com.manguonmo.popworld.dto.request.SePayWebhookRequest;
import com.manguonmo.popworld.entity.Order;
import com.manguonmo.popworld.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService{

    @Value("${sepay.webhook.api-key}")
    private String apiKey;

    private final OrderRepository orderRepository;

    public PaymentServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public boolean processSePayWebhook(SePayWebhookRequest webhookData, String authorizationHeader) {

        if(authorizationHeader ==null || !authorizationHeader.contains(apiKey)){
            log.info("SePay Webhook unauthorized access");
            log.warn("Can than co xam nhap tu ben ngoai");
            return false;

        }

        Pattern pattern = Pattern.compile("PW-\\d+");
        String idOrder =  webhookData.getContent() == null ? webhookData.getDescription(): webhookData.getContent();
        if(idOrder == null){
            return false;
        }
        Matcher matcher = pattern.matcher(idOrder);
        String orderCode = "";
        if(matcher.find()){
             orderCode= matcher.group();
        } else {
            return false;
        }

        Optional<Order> orderCheck = orderRepository.findByOrderCode(orderCode);
        if(orderCheck.isEmpty()){
            return false;
        }
        Order order = orderCheck.get();

        String status = order.getStatus();
        if(status != null){
            if (status.equalsIgnoreCase("CANCELLED") || status.equalsIgnoreCase("EXPIRED")) {
                log.info("Don hang da bi huy hoac qua 15 phut (EXPIRED/CANCELLED)");
                return false;
            } else if(List.of("PROCESSING", "SHIPPING", "DELIVERED", "SHIPPED", "COMPLETED").contains(status)){
                return true;
            }
        }


        BigDecimal clientTranfer = webhookData.getTransferAmount();
        BigDecimal totalAmount = order.getTotalAmount();

        if (clientTranfer.compareTo(totalAmount) < 0 ){
            log.warn("khach chuyen khoan thieu");
            return false;
        }

        order.setStatus("PROCESSING");
        order.setPaidAt(LocalDateTime.now());
        order.setNote(webhookData.getReferenceCode());
        orderRepository.save(order);
        return true;






    }
}
