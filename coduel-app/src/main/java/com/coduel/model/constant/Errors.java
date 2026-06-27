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
    ERR_126("That display name is already taken — please pick another"),

    // ===== match =====
    ERR_110("No match found with id {}"),

    // ===== matchmaking =====
    ERR_111("No problems available to start a match"),
    ERR_112("You are not a participant of match {}"),
    ERR_125("User {} already forfeited for the match {}"),

    // ===== friends =====
    ERR_113("No friend request found with id {}"),
    ERR_130("Acceptor userId doesn't match Addressee userId for requestId {}"),
    ERR_131("Friend requestId {} is not pending"),
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
    ERR_124("A player is already in another match — they must finish it first"),

    // ===== duel challenges =====
    ERR_127("You can only challenge your friends"),
    ERR_128("This challenge no longer exists — it may have expired"),
    ERR_129("You can't challenge yourself"),

    // ===== chat / direct messages =====
    ERR_132("No conversation found with id {}"),
    ERR_133("You are not a participant of conversation {}"),
    ERR_134("You can only message your friends"),
    ERR_135("You can't message yourself"),
    ERR_136("No message found with id {}"),
    ERR_137("That reaction isn't valid"),
    ERR_138("You can only reply to a message in this conversation"),
    ERR_139("You can only edit or delete your own messages"),
    ERR_140("This message can no longer be edited"),
    ERR_141("You can't pin a deleted message"),
    ERR_142("You can pin at most {} messages — unpin one first"),
    ERR_143("No image was uploaded"),
    ERR_144("Image is too large — maximum {} MB"),
    ERR_145("Only PNG, JPEG, GIF or WebP images are allowed"),
    ERR_146("Failed to store the image — please try again"),
    ERR_147("A message needs text or an image"),
    ERR_148("A shared problem reference is required"),
    ERR_149("Only audio voice notes are allowed"),

    ERR_150("You are not invited to this room");

    private final String message;

    Errors(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
