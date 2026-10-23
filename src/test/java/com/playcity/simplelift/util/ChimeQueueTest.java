package com.playcity.simplelift.util;

import com.playcity.simplelift.config.ButtonChimes.Profile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChimeQueueTest {
    static final class Task {
        final Runnable run;
        final int ticks;
        boolean cancelled;
        Task(Runnable run, int ticks) { this.run = run; this.ticks = ticks; }
    }
    private final List<Task> tasks = new ArrayList<>();
    private final ChimeQueue<String> queue = new ChimeQueue<>((run, ticks) -> {
        Task task = new Task(run, ticks);
        tasks.add(task);
        return () -> task.cancelled = true;
    });
    private void finish() {
        tasks.stream().sorted(Comparator.comparingInt(task -> task.ticks))
                .filter(task -> !task.cancelled).forEach(task -> task.run.run());
    }

    @Test void twoPassengersHearOneCompleteArrangementEach() {
        List<Integer> first = new ArrayList<>();
        List<Integer> second = new ArrayList<>();
        queue.play("first", Profile.WARM, note -> first.add(note.delayTicks));
        queue.play("second", Profile.WARM, note -> second.add(note.delayTicks));
        finish();
        assertEquals(java.util.Arrays.asList(0, 8), first);
        assertEquals(first, second);
        assertEquals(0, queue.size());
    }

    @Test void simultaneousLiftsKeepDistinctProfiles() {
        List<Float> wood = new ArrayList<>();
        List<Float> stone = new ArrayList<>();
        queue.play("wood-lift", Profile.WARM, note -> wood.add(note.pitch));
        queue.play("stone-lift", Profile.CLEAR, note -> stone.add(note.pitch));
        finish();
        assertEquals(java.util.Arrays.asList(0.793701F, 0.629961F), wood);
        assertEquals(java.util.Arrays.asList(1.0F, 0.793701F), stone);
    }

    @Test void newArrivalCancelsOldNotesForOnlyThatPlayer() {
        List<String> heard = new ArrayList<>();
        queue.play("same", Profile.WARM, note -> heard.add("old"));
        queue.play("other", Profile.CLEAR, note -> heard.add("other"));
        queue.play("same", Profile.CRYSTAL, note -> heard.add("new"));
        finish();
        assertFalse(heard.contains("old"));
        assertEquals(3, heard.stream().filter("new"::equals).count());
        assertEquals(2, heard.stream().filter("other"::equals).count());
    }

    @Test void logoutAndShutdownSuppressPendingNotes() {
        queue.play("quit", Profile.WARM, note -> fail("Logged-out player heard a note"));
        queue.cancel("quit");
        queue.play("reload", Profile.CLEAR, note -> fail("Cancelled note played"));
        queue.cancelAll();
        finish();
        assertEquals(0, queue.size());
    }

    @Test void silentReplacesOldChimeWithoutSchedulingWork() {
        queue.play("same", Profile.WARM, note -> fail("Old note played"));
        int scheduled = tasks.size();
        queue.play("same", Profile.SILENT, note -> fail("Silent profile played"));
        finish();
        assertEquals(scheduled, tasks.size());
        assertEquals(0, queue.size());
    }

    @Test void partialSchedulerFailureCancelsAlreadyQueuedNotes() {
        ChimeQueue<String> failing = new ChimeQueue<>((run, ticks) -> {
            if (ticks > 0) { throw new IllegalStateException("Scheduler unavailable"); }
            Task task = new Task(run, ticks);
            tasks.add(task);
            return () -> task.cancelled = true;
        });
        assertThrows(IllegalStateException.class, () -> failing.play("rider", Profile.WARM, note -> fail()));
        assertTrue(tasks.get(0).cancelled);
        assertEquals(0, failing.size());
    }
}
