package com.example.knittdaserver.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Member {
    @Id
    Long id;
    String name;
}
