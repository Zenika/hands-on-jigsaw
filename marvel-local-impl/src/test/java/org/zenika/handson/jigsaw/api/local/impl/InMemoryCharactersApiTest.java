package org.zenika.handson.jigsaw.api.local.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.zenika.handson.jigsaw.api.CharactersApi;
import org.zenika.handson.jigsaw.api.MarvelCharacter;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.JRE.*;


public class InMemoryCharactersApiTest {
    private CharactersApi api;

    @BeforeEach
    public void setUp() {
        api = new InMemoryCharactersApi();
    }

    @Test
    @DisplayName("Should find Capitain American by id (1009220) and he should have a thumbnail")
    public void shouldFindCaptainAmericaById() {
        Optional<MarvelCharacter> captainAmerica = api.find(1009220);
        assertAll("Find Captain America by id",
                () -> assertNotNull(captainAmerica, "\"I should find a character with the id 1009220\""),
                () -> assertTrue(captainAmerica.isPresent(), "I should find a character with the id 1009220"),
                () -> assertEquals(captainAmerica.get().name, "Captain America", "It should be Captain America"),
                () -> assertTrue(new File(captainAmerica.get().getImage(MarvelCharacter.ImageType.STANDARD_XLARGE).getFile()).exists()
                        , "It should have a thumbnail"),
                () -> assertTrue(captainAmerica.get().getImage(MarvelCharacter.ImageType.STANDARD_XLARGE)
                        .getFile().endsWith("/img/537ba56d31087_standard_xlarge.jpg"), "This thumbnail should be 537ba56d31087_standard_xlarge.jpg")
        );

    }

    @Test
    @DisplayName("Should find Captain America (again) searching the name containing \"captain a\"")
    public void shouldFindCaptainAmericaByNameContaining() {
        List<MarvelCharacter> characters = api.findByNameContaining("captain a");
        assertAll("Find Captain America by name containing",
                () -> assertNotNull(characters, "I should find characters with the name containing \"captain a\""),
                () -> assertEquals(characters.size(), 3, "I should find 3 characters with the name containing \"captain a\""),
                () -> assertEquals(characters.get(0).id, 1009220, "It should have the id 1009220")
        );
    }

    @Test
    @DisplayName("Should have 229 characters")
    public void shouldFindAllCharacters() {
        List<MarvelCharacter> characters = api.findAll();
        assertEquals(characters.size(), 229, "I should find 229 characters");

    }

    @Test
    @DisplayName("Shoud fuzzy search \"taincapi\" with scoring >= 50 and find 8 characters, but Captain America is the first")
    public void shouldFuzzySearchByName() {
        List<MarvelCharacter> characters = api.fuzzySearchByName("taincapi", 50);
        assertAll("Find Captain America with fuzzy search",
                () -> assertNotNull(characters, "I should find some characher with name is about \"taincapi\""),
                () -> assertEquals(8, characters.size(), "I should X find some characher with name is about \"taincapi\""),
                () -> assertEquals(1009220, characters.get(0).id, "The first characters should be Captain America (id 1009220)"),
                () -> assertEquals(1010669, characters.get(7).id, "The 8 characters should be Titania (id 1010669)")
        );
    }

    @Test
    @EnabledOnJre({JAVA_9, JAVA_10})
    @EnabledIfEnvironmentVariable(named = "MARVEL", matches = "JIGSAW")
    // Enable only with real module cmdline (not Idea which using classpath on test)
    @DisplayName("Dynamic invoke on object that is not open to this module should throw an IllegalAccessException")
    public void shouldFailToFindClassWithJigsaw() {
        assertThrows(IllegalAccessException.class, () -> {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            lookup.findClass("java.sql.Timestamp");
        });

    }

    @Test
    @DisabledIfEnvironmentVariable(named = "MARVEL", matches = "JIGSAW")
    @DisplayName("With classpath you could dynamic invoke what you want")
    public void shouldFindClassWithClasspath() throws IllegalAccessException, ClassNotFoundException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        assertNotNull(lookup.findClass("java.sql.Timestamp"));

    }


}
