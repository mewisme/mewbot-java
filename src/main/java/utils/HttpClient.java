package utils;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client utility for making API requests.
 * Uses OkHttp for efficient HTTP operations.
 */
public class HttpClient {
  private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
  private static HttpClient instance;
  private final OkHttpClient client;

  private HttpClient() {
    this.client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
  }

  /**
   * Gets the singleton instance of HttpClient.
   *
   * @return HttpClient instance
   */
  @NotNull
  public static HttpClient getInstance() {
    if (instance == null) {
      instance = new HttpClient();
    }
    return instance;
  }

  /**
   * Performs a GET request.
   *
   * @param url The URL to request
   * @return Response body as string, or null if error
   */
  @Nullable
  public String get(@NotNull String url) {
    return get(url, null);
  }

  /**
   * Performs a GET request with headers.
   *
   * @param url     The URL to request
   * @param headers Optional headers (can be null)
   * @return Response body as string, or null if error
   */
  @Nullable
  public String get(@NotNull String url, @Nullable Headers headers) {
    Request.Builder requestBuilder = new Request.Builder().url(url);
    
    if (headers != null) {
      requestBuilder.headers(headers);
    }

    Request request = requestBuilder.build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        return response.body().string();
      } else {
        // Log all error details
        logger.error("HTTP GET request failed");
        logger.error("URL: {}", url);
        logger.error("Status Code: {}", response.code());
        logger.error("Status Message: {}", response.message());
        logger.error("Response Headers: {}", response.headers());
        
        // Try to read response body for error details
        if (response.body() != null) {
          try {
            String errorBody = response.body().string();
            logger.error("Response Body: {}", errorBody);
          } catch (IOException e) {
            logger.error("Failed to read error response body", e);
          }
        } else {
          logger.error("Response Body: null");
        }
        
        return null;
      }
    } catch (IOException e) {
      logger.error("Error making HTTP GET request to: {}", url, e);
      return null;
    }
  }

  /**
   * Performs a POST request.
   *
   * @param url         The URL to request
   * @param contentType Content type (e.g., "application/json")
   * @param body        Request body
   * @return Response body as string, or null if error
   */
  @Nullable
  public String post(@NotNull String url, @NotNull String contentType, @NotNull String body) {
    return post(url, contentType, body, null);
  }

  /**
   * Performs a POST request with headers.
   *
   * @param url         The URL to request
   * @param contentType Content type (e.g., "application/json")
   * @param body        Request body
   * @param headers     Optional additional headers (can be null)
   * @return Response body as string, or null if error
   */
  @Nullable
  public String post(@NotNull String url, @NotNull String contentType, @NotNull String body,
      @Nullable Headers headers) {
    RequestBody requestBody = RequestBody.create(body, MediaType.parse(contentType));
    Request.Builder requestBuilder = new Request.Builder()
        .url(url)
        .post(requestBody);

    if (headers != null) {
      requestBuilder.headers(headers);
    }

    Request request = requestBuilder.build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        return response.body().string();
      } else {
        // Log all error details
        logger.error("HTTP POST request failed");
        logger.error("URL: {}", url);
        logger.error("Status Code: {}", response.code());
        logger.error("Status Message: {}", response.message());
        logger.error("Request Headers: {}", headers != null ? headers : "none");
        logger.error("Request Body: {}", body);
        logger.error("Response Headers: {}", response.headers());
        
        // Try to read response body for error details
        if (response.body() != null) {
          try {
            String errorBody = response.body().string();
            logger.error("Response Body: {}", errorBody);
          } catch (IOException e) {
            logger.error("Failed to read error response body", e);
          }
        } else {
          logger.error("Response Body: null");
        }
        
        return null;
      }
    } catch (IOException e) {
      logger.error("Error making HTTP POST request to: {}", url, e);
      logger.error("Request Body: {}", body);
      return null;
    }
  }

  /**
   * Performs an asynchronous GET request.
   *
   * @param url      The URL to request
   * @param callback Callback to handle the response
   */
  public void getAsync(@NotNull String url, @NotNull Callback callback) {
    getAsync(url, null, callback);
  }

  /**
   * Performs an asynchronous GET request with headers.
   *
   * @param url      The URL to request
   * @param headers  Optional headers (can be null)
   * @param callback Callback to handle the response
   */
  public void getAsync(@NotNull String url, @Nullable Headers headers, @NotNull Callback callback) {
    Request.Builder requestBuilder = new Request.Builder().url(url);

    if (headers != null) {
      requestBuilder.headers(headers);
    }

    Request request = requestBuilder.build();
    client.newCall(request).enqueue(callback);
  }

  /**
   * Creates a Headers object from a builder pattern.
   *
   * @return Headers.Builder for chaining
   */
  @NotNull
  public static Headers.Builder headersBuilder() {
    return new Headers.Builder();
  }

  /**
   * Creates headers with a single header.
   *
   * @param name  Header name
   * @param value Header value
   * @return Headers object
   */
  @NotNull
  public static Headers headers(@NotNull String name, @NotNull String value) {
    return new Headers.Builder().add(name, value).build();
  }
}

