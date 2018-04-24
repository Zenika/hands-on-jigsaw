package org.zenika.handson.jigsaw.http;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import feign.Feign;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.gson.GsonDecoder;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    private static InfoAPI infoApi;
    private static CharactersSearchAPI charactersSearchAPI;
    private static Application application;
    private static ImageAPI imageAPI;

    @BeforeAll
    static void setUp() {
        application = new Application(3030, false);
        Runnable task = () -> {
            Vertx vertx = Vertx.vertx();

            vertx.deployVerticle(application);
        };
        new Thread(task).start();
        infoApi = Feign.builder()
                .decoder(new GsonDecoder())
                .target(InfoAPI.class, "http://localhost:3030");
        charactersSearchAPI = Feign.builder()
                .decoder(new GsonDecoder())
                .target(CharactersSearchAPI.class, "http://localhost:3030");
        imageAPI = Feign.builder()
                .decoder(new GsonDecoder())
                .target(ImageAPI.class, "http://localhost:3030");


//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }

    @AfterAll
    static void tearDown() throws Exception {
//        application.getVertx().close();
        application.stop();
    }

    @Test
    @DisplayName("The Information API should provide the Java Version and the Vendor (I'm an Oracle if I said to you who is the vendor)")
    void infoJavaVersionAndVendorShoudExist() {
        Info info = infoApi.getInfo();
        assertNotNull(info.javaVersion);
        assertNotNull(info.javaVendor);
    }

    @Test
    @EnabledOnJre({JRE.JAVA_9, JRE.JAVA_10})
    @DisplayName("With JIGSAW the Java Specification is Greater or equals than 9 & there are some modules in the Layer")
    void infoModulesShouldBeNotEmpty() {
        Info info = infoApi.getInfo();

        assertTrue(Integer.valueOf(info.javaSpecificationVersion) >= 9);
        assertNotNull(info.modules);
        assertTrue(!info.modules.isEmpty());

    }

    @Test
    @EnabledOnJre({JRE.JAVA_9, JRE.JAVA_10})
    @EnabledIfEnvironmentVariable(named = "HTTP", matches = "JIGSAW")
    // Enable only with real module cmdline (not Idea which using classpath on test)
    @DisplayName("With JIGSAW this module should be \"org.zenika.handson.jigsaw.http\"")
    void infoModuleNameShouldBeOrgZenikaHandsonJigsawHttp() {
        Info info = infoApi.getInfo();
        assertEquals("org.zenika.handson.jigsaw.http", info.moduleName);
    }

    @Test
    @DisplayName("Fuzzy search with \"taincapi\" with scoring >= 50 should find 8 characteres and Captain America First ;)")
    void fuzzySearch() {
        //search=taincapi&score=50
        List<Character> characters = charactersSearchAPI.fuzzySearch("taincapi", 50).characters;
        assertNotNull(characters);
        assertEquals(8, characters.size());
        assertEquals(1009220, characters.get(0).id.intValue(), "This must me Captain America");
        assertEquals(1010669, characters.get(7).id.intValue(), "This must be Titania");
    }

    @Test
    @DisplayName("Captain America should have a Thumb")
    void downloadImage() {
        Response response = imageAPI.getImage(1009220);
        assertEquals("image/jpg", response.headers().get("Content-type").iterator().next());
        assertEquals("10275", response.headers().get("Content-length").iterator().next());
        assertEquals(10275, response.body().length().intValue());


    }

    interface InfoAPI {
        @RequestLine("GET /api/info")
        Info getInfo();
    }

    interface CharactersSearchAPI {
        @RequestLine("GET /api/characters?search={search}&score={score}")
        Characters fuzzySearch(@Param("search") String search, @Param("score") int score);
    }

    interface ImageAPI {
        @RequestLine("GET /api/images/{id}")
        Response getImage(@Param("id") int id);
    }

    static class Info {
        String javaSpecificationVersion;
        String javaVersion;
        String javaVendor;
        String moduleName;
        List<String> modules;

    }

    static class Characters {
        @SerializedName("characters")
        @Expose
        List<Character> characters = null;

    }

    static class Character {

        @SerializedName("id")
        @Expose
        public Integer id;
        @SerializedName("name")
        @Expose
        public String name;
        @SerializedName("description")
        @Expose
        public String description;

    }


}