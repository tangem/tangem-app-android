package com.tangem.wallet.cardano;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Base64;
import android.util.Log;

import com.tangem.App;
import com.tangem.Constant;
import com.tangem.data.local.PendingTransactionsStorage;
import com.tangem.data.network.ServerApiAdalite;
import com.tangem.data.network.model.AdaliteResponse;
import com.tangem.data.network.model.AdaliteResponseUtxo;
import com.tangem.data.network.model.AdaliteTxData;
import com.tangem.data.network.model.AdaliteUtxoData;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.Base58;
import com.tangem.wallet.BuildConfig;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;

import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.util.DigestFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.zip.CRC32;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.builder.ArrayBuilder;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnsignedInteger;

import static com.tangem.wallet.Base58.decodeBase58;
import static com.tangem.wallet.Base58.encodeBase58;

public class CardanoEngine extends CoinEngine {

    private static final String TAG = CardanoEngine.class.getSimpleName();

    private static final long protocolMagic = 764824073;

    public CardanoData coinData = null;

    public CardanoEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new CardanoData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof CardanoData) {
            coinData = (CardanoData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for CardanoEngine");
        }
    }

    public CardanoEngine() {
        super();
    }

    private static int getDecimals() {
        return 6;
    }


    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        return App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "ADA";
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.hasBalanceInfo();
    }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
        } else if (coinData.getUnspentOutputs().size() == 0) {
            ctx.setMessage(R.string.loaded_wallet_message_refresh);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "ADA";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        byte[] decAddress = Base58.decodeBase58(address);

        if (decAddress == null || decAddress.length == 0) {
            return false;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(decAddress);
        try {
            List<DataItem> addressList = ((Array) new CborDecoder(bais).decode().get(0)).getDataItems();
            byte[] addressBytes = ((ByteString) addressList.get(0)).getBytes();
            long addressChecksum = ((UnsignedInteger) addressList.get(1)).getValue().longValue();

            final CRC32 crc32 = new CRC32();
            crc32.update(addressBytes);
            long calculatedChecksum = crc32.getValue();

            if (addressChecksum != calculatedChecksum) {
                return false;
            }

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://cardanoexplorer.com/address/" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        return Uri.parse(ctx.getCoinData().getWallet());
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (BuildConfig.FLAVOR == Constant.FLAVOR_TANGEM_CARDANO) {
            return true;
        }
        if (coinData == null) return false;
        if (amount.compareTo(convertToAmount(coinData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        InternalAmount fee;
        InternalAmount amount;

        try {
            checkBlockchainDataExists();
            amount = convertToInternalAmount(amountValue);
            fee = convertToInternalAmount(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee.isZero() || amount.isZero())
            return false;

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee) < 0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        try {
            if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
                balanceValidator.setScore(0);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
                return false;
            }

            if (coinData.isBalanceReceived()) {// && coinData.isBalanceEqual()) { TODO:check
                balanceValidator.setScore(100);
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
                if (coinData.getBalanceInInternalUnits().isZero()) {
                    balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                    balanceValidator.setSecondLine(R.string.empty_string);
                }
            }

//            if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) {
//                balanceValidator.setScore(80);
//                balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
//                balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_get_balance);
//            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) return null;
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        if (!coinData.getAmountEquivalentDescriptionAvailable()) return "";
        try {
            Amount feeAmount = new Amount(fee, getFeeCurrency());
            return feeAmount.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        if (coinData == null || !coinData.getAmountEquivalentDescriptionAvailable()) return "";
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws CborException, IOException {

        ByteArrayOutputStream exBaos = new ByteArrayOutputStream();
        exBaos.write(pkUncompressed);
        exBaos.write(BTCUtils.fromHex("0000000000000000000000000000000000000000000000000000000000000000"));
        byte[] pkExtended = exBaos.toByteArray();

        exBaos.reset();
        new CborEncoder(exBaos).encode(new CborBuilder()
                .addArray()
                .add(0)
                .addArray()
                .add(0)
                .add(pkExtended)
                .end()
                .addMap()
                .end()
                .end()
                .build());
        byte[] forSha3 = exBaos.toByteArray();

        Digest sha3 = DigestFactory.createSHA3_256();
        sha3.update(forSha3, 0, forSha3.length);
        byte[] forBlake = new byte[32];
        sha3.doFinal(forBlake, 0);

        final Blake2b blake2b = Blake2b.Digest.newInstance(28);
        byte[] pkHash = blake2b.digest(forBlake);

        //pkHash + attributes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addArray()
                .add(pkHash)
                .addMap()//additional attributes
                .end()
                .add(0)//address type
                .end()
                .build());
        byte[] addr = baos.toByteArray();

        final CRC32 crc32 = new CRC32();
        crc32.update(addr);
        long checksum = crc32.getValue();

        DataItem addrItem = new CborBuilder().add(addr).build().get(0);
        addrItem.setTag(24);

        //addr + checksum
        baos.reset();
        new CborEncoder(baos).encode(new CborBuilder()
                .addArray()
                .add(addrItem)
                .add(checksum)
                .end()
                .build());

        byte[] hexAddress = baos.toByteArray();
        return encodeBase58(hexAddress);
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("1000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        BigDecimal d = amount.multiply(new BigDecimal("1000000"));
        return new InternalAmount(d, "Lovelace");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Lovelace");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
//        byte[] reversed = new byte[bytes.length]; TODO: check if needed
//        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
//        return reversed;
        return bytes;
    }

    @Override
    public CoinData createCoinData() {
        return new CardanoData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return coinData.getUnspentInputsDescription();
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        checkBlockchainDataExists();

        String myAddress = ctx.getCoinData().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();
        List<CardanoData.UnspentOutput> utxoList = coinData.getUnspentOutputs();
        long fullAmount = coinData.getBalance();

//        long fullAmount = 0; TODO: check
//        for (int i = 0; i < unspentOutputs.size(); ++i) {
//            fullAmount += unspentOutputs.get(i).value;
//        }

        long fees = convertToInternalAmount(feeValue).longValueExact();
        long amount = convertToInternalAmount(amountValue).longValueExact();
        long change = fullAmount - amount;
        if (IncFee) {
            amount = amount - fees;
        } else {
            change = change - fees;
        }

        final long amountFinal = amount;
        final long changeFinal = change;

        //TODO - uncomment
//        if (amount + fees > fullAmount) {
//            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
//        }

        CborBuilder cborBuilder = new CborBuilder();
        ArrayBuilder<CborBuilder> txArray = cborBuilder.addArray();
        ArrayBuilder<ArrayBuilder<CborBuilder>> inputsArray = txArray.startArray();
        ArrayBuilder<ArrayBuilder<CborBuilder>> outputsArray = txArray.startArray();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        //Inputs
        for (CardanoData.UnspentOutput utxo : utxoList) {
            baos.reset();

            //txID + inputPos
            new CborEncoder(baos).encode(new CborBuilder()
                    .addArray()
                    .add(Util.fromHexString(utxo.txID))
                    .add(utxo.Index)
                    .end()
                    .build());
            byte[] input = baos.toByteArray();

            DataItem inputItem = new CborBuilder().add(input).build().get(0);
            inputItem.setTag(24);

            //input type + input
            inputsArray
                    .addArray()
                    .add(0)
                    .add(inputItem)
                    .end();
        }

        //1st output
        DataItem targetAddressItem = new CborDecoder(new ByteArrayInputStream(decodeBase58(targetAddress))).decode().get(0);
        outputsArray
                .addArray()
                .add(targetAddressItem)
                .add(amountFinal)
                .end();
        //2nd output (optional)
        if (changeFinal > 0) {
            DataItem myAddressItem = new CborDecoder(new ByteArrayInputStream(decodeBase58(myAddress))).decode().get(0);
            outputsArray
                    .addArray()
                    .add(myAddressItem)
                    .add(changeFinal)
                    .end();
        }

        inputsArray.end();
        outputsArray.end();

        txArray.addMap().end();

        baos.reset();
        new CborEncoder(baos).encode(cborBuilder.build());
        byte[] txBody = baos.toByteArray();

        final Blake2b blake2b = Blake2b.Digest.newInstance(32);//28?
        byte[] txHash = blake2b.digest(txBody);

        baos.reset();
        new CborEncoder(baos).encode(new CborBuilder().add(protocolMagic).build());
        byte[] magic = baos.toByteArray();

        //dataToSign prefix
        baos.reset();
        baos.write(new byte[]{(byte) 0x01});
        baos.write(magic);
        baos.write(new byte[]{(byte) 0x58, (byte) 0x20});

        baos.write(txHash);
        byte[] dataToSign = baos.toByteArray();


        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] dataForSign = new byte[1][];
                dataForSign[0] = dataToSign;
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                ByteArrayOutputStream exBaos = new ByteArrayOutputStream();
                exBaos.write(ctx.getCard().getWalletPublicKey());
                exBaos.write(BTCUtils.fromHex("0000000000000000000000000000000000000000000000000000000000000000"));
                byte[] pkExtended = exBaos.toByteArray();

                //pubkey + signature
                baos.reset();
                new CborEncoder(baos).encode(new CborBuilder()
                        .addArray()
                        .add(pkExtended)
                        .add(signFromCard)
                        .end()
                        .build());
                byte[] witnessBody = baos.toByteArray();
                DataItem witnessBodyItem = new CborBuilder().add(witnessBody).build().get(0);
                witnessBodyItem.setTag(24);

                //array of witnesses
                CborBuilder witnessBuilder = new CborBuilder();
                ArrayBuilder<CborBuilder> witnessArrayBuilder = witnessBuilder.addArray();

                //witness type + witness body
                for (CardanoData.UnspentOutput utxo : coinData.getUnspentOutputs()) {
                    witnessArrayBuilder
                            .addArray()
                            .add(0)
                            .add(witnessBodyItem)
                            .end();
                }

                baos.reset();
                new CborEncoder(baos).encode(witnessBuilder.build());
                byte[] witness = baos.toByteArray();

                baos.reset();
                baos.write(new byte[]{(byte) 0x82});
                baos.write(txBody);
                baos.write(witness);
//                new CborEncoder(baos).encode(new CborBuilder()
//                        .addArray()
//                        .add(txBody)
//                        .add(witness)
//                        .end()
//                        .build());
                byte[] txForSend = baos.toByteArray();
                notifyOnNeedSendTransaction(txForSend);
                //String hex = BTCUtils.toHex(txForSend);
                return txForSend;
            }
        };
    }

    private int calculateEstimatedTransactionSize(String targetAddress, Amount amount) {
        Amount feeDummy = new Amount(new BigDecimal(0.2).setScale(getDecimals(), RoundingMode.DOWN), getFeeCurrency());
        try {
            OnNeedSendTransaction onNeedSendTransactionBackup = onNeedSendTransaction;
            onNeedSendTransaction = (tx) -> {
            }; // empty function to bypass exception

            SignTask.TransactionToSign ttsDummy = constructTransaction(amount, feeDummy, true, targetAddress);
            byte[] txForSendDummy = ttsDummy.onSignCompleted(new byte[64]);

            onNeedSendTransaction = onNeedSendTransactionBackup;
            Log.e(TAG, "txForSendDummy.length=" + String.valueOf(txForSendDummy.length));
            return txForSendDummy.length;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Can't calculate transaction size -> use default!");
            return 1000;
        }
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiAdalite serverApiAdalite = new ServerApiAdalite();

        ServerApiAdalite.ResponseListener adaliteListener = new ServerApiAdalite.ResponseListener() {
            @Override
            public void onSuccess(String method, AdaliteResponse adaliteResponse) {
                Log.i(TAG, "onSuccess: " + method);
                try {
                    String walletAddress = adaliteResponse.getRight().getCaAddress();
                    if (!walletAddress.equals(coinData.getWallet())) {
                        // todo - check
                        throw new Exception("Invalid wallet address in answer!");
                    }
                    coinData.setBalanceReceived(true);
                    coinData.setBalance(adaliteResponse.getRight().getCaBalance().getGetCoin());
                    coinData.setValidationNodeDescription(serverApiAdalite.getCurrentURL());

                    //check pending
                    if (App.pendingTransactionsStorage.hasTransactions(ctx.getCard())) {
                        for (PendingTransactionsStorage.TransactionInfo pendingTx : App.pendingTransactionsStorage.getTransactions(ctx.getCard()).getTransactions()) {
                            String pendingId = CalculateTxHash(pendingTx.getTx());
                            for (AdaliteTxData walletTx : adaliteResponse.getRight().getCaTxList()) {
                                if (walletTx.getCtbId().equals(pendingId)) {
                                    App.pendingTransactionsStorage.removeTransaction(ctx.getCard(), pendingTx.getTx());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "FAIL ADALITE_ADDRESS Exception");
                    e.printStackTrace();
                }

                if (serverApiAdalite.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }


            @Override
            public void onSuccess(String method, AdaliteResponseUtxo adaliteResponseUtxo) {
                Log.i(TAG, "onSuccess: " + method);
                try {
                    coinData.getUnspentOutputs().clear();
                    for (AdaliteUtxoData utxo : adaliteResponseUtxo.getRight()) {
                        CardanoData.UnspentOutput unspentOutput = new CardanoData.UnspentOutput();
                        unspentOutput.txID = utxo.getCuId();
                        unspentOutput.Amount = utxo.getCuCoins().getGetCoin();
                        unspentOutput.Index = utxo.getCuOutIndex();
                        coinData.getUnspentOutputs().add(unspentOutput);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "FAIL ADALITE_UNSPENT_OUTPUTS Exception");
                    e.printStackTrace();
                }

                if (serverApiAdalite.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onSuccess(String method, String stringResponse) {
                Log.e(TAG, "Wrong response type for requestBalanceAndUnspentTransactions");
                ctx.setError("Wrong response type for requestBalanceAndUnspentTransactions");
                blockchainRequestsCallbacks.onComplete(false);
            }

            @Override
            public void onFail(String method, String message) {
                Log.e(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                if (serverApiAdalite.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(false);
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };

        serverApiAdalite.setResponseListener(adaliteListener);

        serverApiAdalite.requestData(ServerApiAdalite.ADALITE_ADDRESS, ctx.getCoinData().getWallet(), "");
        serverApiAdalite.requestData(ServerApiAdalite.ADALITE_UNSPENT_OUTPUTS, ctx.getCoinData().getWallet(), "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        final double A = 0.155381;
        final double B = 0.000043946;
        int calcSize = calculateEstimatedTransactionSize(targetAddress, amount);

        double fee = A + B * calcSize;
        Amount feeAmount = new Amount(new BigDecimal(fee).setScale(getDecimals(), RoundingMode.UP), getFeeCurrency());

        coinData.minFee = feeAmount;
        coinData.normalFee = feeAmount;
        coinData.maxFee = feeAmount;

        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {
        final ServerApiAdalite serverApiAdalite = new ServerApiAdalite();
        final String txStr = Base64.encodeToString(txForSend, Base64.NO_WRAP);
        // BTCUtils.toHex(txForSend);

        final ServerApiAdalite.ResponseListener responseListener = new ServerApiAdalite.ResponseListener() {
            @Override
            public void onSuccess(String method, AdaliteResponse adaliteResponse) {
                Log.e(TAG, "Wrong response type for requestSendTransaction");
                ctx.setError("Wrong response type for requestSendTransaction");
            }

            @Override
            public void onSuccess(String method, AdaliteResponseUtxo adaliteResponseUtxo) {
                Log.e(TAG, "Wrong response type for requestSendTransaction");
                ctx.setError("Wrong response type for requestSendTransaction");
            }

            @Override
            public void onSuccess(String method, String stringResponse) {
                if (method.equals(ServerApiAdalite.ADALITE_SEND)) {
                    if (stringResponse.equals("\"Transaction sent successfully!\"")) {
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    } else { // TODO: Make check for a valid send response
                        ctx.setError(stringResponse);
                        blockchainRequestsCallbacks.onComplete(false);
                    }
                }
            }


            @Override
            public void onFail(String method, String message) {
                Log.e(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);

            }
        };
        serverApiAdalite.setResponseListener(responseListener);

        serverApiAdalite.requestData(ServerApiAdalite.ADALITE_SEND, "", txStr);
    }

    @Override
    public boolean allowSelectFeeLevel() {
        return false;
    }

    @Override
    public int pendingTransactionTimeoutInSeconds() {
        return 60;
    }

    private String CalculateTxHash(String tx) throws CborException {
        Array txArray = (Array) CborDecoder.decode(BTCUtils.fromHex(tx)).get(0);
        DataItem txBodyItem = txArray.getDataItems().get(0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(txBodyItem);
        byte[] txBody = baos.toByteArray();

        final Blake2b blake2b = Blake2b.Digest.newInstance(32);
        return BTCUtils.toHex(blake2b.digest(txBody));
    }
}
