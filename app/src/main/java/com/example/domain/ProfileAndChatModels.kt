package com.example.domain

import java.security.MessageDigest

object VirtualIdGenerator {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val VALID_REGEX = Regex("^VGD-[A-Z0-9]{4,12}$")

    fun fromUid(uid: String): String {
        val clean = uid.trim()
        if (clean.isEmpty()) return "VGD-PLAYER"
        val digest = MessageDigest.getInstance("SHA-256").digest(clean.toByteArray(Charsets.UTF_8))
        val chars = CharArray(6) { idx ->
            val unsigned = digest[idx].toInt() and 0xFF
            ALPHABET[unsigned % ALPHABET.length]
        }
        return "VGD-${String(chars)}"
    }

    fun normalize(rawInput: String): String {
        val upper = rawInput.trim().uppercase().replace(" ", "")
        if (upper.isEmpty()) return ""
        return if (upper.startsWith("VGD-")) upper else "VGD-$upper"
    }

    fun isValid(virtualId: String): Boolean = VALID_REGEX.matches(virtualId)
}

object GenderOptions {
    val entries = listOf("Male", "Female", "Other", "Prefer not to say")
}

data class VirtualPlayerLookup(
    val virtualId: String,
    val userId: String,
    val displayName: String,
    val nickname: String = "",
    val gender: String = "",
    val photoBase64: String = ""
) {
    val preferredName: String
        get() = if (nickname.isNotBlank()) "$displayName ($nickname)" else displayName
}

data class ChatThread(
    val chatId: String,
    val participantIds: List<String>,
    val peerUserId: String,
    val peerVirtualId: String,
    val peerName: String,
    val peerPhotoBase64: String = "",
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    val lastMessageAtMillis: Long = 0L
)

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderVirtualId: String,
    val senderName: String,
    val text: String,
    val timestampMillis: Long,
    val isFromCurrentUser: Boolean
)
