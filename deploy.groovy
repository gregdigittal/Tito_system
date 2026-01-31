pipeline {
    agent any
    tools {
        maven 'Maven'
        jdk 'jdk_17'
    }
    parameters {
        booleanParam(name: 'api_service', defaultValue: true, description: 'If selected makes api-service image and pushes it to docker registry')
        booleanParam(name: 'sync_service', defaultValue: true, description: 'If selected makes sync-service image and pushes it to docker registry')
        booleanParam(name: 'fee_service', defaultValue: true, description: 'If selected makes fee-service image and pushes it to docker registry')
        booleanParam(name: 'ledger_service', defaultValue: true, description: 'If selected makes ledger-service image and pushes it to docker registry')
        booleanParam(name: 'paygo_service', defaultValue: true, description: 'If selected makes paygo-service image and pushes it to docker registry')
        booleanParam(name: 'ecocash_service', defaultValue: true, description: 'If selected makes ecocash-service image and pushes it to docker registry')
        booleanParam(name: 'onemoney_service', defaultValue: true, description: 'If selected makes onemoney-service image and pushes it to docker registry')
        booleanParam(name: 'fbc_service', defaultValue: true, description: 'If selected makes fbc-service image and pushes it to docker registry')
        string(name: "RELEASE", description: "Version number", defaultValue: "0.1.1")
        choice(name: 'BRANCH', choices: ['develop', 'release', 'master', ''], description: 'Select the branch to build:   DEVELOP = Build from and auto DEPLOY to DEV--     RELEASE = Merge from develop to release; Build and deploy to INT--    MASTER  = Build from master and deploy to tst-pg')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'If selected updates Kubernetes image')
        choice(name: 'NAMESPACE', choices: ['uat', 'prod'], description: 'Select the environment namespace to deploy build')
        booleanParam(name: 'SONARREPORT', defaultValue: true, description: 'If selected will update sonarqube report')
        booleanParam(name: 'SKIPTEST', defaultValue: false, description: 'If selected must NOT perform MAVEN testing')
    }
    environment {
        registryId = 'dockereg'
        registryUrl = 'https://dockerrepo.icecash.mobi:5000'
        registryPrefix = "dockerrepo.icecash.mobi:5000/payments/"
    }
    stages {
        stage('Checkout SourceCode') {
            steps {
                sh '''
                    echo "RELEASE = ${RELEASE}"
                    echo "BRANCH = ${BRANCH}"
                    echo "SKIPTEST = ${SKIPTEST}"
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                    export PATH=/opt/jdk17/bin:/var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
                    export JAVA_HOME=/opt/jdk17/
                    export MAVEN_HOME=/var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven/bin
                    echo "JAVA_HOME = ${JAVA_HOME}"
                    java -version
                '''
                checkout([$class: 'GitSCM', branches: [[name: 'refs/heads/${BRANCH}']], doGenerateSubmoduleConfigurations: false, gitTool: 'Default', submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'pavlo.ice.cash', url: 'https://slsapp.com/git/eweb/j-payments.git']]])
            }
        }
        stage('Build') {
            steps {
                withEnv(["BUILD_NAME=${BRANCH}@${RELEASE}.${BUILD_NUMBER}"]) {
                    sh '''
                        #export JAVA_HOME=/bitnami/jenkins/home/tools/hudson.model.JDK/jdk11_Zulu/jdk-11.0.1
                        export JAVA_HOME=/opt/jdk17/jdk-17.0.2
                        export MAVEN_HOME=/var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven/bin
                        export PATH=/opt/jdk17/jdk-17.0.2/bin:/var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
                        echo "JAVA_HOME = ${JAVA_HOME}"
                        java -version
                        echo "--------------- Build ----------------"
                        mvn clean package -Pdistribution -DskipTests=${SKIPTEST} -Dbuild.number=${BRANCH}@${RELEASE}.${BUILD_NUMBER}
                        echo "================ Build ==============="
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
                    export JAVA_HOME=/opt/jdk17/jdk-17.0.2
                    export MAVEN_HOME=/var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven/bin
                    export PATH=/opt/jdk17/jdk-17.0.2/bin:/var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
                    echo "--------------- Sonarqube report: ----------------"
                    mvn sonar:sonar -Dsonar.host.url=http://sonarqube.dc03.icecash.local -Dsonar.login=admin -Dsonar.password=tSL#2!mF -Dsonar.projectKey=ICEcash-Payments
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
						echo "--------------- api-service image ----------------"
						echo "image = ${registryPrefix}api-service:${RELEASE}.${BUILD_NUMBER}"
						docker build -f ./api-service/Dockerfile -t ${registryPrefix}api-service:${RELEASE}.${BUILD_NUMBER} ./api-service
						docker push ${registryPrefix}api-service:${RELEASE}.${BUILD_NUMBER}
						echo "================ api-service image ==============="
					'''
                }
            }
        }
        stage('api-service k8s') {
            when {
                expression {
                    params.api_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- api-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-api-service payments-api-service=${registryPrefix}api-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== api-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- sync-service image ----------------"
                        echo "image = ${registryPrefix}sync-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./sync-service/Dockerfile -t ${registryPrefix}sync-service:${RELEASE}.${BUILD_NUMBER} ./sync-service
                        docker push ${registryPrefix}sync-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ sync-service image ==============="
                    '''
                }
            }
        }
        stage('sync-service k8s') {
            when {
                expression {
                    params.sync_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- sync-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-sync-service payments-sync-service=${registryPrefix}sync-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== sync-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- fee-service image ----------------"
                        echo "image = ${registryPrefix}fee-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./fee-service/Dockerfile -t ${registryPrefix}fee-service:${RELEASE}.${BUILD_NUMBER} ./fee-service
                        docker push ${registryPrefix}fee-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ fee-service image ==============="
                    '''
                }
            }
        }
        stage('fee-service k8s') {
            when {
                expression {
                    params.fee_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- fee-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-fee-service payments-fee-service=${registryPrefix}fee-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== fee-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- ledger-service image ----------------"
                        echo "image = ${registryPrefix}ledger-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./ledger-service/Dockerfile -t ${registryPrefix}ledger-service:${RELEASE}.${BUILD_NUMBER} ./ledger-service
                        docker push ${registryPrefix}ledger-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ ledger-service image ==============="
                    '''
                }
            }
        }
        stage('ledger-service k8s') {
            when {
                expression {
                    params.ledger_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- ledger-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-ledger-service payments-ledger-service=${registryPrefix}ledger-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== ledger-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- paygo-service image ----------------"
                        echo "image = ${registryPrefix}paygo-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./paygo-service/Dockerfile -t ${registryPrefix}paygo-service:${RELEASE}.${BUILD_NUMBER} ./paygo-service
                        docker push ${registryPrefix}paygo-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ paygo-service image ==============="
                    '''
                }
            }
        }
        stage('paygo-service k8s') {
            when {
                expression {
                    params.paygo_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- paygo-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-paygo-service payments-paygo-service=${registryPrefix}paygo-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== paygo-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- ecocash-service image ----------------"
                        echo "image = ${registryPrefix}ecocash-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./ecocash-service/Dockerfile -t ${registryPrefix}ecocash-service:${RELEASE}.${BUILD_NUMBER} ./ecocash-service
                        docker push ${registryPrefix}ecocash-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ ecocash-service image ==============="
                    '''
                }
            }
        }
        stage('ecocash-service k8s') {
            when {
                expression {
                    params.ecocash_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- ecocash-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-ecocash-service payments-ecocash-service=${registryPrefix}ecocash-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== ecocash-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- onemoney-service image ----------------"
                        echo "image = ${registryPrefix}onemoney-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./onemoney-service/Dockerfile -t ${registryPrefix}onemoney-service:${RELEASE}.${BUILD_NUMBER} ./onemoney-service
                        docker push ${registryPrefix}onemoney-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ onemoney-service image ==============="
                    '''
                }
            }
        }
        stage('onemoney-service k8s') {
            when {
                expression {
                    params.onemoney_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- onemoney-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-onemoney-service payments-onemoney-service=${registryPrefix}onemoney-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== onemoney-service k8s ================"
				  '''
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
                withDockerRegistry(credentialsId: "${registryId}", url: "${registryUrl}")  {
                    sh '''
                        echo "--------------- fbc-service image ----------------"
                        echo "image = ${registryPrefix}fbc-service:${RELEASE}.${BUILD_NUMBER}"
                        docker build -f ./fbc-service/Dockerfile -t ${registryPrefix}fbc-service:${RELEASE}.${BUILD_NUMBER} ./fbc-service
                        docker push ${registryPrefix}fbc-service:${RELEASE}.${BUILD_NUMBER}
                        echo "================ fbc-service image ==============="
                    '''
                }
            }
        }
        stage('fbc-service k8s') {
            when {
                expression {
                    params.fbc_service == true && params.DEPLOY == true
                }
            }
            steps {
                script {
                    sh '''
					echo "--------------- fbc-service k8s ----------------"
                    curl -LO "https://storage.googleapis.com/kubernetes-release/release/v1.20.5/bin/linux/amd64/kubectl"  
                    chmod u+x ./kubectl
					./kubectl set image deployment/payments-fbc-service payments-fbc-service=${registryPrefix}fbc-service:${RELEASE}.${BUILD_NUMBER} -n ${NAMESPACE}
					echo "=============== fbc-service k8s ================"
				  '''
                }
            }
        }
    }
}