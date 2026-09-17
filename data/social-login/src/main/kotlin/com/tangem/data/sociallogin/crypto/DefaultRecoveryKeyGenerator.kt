package com.tangem.data.sociallogin.crypto

import com.tangem.domain.sociallogin.crypto.RecoveryKeyGenerator
import com.tangem.domain.sociallogin.models.RecoveryKey
import java.security.SecureRandom
import javax.inject.Inject

internal class DefaultRecoveryKeyGenerator @Inject constructor() : RecoveryKeyGenerator {

    private val random = SecureRandom()

    override fun generate(): RecoveryKey = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES).also(random::nextBytes))
}