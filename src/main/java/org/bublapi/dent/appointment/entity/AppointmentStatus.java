package org.bublapi.dent.appointment.entity;

public enum AppointmentStatus {
   CREATED,
   CANCELLED,
   CONFIRMED,
   COMPLETED;

   public boolean canTransitionTo(AppointmentStatus target) {
      return switch (this) {
         case CREATED -> target == CONFIRMED || target == CANCELLED;
         case CONFIRMED -> target == COMPLETED || target == CANCELLED;
         case CANCELLED -> false;
         case COMPLETED -> false;
      };
   }
}
