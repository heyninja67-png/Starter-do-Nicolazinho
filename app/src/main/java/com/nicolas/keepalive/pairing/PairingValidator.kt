package com.nicolas.keepalive.pairing

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Valida um código derivado do manifesto da versão.
 * Não há segredo fixo no APK: o vínculo é baseado no manifesto importado.
 *
 * Protocolo:
 * token = HMAC-SHA256(sha256DoPayload, pairingPublicId + ":" + nonce)
 * A versão importada fornece pairingPublicId e payloadSha256.
 */
object PairingValidator {
    fun fingerprint(payloadSha256: String, pairingPublicId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$payloadSha256:$pairingPublicId".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun validateCode(payloadSha256: String, pairingPublicId: String, nonce: String, code: String): Boolean {
        val key = payloadSha256.toByteArray()
        val data = "$pairingPublicId:$nonce".toByteArray()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val expected = mac.doFinal(data).joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(expected.toByteArray(), code.trim().lowercase().toByteArray())
    }
}
