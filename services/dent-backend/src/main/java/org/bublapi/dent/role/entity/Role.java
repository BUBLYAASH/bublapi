package org.bublapi.dent.role.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "roles")
public class Role {

   @EqualsAndHashCode.Include
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Enumerated(EnumType.STRING)
   @Column(length = 50, nullable = false, unique = true)
   private RoleName name;
}
