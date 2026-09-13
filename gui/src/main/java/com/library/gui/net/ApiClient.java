package com.library.gui.net;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.library.gui.model.BookDto;
import com.library.gui.model.ErrorResponseDto;
import com.library.gui.model.LoanDto;
import com.library.gui.model.MemberDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

/**
 * Thin HTTP client wrapping the Library Management System REST API.
 *
 * Error-handling / fallback design:
 *  - Every network call has a request timeout, so the GUI never hangs forever.
 *  - A transient connection failure (server not yet up / brief network blip)
 *    is retried once after a short pause before giving up.
 *  - Any non-2xx HTTP response is parsed into the backend's ErrorResponse
 *    JSON shape so the user sees the *actual* server-side reason (e.g.
 *    "A book with ISBN ... already exists") rather than a generic failure.
 *  - All failure paths surface as a single checked ApiException type, so
 *    every panel can handle errors with one consistent catch block.
 */
public class ApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_ATTEMPTS = 2;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    // ---------------- Books ----------------

    public List<BookDto> getAllBooks() throws ApiException {
        String body = send(request("/api/books").GET());
        return parseList(body, BookDto[].class);
    }

    public List<BookDto> searchBooks(String field, String value) throws ApiException {
        String body = send(request("/api/books/search?" + field + "=" + urlEncode(value)).GET());
        return parseList(body, BookDto[].class);
    }

    public BookDto createBook(BookDto book) throws ApiException {
        String json = toJson(book);
        String body = send(request("/api/books").POST(HttpRequest.BodyPublishers.ofString(json)));
        return parse(body, BookDto.class);
    }

    public BookDto updateBook(String id, BookDto book) throws ApiException {
        String json = toJson(book);
        String body = send(request("/api/books/" + id).PUT(HttpRequest.BodyPublishers.ofString(json)));
        return parse(body, BookDto.class);
    }

    public void deleteBook(String id) throws ApiException {
        send(request("/api/books/" + id).DELETE());
    }

    // ---------------- Members ----------------

    public List<MemberDto> getAllMembers() throws ApiException {
        String body = send(request("/api/members").GET());
        return parseList(body, MemberDto[].class);
    }

    public MemberDto createMember(MemberDto member) throws ApiException {
        String json = toJson(member);
        String body = send(request("/api/members").POST(HttpRequest.BodyPublishers.ofString(json)));
        return parse(body, MemberDto.class);
    }

    public MemberDto updateMember(String id, MemberDto member) throws ApiException {
        String json = toJson(member);
        String body = send(request("/api/members/" + id).PUT(HttpRequest.BodyPublishers.ofString(json)));
        return parse(body, MemberDto.class);
    }

    public void deleteMember(String id) throws ApiException {
        send(request("/api/members/" + id).DELETE());
    }

    // ---------------- Loans ----------------

    public List<LoanDto> getAllLoans() throws ApiException {
        String body = send(request("/api/loans").GET());
        return parseList(body, LoanDto[].class);
    }

    public LoanDto issueBook(String bookId, String memberId) throws ApiException {
        String path = "/api/loans/issue?bookId=" + urlEncode(bookId) + "&memberId=" + urlEncode(memberId);
        String body = send(request(path).POST(HttpRequest.BodyPublishers.noBody()));
        return parse(body, LoanDto.class);
    }

    public LoanDto returnBook(String loanId) throws ApiException {
        String body = send(request("/api/loans/" + loanId + "/return").PUT(HttpRequest.BodyPublishers.noBody()));
        return parse(body, LoanDto.class);
    }

    // ---------------- Core HTTP + fallback logic ----------------

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    /**
     * Executes the request, retrying once on a transient connectivity
     * failure, and translates any error response into a descriptive
     * ApiException using the server's ErrorResponse body when available.
     */
    private String send(HttpRequest.Builder builder) throws ApiException {
        HttpRequest req = builder.build();
        IOException lastNetworkError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();
                if (code >= 200 && code < 300) {
                    return response.body();
                }
                throw toApiException(code, response.body());
            } catch (HttpTimeoutException | java.net.ConnectException te) {
                lastNetworkError = te;
                if (attempt < MAX_ATTEMPTS) {
                    sleepBriefly();
                }
            } catch (IOException e) {
                lastNetworkError = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleepBriefly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException("Request was interrupted.", e);
            }
        }
        throw new ApiException(
                "Could not reach the Library server at " + baseUrl +
                        ". Please make sure the backend is running and try again.",
                lastNetworkError);
    }

    private ApiException toApiException(int code, String body) {
        try {
            ErrorResponseDto err = mapper.readValue(body, ErrorResponseDto.class);
            String message = err.message != null ? err.message : "Request failed with status " + code;
            if (err.details != null && !err.details.isEmpty()) {
                message += "\n- " + String.join("\n- ", err.details);
            }
            return new ApiException(message, code);
        } catch (Exception parseFailure) {
            // Backend didn't return the expected JSON shape (e.g. proxy error page) — fall back gracefully.
            return new ApiException("Request failed with status " + code, code);
        }
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String toJson(Object obj) throws ApiException {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new ApiException("Failed to prepare request data: " + e.getMessage(), e);
        }
    }

    private <T> T parse(String json, Class<T> type) throws ApiException {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new ApiException("Failed to read server response: " + e.getMessage(), e);
        }
    }

    private <T> List<T> parseList(String json, Class<T[]> arrayType) throws ApiException {
        try {
            T[] arr = mapper.readValue(json, arrayType);
            return List.of(arr);
        } catch (Exception e) {
            throw new ApiException("Failed to read server response: " + e.getMessage(), e);
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
