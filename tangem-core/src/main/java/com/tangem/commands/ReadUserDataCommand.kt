package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

class ReadUserDataResponse(
    /**
     * CID, Unique Tangem card ID number.
     */
    val cardId: String,

    /**
     * Data defined by user's App.
     */
    val userData: ByteArray,

    /**
     * Data defined by user's App (confirmed by PIN2).
     */
    val userProtectedData: ByteArray,

    /**
     * Counter initialized by user's App and increased on every signing of new transaction
     */
    val userCounter: Int,

    /**
     * Counter initialized by user's App (confirmed by PIN2) and increased on every signing of new transaction
     */
    val userProtectedCounter: Int

): CommandResponse

/**
 * This command returns two up to 512-byte User_Data, User_Protected_Data and two counters User_Counter and
 * User_Protected_Counter fields.
 * User_Data and User_ProtectedData are never changed or parsed by the executable code the Tangem COS.
 * The App defines purpose of use, format and it's payload. For example, this field may contain cashed information
 * from blockchain to accelerate preparing new transaction.
 * User_Counter and User_ProtectedCounter are counters, that initial values can be set by App and increased on every signing
 * of new transaction (on SIGN command that calculate new signatures). The App defines purpose of use.
 * For example, this fields may contain blockchain nonce value.
 */
class ReadUserDataCommand: CommandSerializer<ReadUserDataResponse>() {

  override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
    val builder = TlvBuilder()
    builder.append(TlvTag.CardId, cardEnvironment.cardId)
    builder.append(TlvTag.Pin, cardEnvironment.pin1)

    return CommandApdu(
            Instruction.ReadUserData, builder.serialize(),
            cardEnvironment.encryptionMode, cardEnvironment.encryptionKey
    )
  }

  override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): ReadUserDataResponse? {
    val tlvData = responseApdu.getTlvData(cardEnvironment.encryptionKey) ?: return null

    return try {
      val mapper = TlvMapper(tlvData)
      ReadUserDataResponse(
          cardId = mapper.map(TlvTag.CardId),
          userData = mapper.map(TlvTag.UserData),
          userProtectedData = mapper.map(TlvTag.UserProtectedData),
          userCounter = mapper.map(TlvTag.UserCounter),
          userProtectedCounter = mapper.map(TlvTag.UserProtectedCounter)
      )
    } catch (exception: Exception) {
      throw TaskError.SerializeCommandError()
    }
  }
}