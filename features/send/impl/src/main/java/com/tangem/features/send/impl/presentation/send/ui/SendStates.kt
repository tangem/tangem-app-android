package com.tangem.features.send.impl.presentation.send.ui

/**
 * Screen states of Send
 */
enum class SendStates {
    Amount,
    Recipient,
    Fee,
    Filled,
    Done,
    ;

    companion object {

        /** Get next [SendStates] */
        fun SendStates.next(): SendStates {
            return if (this.ordinal < SendStates.values().last().ordinal) {
                SendStates.values()[this.ordinal + 1]
            } else {
                this
            }
        }

        /** Get previous [SendStates] */
        fun SendStates.previous(): SendStates {
            return if (this.ordinal > 0) {
                SendStates.values()[this.ordinal - 1]
            } else {
                this
            }
        }
    }
}