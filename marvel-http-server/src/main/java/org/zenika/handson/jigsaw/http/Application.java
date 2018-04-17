package org.zenika.handson.jigsaw.http;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.zenika.handson.jigsaw.api.CharactersApi;
import org.zenika.handson.jigsaw.api.MarvelCharacter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class Application extends AbstractVerticle {

    private static final String URI_CHARACTERS = "/api/characters";
    private static final String URI_INFO = "/api/info";
    private static final String URI_CHARACTERS_IMAGES = "/api/images";
    private static final String SEARCH_QUERY = "search";
    private static final String SEARCH_SCORE = "score";

    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    private static final Logger logger = Logger.getLogger(Application.class.getCanonicalName());
    @SuppressWarnings("ConstantConditions")
    private static final CharactersApi charactersApi = ServiceLoader.load(CharactersApi.class).findFirst().get();
    private static String info = null;
    private final int port;
    private final boolean dev;

    /**
     * Initialise the server.
     *
     * @param port http port
     * @param dev  true for dev (no cache), false for production
     */
    public Application(int port, boolean dev) {
        this.port = port;
        this.dev = dev;
    }

    public static void main(String[] args) {
        int port = Integer.valueOf(System.getProperty("port", "8080"));
        boolean dev = Boolean.valueOf(System.getProperty("dev", "true"));
        logger.info("Starting Server :" + port + ", mode :" + (dev ? "development" : "production"));
        Vertx vertx = Vertx.vertx();

        final int processors = Runtime.getRuntime().availableProcessors();
        final int nbVerticles = dev && processors > 2 ?  processors/2 : processors;
        for (int i = 0; i < nbVerticles; i++) {
            vertx.deployVerticle(new Application(port, dev));
            logger.info("Starting verticle " + (i + 1));
        }

    }

    private static JsonObject marvelCharacterToJson(MarvelCharacter character) {
        return new JsonObject()
                .put("id", character.id)
                .put("name", character.name)
                .put("description", character.description);
    }


    private static JsonObject marvelCharactersToJsonObject(List<MarvelCharacter> characters) {
        final Collector<JsonObject, ?, JsonArray> jsonArrayCollector =
                Collector.of(
                        JsonArray::new,
                        JsonArray::add,
                        (accumulator, jsonObject) -> {
                            accumulator.addAll(jsonObject);
                            return accumulator;
                        },
                        Collector.Characteristics.IDENTITY_FINISH);

        final JsonArray marvelCharactersToJsonArray = characters.stream().map(Application::marvelCharacterToJson)
                .collect(jsonArrayCollector);
        return new JsonObject()
                .put("characters", marvelCharactersToJsonArray);
    }


    @Override
    public void start() {

        final Router router = Router.router(vertx);

        router.get(URI_CHARACTERS).handler(this::handleCharacters);
        router.get(URI_CHARACTERS + "/:id").handler(this::handleCharacterById);

        router.get(URI_CHARACTERS_IMAGES + "/:id").handler(this::handleImageOfCharacterById);

        router.get(URI_INFO).handler(this::handleInfo);


        router.route().handler(
                StaticHandler.create("webapp", this.getClass().getClassLoader())
                        .setIndexPage("index.html")
                        .setCachingEnabled(dev)
                        .setFilesReadOnly(true)
        );
        router.route().handler(event -> event.reroute("index.html")); // No not found => redirect to index.html


        vertx.createHttpServer()
                .requestHandler(router::accept)
                .exceptionHandler(h -> logger.log(Level.WARNING, h, h::getMessage))
                .listen(port);
    }

    private void handleInfo(RoutingContext routingContext) {
        if (null == info) {
            info = getJvmInfo();
        }

        byte[] response = info.getBytes(UTF8_CHARSET);
        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.length))
                .setStatusCode(200)
                .end(Buffer.buffer(response));

    }


    private String getJvmInfo() {
        final String javaSpecificationVersion = System.getProperty("java.specification.version");
        final String javaVersion = "\"" + System.getProperty("java.version") + "\"";
        final String javaVendor = "\"" + System.getProperty("java.specification.vendor") + "\"";
        StringBuilder sb = new StringBuilder("{\n \"javaSpecificationVersion\": ").append(javaSpecificationVersion).append(",\n")
                .append(" \"javaVersion\": ").append(javaVersion).append(",\n")
                .append(" \"javaVendor\": ").append(javaVendor);

        // before java 9 java.specification.version = 1.n
        if (!javaSpecificationVersion.startsWith("1.")) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Class<?> moduleLayerClazz = lookup.findClass("java.lang.ModuleLayer");
                MethodHandle mhBoot = lookup.findStatic(moduleLayerClazz, "boot", MethodType.methodType(moduleLayerClazz));
                Object moduleLayer = mhBoot.invoke();

                MethodHandle mhModules = lookup.findVirtual(moduleLayer.getClass(), "modules", MethodType.methodType(Set.class));
                final Set<?> modules = (Set) mhModules.invoke(moduleLayer);

                Class<?> moduleClazz = lookup.findClass("java.lang.Module");
                MethodHandle mhGetModule = lookup.findVirtual(moduleClazz.getClass(), "getModule", MethodType.methodType(moduleClazz));
                Object module = mhGetModule.invoke(Application.this.getClass());

                MethodHandle mhGetName = lookup.findVirtual(module.getClass(), "getName", MethodType.methodType(String.class));
                final String moduleName = (String) mhGetName.invokeWithArguments(module);

                sb.append(",\n \"moduleName\" : \"").append(moduleName).append("\",\n")
                        .append(" \"modules\": [\n  ")
                        .append(
                                modules.stream().map(mod -> {
                                            String value = null;
                                            try {
                                                value = ("\"" + mhGetName.invokeWithArguments(mod) + "\"");
                                            } catch (Throwable throwable) {
                                                logger.log(Level.WARNING, throwable.getMessage(), throwable);
                                            }
                                            return Optional.ofNullable(value);
                                        }
                                )
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .sorted()
                                        .collect(Collectors.joining(", "))
                        ).append("\n ]");
            } catch (Throwable throwable) {
                logger.log(Level.WARNING, throwable.getMessage(), throwable);
            }

        }
        sb.append("\n}");
        return sb.toString();
    }



    private void handleCharacters(RoutingContext routingContext) {
        final Optional<String> name = Optional.ofNullable(routingContext.request().getParam(SEARCH_QUERY));
        final Optional<Integer> score = Optional.ofNullable(routingContext.request().getParam(SEARCH_SCORE)).map(Integer::valueOf);
        final List<MarvelCharacter> characters = name
                .map(n -> charactersApi.fuzzySearchByName(n, score.orElse(100)))
                .orElseGet(charactersApi::findAll);


        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(marvelCharactersToJsonObject(characters).encodePrettily());

    }

    private void handleCharacterById(RoutingContext routingContext) {
        final int id = Integer.valueOf(routingContext.request().getParam("id"));
        final Optional<MarvelCharacter> character = charactersApi.find(id);
        final HttpServerResponse response = routingContext
                .response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json");


        character.ifPresentOrElse(
                marvelCharacter -> response.end(marvelCharacterToJson(marvelCharacter).encodePrettily()),
                () -> response.setStatusCode(404).end("{\"message\":\"Not Found\"}")
        );


    }

    private void handleImageOfCharacterById(RoutingContext routingContext) {

        final String id = routingContext.request().getParam("id");
        final String type = Optional.ofNullable(routingContext.request().getParam("type")).orElse(MarvelCharacter.ImageType.STANDARD_XLARGE.type);
        final MarvelCharacter.ImageType imageType = MarvelCharacter.ImageType.find(type);
        final Optional<MarvelCharacter> character = charactersApi.find(Integer.valueOf(id));
        final HttpServerResponse response = routingContext.response();
        character.ifPresentOrElse(
                marvelCharacter -> {
                    final URL image = marvelCharacter.getImage(imageType);
                    try (InputStream in = image.openStream()) {
                        final String filename = marvelCharacter.imageId.concat(".").concat(marvelCharacter.extension);
                        final byte[] bytes = in.readAllBytes();
                        response.putHeader(HttpHeaders.CONTENT_TYPE, "image/".concat(marvelCharacter.extension))
                                .putHeader("filename", filename)
                                .putHeader(getCacheHeaders().key, getCacheHeaders().value)
                                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                                .setStatusCode(200)
                                .end(Buffer.buffer(bytes));

                    } catch (IOException ex) {
                        logger.log(Level.WARNING, ex.getMessage(), ex);
                    }
                }, () -> response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .setStatusCode(404)
                        .end("{\"message\":\"Not Found\"}")
        );


    }


    private Header<? extends CharSequence, List<CharSequence>> getCacheHeaders() {
        if (dev) {
            return new Header<>(HttpHeaders.CACHE_CONTROL, List.of("no-cache"));
        } else {
            return new Header<>(HttpHeaders.CACHE_CONTROL, List.of("public", "max-age=600"));
        }
    }

    private class Header<K, V> {
        private final V value;
        private final K key;

        public Header(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
