package com.superadmin.controller;

import com.superadmin.dto.UserDto;
import com.superadmin.services.superadmin.SuperAdminService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/make-admin/{id}")
    private ResponseEntity<?> makeAdmin(@PathVariable UUID id) {
        UserDto userDto = superAdminService.makeAdmin(id);
        if (userDto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(userDto);
    }
    @GetMapping("/make-user/{id}")
    private ResponseEntity<?> makeUser(@PathVariable UUID id) {
        UserDto userDto = superAdminService.makeUser(id);
        if (userDto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(userDto);
    }

}
