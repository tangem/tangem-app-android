package com.tangem.data.yield.supply.status.store

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class YieldModuleStatusDM(
    @SerialName("network_id") val networkId: String,
    @SerialName("derivation_path_type") val derivationPathType: DerivationPathType,
    @SerialName("derivation_path_value") val derivationPathValue: String? = null,
    @SerialName("token_address") val tokenAddress: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("is_initialized") val isInitialized: Boolean,
    @SerialName("is_allowed_to_spend") val isAllowedToSpend: Boolean,
    @SerialName("effective_protocol_balance") val effectiveProtocolBalance: SerializedBigDecimal? = null,
) {

    @Serializable
    enum class DerivationPathType {
        @SerialName("card")
        CARD,

        @SerialName("custom")
        CUSTOM,

        @SerialName("joint")
        JOINT,

        @SerialName("none")
        NONE,
    }
}

internal fun YieldModuleStatusDM.toDomain(source: StatusSource): YieldModuleStatus = YieldModuleStatus(
    networkId = Network.ID(value = networkId, derivationPath = toDerivationPath()),
    tokenAddress = tokenAddress,
    isActive = isActive,
    isInitialized = isInitialized,
    isAllowedToSpend = isAllowedToSpend,
    effectiveProtocolBalance = effectiveProtocolBalance,
    source = source,
)

internal fun YieldModuleStatus.toDataModel(): YieldModuleStatusDM = YieldModuleStatusDM(
    networkId = networkId.rawId.value,
    derivationPathType = when (networkId.derivationPath) {
        is Network.DerivationPath.Card -> YieldModuleStatusDM.DerivationPathType.CARD
        is Network.DerivationPath.Custom -> YieldModuleStatusDM.DerivationPathType.CUSTOM
        is Network.DerivationPath.Joint -> YieldModuleStatusDM.DerivationPathType.JOINT
        Network.DerivationPath.None -> YieldModuleStatusDM.DerivationPathType.NONE
    },
    derivationPathValue = networkId.derivationPath.value,
    tokenAddress = tokenAddress,
    isActive = isActive,
    isInitialized = isInitialized,
    isAllowedToSpend = isAllowedToSpend,
    effectiveProtocolBalance = effectiveProtocolBalance,
)

internal val YieldModuleStatusDM.key: YieldModuleStatus.Key
    get() = YieldModuleStatus.Key(
        networkId = Network.ID(value = networkId, derivationPath = toDerivationPath()),
        tokenAddress = tokenAddress,
    )

private fun YieldModuleStatusDM.toDerivationPath(): Network.DerivationPath {
    val value = derivationPathValue ?: return Network.DerivationPath.None

    return when (derivationPathType) {
        YieldModuleStatusDM.DerivationPathType.CARD -> Network.DerivationPath.Card(value)
        YieldModuleStatusDM.DerivationPathType.CUSTOM -> Network.DerivationPath.Custom(value)
        YieldModuleStatusDM.DerivationPathType.JOINT -> Network.DerivationPath.Joint(value)
        YieldModuleStatusDM.DerivationPathType.NONE -> Network.DerivationPath.None
    }
}