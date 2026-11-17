package com.nicolas.keepalive.database

/**
 * Modelo mínimo persistente. A implementação de Room pode ser adicionada
 * quando o armazenamento de versões for conectado ao importador SAF.
 */
data class InstalledVersion(
    val id: String,
    val version: String,
    val payloadSha256: String,
    val active: Boolean
)
