package com.tangem.devkit.ucase.variants.responses.converter

import com.tangem.commands.*
import com.tangem.commands.personalization.DepersonalizeResponse
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder
import java.lang.reflect.Type

class ConvertersStore : BaseTypedHolder<Type, Any>() {
    init {
//        register(CompletionResult.Success<Card>::class.java, ReadEventConverter())
        register(SignResponse::class.java, SignResponseConverter())
        register(Card::class.java, CardConverter())
        register(DepersonalizeResponse::class.java, DepersonalizeResponseConverter())
        register(CreateWalletResponse::class.java, CreateWalletResponseConverter())
        register(PurgeWalletResponse::class.java, PurgeWalletResponseConverter())
        register(ReadIssuerDataResponse::class.java, ReadIssuerDataResponseConverter())
        register(ReadIssuerExtraDataResponse::class.java, ReadIssuerExtraDataResponseConverter())
        register(WriteIssuerDataResponse::class.java, WriteIssuerDataResponseConverter())
        register(ReadUserDataResponse::class.java, ReadUserDataResponseConverter())
        register(WriteUserDataResponse::class.java, WriteUserDataResponseConverter())
    }
}