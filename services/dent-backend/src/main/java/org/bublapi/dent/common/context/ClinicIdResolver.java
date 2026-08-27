package org.bublapi.dent.common.context;

import java.util.UUID;
import java.util.function.Supplier;

public class ClinicIdResolver implements Supplier<UUID> {
   @Override
   public UUID get() {
      return ClinicContext.getClinicId();
   }
}
