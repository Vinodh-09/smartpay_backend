package com.cognizant.smartpay;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.cognizant.smartpay.service.CaenRfidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application Class for SmartPay Backend
 * 
 * @author Cognizant SmartPay Team
 * @version 1.0.0
 */
@SpringBootApplication
//@EnableScheduling
public class SmartPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPayApplication.class, args);
        System.out.println("===========================================");
        System.out.println("SmartPay Backend Started Successfully!");
        System.out.println("Server running on: http://localhost:8080");
        System.out.println("===========================================");
    }

//    @Autowired
//    private CaenRfidService caenRfidService;
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void startScan() throws CAENRFIDException {
//        caenRfidService.startInventory(); // Start on app startup
//    }
}
