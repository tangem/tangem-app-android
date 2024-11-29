package com.tangem.domain.models.scan.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.TangemSdkAdapter
import com.tangem.domain.models.scan.ScanResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object ScanResponseAsStringSerializer : KSerializer<ScanResponse> {
    private val moshi = Moshi.Builder()
        .add(WalletDerivedKeysMapAdapter())
        .add(ScanResponseDerivedKeysMapAdapter())
        .add(ByteArrayKeyAdapter())
        .add(ExtendedPublicKeysMapAdapter())
        .add(CardBackupStatusAdapter())
        .add(DerivationPathAdapterWithMigration())
        .add(TangemSdkAdapter.DateAdapter())
        .add(TangemSdkAdapter.DerivationNodeAdapter())
        .add(TangemSdkAdapter.FirmwareVersionAdapter()) // For PrimaryCard model
        .add(KotlinJsonAdapterFactory())
        .build()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ScanResponse", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ScanResponse {
        return moshi.adapter(ScanResponse::class.java).fromJson(decoder.decodeString())!!
    }

    override fun serialize(encoder: Encoder, value: ScanResponse) {
        encoder.encodeString(moshi.adapter(ScanResponse::class.java).toJson(value)!!)
    }
}