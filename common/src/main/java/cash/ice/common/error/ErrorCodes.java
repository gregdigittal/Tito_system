package cash.ice.common.error;

public final class ErrorCodes {
    private ErrorCodes() {
    }

    @ErrorDescription(service = "api-service", descr = "request params validation failed")
    public static final String EC1001 = "101-IC1116-0001";      // request params validation failed
    @ErrorDescription(service = "api-service", descr = "failed to send payment request to kafka topic")
    public static final String EC1002 = "101-IC1116-0002";      // failed to send payment request to kafka topic
    @ErrorDescription(service = "api-service", descr = "failed to handle request")
    public static final String EC1003 = "101-IC1117-0003";      // failed to handle request
    @ErrorDescription(service = "api-service", descr = "unexpected exception")
    public static final String EC1004 = "101-IC1116-0004";      // unexpected exception
    @ErrorDescription(service = "api-service", descr = "get response failed, request not found")
    public static final String EC1005 = "101-IC1117-0005";      // get response failed, request not found
    @ErrorDescription(service = "api-service", descr = "pvv getting from security module error")
    public static final String EC1006 = "101-IC1146-0006";      // pvv getting from security module error
    @ErrorDescription(service = "api-service", descr = "pin getting from security module error")
    public static final String EC1007 = "101-IC1146-0007";      // pin getting from security module error
    @ErrorDescription(service = "api-service", descr = "Keycloak user creating error")
    public static final String EC1008 = "101-IC1146-0008";      // Keycloak user creating error
    @ErrorDescription(service = "api-service", descr = "cannot generate new number")
    public static final String EC1009 = "101-IC1146-0009";      // cannot generate new number
    @ErrorDescription(service = "api-service", descr = "user entered either unknown ID or wrong password or wrong MFA code or wrong backup code while logging in, or is not authorized")
    public static final String EC1010 = "101-IC1146-0010";      // user entered either unknown ID or wrong password or wrong MFA code or wrong backup code while logging in, or is not authorized
    @ErrorDescription(service = "api-service", descr = "unknown entity type")
    public static final String EC1011 = "101-IC1146-0011";      // unknown entity type
    @ErrorDescription(service = "api-service", descr = "initiator wasn't found")
    public static final String EC1012 = "101-IC1146-0012";      // initiator wasn't found
    @ErrorDescription(service = "api-service", descr = "multiple entities match account number while logging in")
    public static final String EC1013 = "101-IC1146-0013";      // multiple entities match account number while logging in
    @ErrorDescription(service = "api-service", descr = "document with id does not exist")
    public static final String EC1014 = "101-IC1167-0014";      // document with id does not exist
    @ErrorDescription(service = "api-service", descr = "uploading to minio failed")
    public static final String EC1015 = "101-IC1167-0015";      // uploading to minio failed
    @ErrorDescription(service = "api-service", descr = "downloading from minio failed")
    public static final String EC1016 = "101-IC1167-0016";      // downloading from minio failed
    @ErrorDescription(service = "api-service", descr = "provided illegal entity/address/documentType id to document")
    public static final String EC1017 = "101-IC1167-0017";      // provided illegal entity/address/documentType id to document
    @ErrorDescription(service = "api-service", descr = "payment with id does not exist")
    public static final String EC1018 = "101-IC1200-0018";      // payment with id does not exist
    @ErrorDescription(service = "api-service", descr = "bulk payment parsing error from excel file")
    public static final String EC1019 = "101-IC1200-0019";      // bulk payment parsing error from excel file
    @ErrorDescription(service = "api-service", descr = "bulk payment parsing error, wrong stream data")
    public static final String EC1020 = "101-IC1200-0020";      // bulk payment parsing error, wrong stream data
    @ErrorDescription(service = "api-service", descr = "no permissions to do action (no relationship or securityGroup)")
    public static final String EC1021 = "101-IC1200-0021";      // no permissions to do action (no relationship or securityGroup)
    @ErrorDescription(service = "api-service", descr = "account is absent")
    public static final String EC1022 = "101-IC1200-0022";      // account is absent
    @ErrorDescription(service = "api-service", descr = "approver is absent")
    public static final String EC1023 = "101-IC1200-0023";      // approver is absent
    @ErrorDescription(service = "api-service", descr = "unknown payment collection ID")
    public static final String EC1024 = "101-IC1200-0024";      // unknown payment collection ID
    @ErrorDescription(service = "api-service", descr = "no payments assigned to a collection")
    public static final String EC1025 = "101-IC1200-0025";      // no payments assigned to a collection
    @ErrorDescription(service = "api-service", descr = "unsupported payment method")
    public static final String EC1026 = "101-IC1200-0026";      // unsupported payment method
    @ErrorDescription(service = "api-service", descr = "not PENDING payment")
    public static final String EC1027 = "101-IC1200-0027";      // not PENDING payment
    @ErrorDescription(service = "api-service", descr = "wrong security group")
    public static final String EC1028 = "101-IC1200-0028";      // wrong security group
    @ErrorDescription(service = "api-service", descr = "template already loaded")
    public static final String EC1029 = "101-IC1200-0029";      // template already loaded
    @ErrorDescription(service = "api-service", descr = "unknown payment template")
    public static final String EC1030 = "101-IC1200-0030";      // unknown payment template
    @ErrorDescription(service = "api-service", descr = "payment response timeout")
    public static final String EC1031 = "101-IC1116-0031";      // payment response timeout
    @ErrorDescription(service = "api-service", descr = "old password is incorrect")
    public static final String EC1032 = "101-IC1225-0032";      // old password is incorrect
    @ErrorDescription(service = "api-service", descr = "no OTP code is assigned to user")
    public static final String EC1033 = "101-IC1225-0033";      // no OTP code is assigned to user
    @ErrorDescription(service = "api-service", descr = "unknown 'forgot password' / 'activate new user' key provided")
    public static final String EC1034 = "101-IC1225-0034";      // unknown 'forgot password' / 'activate new user' key provided
    @ErrorDescription(service = "api-service", descr = "account is locked for login")
    public static final String EC1035 = "101-IC1225-0035";      // account is locked for login
    @ErrorDescription(service = "api-service", descr = "expired OTP code")
    public static final String EC1036 = "101-IC1225-0036";      // expired OTP code
    @ErrorDescription(service = "api-service", descr = "unknown or unset MFA type")
    public static final String EC1037 = "101-IC1225-0037";      // unknown or unset MFA type
    @ErrorDescription(service = "api-service", descr = "access token is expired")
    public static final String EC1038 = "101-IC1225-0038";      // access token is expired
    @ErrorDescription(service = "api-service", descr = "unknown language is assigned to user")
    public static final String EC1039 = "101-IC1225-0039";      // unknown language is assigned to user
    @ErrorDescription(service = "api-service", descr = "unknown email template")
    public static final String EC1040 = "101-IC1225-0040";      // unknown email template
    @ErrorDescription(service = "api-service", descr = "error generating TOTP QR code")
    public static final String EC1041 = "101-IC1225-0041";      // error generating TOTP QR code
    @ErrorDescription(service = "api-service", descr = "such email already registered")
    public static final String EC1042 = "101-IC1225-0042";      // such email already registered
    @ErrorDescription(service = "api-service", descr = "language key for channel is not available")
    public static final String EC1043 = "101-IC1225-0043";      // language key for channel is not available
    @ErrorDescription(service = "api-service", descr = "dictionary for lookupKey is not available")
    public static final String EC1044 = "101-IC1225-0044";      // dictionary for lookupKey is not available
    @ErrorDescription(service = "api-service", descr = "invalid PIN code, must contain only numbers")
    public static final String EC1045 = "101-IC1225-0045";      // invalid PIN code, must contain only numbers
    @ErrorDescription(service = "api-service", descr = "msisdn is absent")
    public static final String EC1046 = "101-IC1225-0046";      // msisdn is absent
    @ErrorDescription(service = "api-service", descr = "wrong search criteria")
    public static final String EC1047 = "101-IC1225-0047";      // wrong search criteria
    @ErrorDescription(service = "api-service", descr = "wrong entityId")
    public static final String EC1048 = "101-IC1225-0048";      // wrong entityId
    @ErrorDescription(service = "api-service", descr = "user account was not activated")
    public static final String EC1049 = "101-IC1240-0049";      // user account was not activated
    @ErrorDescription(service = "api-service", descr = "max users amount limit exceeded")
    public static final String EC1050 = "101-IC1242-0050";      // max users amount limit exceeded
    @ErrorDescription(service = "api-service", descr = "I/O error while converting to csv")
    public static final String EC1051 = "101-IC1242-0051";      // I/O error while converting to csv
    @ErrorDescription(service = "api-service", descr = "Wrong OTP entered")
    public static final String EC1052 = "101-IC1277-0052";      // Wrong OTP entered
    @ErrorDescription(service = "api-service", descr = "OTP was not validated")
    public static final String EC1053 = "101-IC1277-0053";      // OTP was not validated
    @ErrorDescription(service = "api-service", descr = "Moz Link Tag request does not exist or expired")
    public static final String EC1054 = "101-IC1277-0054";      // Moz Link Tag request does not exist or expired
    @ErrorDescription(service = "api-service", descr = "Device is absent, inactive or no account was linked to it")
    public static final String EC1055 = "101-IC1277-0055";      // Device is absent, inactive or no account was linked to it
    @ErrorDescription(service = "api-service", descr = "Device with such serial number already exists")
    public static final String EC1056 = "101-IC1277-0056";      // Device with such serial number already exists
    @ErrorDescription(service = "api-service", descr = "InitiatorType does not exist")
    public static final String EC1057 = "101-IC1277-0057";      // InitiatorType does not exist
    @ErrorDescription(service = "api-service", descr = "InitiatorCategory does not exist")
    public static final String EC1058 = "101-IC1277-0058";      // InitiatorCategory does not exist
    @ErrorDescription(service = "api-service", descr = "InitiatorStatus does not exist")
    public static final String EC1059 = "101-IC1277-0059";      // InitiatorStatus does not exist
    @ErrorDescription(service = "api-service", descr = "AccountType does not exist")
    public static final String EC1060 = "101-IC1277-0060";      // AccountType does not exist
    @ErrorDescription(service = "api-service", descr = "TransactionCode does not exist")
    public static final String EC1061 = "101-IC1277-0061";      // TransactionCode does not exist
    @ErrorDescription(service = "api-service", descr = "Currency does not exist")
    public static final String EC1062 = "101-IC1277-0062";      // Currency does not exist
    @ErrorDescription(service = "api-service", descr = "Account type does not exist")
    public static final String EC1063 = "101-IC1277-0063";      // Account type does not exist
    @ErrorDescription(service = "api-service", descr = "Such tag is already linked")
    public static final String EC1064 = "101-IC1277-0064";      // Such tag is already linked
    @ErrorDescription(service = "api-service", descr = "Country does not exist")
    public static final String EC1065 = "101-IC1294-0065";      // Country does not exist
    @ErrorDescription(service = "api-service", descr = "Such tag does not exist")
    public static final String EC1066 = "101-IC1308-0066";      // Such tag does not exist
    @ErrorDescription(service = "api-service", descr = "Such tag is already linked")
    public static final String EC1067 = "101-IC1308-0067";      // Such tag is already linked
    @ErrorDescription(service = "api-service", descr = "Wrong account type")
    public static final String EC1068 = "101-IC1310-0068";      // Wrong account type
    @ErrorDescription(service = "api-service", descr = "Wrong transaction limit")
    public static final String EC1069 = "101-IC1310-0069";      // Wrong transaction limit
    @ErrorDescription(service = "api-service", descr = "forbidden operation")
    public static final String EC1070 = "101-IC1310-0070";      // Forbidden operation
    @ErrorDescription(service = "api-service", descr = "User is not agent or operation is not allowed")
    public static final String EC1071 = "101-IC1443-0071";      // User is not agent or operation is not allowed
    @ErrorDescription(service = "api-service", descr = "Invalid route")
    public static final String EC1072 = "101-IC1448-0072";      // Invalid route
    @ErrorDescription(service = "api-service", descr = "Invalid entity")
    public static final String EC1073 = "101-IC1448-0073";      // Invalid entity
    @ErrorDescription(service = "api-service", descr = "Account is not active")
    public static final String EC1074 = "101-IC1448-0074";      // Account is not active
    @ErrorDescription(service = "api-service", descr = "ID type is not provided")
    public static final String EC1075 = "101-IC1449-0075";      // ID type is not provided
    @ErrorDescription(service = "api-service", descr = "Vehicle does not exist")
    public static final String EC1076 = "101-IC1448-0076";      // Vehicle does not exist
    @ErrorDescription(service = "api-service", descr = "Vehicle does not belong to the user")
    public static final String EC1077 = "101-IC1457-0077";      // Vehicle does not belong to the user
    @ErrorDescription(service = "api-service", descr = "User does not have needed security group")
    public static final String EC1078 = "101-IC1457-0078";      // User does not have needed security group
    @ErrorDescription(service = "api-service", descr = "POS device is not linked to vehicle")
    public static final String EC1079 = "101-IC1447-0079";      // POS device is not linked to vehicle
    @ErrorDescription(service = "api-service", descr = "User is not assigned to the vehicle as an owner, driver or collector")
    public static final String EC1080 = "101-IC1447-0080";      // User is not assigned to the vehicle as an owner, driver or collector
    @ErrorDescription(service = "api-service", descr = "resend OTP is allowed only after N seconds")
    public static final String EC1081 = "101-IC1453-0081";      // resend OTP is allowed only after N seconds
    @ErrorDescription(service = "api-service", descr = "Account does not belong to the user")
    public static final String EC1082 = "101-IC1455-0082";      // Account does not belong to the user
    @ErrorDescription(service = "api-service", descr = "Vehicle is not active")
    public static final String EC1083 = "101-IC1452-0083";      // Vehicle is not active
    @ErrorDescription(service = "api-service", descr = "Incorrect journal amount")
    public static final String EC1084 = "101-IC1422-0084";      // Incorrect journal amount
    @ErrorDescription(service = "api-service", descr = "Account assigned to wrong currency")
    public static final String EC1085 = "101-IC1422-0085";      // Account assigned to wrong currency
    @ErrorDescription(service = "api-service", descr = "Incorrect journal ID")
    public static final String EC1086 = "101-IC1422-0086";      // Incorrect journal ID
    @ErrorDescription(service = "api-service", descr = "Incorrect journal status")
    public static final String EC1087 = "101-IC1427-0087";      // Incorrect journal status
    @ErrorDescription(service = "api-service", descr = "Transaction code not found")
    public static final String EC1088 = "101-IC1427-0088";      // Transaction code not found

    @ErrorDescription(service = "fee-service", descr = "invalid request: amount < 0")
    public static final String EC3001 = "103-IC1118-3001";      // invalid request: amount < 0
    @ErrorDescription(service = "fee-service", descr = "invalid request: unknown currency/initiatorType/... DB record")
    public static final String EC3002 = "103-IC1118-3002";      // invalid request: unknown currency/initiatorType/... DB record
    @ErrorDescription(service = "fee-service", descr = "invalid request: null entityId in initiator")
    public static final String EC3003 = "103-IC1118-3003";      // invalid request: null entityId in initiator
    @ErrorDescription(service = "fee-service", descr = "kyc required")
    public static final String EC3004 = "103-IC1118-3004";      // kyc required
    @ErrorDescription(service = "fee-service", descr = "unexpected exception")
    public static final String EC3005 = "103-IC1118-3005";      // unexpected exception
    @ErrorDescription(service = "fee-service", descr = "invalid public api payment method: third party option")
    public static final String EC3006 = "103-IC1118-3006";      // invalid public api payment method: third party option
    @ErrorDescription(service = "fee-service", descr = "invalid public api payment method: identifier type")
    public static final String EC3007 = "103-IC1118-3007";      // invalid public api payment method: identifier type
    @ErrorDescription(service = "fee-service", descr = "public api: unreadable response")
    public static final String EC3008 = "103-IC1118-3008";      // public api: unreadable response
    @ErrorDescription(service = "fee-service", descr = "public api error")
    public static final String EC3009 = "103-IC1118-3009";      // public api error
    @ErrorDescription(service = "fee-service", descr = "public api error")
    public static final String EC3010 = "103-IC1118-3010";      // public api error
    @ErrorDescription(service = "fee-service", descr = "transaction code is inactive")
    public static final String EC3011 = "103-IC1128-3011";      // transaction code is inactive
    @ErrorDescription(service = "fee-service", descr = "initiator is inactive")
    public static final String EC3012 = "103-IC1128-3012";      // initiator is inactive
    @ErrorDescription(service = "fee-service", descr = "original fee charge is inactive or invalid")
    public static final String EC3013 = "103-IC1128-3013";      // original fee charge is inactive or invalid
    @ErrorDescription(service = "fee-service", descr = "invalid request: need partnerId, but it is null")
    public static final String EC3014 = "103-IC1169-3014";      // invalid request: need partnerId, but it is null
    @ErrorDescription(service = "fee-service", descr = "no fees available for the request")
    public static final String EC3015 = "103-IC1142-3015";      // no fees available for the request
    @ErrorDescription(service = "fee-service", descr = "no ORIGINAL fee for the request")
    public static final String EC3016 = "103-IC1142-3016";      // no ORIGINAL fee for the request
    @ErrorDescription(service = "fee-service", descr = "ZimSwitch payment service error")
    public static final String EC3017 = "103-IC1142-3017";      // zimswitch payment service error
    @ErrorDescription(service = "fee-service", descr = "invalid request: need initiator, but it is null")
    public static final String EC3018 = "103-IC1199-3018";      // invalid request: need initiator, but it is null
    @ErrorDescription(service = "fee-service", descr = "Invalid deviceCode or deviceSerial")
    public static final String EC3019 = "103-IC1276-3019";      // Invalid deviceCode or deviceSerial
    @ErrorDescription(service = "fee-service", descr = "wrong initiator type for such transaction code")
    public static final String EC3020 = "103-IC1276-3020";      // wrong initiator type for such transaction code
    @ErrorDescription(service = "fee-service", descr = "Active initiator status is absent")
    public static final String EC3021 = "103-IC1276-3021";      // Active initiator status is absent
    @ErrorDescription(service = "fee-service", descr = "initiator is absent or inactive")
    public static final String EC3022 = "103-IC1276-3022";      // initiator is absent or inactive
    @ErrorDescription(service = "fee-service", descr = "device is inactive or is not linked to account")
    public static final String EC3023 = "103-IC1276-3023";      // device is inactive or is not linked to account
    @ErrorDescription(service = "fee-service", descr = "Account of initiator is absent")
    public static final String EC3024 = "103-IC1276-3024";      // Account of initiator is absent
    @ErrorDescription(service = "fee-service", descr = "Subsidy account type is absent")
    public static final String EC3025 = "103-IC1276-3025";      // Subsidy account type is absent
    @ErrorDescription(service = "fee-service", descr = "Subsidy account is absent")
    public static final String EC3026 = "103-IC1276-3026";      // Subsidy account is absent
    @ErrorDescription(service = "fee-service", descr = "Account balance for subsidy account is not available")
    public static final String EC3027 = "103-IC1276-3027";      // Account balance for subsidy account is not available
    @ErrorDescription(service = "fee-service", descr = "Primary msisdn of initiator is absent")
    public static final String EC3028 = "103-IC1276-3028";      // Primary msisdn of initiator is absent
    @ErrorDescription(service = "fee-service", descr = "Transaction limit exceeded")
    public static final String EC3029 = "103-IC1276-3029";      // Transaction limit exceeded
    @ErrorDescription(service = "fee-service", descr = "invalid request: need meta.account, but it is null")
    public static final String EC3030 = "103-IC1375-3030";      // invalid request: need meta.account, but it is null
    @ErrorDescription(service = "fee-service", descr = "wrong entityId")
    public static final String EC3031 = "103-IC1276-3031";      // wrong entityId
    @ErrorDescription(service = "fee-service", descr = "Account type does not exist")
    public static final String EC3032 = "103-IC1277-3032";      // Account type does not exist
    @ErrorDescription(service = "fee-service", descr = "Transaction for accounts with different currencies")
    public static final String EC3033 = "103-IC1277-3033";      // Transaction for accounts with different currencies
    @ErrorDescription(service = "fee-service", descr = "Incorrect initiator account number provided")
    public static final String EC3034 = "103-IC1566-3033";      // Incorrect initiator account number provided

    @ErrorDescription(service = "ledger-service", descr = "unexpected exception")
    public static final String EC4001 = "104-IC1142-4001";      // unexpected exception
    @ErrorDescription(service = "ledger-service", descr = "insufficient balance")
    public static final String EC4002 = "104-IC1119-4002";      // insufficient balance
    @ErrorDescription(service = "ledger-service", descr = "ledger payment service error")
    public static final String EC4003 = "104-IC1119-4003";      // ledger payment service error
    @ErrorDescription(service = "ledger-service", descr = "unknown payment service")
    public static final String EC4004 = "104-IC1142-4004";      // unknown payment service

    @ErrorDescription(service = "paygo-service", descr = "cannot generate PayGoID")
    public static final String EC5001 = "105-IC1169-0001";      // cannot generate PayGoId
    @ErrorDescription(service = "paygo-service", descr = "PayGO request validation failed")
    public static final String EC5002 = "105-IC1169-0002";      // paygo request validation failed
    @ErrorDescription(service = "paygo-service", descr = "request to PayGO server failed")
    public static final String EC5003 = "105-IC1169-0003";      // request to paygo server failed
    @ErrorDescription(service = "paygo-service", descr = "unknown PayGO exception")
    public static final String EC5004 = "105-IC1169-0004";      // unknown paygo exception
    @ErrorDescription(service = "paygo-service", descr = "unknown PayGO merchant for transaction code exception")
    public static final String EC5005 = "105-IC1169-0005";      // unknown merchant for transaction code exception
    @ErrorDescription(service = "paygo-service", descr = "unknown PayGO merchant credential exception")
    public static final String EC5006 = "105-IC1169-0006";      // unknown merchant credential exception
    @ErrorDescription(service = "paygo-service", descr = "error response from PayGO DS instead of qr64")
    public static final String EC5007 = "105-IC1169-0007";      // error response from PayGo DS instead of qr64
    @ErrorDescription(service = "paygo-service", descr = "pending payment with PayGoId not found")
    public static final String EC5008 = "105-IC1169-0008";      // pending payment with PayGoId not found
    @ErrorDescription(service = "paygo-service", descr = "absent or wrong payment field")
    public static final String EC5009 = "105-IC1169-0009";      // absent or wrong payment field
    @ErrorDescription(service = "paygo-service", descr = "absent or wrong PayGO merchant")
    public static final String EC5010 = "105-IC1169-0010";      // absent or wrong merchant
    @ErrorDescription(service = "paygo-service", descr = "absent or wrong PayGO credential")
    public static final String EC5011 = "105-IC1169-0011";      // absent or wrong credential
    @ErrorDescription(service = "paygo-service", descr = "PayGO DS returned error response")
    public static final String EC5012 = "105-IC1169-0012";      // PayGo DS returned error response

    @ErrorDescription(service = "ecocash-service", descr = "unknown EcoCash exception")
    public static final String EC6001 = "106-IC1182-0001";      // unknown ecocash exception
    @ErrorDescription(service = "ecocash-service", descr = "request validation failed")
    public static final String EC6002 = "106-IC1182-0002";      // request validation failed
    @ErrorDescription(service = "ecocash-service", descr = "no general EcoCash merchant")
    public static final String EC6003 = "106-IC1182-0003";      // no general merchant
    @ErrorDescription(service = "ecocash-service", descr = "no EcoCash pending payment by clientCorrelator")
    public static final String EC6004 = "106-IC1182-0004";      // no ecocash pending payment by clientCorrelator
    @ErrorDescription(service = "ecocash-service", descr = "EcoCash returned error response")
    public static final String EC6005 = "106-IC1182-0005";      // ecocash returned error response
    @ErrorDescription(service = "ecocash-service", descr = "response field validation error")
    public static final String EC6006 = "106-IC1182-0006";      // response field validation error
    @ErrorDescription(service = "ecocash-service", descr = "no EcoCash pending payment by vendorRef to refund")
    public static final String EC6007 = "106-IC1182-0007";      // no ecocash pending payment by vendorRef to refund
    @ErrorDescription(service = "ecocash-service", descr = "payment expired")
    public static final String EC6008 = "106-IC1182-0008";      // payment expired
    @ErrorDescription(service = "ecocash-service", descr = "failed to perform after payment action")
    public static final String EC6009 = "106-IC1182-0009";      // failed to perform after payment action

    @ErrorDescription(service = "onemoney-service", descr = "no pending payment fo OriginatorConversationID")
    public static final String EC7001 = "107-IC1187-0001";      // no pending payment fo OriginatorConversationID
    @ErrorDescription(service = "onemoney-service", descr = "OneMoney returned error result")
    public static final String EC7002 = "107-IC1187-0002";      // onemoney returned error result
    @ErrorDescription(service = "onemoney-service", descr = "failed to perform after payment action")
    public static final String EC7003 = "107-IC1187-0003";      // failed to perform after payment action
    @ErrorDescription(service = "onemoney-service", descr = "unknown OneMoney exception")
    public static final String EC7004 = "107-IC1187-0004";      // unknown onemoney exception
    @ErrorDescription(service = "onemoney-service", descr = "request sending error")
    public static final String EC7005 = "107-IC1187-0005";      // request sending error
    @ErrorDescription(service = "onemoney-service", descr = "received error response")
    public static final String EC7006 = "107-IC1187-0006";      // received error response
    @ErrorDescription(service = "onemoney-service", descr = "payment expired")
    public static final String EC7007 = "107-IC1187-0007";      // payment expired
    @ErrorDescription(service = "onemoney-service", descr = "no OneMoney pending payment by vendorRef to refund")
    public static final String EC7008 = "107-IC1187-0008";      // no onemoney pending payment by vendorRef to refund
    @ErrorDescription(service = "onemoney-service", descr = "polling status returned non successful response")
    public static final String EC7009 = "107-IC1187-0009";      // polling status returned non successful response
    @ErrorDescription(service = "onemoney-service", descr = "No TransactionID for payment to refund")
    public static final String EC7010 = "107-IC1187-0010";      // No TransactionID for payment to refund

    @ErrorDescription(service = "fbc-service", descr = "unknown FlexCube exception")
    public static final String EC8001 = "108-IC1199-0001";      // unknown flexcube exception
    @ErrorDescription(service = "fbc-service", descr = "received error response")
    public static final String EC8002 = "108-IC1199-0002";      // received error response
    @ErrorDescription(service = "fbc-service", descr = "no FBC pool account configured in mongodb")
    public static final String EC8003 = "108-IC1199-0003";      // no fbc pool account configured in mongodb
    @ErrorDescription(service = "fbc-service", descr = "no response for FlexCube GetBalance request")
    public static final String EC8004 = "108-IC1199-0004";      // no response for GetBalance request
    @ErrorDescription(service = "fbc-service", descr = "received error for FlexCube GetBalance request")
    public static final String EC8005 = "108-IC1199-0005";      // received error for GetBalance request

    @ErrorDescription(service = "mpesa-service", descr = "unknown Vodacom MPESA exception")
    public static final String EC9001 = "109-IC1375-0001";      // unknown Vodacom MPESA exception
    @ErrorDescription(service = "mpesa-service", descr = "Vodacom MPESA returned error response")
    public static final String EC9002 = "109-IC1375-0002";      // Vodacom MPESA returned error response
    @ErrorDescription(service = "mpesa-service", descr = "Vodacom MPESA returned bad request")
    public static final String EC9003 = "109-IC1375-0003";      // Vodacom MPESA returned bad request
    @ErrorDescription(service = "mpesa-service", descr = "Wrong transaction code for Vodacom MPESA payment")
    public static final String EC9004 = "109-IC1375-0004";      // Wrong transaction code for Vodacom MPESA payment
    @ErrorDescription(service = "mpesa-service", descr = "Vodacom MPESA payment got to DLT")
    public static final String EC9005 = "109-IC1375-0005";      // Vodacom MPESA payment got to DLT
    @ErrorDescription(service = "mpesa-service", descr = "Payment expired")
    public static final String EC9008 = "109-IC1375-0008";      // Payment expired

    @ErrorDescription(service = "emola-service", descr = "unknown e-Mola exception")
    public static final String EC9101 = "110-IC1436-0001";      // unknown e-Mola exception
    @ErrorDescription(service = "emola-service", descr = "e-Mola returned error response")
    public static final String EC9102 = "110-IC1436-0002";      // e-Mola returned error response
    @ErrorDescription(service = "emola-service", descr = "Wrong transaction code for e-Mola payment")
    public static final String EC9103 = "110-IC1436-0003";      // Wrong transaction code for e-Mola payment

    @ErrorDescription(service = "zim-api-service", descr = "unexpected exception")
    public static final String EC1101 = "111-IC1552-0001";      // unexpected exception
    @ErrorDescription(service = "zim-api-service", descr = "request params validation failed")
    public static final String EC1102 = "111-IC1552-0002";      // request params validation failed
    @ErrorDescription(service = "zim-api-service", descr = "payment does not exist")
    public static final String EC1103 = "111-IC1552-0003";      // payment does not exist
    @ErrorDescription(service = "zim-api-service", descr = "database entity does not exist")
    public static final String EC1104 = "111-IC1552-0004";      // database entity does not exist
    @ErrorDescription(service = "zim-api-service", descr = "ledger execution error")
    public static final String EC1105 = "111-IC1552-0005";      // ledger execution error
    @ErrorDescription(service = "zim-api-service", descr = "manual refund")
    public static final String EC1106 = "111-IC1552-0006";      // manual refund
    @ErrorDescription(service = "zim-api-service", descr = "Payment expired")
    public static final String EC1108 = "111-IC1552-0008";      // payment expired
    @ErrorDescription(service = "zim-api-service", descr = "simulated milestone error")
    public static final String EC1109 = "111-IC1552-0009";      // simulated milestone error
    @ErrorDescription(service = "zim-api-service", descr = "approving SP returned error")
    public static final String EC1110 = "111-IC1552-0010";      // approving SP returned error
    @ErrorDescription(service = "zim-api-service", descr = "SP polling attempts exceeded")
    public static final String EC1111 = "111-IC1552-0011";      // SP polling attempts exceeded
}
