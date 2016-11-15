package com.bulina.iqoptiontesttask.framework.dataModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.json.JSONObject;

import java.util.Date;

public class Message {

    public enum MESSAGE_TYPE {
        UNKNOWN(0),
        SYNC(1),
        AUTH(2);

        private final int id;

        MESSAGE_TYPE(int id) {
            this.id = id;
        }
    }

    @JsonProperty()
    @Getter
    private String name;

    @JsonProperty()
    @Getter
    private String msg;

    @JsonIgnore
    @Getter
    private Date timeCreated;

    @JsonIgnore
    @Getter
    private String initial;

    @JsonIgnore
    @Getter
    private MESSAGE_TYPE type;

    private Message initMessage() {
        JSONObject jsonMsg = new JSONObject(initial);

        if (jsonMsg == null || !jsonMsg.has("name") || !jsonMsg.has("msg")) {
            System.out.println(initMessage());
            throw new IllegalArgumentException("Message has incorrect format");
        }

        name = jsonMsg.getString("name");
        initTypeByName();
        if(type == MESSAGE_TYPE.SYNC) {
            msg = jsonMsg.get("msg").toString();
        }
        else {
            msg = jsonMsg.getJSONObject("msg").toString();
        }

        return this;
    }

    private Message initTypeByName() {
        if (name.equalsIgnoreCase("timeSync")) {
            type = MESSAGE_TYPE.SYNC;
        } else if (name.equalsIgnoreCase("ssid")) {
            type = MESSAGE_TYPE.AUTH;
        }
        else type = MESSAGE_TYPE.UNKNOWN;

        return this;
    }


    public static Builder Builder() {
        return new Message().new Builder();
    }

    private Message() {
    }


    public class Builder {

        private Builder() {
        }

        public Builder setName(String name) {
            Message.this.name = name;
            return this;
        }

        public Builder setMsg(String msg) {
            Message.this.msg = msg;
            return this;
        }

        public Message build() {
            Message.this.initial = Message.this.toString();
            Message.this.timeCreated = new Date();
            return Message.this.initTypeByName();
        }

        public Message buildFromString(String message) {

            Message.this.initial = message;
            Message.this.timeCreated = new Date();
            return Message.this.initMessage();
        }
    }

    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = null;

        try {
            json = objectMapper.writeValueAsString(this);
            System.out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }
}
