package com.tangem.devkit.ucase.variants.responses.converter

import com.tangem.commands.SignResponse
import com.tangem.commands.personalization.DepersonalizeResponse
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit._arch.structure.impl.TextItem
import com.tangem.devkit.ucase.variants.responses.DepersonalizeId
import com.tangem.devkit.ucase.variants.responses.SignId
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class SignResponseConverter : BaseResponseConverter<SignResponse>() {

    override fun convert(from: SignResponse): List<Item> {
        return listOf(
                TextItem(SignId.cid, from.cardId),
                TextItem(SignId.walletSignedHashes, valueToString(from.walletSignedHashes)),
                TextItem(SignId.walletRemainingSignatures, valueToString(from.walletRemainingSignatures)),
                TextItem(SignId.signature, fieldConverter.byteArray(from.signature))
        )
    }
}

class DepersonalizeResponseConverter : BaseResponseConverter<DepersonalizeResponse>() {
    override fun convert(from: DepersonalizeResponse): List<Item> {
        return listOf(TextItem(DepersonalizeId.isSuccess, stringOf(from.success)))
    }
}