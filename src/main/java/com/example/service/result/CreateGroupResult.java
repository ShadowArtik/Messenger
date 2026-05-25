package com.example.service.result;

public enum CreateGroupResult {
    SUCCESS,
    EMPTY_GROUP_NAME,
    EMPTY_MEMBERS,
    USER_NOT_FOUND,
    ONLY_SELF_SELECTED,
    DATABASE_ERROR
}
