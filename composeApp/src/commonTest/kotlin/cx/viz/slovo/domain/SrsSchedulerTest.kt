package cx.viz.slovo.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SrsSchedulerTest {

    private fun card(id: String, box: Int, lastSeen: Long?) =
        CardProgress(cardId = id, box = box, lastSeenDay = lastSeen)

    @Test fun correct_answer_promotes_one_box_and_caps_at_max() {
        assertEquals(1, SrsScheduler.nextBox(0, correct = true))
        assertEquals(3, SrsScheduler.nextBox(2, correct = true))
        assertEquals(SrsScheduler.MAX_BOX, SrsScheduler.nextBox(SrsScheduler.MAX_BOX, correct = true))
    }

    @Test fun wrong_answer_resets_to_box_zero_from_any_box() {
        assertEquals(0, SrsScheduler.nextBox(0, correct = false))
        assertEquals(0, SrsScheduler.nextBox(5, correct = false))
    }

    @Test fun card_is_not_due_before_its_interval_elapses() {
        // box 1 -> interval 2 days; seen on day 100 -> due day 102
        assertFalse(SrsScheduler.isDue(card("a", 1, 100), today = 101))
    }

    @Test fun card_is_due_exactly_on_and_after_the_interval_day() {
        assertTrue(SrsScheduler.isDue(card("a", 1, 100), today = 102))
        assertTrue(SrsScheduler.isDue(card("a", 1, 100), today = 200))
    }

    @Test fun a_card_never_seen_is_treated_as_due() {
        assertTrue(SrsScheduler.isDue(card("a", 0, null), today = 100))
    }

    @Test fun dueCount_counts_only_due_seen_cards() {
        val seen = listOf(
            card("due1", 0, 100),   // interval 1 -> due day 101
            card("due2", 1, 90),    // interval 2 -> due day 92
            card("fresh", 2, 100),  // interval 4 -> due day 104
        )
        assertEquals(2, SrsScheduler.dueCount(seen, today = 103))
    }

    @Test fun pickForDrill_serves_due_first_most_overdue_first_then_pads_with_soonest_due() {
        val seen = listOf(
            card("notDueSoon", 3, 100),  // due day 107
            card("dueMild", 0, 100),     // due day 101
            card("notDueLater", 4, 100), // due day 114
            card("dueBad", 1, 90),       // due day 92 (most overdue)
        )
        // today 103: due = dueBad(92), dueMild(101); notDue = notDueSoon(107), notDueLater(114)
        val ordered = SrsScheduler.pickForDrill(seen, today = 103, size = 10)
        assertEquals(listOf("dueBad", "dueMild", "notDueSoon", "notDueLater"), ordered)
    }

    @Test fun pickForDrill_respects_the_size_cap() {
        val seen = (1..15).map { card("c$it", 0, 100) }  // all due on day 101
        assertEquals(5, SrsScheduler.pickForDrill(seen, today = 200, size = 5).size)
    }

    @Test fun pickForDrill_on_an_empty_pool_returns_empty() {
        assertEquals(emptyList(), SrsScheduler.pickForDrill(emptyList(), today = 100, size = 10))
    }

    @Test fun boxHistogram_counts_seen_cards_per_box() {
        val seen = listOf(
            card("a", 0, 100), card("b", 0, 100),
            card("c", 2, 100),
            card("d", 5, 100),
        )
        assertEquals(listOf(2, 0, 1, 0, 0, 1), SrsScheduler.boxHistogram(seen).toList())
    }

    @Test fun boxHistogram_on_empty_pool_is_all_zeros() {
        assertEquals(listOf(0, 0, 0, 0, 0, 0), SrsScheduler.boxHistogram(emptyList()).toList())
    }

    @Test fun dueForecast_folds_overdue_and_today_into_index_zero() {
        val seen = listOf(
            card("overdue", 0, 90),   // interval 1 -> due 91; today 103 -> overdue
            card("today", 1, 101),    // interval 2 -> due 103 == today
        )
        assertEquals(2, SrsScheduler.dueForecast(seen, today = 103, days = 7)[0])
    }

    @Test fun dueForecast_places_cards_on_their_exact_offset_day() {
        // box 2 interval 4, seen day 100 -> due 104; today 101 -> offset 3
        val f = SrsScheduler.dueForecast(listOf(card("in3", 2, 100)), today = 101, days = 7)
        assertEquals(1, f[3])
        assertEquals(0, f[0])
    }

    @Test fun dueForecast_drops_cards_due_beyond_the_window() {
        // box 5 interval 30, seen day 100 -> due 130; today 100 -> offset 30 > 7
        val f = SrsScheduler.dueForecast(listOf(card("far", 5, 100)), today = 100, days = 7)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0), f.toList())
    }
}
