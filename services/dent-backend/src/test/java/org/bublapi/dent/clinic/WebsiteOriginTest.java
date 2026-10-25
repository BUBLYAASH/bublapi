package org.bublapi.dent.clinic;

import org.bublapi.dent.clinic.service.WebsiteOrigin;
import org.bublapi.dent.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebsiteOriginTest {
   @Test
   void extractsWebsiteOrigin() {
      assertEquals("https://clinic.ru", WebsiteOrigin.fromWebsite("https://CLINIC.ru:443/booking?q=1#x"));
      assertEquals("http://localhost:3000", WebsiteOrigin.fromWebsite("http://localhost:3000/"));
   }

   @Test
   void rejectsUnsafeValues() {
      for (String value : new String[]{"null", "//clinic.ru", "ftp://clinic.ru", "https://*.clinic.ru",
                                       "https://user:password@clinic.ru", "https://clinic.ru:99999",
                                       "https://clinic.ru:0"}) {
         assertThrows(BadRequestException.class, () -> WebsiteOrigin.fromWebsite(value), value);
      }
      assertThrows(BadRequestException.class, () -> WebsiteOrigin.requireOrigin("https://clinic.ru/path"));
      assertFalse(WebsiteOrigin.isCanonical("https://clinic.ru/"));
      assertFalse(WebsiteOrigin.isCanonical(null));
   }
}
