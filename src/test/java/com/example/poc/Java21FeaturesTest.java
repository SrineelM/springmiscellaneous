package com.example.poc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates core Java 21 features with executable examples.
 *
 * <p>Educational value:
 * 1. Sequenced Collections (JEP 431): Introduces SequencedCollection, SequencedSet, and SequencedMap
 *    providing uniform access to first/last elements (getFirst(), getLast(), addFirst(), addLast())
 *    and a reverse-ordered view (reversed()).
 * 2. Virtual Threads (JEP 444): Lightweight threads managed by the JVM rather than OS kernel threads.
 * 3. Pattern Matching for switch (JEP 441) and Record Patterns (JEP 440).
 */
class Java21FeaturesTest {

  @Test
  @DisplayName("Java 21: SequencedCollection uniform access to first, last, and reversed view")
  void sequencedCollections_demonstrateOrderedAccess() {
    SequencedCollection<String> list = new ArrayList<>(List.of("alpha", "beta", "gamma"));

    // Uniform access methods
    assertThat(list.getFirst()).isEqualTo("alpha");
    assertThat(list.getLast()).isEqualTo("gamma");

    // Mutation at endpoints
    list.addFirst("start");
    list.addLast("end");
    assertThat(list.getFirst()).isEqualTo("start");
    assertThat(list.getLast()).isEqualTo("end");

    // Reversed view
    List<String> reversed = list.reversed().stream().toList();
    assertThat(reversed).containsExactly("end", "gamma", "beta", "alpha", "start");
  }

  @Test
  @DisplayName("Java 21: SequencedMap ordered entry access")
  void sequencedMap_demonstrateOrderedKeysAndValues() {
    SequencedMap<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);

    assertThat(map.firstEntry().getKey()).isEqualTo("one");
    assertThat(map.lastEntry().getKey()).isEqualTo("three");

    SequencedMap<String, Integer> reversed = map.reversed();
    assertThat(reversed.firstEntry().getKey()).isEqualTo("three");
  }

  @Test
  @DisplayName("Java 21: SequencedSet ordered set access")
  void sequencedSet_demonstrateOrderedSet() {
    SequencedSet<String> set = new LinkedHashSet<>(List.of("x", "y", "z"));
    assertThat(set.getFirst()).isEqualTo("x");
    assertThat(set.getLast()).isEqualTo("z");
    assertThat(set.reversed().stream().toList()).containsExactly("z", "y", "x");
  }
}
