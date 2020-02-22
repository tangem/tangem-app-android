package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

/**
[REDACTED_AUTHOR]
 */

class WriteUserDataResponse(
		/**
		 * CID, Unique Tangem card ID number.
		 */
		val cardId: String
): CommandResponse

/**
 * This command write some of User_Data, User_ProtectedData, User_Counter and User_ProtectedCounter fields.
 * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
 * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
 * from blockchain to accelerate preparing new transaction.
 * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
 * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
 * For example, this fields may contain blockchain nonce value.
 *
 * Writing of User_Counter and User_Data protected only by PIN1.
 * User_ProtectedCounter and User_ProtectedData additionaly need PIN2 to confirmation.
 */
class WriteUserDataCommand(
		private val userData: ByteArray? = null,
		private val userProtectedData: ByteArray? = null,
		private val userCounter: Int? = null,
		private val userProtectedCounter: Int? = null
): CommandSerializer<WriteUserDataResponse>() {
	override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
		val serializedTlv = TlvBuilder().apply {
			append(TlvTag.CardId, cardEnvironment.cardId)
			append(TlvTag.Pin, cardEnvironment.pin1)
			append(TlvTag.UserData, userData)
			append(TlvTag.UserCounter, userCounter)
			append(TlvTag.UserProtectedData, userProtectedData)
			append(TlvTag.UserProtectedCounter, userProtectedCounter)
			if (userProtectedCounter != null || userProtectedData != null)
				append(TlvTag.Pin2, cardEnvironment.pin2)
		}.serialize()
		return CommandApdu(Instruction.ReadIssuerData, serializedTlv)
	}

	override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): WriteUserDataResponse? {
		val tlvData = responseApdu.getTlvData() ?: return null

		return try {
			WriteUserDataResponse(TlvMapper(tlvData).map(TlvTag.CardId))
		} catch (exception: Exception) {
			throw TaskError.SerializeCommandError()
		}
	}
}