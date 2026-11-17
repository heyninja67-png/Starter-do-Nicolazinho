package com.nicolas.keepalive.versions

import java.security.MessageDigest

data class VersionManifest(
    val version: String,
    val pairingPublicId: String,
    val payloadSha256: String
)

fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
