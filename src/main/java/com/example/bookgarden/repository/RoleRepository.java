package com.example.bookgarden.repository;

import com.example.bookgarden.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {
    Role findByRoleName(String roleName);
}
