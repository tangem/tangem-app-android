package com.tangem.features.earn.product.api.product

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.product.api.session.EarnAction
import java.math.BigDecimal

/**
 * A light summary of one product: read by `earn:impl` and products, not by hosts.
 *
 * @property position not null when the user has a position, so the product is active
 * @property route where a click on the badge or the For You card leads if this product wins: an existing screen, or
 * `null` to open Earn
 */
data class EarnProductSummary(
    val productId: EarnProductId,
    val rateText: TextReference,
    val risk: EarnRisk?,
    val availability: ProductAvailability,
    val rate: EarnProductRate?,
    val position: EarnPositionSummary?,
    val source: StatusSource,
    val onClickAction: EarnAction,
    val route: Route? = null,
)

enum class EarnRisk { Low, Medium, High }

enum class ProductAvailability { Available, Full, Unavailable }

/** @property cryptoAmount `null`: the position exists but the source has no amount */
data class EarnPositionSummary(val cryptoAmount: BigDecimal?)