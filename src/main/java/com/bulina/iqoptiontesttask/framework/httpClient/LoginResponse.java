package com.bulina.iqoptiontesttask.framework.httpClient;

import lombok.Getter;
import org.json.JSONObject;

import javax.ws.rs.core.NewCookie;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoginResponse {

    @Getter
    private int statusCode;

    @Getter
    private String email;

    @Getter
    private Set<String> ssids;

    private List<NewCookie> cookies;

    private JSONObject body;

    private LoginResponse() {
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static Builder Builder() {
        return new LoginResponse().new Builder();
    }

    private LoginResponse initSsids() {
        ssids = new HashSet<>();

        for (NewCookie cookie : cookies) {
            if (cookie.getName().equalsIgnoreCase("ssid")) {
                ssids.add(cookie.getValue());
            }
        }

        return this;
    }

    private LoginResponse initEmail() {
        if ((body != null) && (body.has("result"))) {
            email = body
                    .getJSONObject("result")
                    .getString("email");
        }
        return this;
    }

    public class Builder {

        private Builder() {
        }

        public Builder setStatusCode(int status) {
            LoginResponse.this.statusCode = status;

            return this;
        }

        public Builder setCookies(List<NewCookie> cookies) {
            LoginResponse.this.cookies = cookies;
            return this;
        }

        public Builder setBody(String body) {

            LoginResponse.this.body = new JSONObject(body);
            return this;
        }

        public LoginResponse build() {
            LoginResponse
                    .this
                    .initSsids()
                    .initEmail();

            return LoginResponse.this;
        }
    }
}
