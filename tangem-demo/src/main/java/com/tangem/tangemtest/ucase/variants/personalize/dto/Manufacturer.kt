package com.tangem.tangemtest.ucase.variants.personalize.dto

class Manufacturer(private val mode: Mode) {

    private val devPrivateKeyArray = byteArrayOf(
            0x1B.toByte(), 0x48.toByte(), 0xCF.toByte(), 0xD2.toByte(), 0x4B.toByte(), 0xBB.toByte(), 0x5B.toByte(), 0x39.toByte(),
            0x47.toByte(), 0x71.toByte(), 0xED.toByte(), 0x81.toByte(), 0xF2.toByte(), 0xBA.toByte(), 0xCF.toByte(), 0x57.toByte(),
            0x47.toByte(), 0x9E.toByte(), 0x47.toByte(), 0x35.toByte(), 0xEB.toByte(), 0x14.toByte(), 0x05.toByte(), 0x08.toByte(),
            0x39.toByte(), 0x27.toByte(), 0x37.toByte(), 0x2D.toByte(), 0x40.toByte(), 0xDA.toByte(), 0x9E.toByte(), 0x92.toByte()
    )
    private val devPublicKeyArray = byteArrayOf(0x04,
            0xBA.toByte(), 0xB8.toByte(), 0x6D.toByte(), 0x56.toByte(), 0x29.toByte(), 0x8C.toByte(), 0x99.toByte(), 0x6F.toByte(),
            0x56.toByte(), 0x4A.toByte(), 0x84.toByte(), 0xFC.toByte(), 0x88.toByte(), 0xE2.toByte(), 0x8A.toByte(), 0xED.toByte(),
            0x38.toByte(), 0x18.toByte(), 0x4B.toByte(), 0x12.toByte(), 0xF0.toByte(), 0x7E.toByte(), 0x51.toByte(), 0x91.toByte(),
            0x13.toByte(), 0xBE.toByte(), 0xF4.toByte(), 0x8C.toByte(), 0x76.toByte(), 0xF3.toByte(), 0xDF.toByte(), 0x3A.toByte(),
            0xDC.toByte(), 0x30.toByte(), 0x35.toByte(), 0x99.toByte(), 0xB0.toByte(), 0x8A.toByte(), 0xC0.toByte(), 0x5B.toByte(),
            0x55.toByte(), 0xEC.toByte(), 0x3D.toByte(), 0xF9.toByte(), 0x8D.toByte(), 0x93.toByte(), 0x38.toByte(), 0x57.toByte(),
            0x3A.toByte(), 0x62.toByte(), 0x42.toByte(), 0xF7.toByte(), 0x6F.toByte(), 0x5D.toByte(), 0x28.toByte(), 0xF4.toByte(),
            0xF0.toByte(), 0xF3.toByte(), 0x64.toByte(), 0xE8.toByte(), 0x7E.toByte(), 0x8F.toByte(), 0xCA.toByte(), 0x2F.toByte()
    )
    private val releasePublicKeyArray = byteArrayOf(0x04.toByte(),
            0x2E.toByte(), 0xDE.toByte(), 0x11.toByte(), 0x9B.toByte(), 0xF3.toByte(), 0x37.toByte(), 0xB2.toByte(), 0x64.toByte(),
            0xFD.toByte(), 0xA1.toByte(), 0x32.toByte(), 0xCF.toByte(), 0xC7.toByte(), 0xC1.toByte(), 0x77.toByte(), 0x82.toByte(),
            0x4D.toByte(), 0x36.toByte(), 0x17.toByte(), 0xDA.toByte(), 0xC8.toByte(), 0x0F.toByte(), 0x25.toByte(), 0xDB.toByte(),
            0xB2.toByte(), 0xA4.toByte(), 0xA8.toByte(), 0xA1.toByte(), 0x18.toByte(), 0x3C.toByte(), 0x03.toByte(), 0xB9.toByte(),
            0x15.toByte(), 0x23.toByte(), 0x05.toByte(), 0xF8.toByte(), 0xF1.toByte(), 0xDB.toByte(), 0x97.toByte(), 0x00.toByte(),
            0x45.toByte(), 0x18.toByte(), 0x48.toByte(), 0x0D.toByte(), 0x50.toByte(), 0x91.toByte(), 0xAD.toByte(), 0xC1.toByte(),
            0xCA.toByte(), 0xB9.toByte(), 0xEA.toByte(), 0xCC.toByte(), 0xC1.toByte(), 0x8E.toByte(), 0x1B.toByte(), 0x9E.toByte(),
            0x9C.toByte(), 0x3B.toByte(), 0xEF.toByte(), 0xB2.toByte(), 0x93.toByte(), 0xDD.toByte(), 0x37.toByte(), 0xB2.toByte()
    )

    val publicKey: ByteArray
        get() = if (mode == Mode.Developer) {
            devPublicKeyArray
        } else {
            releasePublicKeyArray
        }

    @get:Throws(Exception::class)
    val privateKey: ByteArray
        get() = if (mode == Mode.Developer) {
            devPrivateKeyArray
        } else {
            throw Exception("Real manufacturer key is very secret!")
        }

    val name: String
        get() = if (mode == Mode.Release) "SMART CASH" else "SMART CASH SDK"

    enum class Mode {
        Developer, Release
    }
}