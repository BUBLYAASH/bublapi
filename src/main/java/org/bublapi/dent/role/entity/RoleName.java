package org.bublapi.dent.role.entity;

public enum RoleName {
   ADMIN,
   OWNER,
   DOCTOR,
   RECEPTIONIST,
   PATIENT;

   public boolean canAssign(RoleName targetRole) {
      return switch (this) {
         case ADMIN -> true;
         case OWNER -> targetRole == DOCTOR || targetRole == RECEPTIONIST;
         case RECEPTIONIST -> targetRole == DOCTOR;
         case DOCTOR, PATIENT -> false;
      };
   }

   public boolean canRemove(RoleName targetRole) {
      return switch (this) {
         case ADMIN -> true;
         case OWNER -> targetRole == DOCTOR || targetRole == RECEPTIONIST;
         case RECEPTIONIST -> targetRole == DOCTOR;
         case DOCTOR, PATIENT -> false;
      };
   }
}