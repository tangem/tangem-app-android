package com.tangem.blockchain.blockchains.binance.client.encoding.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import com.tangem.blockchain.blockchains.binance.BinanceAccountData;
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.TransactionOption;
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.Transfer;
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto;
import com.tangem.blockchain.blockchains.binance.client.encoding.EncodeUtils;
import com.tangem.blockchain.blockchains.binance.proto.StdSignature;
import com.tangem.blockchain.blockchains.binance.proto.StdTx;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import okhttp3.RequestBody;

/**
 * Assemble a transaction message body with external signature
 * https://testnet-dex.binance.org/doc/encoding.html
 */
public class TransactionRequestAssemblerExtSign {
    private static final okhttp3.MediaType MEDIA_TYPE = okhttp3.MediaType.parse("text/plain; charset=utf-8");
    private static final BigDecimal MULTIPLY_FACTOR = BigDecimal.valueOf(1e8);
    private static final BigDecimal MAX_NUMBER = new BigDecimal(Long.MAX_VALUE);

    //private Wallet wallet;
    private BinanceAccountData binanceAccountData;
    private byte[] pubKeyForSign;
    private TransactionOption options;

    public TransactionRequestAssemblerExtSign(BinanceAccountData binanceAccountData, byte[] pubKeyForSign, TransactionOption options) {
        this.binanceAccountData = binanceAccountData;
        this.pubKeyForSign = pubKeyForSign;
        this.options = options;
    }

    private static long doubleToLong(String d) {
        BigDecimal encodeValue = new BigDecimal(d).multiply(MULTIPLY_FACTOR);
        if (encodeValue.compareTo(MAX_NUMBER) > 0) {
            throw new IllegalArgumentException(d + " is too large.");
        }
        return encodeValue.longValue();

    }

    public byte[] prepareForSign(BinanceDexTransactionMessage msg)
            throws JsonProcessingException {
        SignData sd = new SignData();
        sd.setChainId(binanceAccountData.getChainId());
        sd.setAccountNumber(String.valueOf(binanceAccountData.getAccountNumber()));
        sd.setSequence(String.valueOf(binanceAccountData.getSequence()));
        sd.setMsgs(new BinanceDexTransactionMessage[]{msg});

        sd.setMemo(options.getMemo());
        sd.setSource(String.valueOf(options.getSource()));
        sd.setData(options.getData());
        return EncodeUtils.toJsonEncodeBytes(sd);
    }

    public byte[] encodeSignature(byte[] signatureBytes) throws IOException {
        StdSignature stdSignature = StdSignature.newBuilder().setPubKey(ByteString.copyFrom(pubKeyForSign))
                .setSignature(ByteString.copyFrom(signatureBytes))
                .setAccountNumber(binanceAccountData.getAccountNumber())
                .setSequence(binanceAccountData.getSequence())
                .build();

        return EncodeUtils.aminoWrap(
                stdSignature.toByteArray(), MessageType.StdSignature.getTypePrefixBytes(), false);
    }

    public byte[] encodeStdTx(byte[] msg, byte[] signature) throws IOException {
        StdTx.Builder stdTxBuilder = StdTx.newBuilder()
                .addMsgs(ByteString.copyFrom(msg))
                .addSignatures(ByteString.copyFrom(signature))
                .setMemo(options.getMemo())
                .setSource(options.getSource());
        if (options.getData() != null) {
            stdTxBuilder = stdTxBuilder.setData(ByteString.copyFrom(options.getData()));
        }
        StdTx stdTx = stdTxBuilder.build();
        return EncodeUtils.aminoWrap(stdTx.toByteArray(), MessageType.StdTx.getTypePrefixBytes(), true);
    }

    public static RequestBody createRequestBody(byte[] stdTx) {
        return RequestBody.create(MEDIA_TYPE, EncodeUtils.bytesToHex(stdTx));
    }

    public TransferMessage createTransferMessage(Transfer transfer) {
        Token token = new Token();
        token.setDenom(transfer.getCoin());
        token.setAmount(doubleToLong(transfer.getAmount()));
        List<Token> coins = Collections.singletonList(token);

        InputOutput input = new InputOutput();
        input.setAddress(transfer.getFromAddress());
        input.setCoins(coins);
        InputOutput output = new InputOutput();
        output.setAddress(transfer.getToAddress());
        output.setCoins(coins);

        TransferMessage msgBean = new TransferMessage();
        msgBean.setInputs(Collections.singletonList(input));
        msgBean.setOutputs(Collections.singletonList(output));
        return msgBean;
    }

    private com.tangem.blockchain.blockchains.binance.proto.Send.Input toProtoInput(InputOutput input) {
        byte[] address = Crypto.decodeAddress(input.getAddress());
        com.tangem.blockchain.blockchains.binance.proto.Send.Input.Builder builder =
                com.tangem.blockchain.blockchains.binance.proto.Send.Input.newBuilder().setAddress(ByteString.copyFrom(address));

        for (Token coin : input.getCoins()) {
            com.tangem.blockchain.blockchains.binance.proto.Send.Token protCoin =
                    com.tangem.blockchain.blockchains.binance.proto.Send.Token.newBuilder().setAmount(coin.getAmount())
                            .setDenom(coin.getDenom()).build();
            builder.addCoins(protCoin);
        }
        return builder.build();
    }

    private com.tangem.blockchain.blockchains.binance.proto.Send.Output toProtoOutput(InputOutput output) {
        byte[] address = Crypto.decodeAddress(output.getAddress());
        com.tangem.blockchain.blockchains.binance.proto.Send.Output.Builder builder =
                com.tangem.blockchain.blockchains.binance.proto.Send.Output.newBuilder().setAddress(ByteString.copyFrom(address));

        for (Token coin : output.getCoins()) {
            com.tangem.blockchain.blockchains.binance.proto.Send.Token protCoin =
                    com.tangem.blockchain.blockchains.binance.proto.Send.Token.newBuilder().setAmount(coin.getAmount())
                            .setDenom(coin.getDenom()).build();
            builder.addCoins(protCoin);
        }
        return builder.build();
    }

    public byte[] encodeTransferMessage(TransferMessage msg)
            throws IOException {
        com.tangem.blockchain.blockchains.binance.proto.Send.Builder builder = com.tangem.blockchain.blockchains.binance.proto.Send.newBuilder();
        for (InputOutput input : msg.getInputs()) {
            builder.addInputs(toProtoInput(input));
        }
        for (InputOutput output : msg.getOutputs()) {
            builder.addOutputs(toProtoOutput(output));
        }
        com.tangem.blockchain.blockchains.binance.proto.Send proto = builder.build();
        return EncodeUtils.aminoWrap(proto.toByteArray(), MessageType.Send.getTypePrefixBytes(), false);
    }

    public byte[] buildTransfer(Transfer transfer)
            throws IOException {
        TransferMessage msgBean = createTransferMessage(transfer);
        byte[] msg = encodeTransferMessage(msgBean);
        return prepareForSign(msgBean);
//        byte[] signature = encodeSignature(prepareForSign(msgBean));
//        byte[] stdTx = encodeStdTx(msg, signature);
//        return createRequestBody(stdTx);
    }
}
