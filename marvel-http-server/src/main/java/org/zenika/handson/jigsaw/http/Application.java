package org.zenika.handson.jigsaw.http;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.zenika.handson.jigsaw.api.CharactersApi;
import org.zenika.handson.jigsaw.api.MarvelCharacter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.zenika.handson.jigsaw.api.CharactersApi.marvelCharacterToJson;
import static org.zenika.handson.jigsaw.api.CharactersApi.marvelCharactersToJson;

@SuppressWarnings("WeakerAccess")
public class Application {

    private static final String URI_CHARACTERS = "/api/characters";
    private static final String URI_INFO = "/api/info";
    private static final String URI_CHARACTERS_WITH_SLASH = "/api/characters/";
    private static final String URI_CHARACTERS_IMAGES = "/api/images/";
    private static final String SEARCH_QUERY = "search";
    private static final String SEARCH_SCORE = "score";
    private static final String TYPE_QUERY = "type=";
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    private static final Logger logger = Logger.getLogger(Application.class.getCanonicalName());
    @SuppressWarnings("ConstantConditions")
    private static final CharactersApi charactersApi = ServiceLoader.load(CharactersApi.class).findFirst().get();
    private static String info = null;
    private final int port;
    private final boolean dev;
    private HttpServer server;

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
        Application server = new Application(port, dev);
        try {
            server.start();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            server.stop(1);
        }


    }

    /**
     * Stop the server
     *
     * @param delay maximum waiting time before force killing
     */
    public void stop(int delay) {
        logger.info("Stopping the server");
        if (null != server) server.stop(delay);
    }

    /**
     * Start the server
     *
     * @throws IOException
     */
    @SuppressWarnings("JavaDoc")
    public void start() throws IOException {
        logger.info("Starting Server :" + port + ", mode :" + (dev ? "development" : "production"));
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext(URI_CHARACTERS, new CharacterHandler());
        server.createContext(URI_CHARACTERS_IMAGES, new ImageHandler());
        server.createContext(URI_INFO, new InfoHandler());
        server.createContext("/", new StaticHandler());

        final int processors = Runtime.getRuntime().availableProcessors();
        if (dev) {
            if (processors > 2) {
                server.setExecutor(Executors.newWorkStealingPool(processors / 2));
            }
        } else {
            server.setExecutor(Executors.newWorkStealingPool());
        }
        server.start();
    }

    /**
     * Return the header for the cache depending the mode (dev/prod). In dev there is no cache.
     *
     * @return Map of header for setting the cache control
     */
    private Map<String, List<String>> getCacheHeaders() {
        if (dev) {
            return Map.of("Cache-Control", List.of("no-cache"));
        } else {
            return Map.of("Cache-Control", List.of("public", "max-age=600"));
        }
    }

    private String httpDecode(String query) {
        try {
            return URLDecoder.decode(query, UTF8_CHARSET.name());
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return query;
        }
    }

    private enum HTTP_ERROR {
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found"),
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented");

        final int errorCode;
        final String errorMessage;
        final String jsonMessage;
        final byte[] jsonMessageToBytes;
        final byte[] errorMessageToBytes;

        HTTP_ERROR(int errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.errorMessageToBytes = errorMessage.getBytes(UTF8_CHARSET);
            this.jsonMessage = "{\"message\":\"" + errorMessage + "\"}";
            this.jsonMessageToBytes = jsonMessage.getBytes(UTF8_CHARSET);
        }

    }

    private class StaticHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (final OutputStream os = exchange.getResponseBody()) {
                if ("GET".equals(exchange.getRequestMethod())) {
                    final String tmpPath = exchange.getRequestURI().getPath();
                    final String path = "/".equals(tmpPath) ? "webapp/index.html" : "webapp".concat(tmpPath);


                    URL resource = this.getClass().getModule().getClassLoader().getResource(path);
                    logger.fine("Path : " + path + " => Resource : " + resource);
                    if (null != resource && !tmpPath.endsWith(".class")) {

                        httpResponse(exchange, os, path, resource);
                    } else {
                        // If not found redirect to index page
                        resource = this.getClass().getModule().getClassLoader().getResource("webapp/index.html");
                        assert resource != null;
                        httpResponse(exchange, os, "index.html", resource);
                    }

                } else {
                    exchange.sendResponseHeaders(HTTP_ERROR.NOT_IMPLEMENTED.errorCode, HTTP_ERROR.NOT_IMPLEMENTED.errorMessageToBytes.length);
                    os.write(HTTP_ERROR.NOT_IMPLEMENTED.errorMessageToBytes);
                }
            } finally {
                exchange.close();
            }
        }

        private void httpResponse(HttpExchange exchange, OutputStream os, String path, URL resource) throws IOException {
            assert resource != null;
            try (InputStream in = resource.openStream()) {
                final byte[] response = in.readAllBytes();

                final String[] splitPath = path.split("/");
                final String fileName = splitPath[splitPath.length - 1];
                final String[] splitName = fileName.split("\\.");
                final Optional<String> extension = splitName.length > 0 ? Optional.of(splitName[splitName.length - 1]) : Optional.empty();

                final Headers responseHeaders = exchange.getResponseHeaders();
                extension.ifPresent(e -> responseHeaders.add("Content-Type", getContentType(e)));
                responseHeaders.add("Content-Length", String.valueOf(response.length));

                responseHeaders.putAll(getCacheHeaders());


                exchange.sendResponseHeaders(200, response.length);
                os.write(response);
            } catch (IOException ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
                exchange.sendResponseHeaders(HTTP_ERROR.INTERNAL_SERVER_ERROR.errorCode, HTTP_ERROR.INTERNAL_SERVER_ERROR.errorMessageToBytes.length);
                os.write(HTTP_ERROR.INTERNAL_SERVER_ERROR.errorMessageToBytes);
            }
        }

        private String getContentType(String extension) {
            switch (extension) {
                case "htm":
                case "html":
                    return "text/html";
                case "css":
                    return "text/css";
                case "ico":
                    return "image/x-icon";
                case "jpeg":
                case "jpg":
                    return "image/jpeg";
                case "gif":
                    return "image/gif";
                case "png":
                    return "image/png";
                case "svg":
                    return "image/svg+xml";
                case "js":
                    return "application/javascript";
                case "json":
                    return "application/json";
                default:
                    return extension;
            }
        }
    }

    private class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (final OutputStream os = exchange.getResponseBody()) {

                if (null == info) {
                    info = getJvmInfo();
                }

                byte[] response = info.getBytes(UTF8_CHARSET);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.getResponseHeaders().add("Content-Length", String.valueOf(response.length));
                exchange.sendResponseHeaders(200, response.length);
                os.write(response);

            } finally {
                exchange.close();
            }
        }

        private String getJvmInfo() {
            final String javaSpecificationVersion = System.getProperty("java.specification.version");
            final String javaVersion = "\"" + System.getProperty("java.version") + "\"";
            final String javaVendor = "\"" + System.getProperty("java.specification.vendor") + "\"";
            StringBuilder sb = new StringBuilder("{\n \"javaSpecificationVersion\": ").append(javaSpecificationVersion).append(",\n")
                    .append(" \"javaVersion\": ").append(javaVersion).append(",\n")
                    .append(" \"javaVendor\": ").append(javaVendor);

            if (Integer.valueOf(javaSpecificationVersion) >= 9) {
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
    }

    private class ImageHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            {
                final String method = exchange.getRequestMethod();
                final String path = exchange.getRequestURI().getPath();
                final String query = exchange.getRequestURI().getQuery();
                logger.fine("RECEIVE URL " + path);
                if (!method.equals("GET")) {
                    logger.info(method + " not supported");

                } else if (path.startsWith(URI_CHARACTERS_IMAGES)) {
                    handleImageOfCharacterById(exchange, path, query);
                }

            }

        }

        private void handleImageOfCharacterById(HttpExchange exchange, String path, String query) throws IOException {
            final String strId = path.substring(URI_CHARACTERS_IMAGES.length());
            final String type = null != query && query.startsWith(TYPE_QUERY) ? query.substring(TYPE_QUERY.length()) : MarvelCharacter.ImageType.STANDARD_XLARGE.type;

            final MarvelCharacter.ImageType imageType = MarvelCharacter.ImageType.find(type);
            final OutputStream os = exchange.getResponseBody();
            try (os) {

                final Integer id = Integer.valueOf(strId);
                final Optional<MarvelCharacter> character = charactersApi.find(id);
                if (character.isPresent()) {
                    MarvelCharacter marvelCharacter = character.get();
                    final URL image = marvelCharacter.getImage(imageType);
                    if (null == image) {
                        logger.info("The image of character " + strId + " of type " + imageType + " was not found");
                        exchange.sendResponseHeaders(HTTP_ERROR.NOT_FOUND.errorCode, HTTP_ERROR.NOT_FOUND.jsonMessageToBytes.length);
                        os.write(HTTP_ERROR.NOT_FOUND.jsonMessageToBytes);
                    } else {
                        try (InputStream in = image.openStream()) {
                            final byte[] bytes = in.readAllBytes();
                            final Headers responseHeaders = exchange.getResponseHeaders();
                            responseHeaders.add("Content-Type", "image/".concat(marvelCharacter.extension));
                            responseHeaders.putAll(getCacheHeaders());
                            responseHeaders.add("Content-Length", String.valueOf(bytes.length));
                            exchange.sendResponseHeaders(200, bytes.length);
                            os.write(bytes);
                        } catch (IOException ex) {
                            logger.log(Level.WARNING, ex.getMessage(), ex);
                            exchange.sendResponseHeaders(HTTP_ERROR.INTERNAL_SERVER_ERROR.errorCode, HTTP_ERROR.INTERNAL_SERVER_ERROR.jsonMessageToBytes.length);
                            os.write(HTTP_ERROR.INTERNAL_SERVER_ERROR.jsonMessageToBytes);
                        }
                    }
                } else {
                    exchange.sendResponseHeaders(HTTP_ERROR.NOT_FOUND.errorCode, HTTP_ERROR.NOT_FOUND.jsonMessageToBytes.length);
                    os.write(HTTP_ERROR.NOT_FOUND.jsonMessageToBytes);
                }
            } catch (NumberFormatException nf) {
                logger.info("Incorrect search id (not a number) : " + strId);
                exchange.sendResponseHeaders(HTTP_ERROR.BAD_REQUEST.errorCode, HTTP_ERROR.BAD_REQUEST.jsonMessageToBytes.length);
                os.write(HTTP_ERROR.BAD_REQUEST.jsonMessageToBytes);
            } finally {
                exchange.close();
            }

        }
    }

    private class CharacterHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            {
                final String method = exchange.getRequestMethod();
                final String path = exchange.getRequestURI().getPath();
                final String query = exchange.getRequestURI().getQuery(); // /...
                logger.fine("RECEIVE URL " + path);
                if (!method.equals("GET")) {
                    logger.info(method + " not supported");
                } else if (URI_CHARACTERS.equals(path) || URI_CHARACTERS_WITH_SLASH.equals(path)) {
                    handleCharacters(exchange, query);
                } else if (path.startsWith(URI_CHARACTERS_WITH_SLASH)) {
                    handleCharacterById(exchange, path);
                }

            }

        }

        private void handleCharacters(HttpExchange exchange, String query) throws IOException {

            final String[] args = null == query ? new String[]{} : httpDecode(query).split("&");

            Map<String, String> queryMap = Arrays.stream(args).map(arg -> arg.split("="))
                    .filter(array -> 2 == array.length)
                    .collect(Collectors.toMap(array -> array[0], array -> array[1]));

            Optional<String> name = Optional.ofNullable(queryMap.get(SEARCH_QUERY));
            Optional<Integer> score = Optional.ofNullable(queryMap.get(SEARCH_SCORE)).map(Integer::valueOf);

            final List<MarvelCharacter> characters = name
                    .map(n -> charactersApi.fuzzySearchByName(n, score.orElse(100)))
                    .orElseGet(charactersApi::findAll);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try (final OutputStream os = exchange.getResponseBody()) {
                if (characters.isEmpty()) {
                    exchange.sendResponseHeaders(HTTP_ERROR.NOT_FOUND.errorCode, HTTP_ERROR.NOT_FOUND.jsonMessageToBytes.length);
                    os.write(HTTP_ERROR.NOT_FOUND.jsonMessageToBytes);
                } else {
                    final byte[] response = marvelCharactersToJson(characters).getBytes(UTF8_CHARSET);
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(response.length));
                    exchange.sendResponseHeaders(200, response.length);
                    os.write(response);
                }
            } finally {
                exchange.close();
            }
        }


        private void handleCharacterById(HttpExchange exchange, String path) throws IOException {
            final String strId = path.substring(URI_CHARACTERS_WITH_SLASH.length());
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            final OutputStream os = exchange.getResponseBody();
            try (os) {

                final int id = Integer.valueOf(strId);
                final Optional<MarvelCharacter> character = charactersApi.find(id);

                if (character.isPresent()) {
                    final byte[] response = marvelCharacterToJson(character.get()).getBytes(UTF8_CHARSET);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseHeaders().add("Content-Length", String.valueOf(response.length));
                    os.write(response);
                } else {
                    exchange.sendResponseHeaders(HTTP_ERROR.NOT_FOUND.errorCode, HTTP_ERROR.NOT_FOUND.jsonMessageToBytes.length);
                    os.write(HTTP_ERROR.NOT_FOUND.jsonMessageToBytes);
                }

            } catch (NumberFormatException nf) {
                logger.info("Incorrect search id (not a number) : " + strId);
                exchange.sendResponseHeaders(HTTP_ERROR.BAD_REQUEST.errorCode, HTTP_ERROR.BAD_REQUEST.jsonMessageToBytes.length);
                os.write(HTTP_ERROR.BAD_REQUEST.jsonMessageToBytes);
            } finally {
                exchange.close();
            }

        }


    }

}
