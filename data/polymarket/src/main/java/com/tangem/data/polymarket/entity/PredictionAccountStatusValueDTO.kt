package com.tangem.data.polymarket.entity

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * On-disk representation of the prediction account status, kept separate from the
 * [com.tangem.domain.models.account.PredictionAccountStatusValue] domain model.
 *
 * Every name here is pinned with [SerialName], so the file format survives a rename or a package move of the
 * domain type. Without that the discriminator would be the domain class's fully qualified name, and a rename
 * that compiles and passes every test would silently wipe the cache of every wallet on every device.
 *
 * Three things are deliberately absent. There is no status source: a restored value has not been refreshed in
 * this session by definition, so the converter always brings it back as cached rather than trusting whatever
 * was written last time. There is no fiat rate: it belongs to the app's selected currency, which the user can
 * change while this cache stays valid. And there is no representation of the loading and error states, so a
 * momentary failure cannot be persisted and then read back as if it were the account's real state.
 */
@Serializable
internal sealed interface PredictionAccountStatusValueDTO {

    /** The wallet has no prediction deposit wallet. */
    @Serializable
    @SerialName("not_onboarded")
    data object NotOnboarded : PredictionAccountStatusValueDTO

    /** The deposit wallet is being set up and cannot hold funds yet. */
    @Serializable
    @SerialName("onboarding")
    data class Onboarding(@SerialName("stage") val stage: Stage) : PredictionAccountStatusValueDTO

    /** The deposit wallet is ready and holds a collateral balance. */
    @Serializable
    @SerialName("active")
    data class Active(
        @SerialName("balance") val balance: SerializedBigDecimal,
        @SerialName("is_trading_allowed") val isTradingAllowed: Boolean,
    ) : PredictionAccountStatusValueDTO

    /** The stages a deposit wallet goes through before it is ready to trade. */
    @Serializable
    enum class Stage {

        @SerialName("deploying")
        DEPLOYING,

        @SerialName("deployed")
        DEPLOYED,

        @SerialName("approving")
        APPROVING,
    }
}