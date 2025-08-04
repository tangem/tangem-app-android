package com.tangem.domain.models.serialization

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.json.TangemSdkAdapter
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.scan.serialization.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object MobileWalletAsStringSerializer : KSerializer<MobileWallet> {

    private val moshi = Moshi.Builder()
        .add(WalletDerivedKeysMapAdapter())
        .add(ScanResponseDerivedKeysMapAdapter())
        .add(ByteArrayKeyAdapter())
        .add(ExtendedPublicKeysMapAdapter())
        .add(DerivationPathAdapterWithMigration())
        .add(TangemSdkAdapter.DateAdapter())
        .add(TangemSdkAdapter.DerivationNodeAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MobileWallet", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MobileWallet {
        return moshi.adapter(MobileWallet::class.java).fromJson(decoder.decodeString())!!
    }

    override fun serialize(encoder: Encoder, value: MobileWallet) {
        encoder.encodeString(moshi.adapter(MobileWallet::class.java).toJson(value)!!)
    }
}