package org.bublapi.dent.clinic.service;

import org.bublapi.dent.common.exception.BadRequestException;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;

public final class WebsiteOrigin {
   private WebsiteOrigin() {
   }

   public static String fromWebsite(String value) {
      try {
         URI uri = new URI(value.trim());
         String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
         String host = uri.getHost();
         if (!(scheme.equals("https") || scheme.equals("http")) || host == null
                 || uri.getRawUserInfo() != null || host.contains("*") || uri.getPort() == 0
                 || uri.getPort() > 65535) throw new IllegalArgumentException();
         host = host.toLowerCase(Locale.ROOT);
         if (!host.startsWith("[")) host = IDN.toASCII(host);
         int port = uri.getPort();
         String origin = scheme + "://" + host + ((port == -1 || (scheme.equals("https") && port == 443)
                 || (scheme.equals("http") && port == 80)) ? "" : ":" + port);
         if (origin.length() > 255) throw new IllegalArgumentException();
         return origin;
      } catch (Exception e) {
         throw new BadRequestException("Укажите адрес сайта с http:// или https:// без логина и пароля");
      }
   }

   public static String requireOrigin(String value) {
      String origin = fromWebsite(value);
      if (!(value.trim().equals(origin) || value.trim().equals(origin + "/"))) {
         throw new BadRequestException("Укажите origin без пути, параметров и фрагмента, например https://clinic.ru");
      }
      return origin;
   }

   public static boolean isCanonical(String value) {
      if (value == null) return false;
      try {
         return value.equals(fromWebsite(value));
      } catch (BadRequestException e) {
         return false;
      }
   }
}
