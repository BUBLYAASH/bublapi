package org.bublapi.dent.common.exception;

public class InvalidRefreshTokenException extends RuntimeException {
   public InvalidRefreshTokenException() {
      super("Invalid refresh token");
   }
}
