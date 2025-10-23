pipeline {
	agent any
	environment {
        IMAGE_NAME = 'hg-backend'
    }
    stages {
		stage('Checkout in Parallel') {
			parallel {
				stage('Checkout hg-openapi') {
					steps {
						dir('hg-openapi') {
							checkout([
                                $class: 'GitSCM',
                                branches: [[name: '*/main']],
                                userRemoteConfigs: [[
                                    url: 'https://github.com/Hydrogarden-App/hg-openapi.git',
                                    credentialsId: 'jenkins-hydrogarden-girhub-app'
                                ]]
                            ])
                        }
                    }
                }
                stage('Checkout hg-backend') {
					steps {
						dir('hg-backend') {
							checkout([
                                $class: 'GitSCM',
                                branches: [[name: '*/main']],
                                userRemoteConfigs: [[
                                    url: 'https://github.com/Hydrogarden-App/hg-backend.git',
                                    credentialsId: 'jenkins-hydrogarden-girhub-app'
                                ]]
                            ])
                        }
                    }
                }
            }
        }

        stage('Build hg-openapi') {
			steps {
				dir('hg-openapi') {
					sh 'mvn clean install -B'
                }
            }
        }

        stage('Build hg-backend') {
			steps {
				dir('hg-backend') {
					sh 'mvn clean install -B -DskipTests'
                }
            }
        }

        stage('Build Backend Image') {
			steps {
				script {
					dir('hg-backend') {
						sh """
                        docker build -t ${IMAGE_NAME}:latest .
                    """
                }
                }
            }
        }

    	stage('Deploy to Prod') {
			steps {
				script {
                    sh 'cd /opt/hydrogarden/docker && docker compose down hg-backend && docker compose up -d'
                }
            }
        }

    }
}
