package com.duonglabs.multitenancy.domain;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tenant-agnostic: the repository hits whichever database TenantContext points at. */
@RestController
@RequestMapping("/check-ins")
public class CheckInController {
    public record CheckInRequest(String customerName) {
    }

    private final CheckInRepository repository;

    public CheckInController(CheckInRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckIn create(@RequestBody CheckInRequest request) {
        return repository.save(new CheckIn(request.customerName()));
    }

    @GetMapping
    public List<CheckIn> list() {
        return repository.findAll();
    }
}
