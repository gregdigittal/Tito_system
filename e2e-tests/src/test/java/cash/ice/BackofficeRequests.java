package cash.ice;

public class BackofficeRequests {

    public static final String createStaffMember = """
            mutation {
                registerStaffMember(password: "%s", staffMember: {
                    email: "%s"
                    firstName: "%s"
                    lastName: "%s"
                    msisdn: "%s"
                }) {
                    id
                    email
                    loginStatus
                    mfaType
                    mfaSecretCode
                    mfaBackupCodes
                    mfaQrCode
                    createdDate
                }
            }
            """;
    public static final String newStaffMember = """
            mutation {
                 newStaffMember(staffMember: {
                     email: "%s"
                     firstName: "%s"
                     lastName: "%s"
                     idNumber: "%s"
                     idNumberType: %s
                     msisdn: "%s"
                     department: "%s"
                     securityGroupId: %s},
                     sendEmail: false
                 ) {
                     id
                     email
                     loginStatus
                     mfaType
                     mfaSecretCode
                     mfaBackupCodes
                     createdDate
                 }
            }
            """;
    public static final String activateNewStaffMember = """
            mutation {
                activateNewStaffMember(
                    key: "%s",
                    newPassword: "%s"
                ) {
                    id
                    email
                }
            }
            """;
    public static final String deleteStaffMember = """
            mutation {
                deleteStaffMember(id: %s) {
                    id
                    email
                }
            }
            """;
    public static final String loginStaffMember = """
            mutation {
                loginStaffMember(request: {
                    username: "%s"
                    password: "%s"
                }) {
                    status
                    mfaType
                    msisdn
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
    public static final String loginMfaStaffMember = """
            mutation {
                enterLoginMfaCode(mfaRequest: {
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
    public static final String getCurrentStaffMember = """
            query {
                staffMember {
                    id
                    email
                    loginStatus
                    mfaType
                    mfaSecretCode
                    mfaQrCode
                    mfaBackupCodes
                    locale
                    firstName
                    lastName
                    idNumber
                    idNumberType
                    msisdn
                    entityId
                    department
                    securityGroupId
                    securityGroup {
                        name
                    }
                    createdDate
                }
            }
            """;
    public static final String updateStaffMemberPassword = """
            mutation {
                updateStaffMemberPassword(
                    oldPassword: "%s",
                    newPassword: "%s"
                ) {
                    id
                    email
                }
            }
            """;
    public static final String updateStaffMemberLoginStatus = """
            mutation {
                updateStaffMemberLoginStatus(
                    email: "%s",
                    loginStatus: %s
                ) {
                    id
                    email
                    loginStatus
                }
            }
            """;
    public static final String enterStaffMemberBackupCode = """
            mutation {
                enterLoginBackupCode(mfaRequest: {
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
    public static final String generateStaffMemberBackupCodes = """
            mutation {
                generateNewBackupCodes {
                    id
                    email
                    mfaBackupCodes
                }
            }
            """;
    public static final String staffMemberForgotPassword = """
            mutation {
                 forgotStaffMemberPassword(
                     email: "%s"
                     url: "%s"
                     sendEmail: false
                 )
             }
            """;
    public static final String staffMemberResetPassword = """
            mutation {
                 resetStaffMemberPassword(
                     key: "%s"
                     newPassword: "%s"
                 ) {
                     id
                     email
                 }
             }
            """;
    public static final String resendStaffMemberOTP = """
            mutation {
                resendOtpCode(username: "%s")
            }
            """;
    public static final String refreshStaffMemberToken = """
            mutation {
                 refreshStaffMemberAccessToken(refreshToken: "%s") {
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
    public static final String invalidateStaffMemberToken = """
            mutation {
                 invalidateStaffMemberRefreshToken(refreshToken: "%s") {
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
    public static final String updateStaffMemberMsisdn = """
            mutation {
                updateStaffMemberMsisdn(msisdn: "%s") {
                    id
                    email
                    msisdn
                }
            }
            """;
    public static final String updateStaffMemberMfaType = """
            mutation {
                updateStaffMemberMfaType(mfaType: %s) {
                    id
                    email
                    mfaType
                }
            }
            """;
    public static final String updateStaffMember = """
            mutation {
                updateStaffMember(staffMember: {
                    email: "%s"
                    firstName: "%s"
                    lastName: "%s"
                    idNumber: "%s"
                    idNumberType: %s
                    msisdn: "%s"
                    department: "%s"
                    mfaType: %s
                    locale: "%s"
                    loginStatus: %s
                    securityGroupId: %s
                }, sendEmail: false) {
                    id
                    email
                    firstName
                    lastName
                    idNumber
                    idNumberType
                    msisdn
                    department
                    mfaType
                    locale
                    loginStatus
                    entityId
                    securityGroupId
                }
            }
            """;
    public static final String checkStaffMemberTotpCode = """
            mutation {
                checkStaffMemberTotpCode(totpCode: "%s")
            }
            """;
    public static final String searchStaffMembers = """
            query {
                searchStaffMembers(
                    searchText: "%s",
                    status: %s
                ) {
                    id
                    email
                    firstName
                    lastName
                    loginStatus
                }
            }
            """;
    public static final String createJournal = """
            mutation {
                createJournal(journal: {
                    currencyId: %s
                    transactionCodeId: %s
                    amount: %s
                    drAccountId: %s
                    crAccountId: %s
                    details: "%s"
                    notes: "%s"
                }, fees: %s, sendEmail: %s) {
                    id
                    status
                    amount
                    drAccountId
                    crAccountId
                    transactionCodeId
                    fees {
                        feeId
                        amount
                        chargeType
                        drAccountId
                        crAccountId
                    }
                    details
                    notes
                    createdDate
                }
            }
            """;
    public static final String rejectJournal = """
            mutation {
                rejectJournal(journalId: %s) {
                    id
                    status
                    amount
                    drAccountId
                    crAccountId
                    transactionCodeId
                    fees {
                        feeId
                        amount
                        chargeType
                        drAccountId
                        crAccountId
                    }
                    details
                    notes
                    createdDate
                }
            }
            """;
    public static final String acceptJournal = """
            mutation {
                acceptJournal(journalId: %s) {
                    id
                    status
                    amount
                    drAccountId
                    crAccountId
                    transactionCodeId
                    fees {
                        feeId
                        amount
                        chargeType
                        drAccountId
                        crAccountId
                    }
                    details
                    notes
                    createdDate
                }
            }
            """;
}
