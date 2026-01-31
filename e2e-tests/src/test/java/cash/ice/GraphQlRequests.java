package cash.ice;

public class GraphQlRequests {

    public static final String loginRequestStr = """
            mutation {
                loginUserMoz(request: {
                    username: "%s"
                    password: "%s"
                }) {
                    status
                    mfaType
                    locale
                    accessToken {
                        tokenType
                        token
                        expiresIn
                        refreshToken
                        refreshExpiresIn
                        error
                        errorDescription
                    }
                }
            }
            """;

    public static final String refreshAccessTokenRequestStr = """
            mutation {
                refreshAccessToken(refreshToken: "%s") {
                    status
                    mfaType
                    accessToken {
                        tokenType
                        token
                        expiresIn
                        refreshToken
                        refreshExpiresIn
                        error
                        errorDescription
                    }
                }
            }
            """;

    public static final String invalidateAccessTokenRequestStr = """
            mutation {
                invalidateRefreshToken(refreshToken: "%s") {
                    status
                    mfaType
                    accessToken {
                        tokenType
                        token
                        expiresIn
                        refreshToken
                        refreshExpiresIn
                        error
                        errorDescription
                    }
                }
            }
            """;

    public static final String mozUserDetailsRequestStr = """
            query {
                userMoz {                   # current user
                    id
                    firstName
                    lastName
                    email
                    msisdn {
                        msisdn
                    }
                    idNumber
                    idTypeId
                    idType {
                        description
                    }
                    accounts {
                        id
                        accountNumber
                        accountType {
                            name
                            currencyId
                        }
                        balance
                        createdDate
                    }
                    entityTypeId
                    entityType {
                        description
                    }
                    relationships {
                        partnerAccountId
                        securityGroups
                    }
                    status
                    loginStatus
                    locale
                    createdDate
                }
            }
            """;

    public static final String mozUpdateLocaleRequestStr = """
            mutation {
                updateEntityLocale(locale: "%s") {
                    id
                    firstName
                    lastName
                    email
                    locale
                }
            }
            """;

    public static final String mozGetPosDevices = """
            query {
                userPosDevicesMoz {
                    total
                    content {
                        id
                        code
                        serial
                        status
                        account {
                            accountNumber
                        }
                        metaData
                        createdDate
                    }
                }
            }
            """;

    public static final String mozGetPaymentDevices = """
        query {
            userPaymentDevicesMoz {
                total
                content {
                    initiatorType {
                        description
                    }
                    initiatorCategory {
                        category
                    }
                    identifier
                    initiatorStatus {
                        name
                    }
                }
            }
        }
        """;

    public static final String mozGetStatement = """
        query {
            userStatementMoz(accountType: "%s", currency: "%s") {
                total
                content {
                    id
                    statementDate
                    transactionCodeId
                    transactionCode {
                        code
                    }
                    amount
                    lines {
                        id
                        transactionCode {
                            code
                        }
                        description
                        amount
                    }
                }
            }
        }
        """;

    public static final String mozLinkTagStart = """
            mutation {
                linkTagStartMoz(
                    device: "%s",
                    accountNumber: "%s",
                    dateTime: "%s"
                )
            }
            """;

    public static final String mozLinkTagOtp = """
            mutation {
                linkTagValidateOtpMoz(
                    requestId: "%s",
                    otp: "%s"
                ) {
                    prepaidBalance
                    subsidyBalance
                    firstName
                    lastName
                }
            }
            """;

    public static final String mozLinkTagRegister = """
            mutation {
                linkTagRegisterTagMoz(
                    requestId: "%s",
                    tagNumber: "%s"
                ) {
                    id
                }
            }
            """;

    public static final String mozMakePayment = """
            mutation {
                makePaymentMoz(paymentRequest: {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s"
                    currency: "%s",
                    amount: %s,
                    partnerId: "1",
                    apiVersion: "1",
                    date: "%s",
                    metaData: {
                        deviceCode: "%s",
                        requestBalanceAccountTypes: []
                    }
                }) {
                    vendorRef
                    status
                    message
                    transactionId
                    balance
                    date
                }
            }
            """;

    public static final String mozGetRegAgreement = """
            query {
                userRegistrationAgreement(
                    locale: "%s",
                    accountType: %s
                ) {
                    value
                }
            }
            """;

    public static final String sendOtpMobile = """
            mutation {
                sendOtp(
                    otpType: %s,
                    msisdn: "%s"
                    resend: %s
                )
            }
            """;

    public static final String sendOtpEntity = """
            mutation {
                sendOtpToEntity(
                    otpType: %s,
                    entityId: %s
                )
            }
            """;

    public static final String sendOtpAccount = """
            mutation {
                sendOtpToAccount(
                    otpType: %s,
                    accountNumber: "%s"
                )
            }
            """;

    public static final String mozRegIndividualUser = """
            mutation {
                registerIndividualUserMoz(
                    otp: "%s",
                    user: {
                        accountType: %s,
                        firstName: "%s",
                        lastName: "%s",
                        idType: %s,
                        idNumber: "%s",
                        idUploadDocumentId: %s,
                        nuit: "%s",
                        nuitUploadDocumentId: %s,
                        mobile: "%s",
                        email: "%s",
                        pin: "%s",
                        locale: "%s"
                    }
                ) {
                    id
                    firstName
                    lastName
                    email
                    msisdn {
                        msisdn
                    }
                    idNumber
                    idTypeId
                    idType {
                        description
                    }
                    accounts {
                        id
                        accountNumber
                        accountType {
                            name
                            currencyId
                        }
                        createdDate
                    }
                    entityTypeId
                    entityType {
                        description
                    }
                    relationships {
                        partnerAccountId
                        securityGroupMoz {
                            id
                            name
                            active
                            rightsList
                        }
                    }
                    status
                    loginStatus
                    locale
                    createdDate
                }
            }
            """;

    public static final String mozRegCorporateUser = """
            mutation {
                registerCorporateUserMoz(
                    otp: "%s",
                    company: {
                        name: "%s",
                        nuel: "%s",
                        nuelUploadDocumentId: %s,
                        nuit: "%s",
                        nuitUploadDocumentId: %s,
                        mobile: "%s",
                        email: "%s",
                        address: {
                            countryId: %s,
                            city: "%s",
                            postalCode: "%s",
                            address1: "%s",
                            address2: "%s",
                            notes: "%s"
                        }
                    },
                    representative: {
                        accountType: %s,
                        firstName: "%s",
                        lastName: "%s",
                        idType: %s,
                        idNumber: "%s",
                        idUploadDocumentId: %s,
                        nuit: "%s",
                        nuitUploadDocumentId: %s,
                        mobile: "%s",
                        email: "%s",
                        pin: "%s",
                        locale: "%s"
                    }
                ) {
                    id
                    firstName
                    lastName
                    email
                    msisdn {
                        msisdn
                    }
                    idNumber
                    idTypeId
                    idType {
                        description
                    }
                    accounts {
                        id
                        accountNumber
                        accountType {
                            name
                            description
                            currencyId
                        }
                        createdDate
                    }
                    entityTypeId
                    entityType {
                        description
                    }
                    relationships {
                        partnerAccountId
                        securityGroupMoz {
                            id
                            name
                            active
                            rightsList
                        }
                    }
                    status
                    loginStatus
                    locale
                    createdDate
                }
            }
            """;

    public static final String mozRegIndividualUserByStaff = """
            mutation {
                registerIndividualUserMoz(
                    user: {
                        accountType: %s,
                        firstName: "%s",
                        lastName: "%s",
                        idType: %s,
                        idNumber: "%s",
                        idUploadDocumentId: %s,
                        nuit: "%s",
                        nuitUploadDocumentId: %s,
                        mobile: "%s",
                        email: "%s",
                        locale: "%s"
                    },
                    optionalData: {
                        status: %s,
                        loginStatus: %s,
                        gender: %s,
                        citizenshipCountryId: %s,
                        contactName: "%s",
                        altContactName: "%s",
                        altMobile: "%s",
                        authorisationType: %s,
                        corporateFee: %s,
                        transactionLimitTier: %s,
                        kycStatus: %s,
                        company: "%s"
                        address: {
                            countryId: %s
                            city: "%s"
                            postalCode: "%s"
                            address1: "%s"
                            address2: "%s"
                            notes: "%s"
                        }
                    }
                ) {
                    id
                    email
                    status
                    loginStatus
                    entityTypeId
                    entityType {
                        description
                    }
                    firstName
                    lastName
                    idTypeId
                    idType {
                        description
                    }
                    idNumber
                    kycStatusId
                    msisdn {
                        msisdnType
                        msisdn
                        description
                    }
                    gender
                    citizenshipCountryId
                    birthDate
                    address {
                        id
                        entityId
                        countryId
                        country {
                            isoCode
                            name
                        }
                        city
                        postalCode
                        addressType
                        address1
                        address2
                        notes
                    }
                    mfaType
                    mfaBackupCodes
                    createdDate
                    metaData
                    accounts {
                        id
                        accountNumber
                        accountStatus
                        dailyLimit
                        balanceMinimumEnforce
                        authorisationType
                        createdDate
                        accountType {
                            name
                            currency {
                                isoCode
                            }
                            active
                        }
                        authorisationType
                        balance
                        initiators {
                            id
                            accountId
                            initiatorTypeId
                            initiatorType {
                                id
                                description
                                active
                                entityId
                            }
                            identifier
                            initiatorCategoryId
                            initiatorCategory {
                                id
                                category
                            }
                            initiatorStatusId
                            initiatorStatus {
                                id
                                name
                                permitTransaction
                                active
                            }
                            pvv
                            notes
                            createdDate
                            startDate
                            expiryDate
                            metaData
                            configuration
                        }
                    }
                    relationships {
                        partnerAccountId
                        securityGroupMoz {
                            id
                            name
                            active
                            rightsList
                        }
                    }
                }
            }
            """;

    public static final String mozRegCorporateUserByStaff = """
            mutation {
                registerCorporateUserMoz(
                    company: {
                        name: "%s",
                        nuel: "%s",
                        nuelUploadDocumentId: %s,
                        nuit: "%s",
                        nuitUploadDocumentId: %s,
                        mobile: "%s",
                        email: "%s",
                        address: {
                            countryId: %s,
                            city: "%s",
                            postalCode: "%s",
                            address1: "%s",
                            address2: "%s",
                            notes: "%s"
                        }
                    },
                    representative: {
                        accountType: %s,
                        firstName: "%s",
                        lastName: "%s",
                        idType: %s,
                        idNumber: "%s",
                        idUploadDocumentId: %s,
                        nuit: "%s",
                        nuitUploadDocumentId: %s,
                        mobile: "%s",
                        email: "%s",
                        locale: "%s"
                    },
                    optionalData: {
                        status: %s,
                        loginStatus: %s,
                        gender: %s,
                        citizenshipCountryId: %s,
                        contactName: "%s",
                        altContactName: "%s",
                        altMobile: "%s",
                        authorisationType: %s,
                        corporateFee: %s,
                        transactionLimitTier: %s,
                        kycStatus: %s,
                        address: {
                            countryId: %s
                            city: "%s"
                            postalCode: "%s"
                            address1: "%s"
                            address2: "%s"
                            notes: "%s"
                        }
                    }
                ) {
                    id
                    email
                    status
                    loginStatus
                    entityTypeId
                    entityType {
                        description
                    }
                    firstName
                    lastName
                    idTypeId
                    idType {
                        description
                    }
                    idNumber
                    kycStatusId
                    msisdn {
                        msisdnType
                        msisdn
                        description
                    }
                    gender
                    citizenshipCountryId
                    birthDate
                    address {
                        id
                        entityId
                        countryId
                        country {
                            isoCode
                            name
                        }
                        city
                        postalCode
                        addressType
                        address1
                        address2
                        notes
                    }
                    mfaType
                    mfaBackupCodes
                    createdDate
                    metaData
                    accounts {
                        id
                        accountNumber
                        accountStatus
                        dailyLimit
                        balanceMinimumEnforce
                        authorisationType
                        createdDate
                        accountType {
                            name
                            currency {
                                isoCode
                            }
                            active
                        }
                        authorisationType
                        balance
                        initiators {
                            id
                            accountId
                            initiatorTypeId
                            initiatorType {
                                id
                                description
                                active
                                entityId
                            }
                            identifier
                            initiatorCategoryId
                            initiatorCategory {
                                id
                                category
                            }
                            initiatorStatusId
                            initiatorStatus {
                                id
                                name
                                permitTransaction
                                active
                            }
                            pvv
                            notes
                            createdDate
                            startDate
                            expiryDate
                            metaData
                            configuration
                        }
                    }
                    relationships {
                        partnerAccountId
                        securityGroupMoz {
                            id
                            name
                            active
                            rightsList
                        }
                    }
                }
            }
            """;

    public static final String mozGetRoutes = """
            query {
                routesMoz {
                    id
                    name
                    active
                    details {
                        operatorType
                        minDistance
                        minFare
                        farePerKm
                        maxFare
                        maxDistance
                    }
                }
            }
            """;

    public static final String mozCreateVehicle = """
            mutation {
                createVehicle(vehicle: {
                    vrn: "%s",
                    make: "%s",
                    model: "%s",
                    vehicleType: %s,
                    routeId: %s,
                    driverEntityId: %s,
                    collectorEntityId: %s
                }) {
                    id
                }
            }
            """;

    public static final String mozGetVehicles = """
            query {
                vehicles {
                    total
                    content {
                        id
                        vrn
                        make
                        model
                        vehicleType
                        routeId
                        route {
                            name
                        }
                        driverEntityId
                        driverEntity {
                            firstName
                            lastName
                        }
                        collectorEntityId
                        collectorEntity {
                            firstName
                            lastName
                        }
                        accountId
                        account {
                            accountNumber
                        }
                        status
                    }
                }
            }
            """;

    public static final String mozDeleteVehicle = """
            mutation {
                deleteVehicle(id: %s) {
                    id
                }
            }
            """;

    public static final String mozCreatePosDevice = """
            mutation {
                registerDeviceMoz(request: {
                    serialNumber: "%s",
                    metaData: {
                        productNumber: "%s",
                        model: "%s",
                        bootVersion: "%s",
                        cpuType: "%s",
                        rfidVersion: "%s",
                        osVersion: "%s",
                        imei: "%s",
                        imsi: "%s"
                    }
                })
            }
            """;

    public static final String mozLinkPosDeviceByAgent = """
            mutation {
                linkPosDeviceMoz(
                    posDeviceSerial: "%s",
                    entityId: %s,
                    otp: "%s"
                ) {
                    id
                    serial
                    status
                    account {
                        entity {
                            id
                            firstName
                            lastName
                        }
                    },
                    vehicle {
                        id
                        vrn
                        vehicleType
                        routeId
                        route {
                            name
                        }
                        driverEntityId
                        driverEntity {
                            firstName
                            lastName
                        }
                        collectorEntityId
                        collectorEntity {
                            firstName
                            lastName
                        }
                    }
                }
            }
            """;

    public static final String mozLinkVehicleToPosDeviceByOwner = """
            mutation {
                linkVehicleToPosDeviceMoz(
                    posDeviceSerial: "%s",
                    vehicleId: %s
                ) {
                    id
                    serial
                    status
                    account {
                        entity {
                            id
                            firstName
                            lastName
                        }
                    },
                    vehicle {
                        id
                        vrn
                        vehicleType
                        routeId
                        route {
                            name
                        }
                        driverEntityId
                        driverEntity {
                            firstName
                            lastName
                        }
                        collectorEntityId
                        collectorEntity {
                            firstName
                            lastName
                        }
                    }
                }
            }
            """;

    public static final String mozGetSimpleAccountInfo = """
            query {
                userSimpleAccountInfoMoz(device: "%s") {
                    accountId
                    accountNumber
                    accountType
                    firstName
                    lastName
                    deviceCode
                    deviceStatus
                    route {
                        id
                        name
                        active
                        details {
                            operatorType
                            minDistance
                            minFare
                            farePerKm
                            maxFare
                            maxDistance
                        }
                    }
                }
            }
            """;

    public static final String mozLinkTag = """
            mutation {
                linkNfcTagMoz(
                    otp: "%s",
                    nfcTag: {
                        device: "%s",
                        accountNumber: "%s",
                        tagNumber: "%s",
                        dateTime: "%s"
                    }
                ) {
                    status
                    firstName
                    lastName
                    accountNumber
                    prepaidBalance
                    subsidyBalance
                }
            }
            """;

    public static final String fndsRegUser = """
            mutation {
                registerUserFNDS(
                    otp: "%s",
                    user: {
                        accountType: %s,
                        firstName: "%s",
                        lastName: "%s",
                        idType: %s,
                        idNumber: "%s",
                        mobile: "%s",
                        pin: "%s",
                    }
                ) {
                    id
                    entityType {
                        description
                    }
                    firstName
                    lastName
                    idType {
                        description
                    }
                    idNumber
                    msisdn {
                        msisdn
                    }
                    email
                    fndsKesAccountBalance
                    accounts {
                        id
                        accountNumber
                        accountType {
                            name
                            currencyId
                            currency {
                                isoCode
                            }
                        }
                        balance
                        createdDate
                    }
                    relationships {
                        partnerAccountId
                        securityGroups
                        securityGroupMoz {
                            id
                            name
                            active
                            rightsList
                        }
                    }
                    status
                    loginStatus
                    locale
                    createdDate
                }
            }
            """;

    public static final String fndsLoginUser = """
            mutation {
                loginUserFNDS(request: {
                    username: "%s"
                    password: "%s"
                }) {
                    status
                    mfaType
                    locale
                    accessToken {
                        tokenType
                        token
                        expiresIn
                        refreshToken
                        refreshExpiresIn
                        error
                        errorDescription
                    }
                }
            }
            """;

    public static final String fndsMakePayment = """
            mutation {
                makePaymentFNDS(paymentRequest: {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s"
                    currency: "%s",
                    amount: %s,
                    date: "%s",
                    metaData: {
                        deviceCode: "%s",
                        productId: %s
                    }
                }) {
                    vendorRef
                    status
                    message
                    transactionId
                    balance
                    date
                }
            }
            """;

    public static final String fndsMakeBulkPayment = """
            mutation {
                makeBulkPaymentFNDS(payments: [
                {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s"
                    currency: "%s",
                    amount: %s,
                    date: "%s",
                    metaData: {
                        deviceCode: "%s",
                        productId: %s
                    }
                },
                {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s"
                    currency: "%s",
                    amount: %s,
                    date: "%s",
                    metaData: {
                        deviceCode: "%s",
                        productId: %s
                    }
                }
                ]) {
                    vendorRef
                    status
                    message
                    errorCode
                    transactionId
                    balance
                }
            }
            """;

    public static final String fndsEntitiesSearch = """
            query {
                entitiesSearchFNDS(
                    accountType: %s
                    %s
                    %s
                    %s
                    page: 0
                    size: 30
                    sort: {asc: "firstName"}
                ) {
                    total
                    content {
                        id
                        customerFace
                        fndsKesAccountBalance
                        entityType {
                            description
                        }
                        firstName
                        lastName
                        idType {
                            description
                        }
                        idNumber
                        msisdn {
                            msisdn
                        }
                        status
                        products {
                            relationship
                            active
                            product {
                                name
                                productType
                                price
                                currency {
                                    isoCode
                                }
                            }
                        }
                    }
                }
            }
            """;

    public static final String fndsGetDeviceProducts = """
            query {
                deviceProductsFNDS(deviceSerialOrCode: "%s") {
                    entityId
                    entity {
                        firstName
                    }
                    productId
                    product {
                        name
                    }
                    relationship
                    active
                    createdDate
                    modifiedDate
                }
            }
            """;

    public static final String getCurrencies = """
            query {
                allCurrencies {
                    id
                    isoCode
                    active
                    postilionCode
              }
            }
            """;

    public static final String getCountries = """
            query {
                 allCountries {
                     id
                     isoCode
                     name
                 }
             }
             
            """;
    public static final String getEntityTypes = """
            query {
                allEntityTypes {
                    id
                    description
                    entityTypeGroupId
                    entityTypeGroup {
                        id
                        description
                    }
              }
            }
            """;

    public static final String getTransactionCodes = """
            query {
                transactionCodes(
                    currencyId: %s
                ) {
                    total
                    content {
                        id
                        code
                        description
                        active
                        additionalFees {
                            id
                            chargeType
                            currency {
                                isoCode
                            }
                            processOrder
                            amount
                            drEntityAccount {
                                id
                            }
                            crEntityAccount {
                                id
                            }
                            active
                        }
                    }
                }
            }
            """;
    public static final String getIdTypes = """
            query {
                allIdTypes {
                    id
                    description
                }
            }
            """;

    public static final String getSecurityGroups = """
            query {
                allSecurityGroups {
                    id
                    name
                    description
                    rightsList
                    rights {
                        description
                    }
                }
            }
            """;
    public static final String simpleRegisterEntity = """
            mutation {
                simpleRegisterEntity(
                    entity: {
                        entityType: "%s",
                        firstName: "%s",
                        lastName: "%s",
                        idTypeId: %s
                        idNumber: "%s"
                        mobile: "%s"
                        email: "%s"
                   }
                ) {
                    id
                    email
                    msisdn {
                        msisdnType
                        msisdn
                    }
                    loginStatus
                    mfaType
                    mfaBackupCodes
                    createdDate
                    accounts {
                        id
                        accountNumber
                    }
                 }
            }
            """;
    public static final String registerEntity = """
            mutation {
                registerEntity(
                    entity: {
                        status: %s
                        loginStatus: %s
                        entityType: "%s",
                        firstName: "%s",
                        lastName: "%s",
                        idTypeId: %s
                        idNumber: "%s"
                        kycStatus: %s
                        contactName: "%s"
                        mobile: "%s"
                        altContactName: "%s"
                        altMobile: "%s"
                        email: "%s"
                        gender: %s
                        authorisationType: %s
                        corporateFee: %s
                        transactionLimitTier: %s
                        citizenshipCountryId: %s
                        address: {
                            countryId: %s
                            city: "%s"
                            postalCode: "%s"
                            address1: "%s"
                            address2: "%s"
                            notes: "%s"
                        }
                        company: "%s"
                    }
                ) {
                    id
                    email
                    status
                    loginStatus
                    entityType {
                        description
                    }
                    firstName
                    lastName
                    idTypeId
                    idType {
                        description
                    }
                    idNumber
                    kycStatusId
                    msisdn {
                        msisdnType
                        msisdn
                        description
                    }
                    gender
                    citizenshipCountryId
                    address {
                        addressType
                        countryId
                        city
                        postalCode
                        address1
                        address2
                        notes
                    }
                    mfaType
                    mfaBackupCodes
                    createdDate
                    accounts {
                        accountNumber
                        accountType {
                            name
                            currency {
                                isoCode
                            }
                            active
                        }
                        accountStatus
                        balance
                        authorisationType
                    }
                    metaData
                }
            }
            """;
    public static final String getEntityFull = """
            query {
                entity {
                    id
                    email
                    status
                    loginStatus
                    entityType {
                        description
                    }
                    firstName
                    lastName
                    idTypeId
                    idType {
                        description
                    }
                    idNumber
                    kycStatusId
                    msisdn {
                        msisdnType
                        msisdn
                        description
                    }
                    gender
                    citizenshipCountryId
                    birthDate
                    address {
                        id
                        entityId
                        countryId
                        country {
                            isoCode
                            name
                        }
                        city
                        postalCode
                        addressType
                        address1
                        address2
                        notes
                    }
                    mfaType
                    mfaBackupCodes
                    createdDate
                    metaData
                    accounts {
                        id
                        accountNumber
                        accountStatus
                        dailyLimit
                        balanceMinimumEnforce
                        authorisationType
                        createdDate
                        accountType {
                            name
                            currency {
                                isoCode
                            }
                            active
                        }
                        authorisationType
                        balance
                        # payments {
                        #     id
                        #     status
                        #     description
                        # }
                        initiators {
                            id
                            accountId
                            initiatorTypeId
                            initiatorType {
                                id
                                description
                                active
                                entityId
                            }
                            identifier
                            initiatorCategoryId
                            initiatorCategory {
                                id
                                category
                            }
                            initiatorStatusId
                            initiatorStatus {
                                id
                                name
                                permitTransaction
                                active
                            }
                            pvv
                            notes
                            createdDate
                            startDate
                            expiryDate
                            metaData
                            configuration
                        }
                    }
                    relationships {
                        partnerAccountId
                        securityGroupMoz {
                            id
                            name
                            active
                            rightsList
                        }
                    }
                }
            }
            """;
    public static final String updateEntityByStaff = """
            mutation {
                updateEntity(entityId: %s, details: {
                    entityTypeId: %s,
                    firstName: "%s",
                    lastName: "%s",
                    idType: %s
                    idNumber: "%s"
                    gender: %s,
                    status: %s
                    birthDate: "%s"
                    citizenshipCountryId: %s
                    email: "%s"
                    mfaType: %s
                    locale: "%s"
                }) {
                    id
                    email
                    entityTypeId
                    firstName
                    lastName
                    idTypeId
                    idNumber
                    gender
                    status
                    birthDate
                    citizenshipCountryId
                    mfaType
                    locale
                }
            }
            """;

    public static final String getAccounts = """
            query {
                accounts {
                    id
                    accountNumber
                    accountStatus
                    balance
                    entityId
                    entity {
                        firstName
                    }
                    accountTypeId
                    accountType {
                        name
                    }
                    authorisationType
                    dailyLimit
                    overdraftLimit
                    balanceMinimum
                    balanceWarning
                    balanceMinimumEnforce
                    notificationEnabled
                    autoDebit
                    createdDate
                    initiators {
                        id
                        accountId
                        initiatorTypeId
                        initiatorType {
                            id
                            description
                            active
                            entityId
                        }
                        identifier
                        initiatorCategoryId
                        initiatorCategory {
                            id
                            category
                        }
                        initiatorStatusId
                        initiatorStatus {
                            id
                            name
                            permitTransaction
                            active
                        }
                        pvv
                        notes
                        createdDate
                        startDate
                        expiryDate
                        metaData
                        configuration
                    }
                }
            }
            """;

    public static final String updateAccountActive = """
            mutation {
                setAccountActive(
                    id: %s,
                    active: %s
                ) {
                    id
                    accountNumber
                    accountStatus
                }
            }
            """;

    public static final String existsUserId = """
            query {
                existsUserId(
                    idTypeId: %s
                    idNumber: "%s"
                    isEntity: %s
                    forceCheck: true
                )
            }
            """;
    public static final String existsUserEmail = """
            query {
                existsUserEmail(
                    email: "%s"
                    isEntity: %s
                    forceCheck: true
                )
            }
            """;
    public static final String existsUserMsisdn = """
            query {
                existsUserMsisdn(
                    msisdn: "%s"
                    checkOnlyPrimary: true
                    forceCheck: true
                )
            }
            """;

    public static final String updateLoginStatus = """
            mutation {
                updateEntityLoginStatus(
                    username: "%s"
                    loginStatus: %s
                ) {
                    id
                    email
                    loginStatus
                }
            }
            """;

    public static final String updateMfa = """
            mutation {
                 updateEntityMfa(id: %s, mfaType: %s) {
                     id
                     email
                     loginStatus
                     mfaType
                 }
             }
            """;

    public static final String checkTotpCode = """
            mutation {
                checkEntityTotpCode(totpCode: "%s")
            }
            """;

    public static final String enterLoginMfa = """
            mutation {
                enterEntityLoginMfaCode(mfaRequest: {
                    username: "%s"
                    code: "%s"
                }) {
                    status
                    mfaType
                    accessToken {
                        tokenType
                        token
                        expiresIn
                        refreshToken
                        refreshExpiresIn
                        error
                        errorDescription
                    }
                }
            }
            """;

    public static final String enterBackupCode = """
            mutation {
                enterEntityLoginBackupCode(mfaRequest: {
                    username: "%s"
                    code: "%s"
                }) {
                    status
                    mfaType
                    accessToken {
                        tokenType
                        token
                        expiresIn
                        refreshToken
                        refreshExpiresIn
                        error
                        errorDescription
                    }
                }
            }
            """;

    public static final String generateNewPassword = """
            mutation {
                generateNewEntityPassword(id: %s) {
                    id
                    email
                    loginStatus
                    mfaType
                }
            }
            """;

    public static final String updatePassword = """
            mutation {
                updateEntityPassword(oldPassword: "%s", newPassword: "%s") {
                    id
                    email
                    loginStatus
                    mfaType
                }
            }
            """;

    public static final String forgotPassword = """
            mutation {
                forgotEntityPassword(email: "%s", sendEmail: false)
            }
            """;

    public static final String resetPassword = """
            mutation {
                resetEntityPassword(
                    key: "%s"
                    newPassword: "%s"
                ) {
                    id
                    email
                }
            }
            """;

    public static final String generateNewBackupCodesForCurrentEntity = """
            mutation {
                generateNewBackupCodesForCurrentEntity {
                    id
                    mfaBackupCodes
                }
            }
            """;

    public static final String paymentResponse = """
            query {
                paymentResponse(vendorRef: "%s") {
                    vendorRef
                    date
                    status
                    payload
                    errorCode
                    message
                    transactionId
                    balance
                }
            }
            """;

    public static final String makePaygoPayment = """
            mutation {
                 makePayment(paymentRequest: {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s",
                    currency: "%s",
                    amount: %s,
                    partnerId: "%s",
                    apiVersion: "1",
                    date: "%s",
                    deviceId: "legacyApiX"
                    metaData: {
                        description: "%s"
                    }
                 }) {
                     vendorRef
                     status
                     errorCode
                     message
                     transactionId
                 }
             }
            """;

    public static final String makeInboundPayment = """
            mutation {
                 makePayment(paymentRequest: {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s",
                    currency: "%s",
                    amount: %s,
                    partnerId: "1",
                    apiVersion: "1",
                    date: "%s",
                    deviceId: "legacyApiX"
                    metaData: {
                        accountNumber: "%s"
                    }
                 }) {
                     vendorRef
                     status
                     errorCode
                     message
                     transactionId
                 }
             }
            """;

    public static final String makeFlexcubePayment = """
            mutation {
                makePayment(paymentRequest: {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s",
                    currency: "%s",
                    amount: %s,
                    partnerId: "1",
                    apiVersion: "1",
                    date: "%s",
                    deviceId: "legacyApiX",
                    metaData: {
                        simulate: "00",
                        referenceId: %s,
                        bankBin: "%s",
                        bankAccountNo: "%s",
                        branchCode: "%s",
                        swiftCode: "%s",
                        beneficiaryName: "%s",
                        beneficiaryAddress: "%s",
                        beneficiaryReference: "%s",
                        description : "%s"
                    }
                }) {
                    vendorRef
                    status
                    errorCode
                    message
                    transactionId
                }
            }
            """;

    public static final String makeMozInboundOutboundPayment = """
            mutation {
                 makePayment(paymentRequest: {
                    vendorRef: "%s",
                    tx: "%s",
                    initiatorType: "%s",
                    initiator: "%s",
                    currency: "%s",
                    amount: %s,
                    partnerId: "1",
                    apiVersion: "1",
                    date: "%s",
                    deviceId: "legacyApiX"
                    metaData: {
                        accountNumber: "%s",
                        simulate: "%s"
                    }
                 }) {
                     vendorRef
                     status
                     errorCode
                     message
                     transactionId
                 }
             }
            """;

    public static final String getAccountStatement = """
        query {
            accountStatement(accountType: "%s", currency: "%s") {
                total
                content {
                    id
                    statementDate
                    transactionCodeId
                    transactionCode {
                        code
                    }
                    amount
                    lines {
                        id
                        transactionCode {
                            code
                        }
                        description
                        amount
                    }
                }
            }
        }
        """;

    public static final String searchEntities = """
            query {
                searchEntities(
                    searchBy: %s,
                    searchInput: "%s",
                    exactMatch: %s
                ) {
                    total
                    content {
                        id
                        status
                        firstName
                        lastName
                        idNumber
                        accounts {
                            accountNumber
                            initiators {
                                identifier
                            }
                        }
                        msisdn {
                            msisdnType
                            msisdn
                        }
                    }
                }
            }
            """;
}
