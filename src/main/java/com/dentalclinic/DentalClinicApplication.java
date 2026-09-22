package com.dentalclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DentalClinicApplication {
    public static void main(String[] args) {
        SpringApplication.run(DentalClinicApplication.class, args);
        System.out.println("==================================================");
        System.out.println("🦷 DENTAL CLINIC SYSTEM IS RUNNING ON PORT 8080 🦷");
        System.out.println("👉 Access Web Portal: http://localhost:8080");
        System.out.println("👉 H2 Database Console: http://localhost:8080/h2-console");
        System.out.println("==================================================");
    }
}
