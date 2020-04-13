package com.tangem.tangemtest.ucase.variants.responses.converter

import com.tangem.commands.Card
import com.tangem.commands.SignResponse
import com.tangem.commands.personalization.DepersonalizeResponse
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder
import java.lang.reflect.Type

class ConvertersStore : BaseTypedHolder<Type, Any>() {
    init {
//        register(CompletionResult.Success<Card>::class.java, ReadEventConverter())
        register(SignResponse::class.java, SignResponseConverter())
        register(Card::class.java, CardConverter())
        register(DepersonalizeResponse::class.java, DepersonalizeResponseConverter())
    }
}