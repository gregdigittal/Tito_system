currentBuild.displayName = "#21"
pipeline {
    agent any
    parameters {
        booleanParam(name: 'api_service', defaultValue: true, description: 'If selected makes api-service image and pushes it to docker registry')
        booleanParam(name: 'fee_service', defaultValue: true, description: 'If selected makes fee-service image and pushes it to docker registry')
        booleanParam(name: 'ledger_service', defaultValue: true, description: 'If selected makes ledger-service image and pushes it to docker registry')
        booleanParam(name: 'paygo_service', defaultValue: true, description: 'If selected makes paygo-service image and pushes it to docker registry')
        booleanParam(name: 'ecocash_service', defaultValue: true, description: 'If selected makes ecocash-service image and pushes it to docker registry')
        booleanParam(name: 'onemoney_service', defaultValue: true, description: 'If selected makes onemoney-service image and pushes it to docker registry')
        booleanParam(name: 'fbc_service', defaultValue: true, description: 'If selected makes fbc-service image and pushes it to docker registry')
        booleanParam(name: 'emola_service', defaultValue: true, description: 'If selected makes emola-service image and pushes it to docker registry')
        booleanParam(name: 'mpesa_service', defaultValue: true, description: 'If selected makes mpesa-service image and pushes it to docker registry')
        booleanParam(name: 'zim_banking_service', defaultValue: true, description: 'If selected makes zim-banking-service image and pushes it to docker registry')
        booleanParam(name: 'sync_service', defaultValue: true, description: 'If selected makes sync-service image and pushes it to docker registry')
        string(name: "RELEASE", description: "Version number", defaultValue: "0.1.1")
        choice(name: 'NAMESPACE', choices: ['dev','uat', 'prod'], description: 'Select the environment namespace to deploy build')
        booleanParam(name: 'SONARREPORT', defaultValue: true, description: 'If selected will update sonarqube report')
        booleanParam(name: 'SKIPTEST', defaultValue: false, description: 'If selected must NOT perform MAVEN testing')
        gitParameter branchFilter: 'origin/(.*)', defaultValue: 'develop', name: 'BRANCH', type: 'PT_BRANCH'
        booleanParam(name: 'hetzner', defaultValue: false, description: 'If selected Updates Kubernetes image on the hetzner environment')
        booleanParam(name: 'DC04', defaultValue: false, description: 'If selected Updates Kubernetes image on the DC04 environment')
        booleanParam(name: 'office', defaultValue: false, description: 'If selected Updates Kubernetes image on the office environment')
    }
  
    stages {
        stage('Build') {
            steps {
                withEnv(["BUILD_NAME=${BRANCH}@${RELEASE}.${BUILD_NUMBER}"]){
                sh '''
                 mvn install:install-file -Dfile=${WORKSPACE}/mpesa-service/lib/portal-sdk.jar -DgroupId=mz.co.vm.mpesa.vodacom -DartifactId=portal-sdk -Dversion=0.3 -Dpackaging=jar
                 mvn  clean install -Pdistribution -DskipTests=${SKIPTEST} -Dbuild.number=${BRANCH}@${RELEASE}.${BUILD_NUMBER} 
                '''
                }        
                }
            }	
        
        stage('Sonar') {
            when {
                expression {
                    params.SONARREPORT == true
                }
            }
            steps {
                sh '''
                    echo "--------------- Sonarqube report: ----------------"
                    mvn sonar:sonar -Dsonar.host.url=http://10.0.16.163:9000 -Dsonar.token=sqa_37658d3dfc27e3547e7744f19130efaedff7ac53 -Dsonar.projectKey=ICEcash-Payments -Dsonar.exclusions=**/*.java
                    echo "================ Sonarqube report ==============="
                '''
            }
        }

       stage('api-service image') {
            when {
                expression {
                    params.api_service == true
                }
            }
            steps {
                script{
               def app = docker.build("dockerrepo.icecash.mobi:5000/payments/api-service:${RELEASE}.${BUILD_NUMBER}","./api-service/")
                    app.push()
                }
            }
        } 

       stage('api-service_depoy_to_kubernetes_hetzner') {
            when {
                    expression {
                        params.api_service == true  && params.hetzner == true
                    }
                }
           steps{
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-api-service payments-api-service=dockerrepo.icecash.mobi:5000/payments/api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
           }
       }
        stage('api-service_depoy_to_kubernetes_DC04') {
            when {
                    expression {
                        params.api_service == true  && params.DC04 == true
                    }
                }
           steps{
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-api-service payments-api-service=dockerrepo.icecash.mobi:5000/payments/api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
           }
       }
        stage('api-service_depoy_to_kubernetes_Office') {
            when {
                    expression {
                        params.api_service == true && params.office == true
                    }
                }
           steps{
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-api-service payments-api-service=dockerrepo.icecash.mobi:5000/payments/api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
           }
       }
        stage('sync-service image') {
            when {
                expression {
                    params.sync_service == true
                }
            }
            steps {
                script{
                def app = docker.build("dockerrepo.icecash.mobi:5000/payments/sync-service:${RELEASE}.${BUILD_NUMBER}","./sync-service/")
                    app.push()
                }
            }
        }
        stage('sync-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.sync_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-sync-service payments-sync-service=dockerrepo.icecash.mobi:5000/payments/sync-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('sync-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.sync_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-sync-service payments-sync-service=dockerrepo.icecash.mobi:5000/payments/sync-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('sync-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.sync_service == true && params.office == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-sync-service payments-sync-service=dockerrepo.icecash.mobi:5000/payments/sync-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('fee-service image') {
            when {
                expression {
                    params.fee_service == true
                }
            }
            steps {
                script{
                def app = docker.build("dockerrepo.icecash.mobi:5000/payments/fee-service:${RELEASE}.${BUILD_NUMBER}","./fee-service/")
                app.push()
                }
            }
        }
        stage('fee-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.fee_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-fee-service payments-fee-service=dockerrepo.icecash.mobi:5000/payments/fee-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('fee-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.fee_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-fee-service payments-fee-service=dockerrepo.icecash.mobi:5000/payments/fee-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('fee-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.fee_service == true && params.office == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-fee-service payments-fee-service=dockerrepo.icecash.mobi:5000/payments/fee-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('ledger-service image') {
            when {
                expression {
                    params.ledger_service == true
                }
            }
            steps {
                script{
              def app = docker.build("dockerrepo.icecash.mobi:5000/payments/ledger-service:${RELEASE}.${BUILD_NUMBER}","./ledger-service/")
                app.push()
                }
            }
        }
        stage('ledger-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.ledger_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-ledger-service payments-ledger-service=dockerrepo.icecash.mobi:5000/payments/ledger-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('ledger-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.ledger_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-ledger-service payments-ledger-service=dockerrepo.icecash.mobi:5000/payments/ledger-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('ledger-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.ledger_service == true && params.office == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-ledger-service payments-ledger-service=dockerrepo.icecash.mobi:5000/payments/ledger-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('paygo-service image') {
            when {
                expression {
                    params.paygo_service == true
                }
            }
            steps {
                script{
                def app = docker.build("dockerrepo.icecash.mobi:5000/payments/paygo-service:${RELEASE}.${BUILD_NUMBER}","./paygo-service/")
                app.push()
                }
            }
        }
        stage('paygo-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.paygo_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-paygo-service payments-paygo-service=dockerrepo.icecash.mobi:5000/payments/paygo-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('paygo-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.paygo_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-paygo-service payments-paygo-service=dockerrepo.icecash.mobi:5000/payments/paygo-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('paygo-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.paygo_service == true && params.office == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-paygo-service payments-paygo-service=dockerrepo.icecash.mobi:5000/payments/paygo-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('ecocash-service image') {
            when {
                expression {
                    params.ecocash_service == true
                }
            }
            steps {
                script{
               def app = docker.build("dockerrepo.icecash.mobi:5000/payments/ecocash-service:${RELEASE}.${BUILD_NUMBER}","./ecocash-service/")
                app.push()
                }
                }
            }
        
        stage('ecocash-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.ecocash_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-ecocash-service payments-ecocash-service=dockerrepo.icecash.mobi:5000/payments/ecocash-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
         stage('ecocash-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.ecocash_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-ecocash-service payments-ecocash-service=dockerrepo.icecash.mobi:5000/payments/ecocash-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('ecocash-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.ecocash_service == true && params.office == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-ecocash-service payments-ecocash-service=dockerrepo.icecash.mobi:5000/payments/ecocash-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
         stage('onemoney-service image') {
            when {
                expression {
                    params.onemoney_service == true
                }
            }
            steps {
                script{
               def app = docker.build("dockerrepo.icecash.mobi:5000/payments/onemoney-service:${RELEASE}.${BUILD_NUMBER}","./onemoney-service/")
                app.push() 
                }
            }
        }
        stage('onemoney-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.onemoney_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-onemoney-service payments-onemoney-service=dockerrepo.icecash.mobi:5000/payments/onemoney-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('onemoney-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.onemoney_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-onemoney-service payments-onemoney-service=dockerrepo.icecash.mobi:5000/payments/onemoney-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('onemoney-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.onemoney_service == true && params.office == true
                }
            }
            steps {
                script {
                    withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-onemoney-service payments-onemoney-service=dockerrepo.icecash.mobi:5000/payments/onemoney-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('fbc-service image') {
            when {
                expression {
                    params.fbc_service == true
                }
            }
            steps {
                script{
              def app = docker.build("dockerrepo.icecash.mobi:5000/payments/fbc-service:${RELEASE}.${BUILD_NUMBER}","./fbc-service/")
                app.push() 
                } 
            }
        }
        stage('fbc-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.fbc_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-fbc-service payments-fbc-service=dockerrepo.icecash.mobi:5000/payments/fbc-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('fbc-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.fbc_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-fbc-service payments-fbc-service=dockerrepo.icecash.mobi:5000/payments/fbc-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('fbc-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.fbc_service == true && params.office == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-fbc-service payments-fbc-service=dockerrepo.icecash.mobi:5000/payments/fbc-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('emola-service image') {
            when {
                expression {
                    params.emola_service == true
                }
            }
            steps {
                script{
              def app = docker.build("dockerrepo.icecash.mobi:5000/payments/emola-service:${RELEASE}.${BUILD_NUMBER}","./emola-service/")
                app.push() 
                } 
            }
        }
        stage('emola-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.emola_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-emola-service  payments-emola-service=dockerrepo.icecash.mobi:5000/payments/emola-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
         stage('emola-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.emola_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-emola-service  payments-emola-service=dockerrepo.icecash.mobi:5000/payments/emola-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
         stage('emola-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.emola_service == true && params.office == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-emola-service  payments-emola-service=dockerrepo.icecash.mobi:5000/payments/emola-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('mpesa-service image') {
            when {
                expression {
                    params.mpesa_service == true
                }
            }
            steps {
                script{
              def app = docker.build("dockerrepo.icecash.mobi:5000/payments/mpesa-service:${RELEASE}.${BUILD_NUMBER}","./mpesa-service/")
                app.push() 
                } 
            }
        }
        stage('mpesa-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.mpesa_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-mpesa-service  payments-mpesa-service=dockerrepo.icecash.mobi:5000/payments/mpesa-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('mpesa-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.mpesa_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-mpesa-service  payments-mpesa-service=dockerrepo.icecash.mobi:5000/payments/mpesa-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('mpesa-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.mpesa_service == true && params.office == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-mpesa-service  payments-mpesa-service=dockerrepo.icecash.mobi:5000/payments/mpesa-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('zim_api_service image') {
            when {
                expression {
                    params.zim_api_service == true
                }
            }
            steps {
                script{
              def app = docker.build("dockerrepo.icecash.mobi:5000/payments/zim-api-service:${RELEASE}.${BUILD_NUMBER}","./zim-api-service/")
                app.push() 
                } 
            }
        }
        stage('zim-api-service_depoy_to_kubernetes_hetzner') {
            when {
                expression {
                    params.zim_api_service == true && params.hetzner == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'hetzner-credentials', serverUrl: 'https://136.243.89.211:16443']){
                    sh """
                    kubectl set image deployment/payments-zim-api-service  payments-zim-api-service=dockerrepo.icecash.mobi:5000/payments/zim-api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('zim-api-service_depoy_to_kubernetes_dc04') {
            when {
                expression {
                    params.zim_api_service == true && params.DC04 == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'dc04-credentials', serverUrl: 'https://172.16.55.130:2443']){
                    sh """
                    kubectl set image deployment/payments-zim-api-service  payments-zim-api-service=dockerrepo.icecash.mobi:5000/payments/zim-api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }
        stage('zim-api-service_depoy_to_kubernetes_office') {
            when {
                expression {
                    params.zim_api_service == true && params.office == true
                }
            }
            steps {
                script {
                   withKubeConfig([credentialsId: 'office-credentials', serverUrl: 'https://10.0.16.141:6443']){
                    sh """
                    kubectl set image deployment/payments-zim-api-service  payments-zim-api-service=dockerrepo.icecash.mobi:5000/payments/zim-api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
                    """
                    }
                }
            }
        }

    }
   

}