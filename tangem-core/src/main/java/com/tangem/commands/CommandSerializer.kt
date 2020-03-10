package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.toInt
import com.tangem.common.tlv.TlvTag

/**
 * Simple interface for responses received after sending commands to Tangem cards.
 */
interface CommandResponse

/**
 * Abstract class for all Tangem card commands.
 */
abstract class CommandSerializer<T : CommandResponse> {

    /**
     * Serializes data into a [List] of [com.tangem.common.tlv.Tlv],
     * then creates [CommandApdu] with this data.
     *
     * @return Command data that can be converted to raw bytes with a method [CommandApdu.toBytes].
     */
    abstract fun serialize(cardEnvironment: CardEnvironment): CommandApdu

    /**
     * Deserializes data, received from a card and stored in [ResponseApdu],
     * into a [List] of [com.tangem.common.tlv.Tlv]. Then this method maps it into a [CommandResponse].
     *
     * @return Card response, converted to a [CommandResponse] of a type [T].
     */
    abstract fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): T?

    /**
     * Helper method to parse security delay information received from a card.
     *
     * @return Remaining security delay in milliseconds.
     */
    fun deserializeSecurityDelay(responseApdu: ResponseApdu, cardEnvironment: CardEnvironment): Int? {
        val tlv = responseApdu.getTlvData()
        return tlv?.find { it.tag == TlvTag.Pause }?.value?.toInt()
    }
}