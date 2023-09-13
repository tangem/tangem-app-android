package com.tangem.domain.balancehiding

import kotlinx.coroutines.flow.Flow

interface DeviceFlipDetector {

    fun deviceFlipEvents(): Flow<Unit>
}
