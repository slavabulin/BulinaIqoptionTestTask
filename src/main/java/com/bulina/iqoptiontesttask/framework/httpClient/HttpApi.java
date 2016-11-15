package com.bulina.iqoptiontesttask.framework.httpClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;

public class HttpApi {

    private ClientConfig config;

    private Client client;

    private static final String httpApiHost = "https://iqoption.com/api";

    public HttpApi() throws IOException {
        config = new DefaultClientConfig();
        client = Client.create(config);

    }

    public LoginResponse postToLoginV2(String email, String password) {
        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("email", email);
        formData.add("password", password);

        ClientResponse response = doPost("/login/v2", formData);

        return LoginResponse.
                Builder()
                .setStatusCode(response.getStatus())
                .setCookies(response.getCookies())
                .setBody(response.getEntity(String.class))
                .build();
    }

    private ClientResponse doPost(String uri, MultivaluedMap formData) {
        WebResource webResource =
                client.resource(UriBuilder.fromUri(httpApiHost + uri).build());

        System.out.println("POST request " + webResource);

        ClientResponse clientResponse =
                webResource
                        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                        .post(ClientResponse.class, formData);

        System.out.println(clientResponse);

        return  clientResponse;
    }
}
