package com.example.javafeatures;

/**
 * SEALED INTERFACE + RECORDS (sealed: Java 17 final, records: Java 16 final)
 *
 * - "sealed" restricts which classes may implement/extend this type, using "permits".
 *   The compiler therefore knows the COMPLETE set of subtypes.
 * - Every permitted subtype must be final, sealed or non-sealed.
 *   Records are implicitly final, so they fit naturally.
 * - Records are immutable data carriers: the compiler generates the constructor,
 *   accessors (radius(), width()...), equals(), hashCode() and toString().
 * - In Java 21 the compiler uses this "closed hierarchy" to check that a
 *   pattern-matching switch is exhaustive without needing a default branch.
 */
public sealed interface Shape permits Shape.Circle, Shape.Rectangle, Shape.Triangle {

    /** Compact canonical constructor: validates input, fields are assigned automatically. */
    record Circle(double radius) implements Shape {
        public Circle {
            if (radius <= 0) {
                throw new IllegalArgumentException("radius must be positive: " + radius);
            }
        }
    }

    record Rectangle(double width, double height) implements Shape { }

    record Triangle(double base, double height) implements Shape { }
}
