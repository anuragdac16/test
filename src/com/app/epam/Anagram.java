package com.app.epam;

import java.util.*;
import java.util.stream.Collectors;

public class Anagram {
    public static void main(String[] args) {
        String []words = {"act","tac", "god", "dog", "cat"};
        Map<String, List<String>> map = new HashMap<>();
        Arrays.stream(words).forEach(word ->{
            String sortedWord = word.chars().mapToObj(c -> (char)c).sorted(Comparator.naturalOrder())
                    .map(String::valueOf).collect(Collectors.joining());
            List<String> value = new ArrayList<>();
            if(map.containsKey(sortedWord)){
                value = map.get(sortedWord);
            }
            value.add(word);
            map.put(sortedWord, value);
        });


        Arrays.stream(words).forEach(word -> {
            String sortedValue = word.chars().mapToObj(c -> (char) c)
                    .sorted(Comparator.naturalOrder())
                    .map(String::valueOf)
                    .collect(Collectors.joining());
            List<String> values = new ArrayList<>();
            if(map.containsKey(sortedValue)){
                values = map.get(sortedValue);
            }
            values.add(word);
            map.put(sortedValue, values);

        });
        map.forEach((key, value) -> System.out.println(key + "---" + value));

    }
}
