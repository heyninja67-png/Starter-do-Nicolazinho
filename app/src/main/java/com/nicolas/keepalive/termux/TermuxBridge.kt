package com.nicolas.keepalive.termux

/**
 * Camada segura para integração com Termux.
 *
 * O aplicativo não executa shell arbitrário vindo de ZIPs.
 * Os comandos futuros devem ser uma lista fixa e auditada, usando
 * Termux:Tasker/Run Command conforme a configuração disponível.
 */
object TermuxBridge {
    const val PACKAGE_TERMUX = "com.termux"
    const val PACKAGE_TASKER = "com.termux.tasker"

    fun allowedActions(): List<String> = listOf(
        "start_bot",
        "stop_bot",
        "restart_bot",
        "status",
        "logs"
    )
}
