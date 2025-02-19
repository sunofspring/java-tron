package org.tron.core.services.http;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.tron.common.utils.Commons.decodeFromBase58Check;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.UrlEncoded;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionIdList;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.common.crypto.Hash;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.actuator.TransactionFactory;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.TransactionTrace;
import org.tron.core.services.http.JsonFormat.ParseException;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;


@Slf4j(topic = "API")
public class Util {

  public static final String PERMISSION_ID = "Permission_id";
  public static final String VISIBLE = "visible";
  public static final String TRANSACTION = "transaction";
  public static final String TRANSACTION_EXTENSION = "transactionExtension";
  public static final String VALUE = "value";
  public static final String CONTRACT_TYPE = "contractType";
  public static final String EXTRA_DATA = "extra_data";
  public static final String PARAMETER = "parameter";

  // Used for TVM http interfaces
  public static final String OWNER_ADDRESS = "owner_address";
  public static final String CONTRACT_ADDRESS = "contract_address";
  public static final String FUNCTION_SELECTOR = "function_selector";
  public static final String FUNCTION_PARAMETER = "parameter";
  public static final String CALL_DATA = "data";
  public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String APPLICATION_JSON = "application/json";

  public static String printTransactionFee(String transactionFee) {
    JSONObject jsonObject = new JSONObject();
    JSONObject receipt = JSONObject.parseObject(transactionFee);
    jsonObject.put("Receipt", receipt.get("receipt"));
    return jsonObject.toJSONString();
  }

  public static String printErrorMsg(Exception e) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("Error", e.getClass() + " : " + e.getMessage());
    return jsonObject.toJSONString();
  }

  public static String printBlockList(BlockList list, boolean selfType) {
    List<Block> blocks = list.getBlockList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));
    JSONArray jsonArray = new JSONArray();
    blocks.stream().forEach(block -> jsonArray.add(printBlockToJSON(block, selfType)));
    jsonObject.put("block", jsonArray);

    return jsonObject.toJSONString();
  }

  public static String printBlock(Block block, boolean selfType) {
    return printBlockToJSON(block, selfType).toJSONString();
  }

  public static JSONObject printBlockToJSON(Block block, boolean selfType) {
    BlockCapsule blockCapsule = new BlockCapsule(block);
    String blockID = ByteArray.toHexString(blockCapsule.getBlockId().getBytes());
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(block, selfType));
    jsonObject.put("blockID", blockID);
    if (!blockCapsule.getTransactions().isEmpty()) {
      jsonObject.put("transactions",
          printTransactionListToJSON(blockCapsule.getTransactions(), selfType));
    }
    return jsonObject;
  }

  public static String printTransactionList(TransactionList list, boolean selfType) {
    List<Transaction> transactions = list.getTransactionList();
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));
    JSONArray jsonArray = new JSONArray();
    transactions.stream()
        .forEach(transaction -> jsonArray.add(printTransactionToJSON(transaction, selfType)));
    jsonObject.put(TRANSACTION, jsonArray);

    return jsonObject.toJSONString();
  }

  public static String printTransactionIdList(TransactionIdList list, boolean selfType) {
    JSONObject jsonObject = JSONObject.parseObject(JsonFormat.printToString(list, selfType));

    return jsonObject.toJSONString();
  }

  public static JSONArray printTransactionListToJSON(List<TransactionCapsule> list,
      boolean selfType) {
    JSONArray transactions = new JSONArray();
    list.stream().forEach(transactionCapsule -> transactions
        .add(printTransactionToJSON(transactionCapsule.getInstance(), selfType)));
    return transactions;
  }

  public static String printTransaction(Transaction transaction, boolean selfType) {
    return printTransactionToJSON(transaction, selfType).toJSONString();
  }

  public static String printTransaction(Transaction transaction) {
    return printTransactionToJSON(transaction, true).toJSONString();
  }

  public static String printCreateTransaction(Transaction transaction, boolean selfType) {
    JSONObject jsonObject = printTransactionToJSON(transaction, selfType);
    jsonObject.put(VISIBLE, selfType);
    return jsonObject.toJSONString();
  }

  public static String printTransactionExtention(TransactionExtention transactionExtention,
      boolean selfType) {
    String string = JsonFormat.printToString(transactionExtention, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    if (transactionExtention.getResult().getResult()) {
      JSONObject transactionObject = printTransactionToJSON(transactionExtention.getTransaction(),
          selfType);
      transactionObject.put(VISIBLE, selfType);
      jsonObject.put(TRANSACTION, transactionObject);
    }
    return jsonObject.toJSONString();
  }

  public static String printEstimateEnergyMessage(GrpcAPI.EstimateEnergyMessage message,
      boolean selfType) {
    return JsonFormat.printToString(message, selfType);
  }

  public static String printTransactionSignWeight(TransactionSignWeight transactionSignWeight,
      boolean selfType) {
    String string = JsonFormat.printToString(transactionSignWeight, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    JSONObject jsonObjectExt = jsonObject.getJSONObject(TRANSACTION);
    jsonObjectExt.put(TRANSACTION,
        printTransactionToJSON(transactionSignWeight.getTransaction().getTransaction(), selfType));
    jsonObject.put(TRANSACTION, jsonObjectExt);
    return jsonObject.toJSONString();
  }

  public static String printTransactionApprovedList(TransactionApprovedList transactionApprovedList,
      boolean selfType) {
    String string = JsonFormat.printToString(transactionApprovedList, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    JSONObject jsonObjectExt = jsonObject.getJSONObject(TRANSACTION);
    jsonObjectExt.put(TRANSACTION,
        printTransactionToJSON(transactionApprovedList.getTransaction().getTransaction(),
            selfType));
    jsonObject.put(TRANSACTION, jsonObjectExt);
    return jsonObject.toJSONString();
  }

  public static byte[] generateContractAddress(Transaction trx, byte[] ownerAddress) {
    // get tx hash
    byte[] txRawDataHash = Sha256Hash
        .of(CommonParameter.getInstance().isECKeyCryptoEngine(), trx.getRawData().toByteArray())
        .getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  public static JSONObject printTransactionToJSON(Transaction transaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject
        .parseObject(JsonFormat.printToString(transaction, selfType));
    JSONArray contracts = new JSONArray();
    transaction.getRawData().getContractList().stream().forEach(contract -> {
      try {
        JSONObject contractJson = null;
        Any contractParameter = contract.getParameter();
        switch (contract.getType()) {
          case CreateSmartContract:
            CreateSmartContract deployContract = contractParameter
                .unpack(CreateSmartContract.class);
            contractJson = JSONObject
                .parseObject(JsonFormat.printToString(deployContract, selfType));
            byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
            byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
            jsonTransaction.put(CONTRACT_ADDRESS, ByteArray.toHexString(contractAddress));
            break;
          default:
            Class clazz = TransactionFactory.getContract(contract.getType());
            if (clazz != null) {
              contractJson = JSONObject
                  .parseObject(JsonFormat.printToString(contractParameter.unpack(clazz), selfType));
            }
            break;
        }

        JSONObject parameter = new JSONObject();
        parameter.put(VALUE, contractJson);
        parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
        JSONObject jsonContract = new JSONObject();
        jsonContract.put(PARAMETER, parameter);
        jsonContract.put("type", contract.getType());
        if (contract.getPermissionId() > 0) {
          jsonContract.put(PERMISSION_ID, contract.getPermissionId());
        }
        contracts.add(jsonContract);
      } catch (InvalidProtocolBufferException e) {
        logger.debug("InvalidProtocolBufferException: {}", e.getMessage());
      }
    });

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
    jsonTransaction.put("raw_data_hex", rawDataHex);
    String txID = ByteArray.toHexString(Sha256Hash
        .hash(CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    return jsonTransaction;
  }

  /**
   * Note: the contracts of the returned transaction may be empty
   */
  public static Transaction packTransaction(String strTransaction, boolean selfType) {
    JSONObject jsonTransaction = JSON.parseObject(strTransaction);
    JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
    JSONArray contracts = new JSONArray();
    JSONArray rawContractArray = rawData.getJSONArray("contract");

    String contractType = null;
    for (int i = 0; i < rawContractArray.size(); i++) {
      try {
        JSONObject contract = rawContractArray.getJSONObject(i);
        JSONObject parameter = contract.getJSONObject(PARAMETER);
        contractType = contract.getString("type");
        if (StringUtils.isEmpty(contractType)) {
          logger.debug("no type in the transaction, ignore");
          continue;
        }

        Any any = null;
        Class clazz = TransactionFactory.getContract(ContractType.valueOf(contractType));
        if (clazz != null) {
          Constructor<GeneratedMessageV3> constructor = clazz.getDeclaredConstructor();
          constructor.setAccessible(true);
          GeneratedMessageV3 generatedMessageV3 = constructor.newInstance();
          Message.Builder builder = generatedMessageV3.toBuilder();
          JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(), builder, selfType);
          any = Any.pack(builder.build());
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put(VALUE, value);
          contract.put(PARAMETER, parameter);
          contracts.add(contract);
        }
      } catch (IllegalArgumentException e) {
        logger.debug("invalid contractType: {}", contractType);
      } catch (ParseException e) {
        logger.debug("ParseException: {}", e.getMessage());
      } catch (ClassCastException e) {
        logger.debug("ClassCastException: {}", e.getMessage());
      } catch (JSONException e) {
        logger.debug("JSONException: {}", e.getMessage());
      } catch (Exception e) {
        logger.warn("{}", contractType, e);
      }
    }
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    try {
      JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder, selfType);
      return transactionBuilder.build();
    } catch (ParseException e) {
      logger.debug("ParseException: {}", e.getMessage());
      return null;
    }
  }

  public static void checkBodySize(String body) throws Exception {
    CommonParameter parameter = Args.getInstance();
    if (body.getBytes().length > parameter.getMaxMessageSize()) {
      throw new Exception("body size is too big, the limit is " + parameter.getMaxMessageSize());
    }
  }

  public static boolean getVisible(final HttpServletRequest request) {
    boolean visible = false;
    if (StringUtil.isNotBlank(request.getParameter(VISIBLE))) {
      visible = Boolean.valueOf(request.getParameter(VISIBLE));
    }
    return visible;
  }

  public static boolean existVisible(final HttpServletRequest request) {
    return Objects.nonNull(request.getParameter(VISIBLE));
  }

  public static boolean getVisiblePost(final String input) {
    boolean visible = false;
    if (StringUtil.isNotBlank(input)) {
      JSONObject jsonObject = JSON.parseObject(input);
      if (jsonObject.containsKey(VISIBLE)) {
        visible = Boolean.parseBoolean(jsonObject.getString(VISIBLE));
      }
    }

    return visible;
  }

  public static String getContractType(final String input) {
    String contractType = null;
    JSONObject jsonObject = JSON.parseObject(input);
    if (jsonObject.containsKey(CONTRACT_TYPE)) {
      contractType = jsonObject.getString(CONTRACT_TYPE);
    }
    return contractType;
  }

  public static String getHexAddress(final String address) {
    if (address != null) {
      byte[] addressByte = decodeFromBase58Check(address);
      return ByteArray.toHexString(addressByte);
    } else {
      return null;
    }
  }

  public static String getHexString(final String string) {
    return ByteArray.toHexString(ByteString.copyFromUtf8(string).toByteArray());
  }

  public static Transaction setTransactionPermissionId(JSONObject jsonObject,
      Transaction transaction) {
    if (jsonObject.containsKey(PERMISSION_ID)) {
      int permissionId = jsonObject.getInteger(PERMISSION_ID);
      return setTransactionPermissionId(permissionId, transaction);
    }

    return transaction;
  }

  public static Transaction setTransactionPermissionId(int permissionId, Transaction transaction) {
    if (permissionId > 0) {
      Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
      Transaction.Contract.Builder contract = raw.getContract(0).toBuilder()
          .setPermissionId(permissionId);
      raw.clearContract();
      raw.addContract(contract);
      return transaction.toBuilder().setRawData(raw).build();
    }

    return transaction;
  }

  public static Transaction setTransactionExtraData(JSONObject jsonObject,
      Transaction transaction, boolean visible) {
    if (jsonObject.containsKey(EXTRA_DATA)) {
      String data = jsonObject.getString(EXTRA_DATA);
      return setTransactionExtraData(data, transaction, visible);
    }

    return transaction;
  }

  public static Transaction setTransactionExtraData(String data, Transaction transaction,
      boolean visible) {
    if (data.length() > 0) {
      Transaction.raw.Builder raw = transaction.getRawData().toBuilder();
      if (visible) {
        raw.setData(ByteString.copyFrom(data.getBytes()));
      } else {
        raw.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));
      }
      return transaction.toBuilder().setRawData(raw).build();
    }

    return transaction;
  }

  public static boolean getVisibleOnlyForSign(JSONObject jsonObject) {
    boolean visible = false;
    if (jsonObject.containsKey(VISIBLE)) {
      visible = jsonObject.getBoolean(VISIBLE);
    } else if (jsonObject.getJSONObject(TRANSACTION).containsKey(VISIBLE)) {
      visible = jsonObject.getJSONObject(TRANSACTION).getBoolean(VISIBLE);
    }
    return visible;
  }

  public static String parseMethod(String methodSign, String input) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    //System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (StringUtils.isEmpty(input)) {
      return Hex.toHexString(selector);
    }

    return Hex.toHexString(selector) + input;
  }

  public static long getJsonLongValue(final JSONObject jsonObject, final String key) {
    return getJsonLongValue(jsonObject, key, false);
  }

  public static long getJsonLongValue(JSONObject jsonObject, String key, boolean required) {
    BigDecimal bigDecimal = jsonObject.getBigDecimal(key);
    if (required && bigDecimal == null) {
      throw new InvalidParameterException("key [" + key + "] does not exist");
    }
    return (bigDecimal == null) ? 0L : bigDecimal.longValueExact();
  }

  public static String getMemo(byte[] memo) {
    int index = memo.length;
    for (; index > 0; --index) {
      if (memo[index - 1] != 0) {
        break;
      }
    }

    byte[] inputCheck = new byte[index];
    System.arraycopy(memo, 0, inputCheck, 0, index);
    return new String(inputCheck, Charset.forName("UTF-8"));
  }

  public static void processError(Exception e, HttpServletResponse response) {
    logger.debug("Exception: {}", e.getMessage());
    try {
      response.getWriter().println(Util.printErrorMsg(e));
    } catch (IOException ioe) {
      logger.debug("IOException: {}", ioe.getMessage());
    }
  }

  public static String convertOutput(Account account) {
    if (account.getAssetIssuedID().isEmpty()) {
      return JsonFormat.printToString(account, false);
    } else {
      JSONObject accountJson = JSONObject.parseObject(JsonFormat.printToString(account, false));
      String assetId = accountJson.get("asset_issued_ID").toString();
      accountJson.put("asset_issued_ID",
          ByteString.copyFrom(ByteArray.fromHexString(assetId)).toStringUtf8());
      return accountJson.toJSONString();
    }
  }

  public static void printAccount(Account reply, HttpServletResponse response, Boolean visible)
      throws java.io.IOException {
    if (reply != null) {
      if (visible) {
        response.getWriter().println(JsonFormat.printToString(reply, true));
      } else {
        response.getWriter().println(convertOutput(reply));
      }
    } else {
      response.getWriter().println("{}");
    }
  }

  public static byte[] getAddress(HttpServletRequest request) throws Exception {
    byte[] address = null;
    String addressParam = "address";
    String addressStr = checkGetParam(request, addressParam);
    if (StringUtils.isNotBlank(addressStr)) {
      if (StringUtils.startsWith(addressStr, Constant.ADD_PRE_FIX_STRING_MAINNET)) {
        address = Hex.decode(addressStr);
      } else {
        address = decodeFromBase58Check(addressStr);
      }
    }
    return address;
  }

  private static String checkGetParam(HttpServletRequest request, String key) throws Exception {
    String method = request.getMethod();
    String value = null;

    if (HttpMethod.GET.toString().toUpperCase().equalsIgnoreCase(method)) {
      return request.getParameter(key);
    }
    if (HttpMethod.POST.toString().toUpperCase().equals(method)) {
      String contentType = request.getContentType();
      if (StringUtils.isBlank(contentType)) {
        return null;
      }
      if (APPLICATION_JSON.toLowerCase().contains(contentType)) {
        value = getRequestValue(request);
        if (StringUtils.isBlank(value)) {
          return null;
        }

        JSONObject jsonObject = JSON.parseObject(value);
        if (jsonObject != null) {
          return jsonObject.getString(key);
        }
      } else if (APPLICATION_FORM_URLENCODED.toLowerCase().contains(contentType)) {
        return request.getParameter(key);
      } else {
        return null;
      }
    }
    return value;
  }

  public static String getRequestValue(HttpServletRequest request) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
    String line;
    StringBuilder sb = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }
    return sb.toString();
  }

  public static List<Log> convertLogAddressToTronAddress(TransactionInfo transactionInfo) {
    List<Log> newLogList = new ArrayList<>();

    for (Log log : transactionInfo.getLogList()) {
      Log.Builder logBuilder = Log.newBuilder();
      logBuilder.setData(log.getData());
      logBuilder.addAllTopics(log.getTopicsList());

      byte[] oldAddress = log.getAddress().toByteArray();
      if (oldAddress.length == 0 || oldAddress.length > 20) {
        logBuilder.setAddress(log.getAddress());
      } else {
        byte[] newAddress = new byte[20];
        int start = 20 - oldAddress.length;
        System.arraycopy(oldAddress, 0, newAddress, start, oldAddress.length);
        logBuilder
            .setAddress(ByteString.copyFrom(TransactionTrace.convertToTronAddress(newAddress)));
      }

      newLogList.add(logBuilder.build());
    }

    return newLogList;
  }

  /**
   * Validate parameters for trigger constant and estimate energy
   * - Rule-1: owner address must be set
   * - Rule-2: either contract address is set or call data is set
   * - Rule-3: if try to deploy, function selector and call data can not be both set
   * @param contract parameters in json format
   * @throws InvalidParameterException if validation is not passed, this kind of exception is thrown
   */
  public static void validateParameter(String contract) throws InvalidParameterException {
    JSONObject jsonObject = JSONObject.parseObject(contract);
    if (StringUtils.isEmpty(jsonObject.getString(OWNER_ADDRESS))) {
      throw new InvalidParameterException(OWNER_ADDRESS + " isn't set.");
    }
    if (StringUtils.isEmpty(jsonObject.getString(CONTRACT_ADDRESS))
        && StringUtils.isEmpty(jsonObject.getString(CALL_DATA))) {
      throw new InvalidParameterException("At least one of "
          + CONTRACT_ADDRESS + " and " + CALL_DATA + " must be set.");
    }
    if (StringUtils.isEmpty(jsonObject.getString(CONTRACT_ADDRESS))
        && !StringUtils.isEmpty(jsonObject.getString(FUNCTION_SELECTOR))
        && !StringUtils.isEmpty(jsonObject.getString(CALL_DATA))) {
      throw new InvalidParameterException("While trying to deploy, "
          + FUNCTION_SELECTOR + " and " + CALL_DATA + " can not be both set.");
    }
  }

  public static String getJsonString(String str) {
    if (StringUtils.isEmpty(str)) {
      return EMPTY;
    }
    MultiMap<String> params = new MultiMap<>();
    UrlEncoded.decodeUtf8To(str, params);
    JSONObject json = new JSONObject();
    for (Map.Entry<String, List<String>> entry : params.entrySet()) {
      String key = entry.getKey();
      List<String> values = entry.getValue();
      if (values.size() == 1) {
        json.put(key, values.get(0));
      } else {
        json.put(key, values);
      }
    }
    return json.toString();
  }

}
