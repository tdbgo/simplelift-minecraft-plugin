package com.playcity.simplelift.util;

import com.playcity.simplelift.config.ButtonChimes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Server-thread queue with replacement and cancellation isolated from sound playback. */
public final class ChimeQueue<K> {
    public interface Scheduler {
        /** Schedule for a later tick and return a cancellation callback. */
        Runnable later(Runnable action, int ticks);
    }
    private final Scheduler scheduler;
    private final Map<K, List<Runnable>> pending = new HashMap<>();

    public ChimeQueue(Scheduler scheduler) { this.scheduler = scheduler; }

    public void play(K key, ButtonChimes.Profile profile, Consumer<ButtonChimes.Note> playNote) {
        cancel(key);
        if (profile.notes().isEmpty()) { return; }
        List<Runnable> tasks = new ArrayList<>();
        pending.put(key, tasks);
        try {
            for (int i = 0; i < profile.notes().size(); i++) {
                ButtonChimes.Note note = profile.notes().get(i);
                boolean last = i == profile.notes().size() - 1;
                tasks.add(scheduler.later(() -> {
                    if (pending.get(key) != tasks) { return; }
                    try {
                        playNote.accept(note);
                    } finally {
                        if (last && pending.get(key) == tasks) { pending.remove(key); }
                    }
                }, note.delayTicks));
            }
        } catch (RuntimeException exception) {
            cancel(key);
            throw exception;
        }
    }

    public void cancel(K key) {
        List<Runnable> tasks = pending.remove(key);
        if (tasks != null) { tasks.forEach(Runnable::run); }
    }

    public void cancelAll() {
        pending.values().forEach(tasks -> tasks.forEach(Runnable::run));
        pending.clear();
    }

    public int size() { return pending.size(); }
}
