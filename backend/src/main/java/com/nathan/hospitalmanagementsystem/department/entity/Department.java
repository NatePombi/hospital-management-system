package com.nathan.hospitalmanagementsystem.department.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.boot.UnsupportedOrmXsdVersionException;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "departments")
@Getter
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @CurrentTimestamp
    @Column(nullable = false,name = "created_at")
    private LocalDateTime createdAt;



    private Department(String name, String description){
        this.name = name;
        this.description = description;
    }

    public static Department create(String name,String description){
        String normalizedName = normalizeName(name);
        String normalizedDescription = normalizeDescription(description);

        return new Department(normalizedName,normalizedDescription);
    }

    public void updateDepartment(String name, String description) {
        String normalizedName = normalizeName(name);
        String normalizedDescription = normalizeDescription(description);

        this.name = normalizedName;
        this.description = normalizedDescription;
    }

    @PrePersist
    private void onCreate(){
        if(createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void Deactivate(){
        this.active = false;
    }

    public void Activate(){
        this.active = true;
    }

    private static String normalizeName(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("Department name cannot be null or blank");
        }

        String normalizedName = name.strip();

        if(normalizedName.length() > 100){
            throw new IllegalArgumentException("Department name exceed 100 characters");
        }

        return normalizedName;
    }

    private static String normalizeDescription(String description){
        if(description == null){
            return null;
        }

        String normalizedDescription = description.strip();

        if(normalizedDescription.length() > 500){
            throw new IllegalArgumentException("Department description exceed 500 characters");
        }

        return normalizedDescription;
    }

}
