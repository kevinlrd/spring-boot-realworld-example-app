package io.spring.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;

@Configuration
public class GraphQlWebConfig {

  private static final ObjectMapper GRAPHQL_OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

  @Bean
  public WebGraphQlHandler webGraphQlHandler(
      ExecutionGraphQlService service, ObjectProvider<WebGraphQlInterceptor> interceptors) {
    return WebGraphQlHandler.builder(service)
        .interceptors(interceptors.orderedStream().toList())
        .build();
  }

  @Bean
  public RouterFunction<ServerResponse> graphQlRouterFunction(WebGraphQlHandler webGraphQlHandler) {
    return RouterFunctions.route()
        .POST(
            "/graphql",
            RequestPredicates.contentType(MediaType.APPLICATION_JSON),
            request -> handleGraphQlRequest(request, webGraphQlHandler))
        .build();
  }

  private ServerResponse handleGraphQlRequest(
      ServerRequest request, WebGraphQlHandler webGraphQlHandler) throws Exception {
    byte[] body = request.servletRequest().getInputStream().readAllBytes();
    Map<String, Object> bodyMap = GRAPHQL_OBJECT_MAPPER.readValue(body, MAP_TYPE_REF);

    URI uri = request.uri();
    HttpHeaders headers = request.headers().asHttpHeaders();
    String id = request.servletRequest().getRequestId();
    Locale locale = request.headers().asHttpHeaders().getContentLanguage();

    WebGraphQlRequest graphQlRequest =
        new WebGraphQlRequest(uri, headers, null, request.attributes(), bodyMap, id, locale);

    Mono<ServerResponse> responseMono =
        webGraphQlHandler
            .handleRequest(graphQlRequest)
            .map(
                graphQlResponse -> {
                  try {
                    byte[] responseBytes =
                        GRAPHQL_OBJECT_MAPPER.writeValueAsBytes(
                            graphQlResponse.getExecutionResult().toSpecification());
                    return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBytes);
                  } catch (Exception e) {
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                  }
                });

    return responseMono.block();
  }
}
