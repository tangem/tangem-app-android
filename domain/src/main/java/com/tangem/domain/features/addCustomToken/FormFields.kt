package com.tangem.domain.features.addCustomToken

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.BaseDataField
import com.tangem.domain.common.form.FieldId

/**
[REDACTED_AUTHOR]
 */
enum class CustomTokenFieldId : FieldId {
    ContractAddress,
    Network,
    Name,
    Symbol,
    Decimals,
    DerivationPath,
}

data class TokenNetworkField(
    override val id: FieldId,
    val itemList: List<Blockchain>,
    override val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : BaseDataField<Blockchain>(id, Blockchain.Unknown)

data class TokenField(
    override val id: FieldId,
    override val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : BaseDataField<String>(id, "")

data class TokenDerivationPathField(
    override val id: FieldId,
    override val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : BaseDataField<String>(id, "")
