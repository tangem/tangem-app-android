package com.tangem.lib.visa;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/hyperledger/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.6.1.
 */
@SuppressWarnings("rawtypes")
public class TangemPaymentAccountRegistry extends Contract {
    public static final String BINARY = "60806040523462000030576200001e62000018620000d3565b62000156565b6040516110ab6200016e82396110ab90f35b600080fd5b634e487b7160e01b600052604160045260246000fd5b90601f01601f191681019081106001600160401b038211176200006d57604052565b62000035565b906200008a6200008260405190565b92836200004b565b565b6001600160a01b031690565b90565b6001600160a01b038116036200003057565b905051906200008a826200009b565b9060208282031262000030576200009891620000ad565b620000986200121980380380620000ea8162000073565b928339810190620000bc565b62000098906200008c906001600160a01b031682565b6200009890620000f6565b62000098906200010c565b906200013662000098620001529262000117565b82546001600160a01b0319166001600160a01b03919091161790565b9055565b620001656200008a9162000117565b60006200012256fe6080604052600436101561001257600080fd5b60003560e01c80630cef7172146100b25780630d009297146100ad578063173825d9146100a85780633dc50952146100a35780634149177b1461009e57806379da564f14610099578063a727bc9014610094578063c45a01551461008f578063f00d4b5d1461008a5763f72a1107036100d857610353565b61033a565b610313565b6102cb565b610281565b6101f5565b6101dd565b6101c5565b6101a8565b61016f565b6001600160a01b031690565b90565b6001600160a01b0381165b036100d857565b600080fd5b905035906100ea826100c6565b565b906020828203126100d8576100c3916100dd565b6100c3906100b7906001600160a01b031682565b6100c390610100565b6100c390610114565b906101309061011d565b600052602052604060002090565b6100c3916008021c6100b7565b906100c3915461013e565b60006101666100c3926002610126565b61014b565b9052565b346100d8576101a461018a6101853660046100ec565b610156565b604051918291826001600160a01b03909116815260200190565b0390f35b346100d8576101c06101bb3660046100ec565b6105da565b604051005b346100d8576101c06101d83660046100ec565b610709565b346100d8576101c06101f03660046100ec565b61076f565b346100d8576101c06102083660046100ec565b6108f0565b0190565b9061023161022a610220845190565b8084529260200190565b9260200190565b9060005b8181106102425750505090565b90919261026861026160019286516001600160a01b0316815260200190565b9460200190565b929101610235565b60208082526100c392910190610211565b346100d8576101a461029c6102973660046100ec565b6108f9565b60405191829182610270565b91906040838203126100d8576100c39060206102c482866100dd565b94016100dd565b346100d8576101c06102de3660046102a8565b906109b8565b60009103126100d857565b6100c360008061014b565b61016b9061011d565b6020810192916100ea91906102fa565b346100d8576103233660046102e4565b6101a461032e6102ef565b60405191829182610303565b346100d8576101c061034d3660046102a8565b90610b01565b346100d8576101c06103663660046102a8565b90610b2d565b6100c3906100b7565b6100c3905461036c565b634e487b7160e01b600052604160045260246000fd5b90601f01601f1916810190811067ffffffffffffffff8211176103b757604052565b61037f565b8015156100d1565b905051906100ea826103bc565b906020828203126100d8576100c3916103c4565b6040513d6000823e3d90fd5b906100ea6103fe60405190565b9283610395565b67ffffffffffffffff81116103b757602090601f01601f19160190565b9061043461042f83610405565b6103f1565b918252565b610443603a610422565b7f353630317c52656769737472793a206f6e6c79207061796d656e74206163636f60208201527f756e742063616e2063616c6c20746869732066756e6374696f6e000000000000604082015290565b6100c3610439565b6100c3610492565b60005b8381106104b55750506000910152565b81810151838201526020016104a5565b6104e66104ef60209361020d936104da815190565b80835293849260200190565b958691016104a2565b601f01601f191690565b60208082526100c3929101906104c5565b156105125750565b6105349061051f60405190565b62461bcd60e51b8152918291600483016104f9565b0390fd5b61054a6105456000610375565b61011d565b90602061055660405190565b6385bb392360e01b815233600482015292839060249082905afa9182156105ca576100ea926105969160009161059b575b5061059061049a565b9061050a565b6105cf565b6105bd915060203d6020116105c3575b6105b58183610395565b8101906103d1565b38610587565b503d6105ab565b6103e5565b6100ea903390610b37565b6100ea90610538565b6105f06105456000610375565b9060206105fc60405190565b6385bb392360e01b815233600482015292839060249082905afa9182156105ca576100ea926106359160009161059b575061059061049a565b6106a3565b610644603b610422565b7f353630327c52656769737472793a207061796d656e74206163636f756e74206260208201527f65696e672072656d6f766564206e6f7420696e20746865207365740000000000604082015290565b6100c361063a565b6100c3610693565b6106c66106be6106b76100c3846001610126565b3390610ba3565b61059061069b565b6106d86106d23361011d565b9161011d565b907fe594d081b4382713733fe631966432c9cea5199afb2db5c3c1931f9f9300367961070360405190565b600090a3565b6100ea906105e3565b61071f6105456000610375565b90602061072b60405190565b6385bb392360e01b815233600482015292839060249082905afa9182156105ca576100ea926107649160009161059b575061059061049a565b6100ea903390610c7e565b6100ea90610712565b6107856105456000610375565b90602061079160405190565b6385bb392360e01b815233600482015292839060249082905afa9182156105ca576100ea926107ca9160009161059b575061059061049a565b610873565b6107d96030610422565b7f353630357c52656769737472793a2063617264206265696e672072656d6f766560208201526f19081b9bdd081a5b881d1a19481cd95d60821b604082015290565b6100c36107cf565b6100c361081b565b916001600160a01b0360089290920291821b911b5b9181191691161790565b919061085b6100c36108639361011d565b90835461082b565b9055565b6100ea9160009161084a565b6108a4610889610884836002610126565b610375565b61089b335b916001600160a01b031690565b14610590610823565b6108b960006108b4836002610126565b610867565b6108c56106d23361011d565b907f5f8ec0a3d11bc25dab3cc6b87b3add8f1797a221531def932c4d242f561abf2561070360405190565b6100ea90610778565b6109136100c36100c39261090b606090565b506001610126565b610cef565b6109226042610422565b7f353630307c52656769737472793a206f6e6c79207061796d656e74206163636f60208201527f756e7420666163746f72792063616e2063616c6c20746869732066756e63746960408201526137b760f11b606082015290565b6100c3610918565b6100c361097c565b906100ea916109ae6109a46100b76105456000610375565b3314610590610984565b906100ea91610c7e565b906100ea9161098c565b906109d06105456000610375565b9160206109dc60405190565b6385bb392360e01b815233600482015293849060249082905afa9283156105ca576100ea93610a159160009161059b575061059061049a565b610a83565b610a246040610422565b7f353630337c52656769737472793a207061796d656e74206163636f756e74206260208201527f65696e6720616464656420697320616c726561647920696e2074686520736574604082015290565b6100c3610a1a565b6100c3610a73565b90610a986106be6106b76100c3856001610126565b610abb610ab3610aac6100c3846001610126565b3390610d00565b610590610a7b565b610ad06106d2610aca3361011d565b9361011d565b917f381c0d11398486654573703c51ee8210ce9461764d133f9f0e53b6a539705331610afb60405190565b600090a4565b906100ea916109c2565b906100ea91610b236109a46100b76105456000610375565b906100ea91610b37565b906100ea91610b0b565b906106d2610b5991610545610ab382610b546100c3886001610126565b610d00565b907fa5e1f8b4009110f5525798d04ae2125421a12d0590aa52c13682ff1bd3c492ca61070360405190565b6100c39081906001600160a01b031681565b6100c36100c36100c39290565b90610bd4610bd0610bcb610bc660006100c396610bbe600090565b500194610114565b610b84565b610b96565b9190565b610e66565b6100b76100c36100c39290565b6100c390610bd9565b610bf9603a610422565b7f353630347c52656769737472793a207061796d656e74206163636f756e74206960208201527f7320616c72656164792073657420666f72207468652063617264000000000000604082015290565b6100c3610bef565b6100c3610c48565b906001600160a01b0390610840565b90610c776100c36108639261011d565b8254610c58565b906106d2610cc491610cb0610c97610884866002610126565b610ca761088e6100b76000610be6565b14610590610c50565b61054581610cbf866002610126565b610c67565b907fc063fc300750e8c5649c6b3779f6c4b05e7b38b170ee5c4a4b222ede9a28808361070360405190565b606090610cfb90610fc8565b905090565b90610d1b610bd0610bcb610bc660006100c396610bbe600090565b611005565b90610130565b6100c39081565b6100c39054610d26565b634e487b7160e01b600052601160045260246000fd5b91908203918211610d5a57565b610d37565b634e487b7160e01b600052603260045260246000fd5b8054821015610d9857610d8f600191600052602060002090565b91020190600090565b610d5f565b6100c3916008021c81565b906100c39154610d9d565b9160001960089290920291821b911b610840565b9190610dd66100c36108639390565b908354610db3565b9060001990610840565b90610df86100c361086392610b96565b8254610dde565b634e487b7160e01b600052603160045260246000fd5b6100ea91600091610dc7565b80548015610e44576000190190610e41610e3b8383610d75565b90610e15565b55565b610dff565b9190610dd66100c361086393610b96565b6100ea91600091610e49565b90610e7c610e778260018501610d20565b610d2d565b610e866000610b96565b8114610f3457610eeb6100c392600092610ee095610ee56001978893610eb4610eae86610b96565b82610d4d565b88850191610ed2610ec3845490565b610ecc89610b96565b90610d4d565b808303610ef0575b50505090565b610e21565b01610d20565b610e5a565b610f19610f1f610f2c94610f10610f0a610f279589610d75565b90610da8565b92839188610d75565b90610dc7565b888801610d20565b610de8565b388080610eda565b505050600090565b90610f57610f4b610220845490565b92600052602060002090565b9060005b818110610f685750505090565b909192610f8c610f85600192610f7d87610d2d565b815260200190565b9460010190565b929101610f5b565b906100c391610f3c565b906100ea610fb892610faf60405190565b93848092610f94565b0383610395565b6100c390610f9e565b60006100c391610fd6606090565b5001610fbf565b90815491680100000000000000008310156103b75782610f199160016100ea95018155610d75565b611016611012838361104a565b1590565b156110435761103e91610f27906001611037846110338482610fdd565b5490565b9301610d20565b600190565b5050600090565b611063916001610e779261105c600090565b5001610d20565b611070610bd06000610b96565b14159056fea26469706673582212205e9abf37b9af5365c966f2cb4bdb949d15aac5e8ec856dbc6a6c8fc7e9de783764736f6c63430008160033";

    private static String librariesLinkedBinary;

    public static final String FUNC_ADDCARD = "addCard";

    public static final String FUNC_ADDCARDONDEPLOY = "addCardOnDeploy";

    public static final String FUNC_CHANGEOWNER = "changeOwner";

    public static final String FUNC_FACTORY = "factory";

    public static final String FUNC_INITOWNER = "initOwner";

    public static final String FUNC_INITOWNERONDEPLOY = "initOwnerOnDeploy";

    public static final String FUNC_PAYMENTACCOUNTBYCARD = "paymentAccountByCard";

    public static final String FUNC_PAYMENTACCOUNTSBYOWNER = "paymentAccountsByOwner";

    public static final String FUNC_REMOVECARD = "removeCard";

    public static final String FUNC_REMOVEOWNER = "removeOwner";

    public static final Event CARDREGISTERED_EVENT = new Event("CardRegistered",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event CARDREMOVED_EVENT = new Event("CardRemoved",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event OWNERCHANGED_EVENT = new Event("OwnerChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event OWNERREGISTERED_EVENT = new Event("OwnerRegistered",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event OWNERREMOVED_EVENT = new Event("OwnerRemoved",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j,
                                           Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j,
                                           Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j,
                                           TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j,
                                           TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<CardRegisteredEventResponse> getCardRegisteredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CARDREGISTERED_EVENT, transactionReceipt);
        ArrayList<CardRegisteredEventResponse> responses = new ArrayList<CardRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CardRegisteredEventResponse typedResponse = new CardRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.card = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CardRegisteredEventResponse getCardRegisteredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CARDREGISTERED_EVENT, log);
        CardRegisteredEventResponse typedResponse = new CardRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.card = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<CardRegisteredEventResponse> cardRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCardRegisteredEventFromLog(log));
    }

    public Flowable<CardRegisteredEventResponse> cardRegisteredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CARDREGISTERED_EVENT));
        return cardRegisteredEventFlowable(filter);
    }

    public static List<CardRemovedEventResponse> getCardRemovedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CARDREMOVED_EVENT, transactionReceipt);
        ArrayList<CardRemovedEventResponse> responses = new ArrayList<CardRemovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CardRemovedEventResponse typedResponse = new CardRemovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.card = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CardRemovedEventResponse getCardRemovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CARDREMOVED_EVENT, log);
        CardRemovedEventResponse typedResponse = new CardRemovedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.card = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<CardRemovedEventResponse> cardRemovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCardRemovedEventFromLog(log));
    }

    public Flowable<CardRemovedEventResponse> cardRemovedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CARDREMOVED_EVENT));
        return cardRemovedEventFlowable(filter);
    }

    public static List<OwnerChangedEventResponse> getOwnerChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERCHANGED_EVENT, transactionReceipt);
        ArrayList<OwnerChangedEventResponse> responses = new ArrayList<OwnerChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnerChangedEventResponse typedResponse = new OwnerChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OwnerChangedEventResponse getOwnerChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERCHANGED_EVENT, log);
        OwnerChangedEventResponse typedResponse = new OwnerChangedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.newOwner = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<OwnerChangedEventResponse> ownerChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOwnerChangedEventFromLog(log));
    }

    public Flowable<OwnerChangedEventResponse> ownerChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERCHANGED_EVENT));
        return ownerChangedEventFlowable(filter);
    }

    public static List<OwnerRegisteredEventResponse> getOwnerRegisteredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERREGISTERED_EVENT, transactionReceipt);
        ArrayList<OwnerRegisteredEventResponse> responses = new ArrayList<OwnerRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnerRegisteredEventResponse typedResponse = new OwnerRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OwnerRegisteredEventResponse getOwnerRegisteredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERREGISTERED_EVENT, log);
        OwnerRegisteredEventResponse typedResponse = new OwnerRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OwnerRegisteredEventResponse> ownerRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOwnerRegisteredEventFromLog(log));
    }

    public Flowable<OwnerRegisteredEventResponse> ownerRegisteredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERREGISTERED_EVENT));
        return ownerRegisteredEventFlowable(filter);
    }

    public static List<OwnerRemovedEventResponse> getOwnerRemovedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERREMOVED_EVENT, transactionReceipt);
        ArrayList<OwnerRemovedEventResponse> responses = new ArrayList<OwnerRemovedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnerRemovedEventResponse typedResponse = new OwnerRemovedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OwnerRemovedEventResponse getOwnerRemovedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERREMOVED_EVENT, log);
        OwnerRemovedEventResponse typedResponse = new OwnerRemovedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<OwnerRemovedEventResponse> ownerRemovedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOwnerRemovedEventFromLog(log));
    }

    public Flowable<OwnerRemovedEventResponse> ownerRemovedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERREMOVED_EVENT));
        return ownerRemovedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addCard(String card) {
        final Function function = new Function(
                FUNC_ADDCARD,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, card)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> addCardOnDeploy(String card,
                                                                  String paymentAccount) {
        final Function function = new Function(
                FUNC_ADDCARDONDEPLOY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, card),
                        new org.web3j.abi.datatypes.Address(160, paymentAccount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changeOwner(String previousOwner,
                                                              String newOwner) {
        final Function function = new Function(
                FUNC_CHANGEOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, previousOwner),
                        new org.web3j.abi.datatypes.Address(160, newOwner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> factory() {
        final Function function = new Function(FUNC_FACTORY,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> initOwner(String owner) {
        final Function function = new Function(
                FUNC_INITOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initOwnerOnDeploy(String owner,
                                                                    String paymentAccount) {
        final Function function = new Function(
                FUNC_INITOWNERONDEPLOY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                        new org.web3j.abi.datatypes.Address(160, paymentAccount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> paymentAccountByCard(String param0) {
        final Function function = new Function(FUNC_PAYMENTACCOUNTBYCARD,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<List> paymentAccountsByOwner(String owner) {
        final Function function = new Function(FUNC_PAYMENTACCOUNTSBYOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> removeCard(String card) {
        final Function function = new Function(
                FUNC_REMOVECARD,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, card)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> removeOwner(String owner) {
        final Function function = new Function(
                FUNC_REMOVEOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j,
                                                    Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j,
                                                    TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j,
                                                    Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j,
                                                    TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j,
                                                                  Credentials credentials, ContractGasProvider contractGasProvider, String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j,
                                                                  TransactionManager transactionManager, ContractGasProvider contractGasProvider,
                                                                  String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j,
                                                                  Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j,
                                                                  TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit,
                                                                  String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class CardRegisteredEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String card;
    }

    public static class CardRemovedEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String card;
    }

    public static class OwnerChangedEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String previousOwner;

        public String newOwner;
    }

    public static class OwnerRegisteredEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String owner;
    }

    public static class OwnerRemovedEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String owner;
    }
}