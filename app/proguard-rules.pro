-dontusemixedcaseclassnames
-flattenpackagehierarchy
-adaptclassstrings

# firebase
-keep public class com.google.firebase.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keepclasseswithmembers class com.google.firebase.FirebaseException

# huawei push kit
-ignorewarnings
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

# hedera sdk
-keep class com.hedera.hashgraph.sdk.** { *; }
-keep interface com.hedera.hashgraph.sdk.** { *; }
-dontwarn com.esaulpaugh.headlong.**
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn io.grpc.stub.**

# trustwallet sdk
-keep class wallet.core.jni.** { *; }

# solana
-keep class org.p2p.solanaj.** { *; }
-keep class com.tangem.blockchain.blockchains.solana.solanaj.model.** { *; }

# binance
-keep class com.tangem.blockchain.blockchains.binance.client.** { *; }

# shadow gson
-keep,allowobfuscation,allowshrinking class shadow.com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends shadow.com.google.gson.reflect.TypeToken

# polkadot
-keep class io.emeraldpay.polkaj.api.RpcRequest { *; }
-keep class io.emeraldpay.polkaj.api.RpcResponse { *; }
-keep class io.emeraldpay.polkaj.api.RpcResponseError { *; }

# kethereum
-keep class org.kethereum.bip32.model.ExtendedKey**
-keepclassmembers class org.kethereum.bip32.model.ExtendedKey** { *; }

# some crypto
-dontwarn net.i2p.crypto.**
-dontwarn java.net.http.**
-dontwarn lombok.**

-keep class org.spongycastle.** { *; }

-dontwarn org.apache.hc.core5.**
-dontwarn org.apache.hc.client5.**
-dontwarn org.apache.log4j.config.**

-dontwarn javax.naming.**
-dontwarn javax.xml.stream.**
-dontwarn javax.script.**

-dontwarn org.java_websocket.client.WebSocketClient
-dontwarn org.java_websocket.handshake.ServerHandshake

-dontwarn aQute.bnd.annotation.spi.ServiceProvider

-dontwarn org.threeten.bp.**
-keep class org.threeten.bp.*
-keepclassmembers class org.threeten.bp.** { *; }

# joda time
# These aren't necessary if including joda-convert
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

-keepnames class org.joda.** implements java.io.Serializable
-keepclassmembers class org.joda.** implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
# joda time

# The name of @JsonClass types is used to look up the generated adapter.
-keepnames @com.squareup.moshi.JsonClass class *

-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Proguard configuration for Jackson 2.x
-keep class com.fasterxml.** { *; }
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class * implements com.fasterxml.jackson.core.type.TypeReference

-keep class kotlin.reflect.**
-keep public class kotlin.reflect.jvm.internal.impl.** { public *; }

# Kotlin serialization looks up the generated serializer classes through a function on companion
# objects. The companions are looked up reflectively so we need to explicitly keep these functions.
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# If a companion has the serializer function, keep the companion field on the original type so that
# the reflective lookup succeeds.
-if class **.*$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
  <1>.<2>$Companion Companion;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keep class * implements android.os.Parcelable {
     public static final android.os.Parcelable$Creator *;
}

-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exceptions

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembernames interface * {
    @retrofit.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions.*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# public tangem android sdk
-keep class com.tangem.operations.** { *; }
-keep class com.tangem.crypto.** { *; }
-keep class com.tangem.common.card.** { *; }

# TODO remove (easy to fix in sdk)
-keep class com.tangem.common.json.** { *; }
-keep class com.tangem.common.SuccessResponse { *; }
-keep class com.tangem.common.UserCode { *; }
-keep class com.tangem.common.UserCodeType { *; }

# non-sensitive enums
-keep enum com.tangem.domain.apptheme.model.AppThemeMode { *; }

-keep class com.reown.walletkit.client.Wallet$Model { *; }
-keep class com.reown.walletkit.client.Wallet { *; }

-keep class **.R
-keep class **.R$* {
    <fields>;
}
