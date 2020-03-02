package com.tangem.common.tlv

import com.google.common.truth.Truth.assertThat
import com.tangem.commands.*
import com.tangem.common.extensions.hexToBytes
import com.tangem.tasks.TaskError
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class TlvMapperTest {

    private val rawData = byteArrayOf(1, 8, -53, 34, 0, 0, 0, 2, 115, 116, 32, 11, 83, 77, 65, 82, 84, 32, 67, 65, 83, 72, 0, 2, 1, 2, -128, 6, 50, 46, 49, 49, 114, 0, 3, 65, 4, -49, 11, -50, -66, -121, -25, -2, 65, 65, -13, 14, 49, 27, -82, -33, -85, -113, 65, 20, 8, -39, -75, 57, 45, 65, -31, 35, 44, 38, 40, 63, -44, 113, -45, -75, -95, -118, 118, 29, 65, 117, -24, -53, 82, -72, 91, -20, -96, -77, -103, -14, -63, 52, -127, -123, -27, -16, -128, -67, -3, -104, -26, -22, 65, 10, 4, 0, 0, 126, 33, 12, 90, -127, 2, 0, 41, -126, 4, 7, -29, 5, 2, -125, 7, 84, 65, 78, 71, 69, 77, 0, -124, 3, 69, 84, 72, -122, 64, 111, -103, 48, -114, -40, 18, -103, 26, -102, -12, -38, -78, -90, -9, -98, 88, -47, -100, -24, 24, -105, -70, -72, 6, 94, -96, -77, 11, -123, -28, -118, 37, 63, 107, -55, -11, 23, -12, 13, -23, -121, -63, 36, -59, 70, 116, 91, -125, -34, -69, 23, -112, 6, 17, 4, -49, 68, -56, 29, -45, 81, 10, 97, 83, 48, 65, 4, -127, -106, -86, 75, 65, 10, -60, 74, 59, -100, -50, 24, -25, -66, 34, 106, -22, 7, 10, -52, -125, -87, -49, 103, 84, 15, -84, 73, -81, 37, 18, -97, 106, 83, -118, 40, -83, 99, 65, 53, -114, 60, 79, -103, 99, 6, 79, 126, 54, 83, 114, -90, 81, -45, 116, -27, -62, 60, -35, 55, -3, 9, -101, -14, 5, 10, 115, 101, 99, 112, 50, 53, 54, 107, 49, 0, 8, 4, 0, 15, 66, 64, 7, 1, 0, 9, 2, 11, -72, 96, 65, 4, -42, -5, -41, -84, -23, 88, 2, 86, -63, -118, -123, -10, -66, -82, -107, -68, -93, 111, 47, 93, -20, -86, 74, 28, 21, 81, 93, -21, -124, -57, -102, 55, 17, 84, -66, -68, -22, -128, 126, -99, -65, -54, -42, 59, -25, -21, -124, 5, 59, -16, -72, 73, 48, 16, -27, 103, -112, -73, 2, 96, -51, 41, -42, 116, 98, 4, 0, 15, 66, 52, 99, 4, 0, 0, 0, 13, 15, 1, 0)

    private val tlvData = Tlv.deserialize(rawData)

    private val tlvMapper = TlvMapper(tlvData!!)

    private val cardDataRaw: ByteArray = tlvMapper.map(TlvTag.CardData)
    private val cardDataMapper = TlvMapper(Tlv.deserialize(cardDataRaw)!!)

    @Test
    fun `map optional when value is present`() {
        val settingsMask: SettingsMask? = tlvMapper.mapOptional(TlvTag.SettingsMask)
        assertThat(settingsMask)
                .isNotNull()
    }

    @Test
    fun `map optional when no tag returns null`() {
        val tokenSymbol: String? = tlvMapper.mapOptional(TlvTag.TokenSymbol)
        assertThat(tokenSymbol)
                .isNull()
    }

    @Test
    fun `map when value is null throws MissingTagException`() {
        assertThrows<TaskError.MissingTag> {
            tlvMapper.map<String>(TlvTag.TokenSymbol)
        }
    }

    @Test
    fun `map optional to wrong type throws WrongTypeException`() {
        assertThrows<TaskError.WrongType> {
            tlvMapper.mapOptional<String?>(TlvTag.CardData)
        }
    }

    @Test
    fun `map to wrong type throws WrongTypeException`() {
        assertThrows<TaskError.WrongType> {
            tlvMapper.map<String>(TlvTag.CardData)
        }
    }

    @Test
    fun `map boolean missing flag returns false`() {
        val terminalIsLinked: Boolean = tlvMapper.map(TlvTag.TerminalIsLinked)
        assertThat(terminalIsLinked)
                .isFalse()
    }

    @Test
    fun `map SettingsMask returns correct value`() {
        val settingsMask: SettingsMask = tlvMapper.map(TlvTag.SettingsMask)
        assertThat(settingsMask)
                .isNotNull()
        assertThat(settingsMask.rawValue)
                .isEqualTo(32289)
        assertThat(settingsMask.contains(SettingsMask.skipSecurityDelayIfValidatedByLinkedTerminal))
                .isFalse()
        assertThat(settingsMask.contains(SettingsMask.isReusable))
                .isTrue()
        assertThat(settingsMask.contains(SettingsMask.allowSwapPIN2))
                .isTrue()
        assertThat(settingsMask.contains(SettingsMask.useDynamicNdef))
                .isTrue()
        assertThat(settingsMask.contains(SettingsMask.forbidPurgeWallet))
                .isFalse()
    }

    @Test
    fun `map SigningMethods single value returns correct value`() {
        val signingMethods: SigningMethod = tlvMapper.map(TlvTag.SigningMethod)
        assertThat(signingMethods.contains(SigningMethod.signHash))
                .isTrue()
    }

    @Test
    fun `map SigningMethods set of methods returns correct value`() {
        val localMapper = TlvMapper(Tlv.deserialize("070195".hexToBytes())!!)

        val signingMethod: SigningMethod = localMapper.map(TlvTag.SigningMethod)
        assertThat(signingMethod.contains(SigningMethod.signHash))
                .isTrue()
        assertThat(signingMethod.contains(SigningMethod.signHashValidatedByIssuer))
                .isTrue()
        assertThat(signingMethod.contains(SigningMethod.signHashValidatedByIssuerAndWriteIssuerData))
                .isTrue()
        assertThat(signingMethod.contains(SigningMethod.signRaw))
                .isFalse()
        assertThat(signingMethod.contains(SigningMethod.signRawValidatedByIssuer))
                .isFalse()
        assertThat(signingMethod.contains(SigningMethod.signRawValidatedByIssuerAndWriteIssuerData))
                .isFalse()
        assertThat(signingMethod.contains(SigningMethod.signPos))
                .isFalse()
    }

    @Test
    fun `map CardStatus returns correct value`() {
        val cardStatus: CardStatus = tlvMapper.map(TlvTag.Status)
        assertThat(cardStatus)
                .isEqualTo(CardStatus.Loaded)
    }

    @Test
    fun `map ProductMask with raw value 5 returns correct value`() {
        val localMapper = TlvMapper(listOf(Tlv(TlvTag.ProductMask, byteArrayOf(5))))
        val productMask: ProductMask = localMapper.map(TlvTag.ProductMask)
        assertThat(productMask.contains(ProductMask.note) && productMask.contains(ProductMask.idCard))
                .isTrue()
    }

    @Test
    fun `map ProductMask with raw value 1 returns correct value`() {
        val localMapper = TlvMapper(listOf(Tlv(TlvTag.ProductMask, byteArrayOf(1))))
        val productMask: ProductMask = localMapper.map(TlvTag.ProductMask)
        assertThat(productMask.contains(ProductMask.note))
                .isTrue()
    }

    @Test
    fun `map Enum with unknown code throws ConversionException error`() {
        val localMapper = TlvMapper(listOf(Tlv(TlvTag.CurveId, "test".toByteArray())))
        assertThrows<TaskError.ConvertError> {
            localMapper.map<EllipticCurve>(TlvTag.CurveId)
        }
    }

    @Test
    fun `map DateTime returns correct value`() {
        val date: Date = cardDataMapper.map(TlvTag.ManufactureDateTime)
        val expected = Calendar.getInstance().apply { this.set(2019, 4, 2, 0, 0, 0) }.time
        assertThat(date.toString())
                .isEqualTo(expected.toString())
    }

    @Test
    fun `map EllipticCurve returns correct value`() {
        val ellipticCurve: EllipticCurve = tlvMapper.map(TlvTag.CurveId)
        assertThat(ellipticCurve)
                .isEqualTo(EllipticCurve.Secp256k1)
    }

    @Test
    fun `map ByteArray returns correctly`() {
        val cardPublicKey: ByteArray = tlvMapper.map(TlvTag.CardPublicKey)
        assertThat(cardPublicKey)
                .isInstanceOf(ByteArray::class.java)
    }

    @Test
    fun `map Int returns correct value`() {
        val signedHashes: Int = tlvMapper.map(TlvTag.SignedHashes)
        assertThat(signedHashes)
                .isEqualTo(13)
    }

    @Test
    fun `map Int with wrong value throws ConversionException`() {
        val localMapper = TlvMapper(listOf(Tlv(TlvTag.SignedHashes, byteArrayOf(1, 2, 3, 4, 5))))
        assertThrows<TaskError.ConvertError> {
            localMapper.map<Int>(TlvTag.SignedHashes)
        }
    }

    @Test
    fun `map UTF8 returns correct value`() {
        val blockchainId: String = cardDataMapper.map(TlvTag.BlockchainId)
        assertThat(blockchainId)
                .isEqualTo("ETH")
    }

    @Test
    fun `map Hex returns correct value`() {
        val cardId: String = tlvMapper.map(TlvTag.CardId)
        assertThat(cardId)
                .isEqualTo("cb22000000027374")
    }
}