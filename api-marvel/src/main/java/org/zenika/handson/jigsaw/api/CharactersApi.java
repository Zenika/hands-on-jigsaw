package org.zenika.handson.jigsaw.api;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface CharactersApi {

    /**
     * Serialize MarvelCharacter to Json
     *
     * @param character the character to serialize
     * @return json representation
     */
    static String marvelCharacterToJson(MarvelCharacter character) {
        StringBuilder sb = new StringBuilder("{\n")
                .append("  \"id\": ").append(character.id).append(",\n")
                .append("  \"name\": \"").append(character.name).append("\",\n")
                .append("  \"description\": \"").append(character.description).append("\"\n")
                .append("}");

        return sb.toString();
    }

    /**
     * Serialize a List of MarvelCharacter to Json Array of MarvelCharacter
     *
     * @param characters list of characters to serialize
     * @return the Json array of MarvelCharacter
     */
    static String marvelCharactersToJson(List<MarvelCharacter> characters) {
        StringBuilder sb = new StringBuilder(2048)
                .append("{ \"characters\": [\n")
                .append(characters.stream().map(CharactersApi::marvelCharacterToJson)
                        .collect(Collectors.joining(",\n")))
                .append("] }\n");

        return sb.toString();
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
    Optional<MarvelCharacter> find(int id);

    /**
     * Find all the Characters
     * Data provided by Marvel. © 2014 Marvel
     * https://developer.marvel.com
     *
     * @return All the characters
     */
    List<MarvelCharacter> findAll();

    /**
     * Find all the Characters containing name ignoring case
     *
     * @param name the name (or part of the name) for searching
     * @return All the characters that contains the name
     */
    List<MarvelCharacter> findByNameContaining(String name);

    /**
     * Fuzzy search Characters by name. The result is orderer by the score
     *
     * @param name         the name (or part of the name) for searching
     * @param minScore the minimumScore (between 0 and 100, 100 is strictly equals)
     * @return The characters that matching name with score &ge; minScore
     */
    List<MarvelCharacter> fuzzySearchByName(String name, int minScore);



}
