package com.tangem.blockchain.blockchains.binance.client.encoding.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.tangem.blockchain.blockchains.binance.client.Wallet;
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.TokenFreeze;
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.TokenUnfreeze;
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.TransactionOption;
import com.tangem.blockchain.blockchains.binance.client.domain.broadcast.Transfer;
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto;
import com.tangem.blockchain.blockchains.binance.client.encoding.EncodeUtils;
import com.tangem.blockchain.blockchains.binance.proto.StdSignature;
import com.tangem.blockchain.blockchains.binance.proto.StdTx;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import okhttp3.RequestBody;

/**
 * Assemble a transaction message body.
 * https://testnet-dex.binance.org/doc/encoding.html
 */
public class TransactionRequestAssembler {
    private static final okhttp3.MediaType MEDIA_TYPE = okhttp3.MediaType.parse("text/plain; charset=utf-8");
    private static final BigDecimal MULTIPLY_FACTOR = BigDecimal.valueOf(1e8);
    private static final BigDecimal MAX_NUMBER = new BigDecimal(Long.MAX_VALUE);

    private Wallet wallet;
    private TransactionOption options;

    public TransactionRequestAssembler(Wallet wallet, TransactionOption options) {
        this.wallet = wallet;
        this.options = options;
    }

    public static long doubleToLong(String d) {
        BigDecimal encodeValue = new BigDecimal(d).multiply(MULTIPLY_FACTOR);
        if (encodeValue.compareTo(MAX_NUMBER) > 0) {
            throw new IllegalArgumentException(d + " is too large.");
        }
        return encodeValue.longValue();

    }

    public static String longToDouble(long l) {
        return BigDecimal.valueOf(l).divide(MULTIPLY_FACTOR).toString();
    }

    @VisibleForTesting
    byte[] sign(BinanceDexTransactionMessage msg)
            throws JsonProcessingException, NoSuchAlgorithmException {
        SignData sd = new SignData();
        sd.setChainId(wallet.getChainId());
        sd.setAccountNumber(String.valueOf(wallet.getAccountNumber()));
        sd.setSequence(String.valueOf(wallet.getSequence()));
        sd.setMsgs(new BinanceDexTransactionMessage[]{msg});

        sd.setMemo(options.getMemo());
        sd.setSource(String.valueOf(options.getSource()));
        sd.setData(options.getData());
        return Crypto.sign(EncodeUtils.toJsonEncodeBytes(sd), wallet.getEcKey());
    }

    @VisibleForTesting
    byte[] encodeSignature(byte[] signatureBytes) throws IOException {
        StdSignature stdSignature = StdSignature.newBuilder().setPubKey(ByteString.copyFrom(wallet.getPubKeyForSign()))
                .setSignature(ByteString.copyFrom(signatureBytes))
                .setAccountNumber(wallet.getAccountNumber())
                .setSequence(wallet.getSequence())
                .build();

        return EncodeUtils.aminoWrap(
                stdSignature.toByteArray(), MessageType.StdSignature.getTypePrefixBytes(), false);
    }

    @VisibleForTesting
    byte[] encodeStdTx(byte[] msg, byte[] signature) throws IOException {
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

    private RequestBody createRequestBody(byte[] stdTx) {
        return RequestBody.create(MEDIA_TYPE, EncodeUtils.bytesToHex(stdTx));
    }

    private String generateOrderId() {
        return EncodeUtils.bytesToHex(wallet.getAddressBytes()).toUpperCase() + "-" + (wallet.getSequence() + 1);
    }

    @VisibleForTesting
    NewOrderMessage createNewOrderMessage(
            com.tangem.blockchain.blockchains.binance.client.domain.broadcast.NewOrder newOrder) {
        return NewOrderMessage.newBuilder()
                .setId(generateOrderId())
                .setOrderType(newOrder.getOrderType())
                .setPrice(newOrder.getPrice())
                .setQuantity(newOrder.getQuantity())
                .setSender(wallet.getAddress())
                .setSide(newOrder.getSide())
                .setSymbol(newOrder.getSymbol())
                .setTimeInForce(newOrder.getTimeInForce())
                .build();
    }

    @VisibleForTesting
    byte[] encodeNewOrderMessage(NewOrderMessage newOrder)
            throws IOException {
        com.tangem.blockchain.blockchains.binance.proto.NewOrder proto = com.tangem.blockchain.blockchains.binance.proto.NewOrder.newBuilder()
                .setSender(ByteString.copyFrom(wallet.getAddressBytes()))
                .setId(newOrder.getId())
                .setSymbol(newOrder.getSymbol())
                .setOrdertype(newOrder.getOrderType().toValue())
                .setSide(newOrder.getSide().toValue())
                .setPrice(newOrder.getPrice())
                .setQuantity(newOrder.getQuantity())
                .setTimeinforce(newOrder.getTimeInForce().toValue())
                .build();
        return EncodeUtils.aminoWrap(proto.toByteArray(), MessageType.NewOrder.getTypePrefixBytes(), false);
    }

    public RequestBody buildNewOrder(com.tangem.blockchain.blockchains.binance.client.domain.broadcast.NewOrder newOrder)
            throws IOException, NoSuchAlgorithmException {
        NewOrderMessage msgBean = createNewOrderMessage(newOrder);
        byte[] msg = encodeNewOrderMessage(msgBean);
        byte[] signature = encodeSignature(sign(msgBean));
        byte[] stdTx = encodeStdTx(msg, signature);
        return createRequestBody(stdTx);
    }

    @VisibleForTesting
    CancelOrderMessage createCancelOrderMessage(
            com.tangem.blockchain.blockchains.binance.client.domain.broadcast.CancelOrder cancelOrder) {
        CancelOrderMessage bean =
                new CancelOrderMessage();
        bean.setRefId(cancelOrder.getRefId());
        bean.setSymbol(cancelOrder.getSymbol());
        bean.setSender(wallet.getAddress());
        return bean;
    }

    @VisibleForTesting
    byte[] encodeCancelOrderMessage(CancelOrderMessage cancelOrder)
            throws IOException {
        com.tangem.blockchain.blockchains.binance.proto.CancelOrder proto = com.tangem.blockchain.blockchains.binance.proto.CancelOrder.newBuilder()
                .setSender(ByteString.copyFrom(wallet.getAddressBytes()))
                .setSymbol(cancelOrder.getSymbol())
                .setRefid(cancelOrder.getRefId())
                .build();
        return EncodeUtils.aminoWrap(proto.toByteArray(), MessageType.CancelOrder.getTypePrefixBytes(), false);
    }

    public RequestBody buildCancelOrder(com.tangem.blockchain.blockchains.binance.client.domain.broadcast.CancelOrder cancelOrder)
            throws IOException, NoSuchAlgorithmException {
        CancelOrderMessage msgBean = createCancelOrderMessage(cancelOrder);
        byte[] msg = encodeCancelOrderMessage(msgBean);
        byte[] signature = encodeSignature(sign(msgBean));
        byte[] stdTx = encodeStdTx(msg, signature);
        return createRequestBody(stdTx);
    }

    @VisibleForTesting
    TransferMessage createTransferMessage(Transfer transfer) {
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

    @VisibleForTesting
    byte[] encodeTransferMessage(TransferMessage msg)
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

    public RequestBody buildTransfer(Transfer transfer)
            throws IOException, NoSuchAlgorithmException {
        TransferMessage msgBean = createTransferMessage(transfer);
        byte[] msg = encodeTransferMessage(msgBean);
        byte[] signature = encodeSignature(sign(msgBean));
        byte[] stdTx = encodeStdTx(msg, signature);
        return createRequestBody(stdTx);
    }

    @VisibleForTesting
    TokenFreezeMessage createTokenFreezeMessage(TokenFreeze freeze) {
        TokenFreezeMessage msg = new TokenFreezeMessage();
        msg.setAmount(doubleToLong(freeze.getAmount()));
        msg.setFrom(wallet.getAddress());
        msg.setSymbol(freeze.getSymbol());
        return msg;
    }

    @VisibleForTesting
    byte[] encodeTokenFreezeMessage(TokenFreezeMessage freeze) throws IOException {
        byte[] address = Crypto.decodeAddress(freeze.getFrom());
        com.tangem.blockchain.blockchains.binance.proto.TokenFreeze proto =
                com.tangem.blockchain.blockchains.binance.proto.TokenFreeze.newBuilder().setFrom(ByteString.copyFrom(address))
                        .setAmount(freeze.getAmount())
                        .setSymbol(freeze.getSymbol())
                        .build();
        return EncodeUtils.aminoWrap(proto.toByteArray(), MessageType.TokenFreeze.getTypePrefixBytes(), false);
    }

    public RequestBody buildTokenFreeze(TokenFreeze freeze)
            throws IOException, NoSuchAlgorithmException {
        TokenFreezeMessage msgBean = createTokenFreezeMessage(freeze);
        byte[] msg = encodeTokenFreezeMessage(msgBean);
        byte[] signature = encodeSignature(sign(msgBean));
        byte[] stdTx = encodeStdTx(msg, signature);
        return createRequestBody(stdTx);
    }

    @VisibleForTesting
    TokenUnfreezeMessage createTokenUnfreezeMessage(TokenUnfreeze unfreeze) {
        TokenUnfreezeMessage msg = new TokenUnfreezeMessage();
        msg.setAmount(doubleToLong(unfreeze.getAmount()));
        msg.setFrom(wallet.getAddress());
        msg.setSymbol(unfreeze.getSymbol());
        return msg;
    }

    @VisibleForTesting
    byte[] encodeTokenUnfreezeMessage(TokenUnfreezeMessage unfreeze) throws IOException {
        byte[] address = Crypto.decodeAddress(unfreeze.getFrom());
        com.tangem.blockchain.blockchains.binance.proto.TokenUnfreeze proto =
                com.tangem.blockchain.blockchains.binance.proto.TokenUnfreeze.newBuilder().setFrom(ByteString.copyFrom(address))
                        .setAmount(unfreeze.getAmount())
                        .setSymbol(unfreeze.getSymbol())
                        .build();
        return EncodeUtils.aminoWrap(proto.toByteArray(), MessageType.TokenUnfreeze.getTypePrefixBytes(), false);
    }

    public RequestBody buildTokenUnfreeze(TokenUnfreeze unfreeze)
            throws IOException, NoSuchAlgorithmException {
        TokenUnfreezeMessage msgBean = createTokenUnfreezeMessage(unfreeze);
        byte[] msg = encodeTokenUnfreezeMessage(msgBean);
        byte[] signature = encodeSignature(sign(msgBean));
        byte[] stdTx = encodeStdTx(msg, signature);
        return createRequestBody(stdTx);
    }
}
