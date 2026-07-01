package com.tangem.datasource.local.txhistory.db.entity.express

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted representation of an express provider.
 *
 * Mirrors [com.tangem.datasource.api.express.models.response.ExchangeProvider]. Mapped into
 * [com.tangem.domain.express.models.ExpressProvider] when read back.
 */
@Entity(tableName = "express_provider")
data class ExpressProviderEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    /** Raw provider type (`dex` / `cex` / `dex-bridge` / `onramp`). Typed view: ExpressProviderType. */
    @ColumnInfo(name = "type")
    val type: String,

    /** Large logo image URL. */
    @ColumnInfo(name = "image_large")
    val imageLarge: String,

    /** Small logo image URL. */
    @ColumnInfo(name = "image_small")
    val imageSmall: String,

    @ColumnInfo(name = "terms_of_use")
    val termsOfUse: String?,

    @ColumnInfo(name = "privacy_policy")
    val privacyPolicy: String?,

    @ColumnInfo(name = "is_recommended")
    val isRecommended: Boolean,

    /** Raw decimal string (BigDecimal) or `null`. */
    @ColumnInfo(name = "slippage")
    val slippage: String?,

    @ColumnInfo(name = "is_exchange_only_within_single_address")
    val isExchangeOnlyWithinSingleAddress: Boolean,

    @ColumnInfo(name = "is_extra_id_supported")
    val isExtraIdSupported: Boolean,
)