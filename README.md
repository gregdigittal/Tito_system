# ICE Cash Payments project

Payments project consists of 8 microservices:
- `api-service`
- `fee-service`
- `ledger-service`
- `paygo-service`
- `ecocash-service`
- `onemoney-service`
- `fbc-service`
- `sync-service`

It also uses the following systems:
- `Kafka`, is used as data bus between microservices
- `MongoDB Cluster`, needed for api-service to save requests/responses and for several external integration microservices, like paygo-service, ecocash-service, onemoney-service, fbc-service.
- `MariaDB`, needed for api-service and fee-service, it contains fees/charges data, currencies, transaction codes, initiators, entities with accounts... Also ledger-service uses it to save payment transactions.
- `Keycloak`, needed for api-service to store users' login data and theirs permission roles, and to authenticate users who try to login. If authentication is successful, keycloak returns access token which must be used to have access to all other application resources.
- `Redis`, used to cache frequently requested data in a database.
- `MinIO`, this is the S3 storage we use to store the documents that the user uploads to the server.

In order to use the project, the following options are available:
- use https://uat-gateway.icecash.mobi/payments/ prefix for all endpoints, this is our Kubernetes UAT environment. (`uat-k8s` spring profile is used in this case)
- Use InteliJ IDEA to launch all microservices with `local` spring profile enabled. Local environment prefix for all application endpoints is the following: http://localhost:8281/. Each microservice has `{Service}Application` class for that (eg. api-service service uses ApiApplication class as entry point, fee-service uses FeeApplication and so on). This case is the best for local development, but requires that all dependant systems are already configured and working. To quickly configure all dependant systems, use `docker-compose.xml` file in the root of the project, it launches all dependant systems (except MinIO), run the following command:

      docker-compose up

  NOTE: In order to use MinIO locally, `minio` pod from UAT Kubernetes can be used. For that you can forward it's port and replace the port in `minio.url` property of `application.yml` config in `api-service` service with the newly forwarded port.

IMPORTANT: Before running the application for the first time, you must first set up the Keycloak realm for local environment. To do this, first make sure Keycloak is up and running, then go to http://localhost:8180/, click "Admin Console", enter "admin" as the username and "admin" as the password. After logging into the admin console, in the upper left corner, move the mouse arrow under the "Master" field to display the drop-down list, and click the "Add realm" button. Enter "payments-local" in the "Name" field, click the "Choose File" button in the "Import" field, and select the "payments-local-realm.json" file in the root folder of the project. Then click the "Create" button to create and configure a new realm.  

IMPORTANT: To avoid issues with different environments, a specific Spring profile must be explicitly used for each microservice. Therefore, for a local environment, do not forget to set the `local` profile in the launch settings (`Active profiles:` field) before the first launch.

IMPORTANT: The project uses `Liquibase` jobs to migrate the database located in the `api-service` project, so api-service must be launched first! On the first launch it creates the database and all needed tables. For a local environment it also adds EcoCash/PayGO test merchants to MongoDB and creates a test user in Keycloak, so make sure MongoDB and Keycloak are up and running as well as MariaDB before launching the application for the first time. Please also note that Liquibase locks the database before it starts running and, unfortunately, does not unlock it in case of errors or manual shutdowns. In this case the database might be unlocked manually, for this, in the `DATABASECHANGELOGLOCK` MariaDB table, set the `LOCKED` field value to `false`, and other fields to `NULL`. **See [docs/FIRST_RUN_AND_LIQUIBASE.md](docs/FIRST_RUN_AND_LIQUIBASE.md) for a concise first-run and Liquibase unlock runbook.**

NOTE: After the first run in the local environment, a test user will be created, which can be authenticated to use all secure URLs: 

    John Doe 
    id number: 3333333333333333 
    account number: 33333333335 
    pin: 3333
    card number: 3333333333333331 
    mobile: 263123456789

You can use an ID number, an account number, or a card number as a "username" (or `enterId`) for authentication. The PIN must be used as the "password".

It is not necessary to run all microservices for the application to work. For most operations, such as working with users, documentation, creating and processing pending payments, it is enough to run `api-service`. To execute payments (both a single call and a pending payment approve action), `api-service`, `fee-service`, `ledger-service` must be launched. On the other hand, `paygo-service`, `ecocash-service`, `onemoney-service`, `fbc-service` might be launched only if it is necessary to make PayGO, EcoCash, OneMoney, FBC payments, respectively. 

IMPORTANT: `sync-service` on the first launch uses `Liquibase` to migrate real data from the legacy database, we need this functionality so that the project database is always up-to-date. Data migration uses a lot of SQL queries and takes a long time to complete (hours). By default, for a `local` environment, the database is filled with test data for faster startup, so the local sync-service will not start correctly and is not recommended for running in this environment! If, nevertheless, the database needs to be filled with real data, then you can replace `liquibase.change-log-file` property to `liquibase-db-changelog.xml` for local profile in `sqldb.yml` before the first launch of api-service microservice. This will avoid filling the database with test data, which means that the sync-service will work correctly. In case the sync-service needs to be started with test data, it is better to comment out all (or failed) `changeSet` sections in `liquibase-db-changelog-sync.xml` file.  
___

### Register User request example:
This is API for new users registration. All individual clients must enter their 'First name', 'Last name', 'Mobile', 'ID number', 'ID Type identifier'. 'Entity type' for individual clients is 'Personal'. Other fields are optional, but 'Email' must have correct format. If 'Card' is provided, it must already exist.

For organizations registration, 'First name' is skipped, 'Last name' represents organization name. 

- POST /api/users/register HTTP/1.1
      
      {
          "idTypeId": 1,
          "idNumber": "1234567890123456",
          "firstName": "firstName",
          "lastName": "lastName",
          "entityType": "Personal",
          "email": "test@test.com",
          "company": "ICEcash",
          "card": "1234567890123456",
          "mobile": "123-45-67"
      }

Successful response is HTTP/1.1 200 OK

#### Possible response examples:

- Successful response:

        {
            "date": "2022-01-11T15:35:15.565678300Z",
            "status": "SUCCESS",
            "message": "Registration processed successfully"
        }

- Error response if wrong initiator provided:

        {
            "date": "2022-01-11T15:35:15.565678300Z",
            "status": "ERROR",
            "errorCode": "101-IC1146-0012",
            "message": "Initiator '1234567890123456' does not exist"
        }

- Error response if wrong entity type provided:

        {
            "date": "2022-01-11T15:35:15.565678300Z",
            "status": "ERROR",
            "errorCode": "101-IC1146-0011",
            "message": "Unknown entityType: wrongEntityType"
        }
___

### Login User request example:
In order to use protected resources, the user must first perform 'login' call and have the required permissions. Successful 'login' call returns access token which must be used to get all protected resources. When access token is expired, new one must be extracted.

- POST /api/users/login HTTP/1.1

        {
            "enterId": "1234567890123456",
            "pin": "1748"
        }

Error response is HTTP/1.1 401 Unauthorized

#### Possible response examples:

- Successful response:

        HTTP/1.1 200 OK

        {
            "access_token: "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiUFEzYzh...",
            "expires_in": 300,
            "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJj...",
            "refresh_expires_in": 1800,
            "token_type": "Bearer",
            "not-before-policy": 0
        }

#### Postman usage:
In order to use protected resource by Postman, it contains convenient way of taking token. For that you should select 'Authorization' tab and select 'Type': OAuth 2.0. In the right window enter the following 'Configuration Options':
- Grant Type: Password Credentials
- Access Token URL: {{HOST}}/api/users/login/form
- Client ID: icecash
- Username: It may be Account number, Card number or ID number
- Password: PIN
- Scope: openid

After that press 'Get New Access Token' button. When authentication is complete, press 'Proceed' button, and then press 'Use Token' button. Access Token will automatically be set and now you can use protected resource.

___
### Payment request example:

- POST /api/core/payment HTTP/1.1

      {     
          "vendorRef": "test1",
          "tx": "PAY",
          "initiatorType": "card",
          "currency": "ZWL",
          "amount": 20,
          "apiVersion": "1",
          "partnerId": "1",
          "date": "2021-09-08T12:30",
          "initiator": "1"
      }

Successful response is HTTP/1.1 202 Accepted
___

### Payment response example

- GET /api/core/payment/response/vendorRefTest1 HTTP/1.1

#### Possible response examples:

- Successful response:

        {
            "vendorRef": "test1",
            "date": "2021-10-05T21:36:07.759Z",
            "status": "SUCCESS",
            "errorCode": null,
            "message": "Transaction processed successfully",
            "transactionId": 245,
            "balance": 86858.73
        }

- Error response if unavailable currency provided:

        {
            "vendorRef": "test1",
            "date": "2021-10-05T21:41:34.036Z",
            "status": "ERROR",
            "errorCode": "101-IC1097-0002",
            "message": "Invalid currency requested: UAH",
            "transactionId": null,
            "balance": null
        }

- Error response in case of insufficient balance for the transaction:

        {
            "vendorRef": "test1",
            "date": "2021-10-05T21:40:00.681Z",
            "status": "ERROR",
            "errorCode": "102-IC1097-0002",
            "message": "Insufficient balance for the transaction. Current balance: ZWL $86834.71",
            "transactionId": null,
            "balance": null
        }

- Processing response if operation is not finished yet:

        {
            "vendorRef": "test1",
            "date": "2021-10-05T21:46:58.261298600Z",
            "status": "PROCESSING",
            "errorCode": null,
            "message": "Operation is in progress",
            "transactionId": null,
            "balance": null
        }

- Error responses if kafka or other microservices are unavailable:

        500 Internal Server Error
        {
            "vendorRef": "test1",
            "date": "2021-10-05T21:51:46.638592500Z",
            "errorCode": "104-IC1117-0001",
            "message": "Failed to fetch payment response"
        }

        500 Internal Server Error
        {
            "vendorRef": "test1",
            "date": "2021-10-05T21:53:35.406929900Z",
            "errorCode": "103-IC1116-0003",
            "message": "Failed to send payment request to kafka topic: ice.cash.payment.RequestTopic"
        }

#### Other payment request examples:

- Ecocash payment:

  The following transaction codes are available for EcoCash payments:
  - "EPAYG" - Ecocash payment general,
  - "ZTP" - Zinara Toll Payments,
  - "LPP" - Zinara Licence Payments


       {
           "vendorRef": "testeco_1",
           "tx": "ZTP",
           "initiatorType": "ecocash",
           "currency": "ZWL",
           "amount": "1.00",
           "partnerId": "1",
           "apiVersion": "1",
           "date": "2020-11-02T09:00",
           "initiator": "+263777104442",
           "deviceId": "legacyApiX",
           "meta": {     
               "description" : "Insurance policy #993819"
           }
       }

- OneMoney payment:

       {
           "vendorRef": "testone_1",
           "tx": "OPAYG",
           "initiatorType": "netone",
           "currency": "ZWL",
           "amount": "1.00",
           "partnerId": "1",
           "apiVersion": "1",
           "date": "2020-11-02T09:00",
           "initiator": "263718733835",
           "deviceId": "legacyApiX",
           "meta": {     
               "description" : "Insurance policy #993819"   
           }
       }

- PayGO payment:

       {
           "vendorRef": "testpg_1",
           "tx": "PGCBZ",
           "initiatorType": "paygo",
           "currency": "ZWL",
           "amount": "1.00",
           "partnerId": "1",
           "apiVersion": "1",
           "date": "2020-11-02T09:00",
           "initiator": "263772980098",
           "deviceId": "legacyApiX",
           "meta": {     
               "description" : "Insurance policy #993819"   
           }
       }

- Flexcube payment:
  
  For test reasons, "simulate" key can be added to "meta" to simulate the response. Value represents the type of simulation:
  - "00" - simulate success response
  - "01" - simulate error response
  - "02" - simulate timeout with successful poll response
  - "03" - simulate timeout with error poll response


       {
           "vendorRef": "testflex_1",
           "tx": "TRN",
           "initiatorType": "icecash",
           "currency": "ZWL",
           "amount": "1.00",
           "initiator": "753",
           "partnerId": "1",
           "apiVersion": "1",
           "date": "2020-11-02T09:00",
           "deviceId": "legacyApiX",
           "meta": {
               "referenceId": 1584240,
               "bankBin": "588882",
               "bankAccountNo": "10099209810",
               "branchCode": "001",
               "swiftCode": "COBZZWH0",
               "beneficiaryName": "Gordon Gangata",
               "beneficiaryAddress": "129 Main Street, Harare",
               "beneficiaryReference": "Test RTGS transaction",
               "description" : "pay test"
           }
       }
