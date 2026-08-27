package org.bublapi.dent.common.context;

import org.bublapi.dent.clinic.entity.Clinic;

import java.util.UUID;

public class ClinicContext {
   private static final ThreadLocal<Clinic> current = new ThreadLocal<>();

   public static void set(Clinic clinic) {
      current.set(clinic);
   }

   public static Clinic get() {
      return current.get();
   }

   public static UUID getClinicId() {
      return current.get() != null ? current.get().getId() : null;
   }

   public static void clear() {
      current.remove();
   }
}
