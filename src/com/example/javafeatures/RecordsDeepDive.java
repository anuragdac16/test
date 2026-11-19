package com.example.javafeatures;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RECORDS IN DEPTH (JEP 395 - final in Java 16, so available in 17 and 21).
 *
 * A record is a transparent, shallowly-immutable DATA CARRIER. For
 *     record Person(String name, int age) { }
 * the compiler generates:
 *   - private final fields           : name, age
 *   - a canonical constructor        : Person(String name, int age)
 *   - accessors (NOT getName())      : name(), age()
 *   - equals() / hashCode()          : based on ALL components
 *   - toString()                     : Person[name=Asha, age=30]
 *
 * RULES / RESTRICTIONS
 *   - Implicitly final: a record cannot be extended and cannot extend a class
 *     (it implicitly extends java.lang.Record). It CAN implement interfaces.
 *   - No extra INSTANCE fields allowed; all state lives in the header components.
 *   - Can have: static fields, static methods, instance methods, extra constructors,
 *     nested types, generics, annotations.
 *   - Records can be declared top-level, nested (implicitly static) or LOCAL (inside a method).
 *   - Shallow immutability: fields are final, but a mutable component
 *     (List, array, Date...) can still be mutated -> use defensive copies (see Team).
 */
public class RecordsDeepDive {

    // ------------------------------------------------------------------
    // 1. BASIC RECORD
    // ------------------------------------------------------------------
    record Person(String name, int age) { }

    // ------------------------------------------------------------------
    // 2. COMPACT CANONICAL CONSTRUCTOR: validate and/or normalize
    // ------------------------------------------------------------------
    /**
     * A compact constructor has NO parameter list. Parameters are implicit and
     * you may reassign them (normalization); the fields are assigned automatically
     * AFTER the body finishes. Do NOT write "this.value = value" inside it.
     */
    record Email(String value) {
        Email {
            Objects.requireNonNull(value, "email must not be null");
            value = value.trim().toLowerCase();               // normalization
            if (!value.contains("@")) {                        // validation
                throw new IllegalArgumentException("Invalid email: " + value);
            }
        }
    }

    // ------------------------------------------------------------------
    // 3. EXTRA MEMBERS: static field/factory, custom constructor, instance method,
    //    overridden toString()
    // ------------------------------------------------------------------
    record Temperature(double celsius) {
        static final double ABSOLUTE_ZERO = -273.15;           // static fields are allowed

        Temperature {
            if (celsius < ABSOLUTE_ZERO) {
                throw new IllegalArgumentException("Below absolute zero: " + celsius);
            }
        }

        // Non-canonical constructor MUST delegate to another constructor via this(...).
        Temperature() {
            this(20.0);
        }

        // Static factory method: a named, readable alternative to 'new'.
        static Temperature ofFahrenheit(double f) {
            return new Temperature((f - 32) * 5 / 9);
        }

        // Derived value computed from components (a record may not add instance FIELDS,
        // but it may add instance METHODS).
        double fahrenheit() {
            return celsius * 9 / 5 + 32;
        }

        // Any generated method can be overridden.
        @Override
        public String toString() {
            return "%.1f C".formatted(celsius);
        }
    }

    // ------------------------------------------------------------------
    // 4. RECORDS CAN IMPLEMENT INTERFACES
    // ------------------------------------------------------------------
    interface Describable {
        String describe();
    }

    record Book(String title, String author, int year) implements Describable, Comparable<Book> {
        @Override
        public String describe() {
            return "'%s' by %s (%d)".formatted(title, author, year);
        }

        @Override
        public int compareTo(Book other) {
            return Integer.compare(year, other.year);
        }
    }

    // ------------------------------------------------------------------
    // 5. GENERIC RECORD
    // ------------------------------------------------------------------
    record Pair<A, B>(A first, B second) {
        // "Wither"-style method: records are immutable, so "changing" returns a NEW record.
        <C> Pair<A, C> withSecond(C newSecond) {
            return new Pair<>(first, newSecond);
        }
    }

    // ------------------------------------------------------------------
    // 6. SHALLOW IMMUTABILITY + DEFENSIVE COPY
    // ------------------------------------------------------------------
    /**
     * Without List.copyOf(...) the caller could keep the original list and mutate it,
     * silently changing the record. List.copyOf returns an unmodifiable copy.
     */
    record Team(String name, List<String> members) {
        Team {
            members = List.copyOf(members);
        }
    }

    // PITFALL: arrays use reference equality, so two records holding equal arrays are NOT equal.
    record ArrayHolder(int[] data) { }

    // ------------------------------------------------------------------
    // 7. COMPOSITION: nested records
    // ------------------------------------------------------------------
    record Address(String city, String country) { }
    record Customer(String name, Address address) { }

    // ------------------------------------------------------------------
    // DEMOS
    // ------------------------------------------------------------------
    static void basicsDemo() {
        Person p1 = new Person("Asha", 30);
        Person p2 = new Person("Asha", 30);
        System.out.println("toString   : " + p1);
        System.out.println("accessors  : " + p1.name() + ", " + p1.age()); // name(), not getName()
        System.out.println("equals     : " + p1.equals(p2) + " (same values)");
        System.out.println("== operator: " + (p1 == p2) + " (different objects)");
        System.out.println("hashCode ok: " + (p1.hashCode() == p2.hashCode()));
    }

    static void compactConstructorDemo() {
        System.out.println("normalized : " + new Email("  Asha@Example.COM "));
        try {
            new Email("not-an-email");
        } catch (IllegalArgumentException e) {
            System.out.println("validation : " + e.getMessage());
        }
    }

    static void extraMembersDemo() {
        Temperature t = Temperature.ofFahrenheit(98.6);
        System.out.println("factory    : " + t + " -> " + "%.1f F".formatted(t.fahrenheit()));
        System.out.println("no-arg ctor: " + new Temperature());
        System.out.println("static const: " + Temperature.ABSOLUTE_ZERO);
    }

    static void interfaceAndGenericsDemo() {
        List<Book> books = List.of(
                new Book("Clean Code", "Robert Martin", 2008),
                new Book("Effective Java", "Joshua Bloch", 2001),
                new Book("Java Concurrency in Practice", "Brian Goetz", 2006));

        System.out.println("describe() : " + books.get(0).describe());
        System.out.println("sorted     : " + books.stream().sorted().map(Book::title).toList());

        Pair<String, Integer> pair = new Pair<>("age", 30);
        Pair<String, String> changed = pair.withSecond("thirty");
        System.out.println("generic    : " + pair + " -> " + changed);
    }

    static void immutabilityDemo() {
        java.util.ArrayList<String> source = new java.util.ArrayList<>(List.of("Asha", "Ravi"));
        Team team = new Team("Core", source);
        source.add("Hacker");                                   // does NOT affect the record
        System.out.println("defensive copy: " + team.members());

        try {
            team.members().add("X");                            // unmodifiable
        } catch (UnsupportedOperationException e) {
            System.out.println("members list is unmodifiable");
        }

        ArrayHolder a = new ArrayHolder(new int[]{1, 2});
        ArrayHolder b = new ArrayHolder(new int[]{1, 2});
        System.out.println("array pitfall equals: " + a.equals(b)
                + " (arrays compared by reference; use List or override equals)");
    }

    static void collectionsAndStreamsDemo() {
        // Because equals/hashCode are value-based, records are ideal Set elements / Map keys.
        Set<Person> set = new HashSet<>(List.of(new Person("A", 1), new Person("A", 1), new Person("B", 2)));
        System.out.println("set size   : " + set.size() + " (duplicate removed)");

        List<Person> people = List.of(
                new Person("Asha", 30), new Person("Ravi", 25),
                new Person("Meena", 30), new Person("Kiran", 25));

        Map<Integer, List<String>> byAge = people.stream()
                .collect(Collectors.groupingBy(Person::age,
                        java.util.TreeMap::new,
                        Collectors.mapping(Person::name, Collectors.toList())));
        System.out.println("groupingBy : " + byAge);

        System.out.println("sorted     : " + people.stream()
                .sorted(Comparator.comparingInt(Person::age).thenComparing(Person::name))
                .toList());
    }

    static void compositionDemo() {
        Customer c = new Customer("Asha", new Address("Hyderabad", "India"));
        System.out.println("nested     : " + c);
        System.out.println("navigate   : " + c.address().city());

        // JAVA 21: record patterns deconstruct nested records directly (see Java21Features).
        if (c instanceof Customer(String name, Address(String city, String country))) {
            System.out.println("deconstruct: " + name + " lives in " + city + ", " + country);
        }
    }

    static void reflectionDemo() {
        System.out.println("isRecord   : " + Person.class.isRecord());
        for (RecordComponent rc : Person.class.getRecordComponents()) {
            System.out.println("component  : " + rc.getName() + " : " + rc.getType().getSimpleName());
        }
        System.out.println("superclass : " + Person.class.getSuperclass().getName());
    }

    public static void runAll() {
        System.out.println("\n===== RECORDS DEEP DIVE =====");
        System.out.println("-- 1. Basics (generated members) --");
        basicsDemo();
        System.out.println("-- 2. Compact constructor (validate + normalize) --");
        compactConstructorDemo();
        System.out.println("-- 3. Extra members (static factory, custom ctor, methods) --");
        extraMembersDemo();
        System.out.println("-- 4/5. Interfaces and generics --");
        interfaceAndGenericsDemo();
        System.out.println("-- 6. Shallow immutability --");
        immutabilityDemo();
        System.out.println("-- 7. Collections and streams --");
        collectionsAndStreamsDemo();
        System.out.println("-- 8. Composition + record patterns (Java 21) --");
        compositionDemo();
        System.out.println("-- 9. Reflection --");
        reflectionDemo();
    }
}
