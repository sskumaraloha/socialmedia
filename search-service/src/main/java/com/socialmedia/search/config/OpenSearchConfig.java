package com.socialmedia.search.config;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Bean
    public OpenSearchClient openSearchClient(@Value("${app.search.opensearch.host:localhost}") String host,
            @Value("${app.search.opensearch.port:9200}") int port,
            @Value("${app.search.opensearch.scheme:http}") String scheme) {
        RestClient restClient = RestClient.builder(new HttpHost(scheme, host, port)).build();
        OpenSearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
