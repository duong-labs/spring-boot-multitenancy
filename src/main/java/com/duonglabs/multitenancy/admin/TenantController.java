package com.duonglabs.multitenancy.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants")
public class TenantController {
    public record CreateTenantRequest(String code, String name) {
    }

    public record TenantResponse(String code, String name, String dbUrl) {
        static TenantResponse of(Tenant t) {
            return new TenantResponse(t.getCode(), t.getName(), t.getDbUrl());
        }
    }

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;

    public TenantController(TenantService tenantService, TenantRepository tenantRepository) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@RequestBody CreateTenantRequest request) {
        return TenantResponse.of(tenantService.create(request.code(), request.name()));
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(TenantResponse::of).toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badRequest(IllegalArgumentException e) {
        return e.getMessage();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflict(IllegalStateException e) {
        return e.getMessage();
    }
}
