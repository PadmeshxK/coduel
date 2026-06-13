package com.coduel.model.constant;

public enum Errors {

    // ===== execution (100–) =====
    ERR_100("Language {} is not supported"),
    ERR_101("Code execution failed: {}"),
    ERR_102("Server is at execution capacity; please retry shortly"),

    // ===== problem =====
    ERR_103("No problem found with slug {}"),
    ERR_104("A problem with slug {} already exists"),
    ERR_105("Page size can't be more than {} or less than 1, given page size {}"),

    // ===== submission =====
    ERR_106("No submission found with id {}"),

    ERR_107("No problem found with id {}"),

    // ===== user =====
    ERR_108("No user found with id {}"),
    ERR_109("No user found for the authenticated account"),

    // ===== match =====
    ERR_110("No match found with id {}"),

    // ===== matchmaking =====
    ERR_111("No problems available to start a match"),
    ERR_112("You are not a participant of match {}");

    private final String message;

    Errors(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
