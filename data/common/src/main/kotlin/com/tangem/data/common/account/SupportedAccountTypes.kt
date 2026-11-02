package com.tangem.data.common.account

import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import javax.inject.Inject

/**
 * The account record types this build works with. A record of any other type still sits in the backend document,
 * but the app neither shows it nor builds currencies for it, and keeps it untouched on save.
 */
class SupportedAccountTypes @Inject constructor(
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles,
) {

    val types: Set<WalletAccountDTO.Type>
        get() = buildSet {
            add(WalletAccountDTO.Type.CRYPTO)
            if (jointAccountFeatureToggles.isJointAccountEnabled) add(WalletAccountDTO.Type.JOINT)
            if (collectiblesFeatureToggles.isCollectiblesEnabled) add(WalletAccountDTO.Type.COLLECTIBLES)
        }

    fun isSupported(type: WalletAccountDTO.Type): Boolean = type in types

    fun isSupported(record: WalletAccountDTO): Boolean = isSupported(record.type ?: WalletAccountDTO.Type.CRYPTO)
}