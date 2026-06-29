package com.antigastos.boludos.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Smoke tests del guard anti-spam de los workers.
 *
 * No tocan WorkManager ni Android: se prueban contra `decideConsume`,
 * que es el corazón puro del state. Si esto se rompe, los workers van
 * a empezar a duplicar notifs.
 */
class WorkerNotificationStateTest {

    private val day = LocalDate.of(2026, 5, 10)

    @Test
    fun firstFireOfDay_returnsTrue() {
        val (fired, _) = WorkerNotificationState.decideConsume(prev = null, kind = "daily_gift", day = day)
        assertTrue("primer fire del día debe disparar", fired)
    }

    @Test
    fun secondFireSameDay_returnsFalse() {
        val key1 = WorkerNotificationState.computeKey("daily_gift", day)
        val (fired, _) = WorkerNotificationState.decideConsume(prev = key1, kind = "daily_gift", day = day)
        assertFalse("segundo fire del mismo día NO debe disparar", fired)
    }

    @Test
    fun fireOnDifferentDay_returnsTrue() {
        val key1 = WorkerNotificationState.computeKey("daily_gift", day.minusDays(1))
        val (fired, _) = WorkerNotificationState.decideConsume(prev = key1, kind = "daily_gift", day = day)
        assertTrue("nuevo día debe volver a disparar", fired)
    }

    @Test
    fun differentKinds_areIndependent() {
        val keyA = WorkerNotificationState.computeKey("daily_gift", day)
        // Mismo día, otro kind: la clave previa de daily_gift no afecta
        // a budget_check.
        val (fired, _) = WorkerNotificationState.decideConsume(prev = keyA, kind = "budget_check", day = day)
        assertTrue("kinds distintos no comparten escudo", fired)
    }

    @Test
    fun computeKey_usesIsoDateAndKind() {
        val k = WorkerNotificationState.computeKey("budget_check", day)
        assertEquals("budget_check_2026-05-10", k)
        assertNotEquals(
            "claves de días distintos no deben colisionar",
            WorkerNotificationState.computeKey("budget_check", day.minusDays(1)),
            k,
        )
    }

    @Test
    fun budgetCheckGuard_blocksRetriesSameDay() {
        // Simulamos: WM dispara budget_check 4 veces el mismo día
        // (reintentos por backoff). Solo la primera debería disparar la notif.
        var stored: String? = null
        var firedCount = 0
        repeat(4) {
            val (fire, newKey) = WorkerNotificationState.decideConsume(stored, "budget_check", day)
            if (fire) {
                firedCount += 1
                stored = newKey
            }
        }
        assertEquals(1, firedCount)
    }
}
