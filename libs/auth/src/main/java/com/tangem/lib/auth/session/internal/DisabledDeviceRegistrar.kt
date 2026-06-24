package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.left
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.lib.auth.session.DeviceRegistrationError
import com.tangem.utils.annotations.RemoveWithToggle

@RemoveWithToggle("AND_15438_BACKEND_AUTHENTICATION_ENABLED")
internal object DisabledDeviceRegistrar : DeviceRegistrar {

    override suspend fun register(): Either<DeviceRegistrationError, Unit> {
        return DeviceRegistrationError.Disabled.left()
    }
}