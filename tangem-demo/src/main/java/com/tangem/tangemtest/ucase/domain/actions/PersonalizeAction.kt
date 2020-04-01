package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.personalize.dto.DefaultPersonalizeParams

/**
[REDACTED_AUTHOR]
 */
class PersonalizeAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        val cardConfig = attrs.payload.remove(cardConfig) as? CardConfig
                ?: throw IllegalArgumentException("CardConfig must be in the payloads of the ParamsManager")

        val issuer = DefaultPersonalizeParams.issuer()
        val acquirer = DefaultPersonalizeParams.acquirer()
        val manufacturer = DefaultPersonalizeParams.manufacturer()

        attrs.cardManager.personalize(cardConfig, issuer, manufacturer, acquirer) {
            handleResponse(it, null, attrs, callback)
        }
    }

    companion object {
        val cardConfig = "cardConfig"
    }
}