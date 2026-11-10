package com.example.model

sealed class ShizukuStatus {
    object Checking : ShizukuStatus()
    object NotInstalled : ShizukuStatus()
    object ServiceStopped : ShizukuStatus()
    object Unauthorized : ShizukuStatus()
    data class Authorized(
        val version: Int,
        val uid: Int = 2000,
        val seContext: String = ""
    ) : ShizukuStatus()
    data class Error(val message: String) : ShizukuStatus()

    val isAvailable: Boolean
        get() = this is Authorized

    val label: String
        get() = when (this) {
            is Checking -> "Verificando..."
            is NotInstalled -> "Não instalado"
            is ServiceStopped -> "Serviço parado"
            is Unauthorized -> "Não autorizado"
            is Authorized -> "Privilegiado (v$version • UID $uid)"
            is Error -> "Erro de conexão"
        }
}
