package com.playcity.simplelift.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BoundedFloodFill {

    private BoundedFloodFill() {
    }

    public static <T> Set<T> collect(T origin, int maximumSize,
            Function<T, ? extends Iterable<T>> neighbors, Predicate<T> included) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        if (!included.test(origin)) {
            return Collections.emptySet();
        }

        Set<T> visited = new LinkedHashSet<>();
        Deque<T> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty()) {
            T current = queue.removeFirst();
            for (T neighbor : neighbors.apply(current)) {
                if (!included.test(neighbor) || !visited.add(neighbor)) {
                    continue;
                }
                if (visited.size() > maximumSize) {
                    throw new SizeLimitExceededException(maximumSize);
                }
                queue.addLast(neighbor);
            }
        }
        return Collections.unmodifiableSet(new LinkedHashSet<T>(visited));
    }

    public static final class SizeLimitExceededException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        private final int maximumSize;

        private SizeLimitExceededException(int maximumSize) {
            super("Connected area exceeds maximum size " + maximumSize);
            this.maximumSize = maximumSize;
        }

        public int maximumSize() {
            return maximumSize;
        }
    }
}
