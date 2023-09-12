package com.tangem.domain.balancehiding

import kotlinx.coroutines.flow.Flow

abstract class DeviceFlipDetector {

    abstract fun deviceFlipEvents(): Flow<Unit>
}
