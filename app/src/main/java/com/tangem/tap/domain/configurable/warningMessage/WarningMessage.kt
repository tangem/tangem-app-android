package com.tangem.tap.domain.configurable.warningMessage

import androidx.annotation.StringRes
import com.squareup.moshi.Json
import com.tangem.blockchain.common.Blockchain

/**
[REDACTED_AUTHOR]
 */
data class WarningMessage(
    val title: String,
    val message: String,
    val type: Type,
    val priority: Priority,
    val location: List<Location>,
    private val blockchains: List<String>?,
    @StringRes val titleResId: Int? = null,
    @StringRes val messageResId: Int? = null,
    val origin: Origin = Origin.Remote,
    @StringRes val buttonTextId: Int? = null,
    val messageFormatArg: String? = null
) {
    val blockchainList: List<Blockchain>? by lazy {
        blockchains?.map { Blockchain.fromId(it.toUpperCase()) }
    }


    var isHidden = false

    enum class Priority {
        @Json(name = "critical")
        Critical,

        @Json(name = "warning")
        Warning,

        @Json(name = "info")
        Info
    }

    enum class Type {
        @Json(name = "permanent")
        Permanent,     // нельзя скрыть

        @Json(name = "temporary")
        Temporary,      // можно скрыть (кнопка ОК)

        AppRating,

        TestCard

    }

    enum class Location {
        @Json(name = "main")
        MainScreen,

        @Json(name = "send")
        SendScreen
    }

    enum class Origin {
        Local, Remote
    }
}