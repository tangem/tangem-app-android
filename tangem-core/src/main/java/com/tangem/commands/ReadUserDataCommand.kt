package com.tangem.commands

import com.tangem.SessionEnvironment
import com.tangem.SessionError
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

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
class ReadUserDataCommand: Command<ReadUserDataResponse>() {

  override fun serialize(environment: SessionEnvironment): CommandApdu {
    val builder = TlvBuilder()
    builder.append(TlvTag.CardId, environment.card?.cardId)
    builder.append(TlvTag.Pin, environment.pin1)

    return CommandApdu(
            Instruction.ReadUserData, builder.serialize(),
            environment.encryptionMode, environment.encryptionKey
    )
  }

  override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): ReadUserDataResponse {
    val tlvData = apdu.getTlvData(environment.encryptionKey)
            ?: throw SessionError.DeserializeApduFailed()

      val decoder = TlvDecoder(tlvData)
      return ReadUserDataResponse(
          cardId = decoder.decode(TlvTag.CardId),
          userData = decoder.decode(TlvTag.UserData),
          userProtectedData = decoder.decode(TlvTag.UserProtectedData),
          userCounter = decoder.decode(TlvTag.UserCounter),
          userProtectedCounter = decoder.decode(TlvTag.UserProtectedCounter)
      )
  }
}