package com.seirantakhirov.test;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrptApi {
    public static void main(String[] args) {
        DocumentHttpClient documentHttpClient = DocumentHttpClient.getInstance(10, TimeUnit.SECONDS);
        Object object = documentHttpClient.createDocument(null, "signature");
    }
}

class DocumentHttpClient extends BaseHttpClient {
    private static volatile DocumentHttpClient instance;
    private static final AtomicInteger countOfRequests = new AtomicInteger(0);
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    private static final Runnable resetCountOfRequests = () -> countOfRequests.set(0);
    private final Integer maxCountOfRequests;

    public static DocumentHttpClient getInstance(Integer maxCountOfRequests, TimeUnit timeUnit) {
        if (instance == null) {
            synchronized (DocumentHttpClient.class) {
                if (instance == null) {
                    instance = new DocumentHttpClient(maxCountOfRequests, timeUnit);
                }
            }
        }
        return instance;
    }

    private DocumentHttpClient(Integer maxCountOfRequests, TimeUnit timeUnit) {
        if (maxCountOfRequests == null || maxCountOfRequests < 1) {
            throw new IllegalArgumentException("Max count of requests must be greater than 0");
        }
        this.maxCountOfRequests = maxCountOfRequests;
        scheduledThreadPoolExecutor.schedule(resetCountOfRequests, 1, timeUnit);
    }

    public synchronized Object createDocument(DocumentCreateRequest request, String signature) {
        if (countOfRequests.get() >= maxCountOfRequests) {
            return null;
        }
        countOfRequests.incrementAndGet();
        try {
            //TODO 1)В ТЗ не ясно какой именно контракт для подписи, я выбрал заголовки
            //TODO 2)В ТЗ нет описания ответа, я выбрал Object
            return basePostRequest(httpClient, mapper.writeValueAsString(request),
                    DocumentUri.CREATE_PATH.getUri(), Object.class, Map.of("signature-header", signature));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

abstract class BaseHttpClient {
    private static final String ERROR_MESSAGE_TEMPLATE = "Status code: %d, body: %s";
    private static final String LOG_ERROR_MESSAGE_TEMPLATE = "Error while sending request to %s. Error: %s";
    private static final Logger logger = Logger.getLogger(BaseHttpClient.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    protected <T> T basePostRequest(HttpClient client, String requestBody,
                                    String uri, Class<T> typeOfResponse, Map<String, String> headers) {
        try {
            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(httpRequestBuilder::setHeader);
            }
            HttpResponse<String> httpResponse = client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException(ERROR_MESSAGE_TEMPLATE.formatted(httpResponse.statusCode(), httpResponse.body()));
            } else {
                return mapper.readValue(httpResponse.body(), typeOfResponse);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, LOG_ERROR_MESSAGE_TEMPLATE.formatted(uri, e.getMessage()));
            throw new RuntimeException(e);
        }
    }
}

enum DocumentUri {
    DOCUMENT_BASE_URI("https://ismp.crpt.ru/api/v3/lk/documents"),
    CREATE_PATH(DOCUMENT_BASE_URI.uri + "/create");

    private final String uri;

    DocumentUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }
}

    record DocumentCreateRequest(

            @JsonProperty("reg_number")
            String regNumber,

            @JsonProperty("production_date")
            String productionDate,

            @JsonProperty("description")
            Description description,

            @JsonProperty("doc_type")
            String docType,

            @JsonProperty("doc_id")
            String docId,

            @JsonProperty("owner_inn")
            String ownerInn,

            @JsonProperty("products")
            List<ProductsItem> products,

            @JsonProperty("reg_date")
            String regDate,

            @JsonProperty("participant_inn")
            String participantInn,

            @JsonProperty("doc_status")
            String docStatus,

            @JsonProperty("importRequest")
            Boolean importRequest,

            @JsonProperty("production_type")
            String productionType,

            @JsonProperty("producer_inn")
            String producerInn
    ) {
    }

    record ProductsItem(

            @JsonProperty("uitu_code")
            String uituCode,

            @JsonProperty("certificate_document_date")
            String certificateDocumentDate,

            @JsonProperty("production_date")
            String productionDate,

            @JsonProperty("certificate_document_number")
            String certificateDocumentNumber,

            @JsonProperty("tnved_code")
            String tnvedCode,

            @JsonProperty("certificate_document")
            String certificateDocument,

            @JsonProperty("producer_inn")
            String producerInn,

            @JsonProperty("owner_inn")
            String ownerInn,

            @JsonProperty("uit_code")
            String uitCode
    ) {
    }

    record Description(
            @JsonProperty("participantInn")
            String participantInn
    ) {
    }


