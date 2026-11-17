package com.nicolas.keepalive

import com.nicolas.keepalive.pairing.PairingValidator
import org.junit.Assert.*
import org.junit.Test

class PairingValidatorTest {
    @Test fun fingerprintIsDeterministic() {
        assertEquals(
            PairingValidator.fingerprint("abc", "v1"),
            PairingValidator.fingerprint("abc", "v1")
        )
    }
}
