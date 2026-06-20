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
    ERR_112("You are not a participant of match {}"),
    ERR_125("User {} already forfeited for the match {}"),

    // ===== friends =====
    ERR_113("No friend request found with id {}"),
    ERR_114("You can't send a friend request to yourself"),
    ERR_115("A friend request or friendship already exists with user {}"),

    // ===== private rooms =====
    ERR_116("Room is full — maximum {} players allowed"),
    ERR_117("Only the room host can perform this action"),
    ERR_118("At least 2 players are required to start a match"),
    ERR_119("This room is no longer open for changes"),
    ERR_120("No room found with id {}"),
    ERR_121("You are not a member of room {}"),
    ERR_122("Everyone must be ready before the match can start"),
    ERR_123("A match is already in progress in this room"),
    ERR_124("A player is already in another match — they must finish it first");

    private final String message;

    Errors(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
