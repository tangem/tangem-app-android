package com.tangem.features.tangempay.multichain

import com.tangem.domain.models.account.PaymentNetworkStatus

/**
 * Pure routing decision for `onClickReceive`: whether the multichain Choose-network bottom sheet
 * should be shown instead of the legacy single-network Receive bottom sheet.
 *
 * @param isMultichainEnabled [com.tangem.features.tangempay.TangemPayFeatureToggles.isAccountMultichainEnabled]
 * @param networks [com.tangem.domain.models.account.PaymentAccountStatusValue.Loaded.networks]
 */
internal fun shouldUseChooseNetwork(isMultichainEnabled: Boolean, networks: List<PaymentNetworkStatus>): Boolean =
    isMultichainEnabled && networks.isNotEmpty()