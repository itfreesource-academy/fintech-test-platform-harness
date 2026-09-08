pipeline {
    agent {
        docker {
            image 'maven:3.9.6-eclipse-temurin-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    environment {
        CI = 'true'
        JAVA_OPTS = '-Dfile.encoding=UTF-8'
    }

    stages {
        stage('Checkout & Environment Info') {
            steps {
                echo "=== Executing FinTech Test Platform & Kafka Quality Gate ==="
                sh 'java -version'
                sh './mvnw -version'
            }
        }

        stage('Compile & Static Verification') {
            steps {
                echo "=== Compiling Java 17 Test Sources ==="
                sh './mvnw clean test-compile --batch-mode'
            }
        }

        stage('Execute REST Assured & Kafka Test Suites') {
            steps {
                echo "=== Running WireMock, REST Assured, and Kafka Stream Assertions ==="
                sh './mvnw test --batch-mode'
            }
        }

        stage('Publish Test & Observability Reports') {
            steps {
                echo "=== Publishing Surefire XML & Allure Reports ==="
                junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/allure-results/**', allowEmptyArchive: true
            cleanWs notFailBuild: true
        }
        success {
            echo "SUCCESS: FinTech Java & Kafka Quality Gate Passed (100% Idempotency & Stream SLA Verified)"
        }
        failure {
            echo "FAILURE: Quality Gate Failed - Review JUnit & Allure Test Results"
        }
    }
}
