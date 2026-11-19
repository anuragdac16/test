package com.example.javafeatures;

/**
 * Entry point: runs the Java 17 demo and then the Java 21 demo.
 *
 * Project layout (package = folder structure):
 *   com/example/javafeatures/Shape.java
 *   com/example/javafeatures/Java17Features.java
 *   com/example/javafeatures/Java21Features.java
 *   com/example/javafeatures/Main.java
 *
 * Build & run (JDK 21 required, since Java21Features uses 21 APIs):
 *   javac -d out com/example/javafeatures/*.java
 *   java -cp out com.example.javafeatures.Main
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Java17Features.runAll();
        Java21Features.runAll();
        RecordsDeepDive.runAll();
    }
}
