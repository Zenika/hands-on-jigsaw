package org.zenika.handson.jigsaw.api.local.impl;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import org.zenika.handson.jigsaw.api.CharactersApi;
import org.zenika.handson.jigsaw.api.MarvelCharacter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class InMemoryCharactersApi implements CharactersApi {

    private static final Logger logger = Logger.getLogger(InMemoryCharactersApi.class.getCanonicalName());
    private static final Function<String, MarvelCharacter> lineToCharacter = (line) -> {
        final String[] columns = line.split("~");

        return new InMarvelCharacter(Integer.valueOf(columns[0]), columns[1], columns[2], columns[3], columns[4]);
    };
    private static final Map<Integer, MarvelCharacter> charactersAsMap = loadDataFromFile();
    private static final List<MarvelCharacter> charactersAsList =
            Collections.unmodifiableList(
                    charactersAsMap.values().stream().sorted(Comparator.comparing(c -> c.name)).collect(Collectors.toList())
            );
    private static final List<String> charactersNameAsList =
            Collections.unmodifiableList(
                    charactersAsList.stream().map(c -> c.name).collect(Collectors.toList())
            );


    // Data provided by Marvel. © 2014 Marvel
    // Marvel Developper Portal https://developer.marvel.com
    // curl & jq & bash have super power too
    private static Map<Integer, MarvelCharacter> loadDataFromFile() {

        try (InputStream is = InMemoryCharactersApi.class.getModule().getResourceAsStream("data/all.txt");
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().map(lineToCharacter).collect(Collectors.toMap(c -> c.id, c -> c));
        } catch (IOException e) {
            e.printStackTrace();
            throw new DoctorStrangeException("Strange, the file have disappeared in your hat", e); // should not arrive and must crash
        }


    }

    /**
     * Find the Character by is id
     * <p>
     * Data provided by Marvel. © 2014 Marvel
     * https://developer.marvel.com
     *
     * @param id of the character
     * @return the character if the id exist
     */
    @Override
    public Optional<MarvelCharacter> find(int id) {
        return Optional.ofNullable(charactersAsMap.get(id));
    }

    /**
     * Find all the Characters
     * Data provided by Marvel. © 2014 Marvel
     * https://developer.marvel.com
     *
     * @return All the characters
     */
    @Override
    public List<MarvelCharacter> findAll() {
        return charactersAsList;
    }

    /**
     * Find all the Characters containing name ignoring case
     *
     * @param name the name (or part of the name) for searching
     * @return All the characters that contains the name
     */
    @Override
    public List<MarvelCharacter> findByNameContaining(String name) {
        if (null == name || name.trim().isEmpty()) {
            return findAll();
        }
        final String lowerName = name.trim().toLowerCase();
        return charactersAsList.stream().filter(c -> c.name.toLowerCase().contains(lowerName)).collect(Collectors.toList());
    }


    @Override
    public List<MarvelCharacter> fuzzySearchByName(String name, int minScore) {
        if ((null == name || name.trim().isEmpty()) || minScore < 0) {
            return findAll();
        }

        final List<ExtractedResult> results = FuzzySearch.extractSorted(name, charactersNameAsList, minScore);
        return results.stream().map(e -> charactersAsList.get(e.getIndex())).collect(Collectors.toList());


    }


    @SuppressWarnings("WeakerAccess")
    private static class InMarvelCharacter extends MarvelCharacter {

        public InMarvelCharacter(int id, String name, String description, String imageId, String extension) {
            super(id, name, description, imageId, extension);
        }

        @Override
        public URL getImage(ImageType type) {

            switch (type) {
                case LARGE:
//                    return this.getClass().getClassLoader().getResource("img/".concat(imageId).concat(".").concat(extension));
                    return img.PlaceHolder.class.getModule().getClassLoader().getResource("img/".concat(imageId).concat(".").concat(extension));
                case PORTRAIT_XLARGE:
//                    return this.getClass().getClassLoader().getResource("img/".concat(imageId).concat("_portrait_xlarge.").concat(extension));
                    return img.PlaceHolder.class.getModule().getClassLoader().getResource("img/".concat(imageId).concat("_portrait_xlarge.").concat(extension));
                case STANDARD_XLARGE:
//                    return this.getClass().getClassLoader().getResource("img/".concat(imageId).concat("_standard_xlarge.").concat(extension));
                    return img.PlaceHolder.class.getModule().getClassLoader().getResource("img/".concat(imageId).concat("_standard_xlarge.").concat(extension));
                default:
                    // should not arrive
                    throw new DoctorStrangeException("Strange, this type of image is not supported");
            }

        }


    }

    class CharacterScore implements Comparable<CharacterScore> {
        final int score;
        final MarvelCharacter character;

        CharacterScore(int score, MarvelCharacter character) {
            this.score = score;
            this.character = character;
        }

        @Override
        public int compareTo(CharacterScore o) {
            return Integer.compare(this.score, o.score);
        }
    }

}

class DoctorStrangeException extends RuntimeException {
    public DoctorStrangeException(String message) {
        super(message);
    }

    public DoctorStrangeException(String message, Throwable exception) {
        super(message, exception);
    }
}
