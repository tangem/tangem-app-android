package com.tangem.tap.moshi

import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.local.moshi.SdkMoshiConfigurer
import com.tangem.domain.models.scan.serialization.addScanResponseAdapters

/** The @IntoSet [SdkMoshiConfigurer]s of tangem-sdk-api and onboarding, whose DI modules are internal to them */
internal object TestSdkMoshiConfigurers {

    val cardSdk = SdkMoshiConfigurer { builder ->
        builder.apply {
            MoshiJsonConverter.getTangemSdkAdapters().forEach { add(it) }
            MoshiJsonConverter.getTangemSdkTypedAdapters().forEach { (type, adapter) -> add(type, adapter) }
        }
    }

    val scanResponse = SdkMoshiConfigurer { it.addScanResponseAdapters() }
}