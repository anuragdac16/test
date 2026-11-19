package com.example.javafeatures;

import com.example.javafeatures.Shape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Demonstrates features that are FINAL (standard) in Java 21 (LTS).
 * Requires JDK 21+.
 *
 * NOT covered because they are still PREVIEW in 21 (need --enable-preview):
 *   String Templates (JEP 430), Unnamed Patterns and Variables "_" (JEP 443),
 *   Unnamed Classes and Instance Main Methods (JEP 445).
 */
public class Java21Features {

    // Helper records for the record-pattern demos.
    record Point(int x, int y) { }
    record Line(Point from, Point to) { }

    // ------------------------------------------------------------------
    // 1. PATTERN MATCHING FOR switch (JEP 441, final in 21)
    // ------------------------------------------------------------------
    /**
     * switch can now match on TYPES (not just constants):
     *  - Exhaustive over the sealed Shape: NO 'default' needed. If someone adds a new
     *    permitted Shape, this method fails to compile until it is handled.
     */
    public static double area(Shape shape) {
        return switch (shape) {
            case Shape.Circle c -> Math.PI * c.radius() * c.radius();
            case Shape.Rectangle r -> r.width() * r.height();
            case Shape.Triangle t -> 0.5 * t.base() * t.height();
        };
    }

    /**
     * - 'case null' lets switch handle null explicitly (previously it threw NPE).
     * - Guards use 'when' to refine a pattern: case Integer i when i > 100.
     *   Order matters: the more specific (guarded) case must come BEFORE the general one.
     */
    public static String describe(Object obj) {
        return switch (obj) {
            case null -> "It's null";
            case Integer i when i > 100 -> "Big integer: " + i;
            case Integer i -> "Small integer: " + i;
            case String s when s.isBlank() -> "Blank string";
            case String s -> "String of length " + s.length();
            case Shape.Circle c -> "Circle with radius " + c.radius();
            case List<?> list -> "List with " + list.size() + " element(s)";
            default -> "Something else: " + obj.getClass().getSimpleName();
        };
    }

    // ------------------------------------------------------------------
    // 2. RECORD PATTERNS (JEP 440, final in 21)
    // ------------------------------------------------------------------
    /**
     * Deconstruct a record directly in instanceof or switch, including NESTED records.
     * 'var' infers component types. No accessor calls like line.from().x() needed.
     */
    public static String recordPatternDemo(Object obj) {
        if (obj instanceof Line(Point(var x1, var y1), Point(var x2, var y2))) {
            double length = Math.hypot(x2 - x1, y2 - y1);
            return "Line (%d,%d)->(%d,%d), length=%.2f".formatted(x1, y1, x2, y2, length);
        }
        // Same idea inside a switch:
        return switch (obj) {
            case Point(int x, int y) when x == 0 && y == 0 -> "Origin point";
            case Point(int x, int y) -> "Point at (" + x + "," + y + ")";
            default -> "Not a point/line";
        };
    }

    // ------------------------------------------------------------------
    // 3. SEQUENCED COLLECTIONS (JEP 431, final in 21)
    // ------------------------------------------------------------------
    /**
     * New interfaces SequencedCollection, SequencedSet, SequencedMap give every
     * ordered collection uniform methods: getFirst(), getLast(), addFirst(), addLast(),
     * removeFirst(), removeLast() and reversed() (a live reverse VIEW).
     */
    public static void sequencedCollectionsDemo() {
        // List
        List<String> list = new ArrayList<>(List.of("B", "C", "D"));
        list.addFirst("A");   // before Java 21: list.add(0, "A")
        list.addLast("E");
        System.out.println("List first/last : " + list.getFirst() + " / " + list.getLast()); // was list.get(list.size()-1)
        System.out.println("List reversed    : " + list.reversed());

        // SequencedCollection is the common supertype of List, Deque, LinkedHashSet...
        SequencedCollection<String> seq = list;
        System.out.println("Removed last     : " + seq.removeLast());

        // LinkedHashSet -> SequencedSet
        SequencedSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        set.addFirst(0);
        System.out.println("Set reversed     : " + set.reversed());

        // LinkedHashMap -> SequencedMap
        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.putFirst("zero", 0);  // insert at the front
        System.out.println("Map firstEntry   : " + map.firstEntry());
        System.out.println("Map lastEntry    : " + map.lastEntry());
        System.out.println("Map reversed     : " + map.reversed());
    }

    // ------------------------------------------------------------------
    // 4. VIRTUAL THREADS (JEP 444, final in 21)
    // ------------------------------------------------------------------
    /**
     * Virtual threads are lightweight threads managed by the JVM (not 1:1 with OS threads).
     * You can run thousands/millions of blocking tasks cheaply - ideal for I/O-bound work.
     * Write plain blocking code; no reactive style required.
     */
    public static void virtualThreadsDemo() throws InterruptedException, ExecutionException {
        // (a) Start a single virtual thread directly.
        Thread vt = Thread.ofVirtual().name("my-virtual-thread").start(() ->
                System.out.println("Hello from " + Thread.currentThread().getName()
                        + " (virtual=" + Thread.currentThread().isVirtual() + ")"));
        vt.join();

        // (b) One new virtual thread PER TASK via an executor.
        //     ExecutorService is AutoCloseable (since 19): close() waits for tasks to finish.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 1; i <= 1_000; i++) {
                final int n = i;
                futures.add(executor.submit(() -> {
                    Thread.sleep(10); // blocking call is cheap on a virtual thread
                    return n;
                }));
            }
            long sum = 0;
            for (Future<Integer> f : futures) {
                sum += f.get();
            }
            System.out.println("Sum of 1..1000 computed by 1000 virtual threads = " + sum);
        }
    }

    // ------------------------------------------------------------------
    // 5. SMALL API ADDITIONS in 21
    // ------------------------------------------------------------------
    public static void smallApiAdditionsDemo() {
        // String.indexOf(ch, beginIndex, endIndex): search only within a range.
        System.out.println("indexOf in range : " + "hello world".indexOf('o', 5, 11)); // 7

        // StringBuilder.repeat(): append a char/CharSequence N times.
        StringBuilder sb = new StringBuilder();
        sb.repeat("=-", 5).repeat('*', 3);
        System.out.println("StringBuilder.repeat: " + sb);

        // Math.clamp(value, min, max): keep a value inside a range.
        System.out.println("Math.clamp       : " + Math.clamp(150, 0, 100)); // 100
        System.out.println("Math.clamp       : " + Math.clamp(-5, 0, 100));  // 0

        // Character emoji helpers.
        int smiley = 0x1F600;
        System.out.println("isEmoji          : " + Character.isEmoji(smiley));
    }

    // ------------------------------------------------------------------
    // Runs every Java 21 demo
    // ------------------------------------------------------------------
    public static void runAll() throws Exception {
        System.out.println("\n===== JAVA 21 FEATURES =====");

        System.out.println("-- Pattern matching for switch (sealed, exhaustive) --");
        for (Shape s : List.of(new Shape.Circle(1), new Shape.Rectangle(2, 3), new Shape.Triangle(4, 5))) {
            System.out.printf("%s -> area = %.2f%n", s, area(s));
        }

        System.out.println("-- Pattern matching for switch (guards + null) --");
        Object[] samples = {42, 500, "hi", " ", new Shape.Circle(3), List.of(1, 2), null, 3.14};
        for (Object o : samples) {
            System.out.println(describe(o));
        }

        System.out.println("-- Record patterns --");
        System.out.println(recordPatternDemo(new Line(new Point(0, 0), new Point(3, 4))));
        System.out.println(recordPatternDemo(new Point(0, 0)));
        System.out.println(recordPatternDemo(new Point(2, 7)));
        System.out.println(recordPatternDemo("text"));

        System.out.println("-- Sequenced collections --");
        sequencedCollectionsDemo();

        System.out.println("-- Virtual threads --");
        virtualThreadsDemo();

        System.out.println("-- Small API additions --");
        smallApiAdditionsDemo();
    }
}
