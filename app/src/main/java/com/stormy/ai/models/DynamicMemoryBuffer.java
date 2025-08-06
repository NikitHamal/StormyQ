package com.stormy.ai.models; // Putting it in models package

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A simple dynamic memory buffer (working memory) that stores a limited number
 * of recent AnswerResult objects. When the buffer is full, older items are removed.
 * This can be extended with decay or relevance-based removal if needed.
 */
public class DynamicMemoryBuffer {
    private static final int DEFAULT_CAPACITY = 3; // Default capacity of the buffer
    private final int capacity;
    private final Queue<AnswerResult> buffer; // Using a Queue (LinkedList) for FIFO behavior

    /**
     * Constructor with default capacity.
     */
    public DynamicMemoryBuffer() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructor with a specified capacity.
     * @param capacity The maximum number of AnswerResult objects to store.
     */
    public DynamicMemoryBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        this.capacity = capacity;
        this.buffer = new LinkedList<>();
    }

    /**
     * Adds an AnswerResult to the buffer. If the buffer is full, the oldest item is removed.
     * @param result The AnswerResult to add.
     */
    public void addResult(AnswerResult result) {
        if (result == null) {
            return; // Do not add null results
        }
        if (buffer.size() >= capacity) {
            buffer.poll(); // Remove the oldest item (head of the queue)
        }
        buffer.offer(result); // Add the new item to the end of the queue
    }

    /**
     * Retrieves all AnswerResult objects currently in the buffer.
     * @return A List of AnswerResult objects.
     */
    public List<AnswerResult> getResults() {
        return new LinkedList<>(buffer); // Return a copy to prevent external modification
    }

    /**
     * Clears all items from the buffer.
     */
    public void clear() {
        buffer.clear();
    }

    /**
     * Checks if the buffer is empty.
     * @return true if the buffer is empty, false otherwise.
     */
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /**
     * Returns the current size of the buffer.
     * @return The number of items in the buffer.
     */
    public int size() {
        return buffer.size();
    }
}
