package com.tangem.lib.auth.devicekey.internal

import arrow.core.None
import arrow.core.Option
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.devicekey.DeviceKeySigningException

internal object DisabledDeviceKeyManager : DeviceKeyManager {

    override suspend fun generateIfMissing(): Boolean = false

    override suspend fun getPublicKey(): Option<ByteArray> = None

    override suspend fun sign(data: ByteArray): ByteArray =
        throw DeviceKeySigningException("DeviceKeyManager is disabled: backend authentication feature toggle is off")
}