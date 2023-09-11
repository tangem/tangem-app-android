package com.tangem.domain.balance_hiding

import kotlinx.coroutines.flow.Flow

abstract class DeviceFlipDetector {

    abstract fun deviceFlipEvents(): Flow<Unit>
}
