package com.example.javafeatures;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

/**
 * Thread-safe generic LRU cache: HashMap + doubly linked list.
 * get/put are O(1). Head side = most recently used, tail side = least recently used.
 */
public class LRUCache<K, V> {

    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev, next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map;
    private final Node<K, V> head = new Node<>(null, null); // dummy sentinel
    private final Node<K, V> tail = new Node<>(null, null); // dummy sentinel
    private final ReentrantLock lock = new ReentrantLock();
    private final BiConsumer<K, V> onEvict; // optional listener, may be null

    private long hits, misses, evictions;

    public LRUCache(int capacity) {
        this(capacity, null);
    }

    public LRUCache(int capacity, BiConsumer<K, V> onEvict) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.onEvict = onEvict;
        this.map = new HashMap<>(capacity * 4 / 3 + 1);
        head.next = tail;
        tail.prev = head;
    }

    /** Returns the value and marks the key as most recently used. */
    public Optional<V> get(K key) {
        Objects.requireNonNull(key);
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node == null) {
                misses++;
                return Optional.empty();
            }
            hits++;
            moveToFront(node);
            return Optional.of(node.value);
        } finally {
            lock.unlock();
        }
    }

    /** Inserts or updates the key. Evicts the LRU entry if capacity is exceeded. */
    public void put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        lock.lock();
        try {
            Node<K, V> node = map.get(key);
            if (node != null) {
                node.value = value;
                moveToFront(node);
                return;
            }
            if (map.size() >= capacity) {
                evictLru();
            }
            node = new Node<>(key, value);
            map.put(key, node);
            addToFront(node);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(K key) {
        lock.lock();
        try {
            Node<K, V> node = map.remove(key);
            if (node == null) return false;
            unlink(node);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    public String stats() {
        lock.lock();
        try {
            long total = hits + misses;
            double ratio = total == 0 ? 0 : (double) hits / total;
            return String.format("hits=%d misses=%d evictions=%d hitRatio=%.2f",
                    hits, misses, evictions, ratio);
        } finally {
            lock.unlock();
        }
    }

    // ---- linked list helpers (call only while holding the lock) ----

    private void addToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = node.next = null;
    }

    private void moveToFront(Node<K, V> node) {
        unlink(node);
        addToFront(node);
    }

    private void evictLru() {
        Node<K, V> lru = tail.prev;
        if (lru == head) return; // empty
        unlink(lru);
        map.remove(lru.key); // why Node stores the key
        evictions++;
        if (onEvict != null) onEvict.accept(lru.key, lru.value);
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder("[");
            for (Node<K, V> n = head.next; n != tail; n = n.next) {
                sb.append(n.key).append('=').append(n.value);
                if (n.next != tail) sb.append(", ");
            }
            return sb.append("] (MRU -> LRU)").toString();
        } finally {
            lock.unlock();
        }
    }

    // ---- Short alternative: LinkedHashMap in access-order mode ----
    static class SimpleLRU<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        SimpleLRU(int capacity) {
            super(16, 0.75f, true); // true = access order
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
        // Not thread-safe: wrap with Collections.synchronizedMap(...) if needed.
    }

    // ---- Demo ----
    public static void main(String[] args) throws Exception {
        LRUCache<Integer, String> cache =
                new LRUCache<>(3, (k, v) -> System.out.println("evicted " + k + "=" + v));

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        System.out.println(cache);                 // [3=C, 2=B, 1=A]
        cache.get(1);                              // 1 becomes MRU
        System.out.println(cache);                 // [1=A, 3=C, 2=B]
        cache.put(4, "D");                         // evicts 2
        System.out.println(cache);                 // [4=D, 1=A, 3=C]
        System.out.println(cache.get(2).isPresent()); // false
        System.out.println(cache.stats());

        // Concurrency smoke test
        LRUCache<Integer, Integer> shared = new LRUCache<>(100);
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 100_000; i++) {
                    int k = (int) (Math.random() * 500);
                    if (shared.get(k).isEmpty()) shared.put(k, k);
                }
            });
            threads[t].start();
        }
        for (Thread th : threads) th.join();
        System.out.println("size=" + shared.size() + " " + shared.stats());

        SimpleLRU<String, Integer> simple = new SimpleLRU<>(2);
        simple.put("a", 1);
        simple.put("b", 2);
        simple.get("a");
        simple.put("c", 3); // evicts b
        System.out.println(simple.keySet()); // [a, c]
    }
}
