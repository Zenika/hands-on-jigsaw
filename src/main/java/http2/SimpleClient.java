package http2;


import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class SimpleClient {
    public static void main(String[] args) {
        HttpClient httpClient = HttpClient.newHttpClient();
        final URI uri = URI.create("https://www.google.com");
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        try {
            long start = System.currentTimeMillis();


            // Sync
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandler.asString());
            System.out.println(httpResponse.statusCode());
            System.out.println(httpResponse.body());

            // Async

            CompletableFuture<HttpResponse<String>> asyncHttpResponse = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandler.asString());
            HttpResponse<String> response = asyncHttpResponse.get();
            System.out.println(response.statusCode());
            System.out.println(response.body());
            long end = System.currentTimeMillis();

            System.out.println("Time : " + (end - start)/1000d);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}

