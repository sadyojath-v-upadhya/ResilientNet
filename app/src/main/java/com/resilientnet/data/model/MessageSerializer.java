package com.resilientnet.data.model;

import com.resilientnet.data.local.entity.MessageEntity;

import org.json.JSONException;
import org.json.JSONObject;

public final class MessageSerializer {

    private MessageSerializer() {
        // Utility class
    }

    public static String serialize(MessageEntity message)
            throws JSONException {

        JSONObject json = new JSONObject();

        json.put(
                "messageId",
                message.getMessageId()
        );

        json.put(
                "senderId",
                message.getSenderId()
        );

        if (message.getReceiverId() != null) {
            json.put(
                    "receiverId",
                    message.getReceiverId()
            );
        }

        json.put(
                "messageType",
                message.getMessageType()
        );

        json.put(
                "content",
                message.getContent()
        );

        json.put(
                "timestamp",
                message.getTimestamp()
        );

        if (message.getLatitude() != null) {
            json.put(
                    "latitude",
                    message.getLatitude()
            );
        }

        if (message.getLongitude() != null) {
            json.put(
                    "longitude",
                    message.getLongitude()
            );
        }

        json.put(
                "priority",
                message.getPriority()
        );

        json.put(
                "status",
                message.getStatus()
        );

        json.put(
                "hopCount",
                message.getHopCount()
        );

        return json.toString();
    }

    public static MessageEntity deserialize(
            String jsonString)
            throws JSONException {

        JSONObject json =
                new JSONObject(jsonString);

        MessageEntity message =
                new MessageEntity();

        message.setMessageId(
                json.getString("messageId")
        );

        message.setSenderId(
                json.getString("senderId")
        );

        if (json.has("receiverId")
                && !json.isNull("receiverId")) {

            message.setReceiverId(
                    json.getString("receiverId")
            );
        }

        message.setMessageType(
                json.getString("messageType")
        );

        message.setContent(
                json.getString("content")
        );

        message.setTimestamp(
                json.getLong("timestamp")
        );

        if (json.has("latitude")
                && !json.isNull("latitude")) {

            message.setLatitude(
                    json.getDouble("latitude")
            );
        }

        if (json.has("longitude")
                && !json.isNull("longitude")) {

            message.setLongitude(
                    json.getDouble("longitude")
            );
        }

        message.setPriority(
                json.getInt("priority")
        );

        message.setStatus(
                json.getString("status")
        );

        message.setHopCount(
                json.getInt("hopCount")
        );

        return message;
    }
}