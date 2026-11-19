package com.example.javafeatures;

import com.example.javafeatures.Shape;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;

/**
 * Demonstrates features that are FINAL (standard) in Java 17 (LTS).
 * Some were finalized in earlier releases (noted below) but are all usable in 17.
 *
 * Compile/run with JDK 17+:  javac -d out *.java && java -cp out com.example.javafeatures.Main
 */
public class Java17Features {

    // ------------------------------------------------------------------
    // 1. SEALED CLASSES (JEP 409, final in 17) + PATTERN MATCHING FOR instanceof
    //    (JEP 394, final in 16)
    // ------------------------------------------------------------------
    /**
     * "obj instanceof Circle c" tests the type AND binds the variable 'c' in one step,
     * removing the old explicit cast: Circle c = (Circle) shape;
     * NOTE: In Java 17, pattern matching for *switch* is only a preview feature,
     * so we use an if/else chain here. Java 21 makes the switch version final.
     */
    public static double area(Shape shape) {
        if (shape instanceof Shape.Circle c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof Shape.Rectangle r) {
            return r.width() * r.height();
        } else if (shape instanceof Shape.Triangle t) {
            return 0.5 * t.base() * t.height();
        }
        throw new IllegalStateException("Unknown shape: " + shape);
    }

    // ------------------------------------------------------------------
    // 2. RECORDS (JEP 395, final in 16) - including LOCAL records
    // ------------------------------------------------------------------
    public static void recordsDemo() {
        // A record declared inside a method: handy for grouping temporary data.
        record Employee(String name, int salary) { }

        List<Employee> staff = List.of(
                new Employee("Asha", 90_000),
                new Employee("Ravi", 75_000),
                new Employee("Meena", 110_000));

        // toString(), equals(), hashCode() are auto-generated.
        System.out.println("Record toString : " + staff.get(0));
        System.out.println("Record equals   : " + staff.get(0).equals(new Employee("Asha", 90_000)));

        // Stream.toList() (Java 16) returns an UNMODIFIABLE list - shorter than collect(Collectors.toList()).
        List<String> highEarners = staff.stream()
                .filter(e -> e.salary() > 80_000)
                .map(Employee::name)
                .toList();
        System.out.println("High earners    : " + highEarners);
    }

    // ------------------------------------------------------------------
    // 3. SWITCH EXPRESSIONS (JEP 361, final in 14)
    // ------------------------------------------------------------------
    /**
     * - The switch returns a value; arrows (->) mean NO fall-through, so no 'break'.
     * - Multiple labels can share a case: case "SAT", "SUN".
     * - 'yield' returns a value from a block branch.
     * - Over an enum/sealed type the compiler enforces exhaustiveness.
     */
    public static String dayType(String day) {
        return switch (day.toUpperCase()) {
            case "SAT", "SUN" -> "Weekend";
            case "MON", "TUE", "WED", "THU", "FRI" -> "Weekday";
            default -> {
                String msg = "Invalid day: " + day;
                yield msg; // 'yield' produces the value of this block
            }
        };
    }

    // ------------------------------------------------------------------
    // 4. TEXT BLOCKS (JEP 378, final in 15)
    // ------------------------------------------------------------------
    /**
     * Triple-quote strings span multiple lines, need no escaping of quotes or \n,
     * and incidental leading indentation is removed automatically.
     */
    public static String jsonTextBlock() {
        return """
                {
                  "name": "Java",
                  "version": 17,
                  "lts": true
                }
                """;
    }

    // ------------------------------------------------------------------
    // 5. NEW STRING HELPERS (11-15): formatted(), strip(), isBlank(), repeat(), transform()
    // ------------------------------------------------------------------
    public static void stringHelpersDemo() {
        // formatted() == String.format(...) but called on the template itself.
        System.out.println("formatted()  : " + "Hello, %s! You are %d.".formatted("Asha", 30));
        System.out.println("isBlank()    : " + "   ".isBlank());
        System.out.println("strip()      : [" + "  padded  ".strip() + "]");
        System.out.println("repeat()     : " + "ab".repeat(3));
        // transform() applies a function to the string - good for chaining.
        System.out.println("transform()  : " + "java".transform(String::toUpperCase));
    }

    // ------------------------------------------------------------------
    // 6. HELPFUL NullPointerException MESSAGES (JEP 358, on by default since 15)
    // ------------------------------------------------------------------
    /**
     * The JVM now says exactly WHAT was null, e.g.:
     *   Cannot invoke "String.length()" because "<local1>" is null
     * (local variable names appear only if compiled with -g; otherwise "<local1>").
     */
    public static void helpfulNpeDemo() {
        try {
            String text = null;
            System.out.println(text.length());
        } catch (NullPointerException e) {
            System.out.println("Helpful NPE  : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 7. ENHANCED PSEUDO-RANDOM NUMBER GENERATORS (JEP 356, new in 17)
    // ------------------------------------------------------------------
    /**
     * New java.util.random package: a common RandomGenerator interface and
     * pluggable algorithms (e.g. "L64X128MixRandom"). Same seed => same sequence.
     */
    public static void randomGeneratorDemo() {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
        System.out.println("RandomGenerator (seed 42): "
                + rng.nextInt(100) + ", " + rng.nextInt(100) + ", " + rng.nextInt(100));

        // Default/legacy-friendly way to obtain a generator:
        RandomGenerator any = RandomGenerator.getDefault();
        System.out.println("Default generator next int < 10: " + any.nextInt(10));
    }

    // ------------------------------------------------------------------
    // Runs every Java 17 demo
    // ------------------------------------------------------------------
    public static void runAll() {
        System.out.println("===== JAVA 17 FEATURES =====");

        System.out.println("-- Sealed + instanceof pattern --");
        List<Shape> shapes = List.of(
                new Shape.Circle(2),
                new Shape.Rectangle(3, 4),
                new Shape.Triangle(5, 6));
        for (Shape s : shapes) {
            System.out.printf("%s -> area = %.2f%n", s, area(s));
        }

        System.out.println("-- Records --");
        recordsDemo();

        System.out.println("-- Switch expression --");
        Stream.of("sat", "Mon", "xyz").forEach(d -> System.out.println(d + " -> " + dayType(d)));

        System.out.println("-- Text block --");
        System.out.print(jsonTextBlock());

        System.out.println("-- String helpers --");
        stringHelpersDemo();

        System.out.println("-- Helpful NPE --");
        helpfulNpeDemo();

        System.out.println("-- Random generators --");
        randomGeneratorDemo();
    }
}
