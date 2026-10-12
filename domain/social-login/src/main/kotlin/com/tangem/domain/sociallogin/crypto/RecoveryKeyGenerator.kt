package com.tangem.domain.sociallogin.crypto

import com.tangem.domain.sociallogin.models.RecoveryKey

interface RecoveryKeyGenerator {

    fun generate(): RecoveryKey
}