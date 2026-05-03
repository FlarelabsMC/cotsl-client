package com.flarelabsmc.cotsl.launch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Networking {
    public static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .build();

    public static HttpResponse<String> requestString(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();

        return HTTP_CLIENT.send(request, handler);
    }

    public static HttpResponse<InputStream> requestStream(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();
        HttpResponse.BodyHandler<InputStream> handler = HttpResponse.BodyHandlers.ofInputStream();

        return HTTP_CLIENT.send(request, handler);
    }

    /// Makes a web request to `uri` and writes its body to `filePath` using `options`
    public static HttpResponse<Path> downloadFileOptions(URI uri, Path filePath, OpenOption... options) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .build();
        HttpResponse.BodyHandler<Path> handler = HttpResponse.BodyHandlers.ofFile(filePath, options);

        return HTTP_CLIENT.send(request, handler);
    }

    /// Makes a web request to `uri` and writes its body to `filePath` using the `CREATE` and `WRITE` options
    public static HttpResponse<Path> downloadFile(URI uri, Path filePath) throws IOException, InterruptedException {
        return downloadFileOptions(uri, filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
}
