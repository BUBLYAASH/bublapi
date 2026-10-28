package org.bublapi.dent.integration.appointment;

import org.bublapi.dent.integration.IntegrationTestSupport;
import org.bublapi.dent.role.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentConcurrencyIntegrationTest extends IntegrationTestSupport {

   @Autowired
   private JdbcTemplate jdbcTemplate;

   @Test
   void shouldNotCreateOverlappingAppointmentsConcurrently() throws Exception {

      var context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime firstTime = futureDateAt(10, 0);
      LocalDateTime secondTime = futureDateAt(10, 15);

      addRegularWorkingHours(context.doctor(), firstTime.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      List<Integer> statuses = executeConcurrently(
              () -> createStaffAppointment(context, firstTime).andReturn().getResponse().getStatus(),
              () -> createStaffAppointment(context, secondTime).andReturn().getResponse().getStatus());

      assertThat(statuses).contains(200).hasSize(2);

      assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);

      assertThat(statuses).filteredOn(status -> status == 400 || status == 409).hasSize(1);

      assertThat(countAppointments(context)).isEqualTo(1);
   }

   @Test
   void shouldAllowAdjacentAppointmentsConcurrently() throws Exception {

      var context = createAppointmentContext(RoleName.OWNER);
      LocalDateTime firstTime = futureDateAt(10, 0);
      LocalDateTime secondTime = futureDateAt(10, 30);

      addRegularWorkingHours(context.doctor(), firstTime.toLocalDate(), LocalTime.of(9, 0), LocalTime.of(18, 0));

      List<Integer> statuses = executeConcurrently(
              () -> createStaffAppointment(context, firstTime).andReturn().getResponse().getStatus(),
              () -> createStaffAppointment(context, secondTime).andReturn().getResponse().getStatus());

      assertThat(statuses).containsExactlyInAnyOrder(200, 200);

      assertThat(countAppointments(context)).isEqualTo(2);
   }

   private List<Integer> executeConcurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {

      ExecutorService executor = Executors.newFixedThreadPool(2);

      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);

      try {
         Future<Integer> firstFuture = executor.submit(() -> executeWhenReady(first, ready, start));

         Future<Integer> secondFuture = executor.submit(() -> executeWhenReady(second, ready, start));

         assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

         start.countDown();

         return List.of(firstFuture.get(30, TimeUnit.SECONDS), secondFuture.get(30, TimeUnit.SECONDS));
      } finally {
         start.countDown();
         executor.shutdownNow();
      }
   }

   private int executeWhenReady(Callable<Integer> action, CountDownLatch ready, CountDownLatch start) throws Exception {

      ready.countDown();

      if (!start.await(10, TimeUnit.SECONDS)) {
         throw new IllegalStateException("Concurrent test start timed out");
      }

      return action.call();
   }

   private long countAppointments(AppointmentContext context) {
      Long count = jdbcTemplate.queryForObject("""
                                                       SELECT COUNT(*)
                                                       FROM appointments
                                                       WHERE clinic_id = ?
                                                         AND doctor_id = ?
                                                       """, Long.class, context.clinic().getId(),
                                               context.doctor().getId());

      return count == null ? 0 : count;
   }
}
