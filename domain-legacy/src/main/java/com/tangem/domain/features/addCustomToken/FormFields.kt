package com.tangem.domain.features.addCustomToken

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.BaseDataField
import com.tangem.domain.common.form.Field
import com.tangem.domain.common.form.FieldId

/**
 * Created by Anton Zhilenkov on 30/03/2022.
 */
enum class CustomTokenFieldId : FieldId {
    ContractAddress,
    Network,
    Name,
    Symbol,
    Decimals,
    DerivationPath,
}

data class TokenField(
    override val id: FieldId,
) : BaseDataField<String>(id, Field.Data("", false))

data class TokenBlockchainField(
    override val id: FieldId,
    val itemList: List<Blockchain>,
) : BaseDataField<Blockchain>(id, Field.Data(Blockchain.Unknown, false))

data class TokenDerivationPathField(
    override val id: FieldId,
    val itemList: List<Blockchain>,
) : BaseDataField<Blockchain>(id, Field.Data(Blockchain.Unknown, false))
