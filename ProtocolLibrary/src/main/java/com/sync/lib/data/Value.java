package com.sync.lib.data;

public enum Value {
    TYPE("type"),
    TOPIC("to"),
    PRIORITY("priority"),
    DATA("data"),
    DEVICE_NAME("device_name"),
    DEVICE_ID("device_id"),
    SEND_DEVICE_ID("send_device_id"),
    SEND_DEVICE_NAME("send_device_name"),
    PAIR_ACCEPT("pair_accept"),
    SENT_DATE("date"),
    REQUEST_DATA("request_data"),
    RECEIVE_DATA("receive_data"),
    REQUEST_ACTION("request_action"),
    ACTION_ARGS("action_args"),
    ENCRYPTED("encrypted"),
    IS_FIRST_FETCHED("is_first_fetch"),
    ENCRYPTED_DATA("encrypted_data"),;

    private final String id;
    Value(String id) { this.id = id; }
    public String id() { return id; }
}