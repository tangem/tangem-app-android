package com.tangem.tap.features.details.redux

import com.tangem.commands.Card
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class SetCard(val card: Card): DetailsAction()

}