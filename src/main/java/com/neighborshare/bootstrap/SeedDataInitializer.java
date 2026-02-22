package com.neighborshare.bootstrap;

import com.neighborshare.domain.entity.Apartment;
import com.neighborshare.domain.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer implements CommandLineRunner {

    private final ApartmentRepository apartmentRepository;

    @Override
    public void run(String... args) {
        if (apartmentRepository.count() > 0) {
            return;
        }

        Apartment demoApartment = Apartment.builder()
            .name("Demo Apartment")
            .inviteCode("DEMO123")
            .address("100 Main Street")
            .city("San Francisco")
            .country("USA")
            .createdBy(UUID.randomUUID())
            .build();

        apartmentRepository.save(demoApartment);
        log.info("Seeded default apartment with invite code DEMO123");
    }
}
