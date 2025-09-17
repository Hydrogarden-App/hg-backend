pipeline {
	agent any
	environment {
		DOCKER_REGISTRY = '10.8.0.1:5000'
        IMAGE_NAME = 'hg-backend'
        DOCKER_TAG = 'latest'
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
                                    credentialsId: 'hydrogarden-app-jenkers'
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
                                    credentialsId: 'hydrogarden-app-jenkers'
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
                        docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${DOCKER_TAG} .
                    """
                }
                }
            }
        }

        stage('Push Docker Image') {
			steps {
				script {
					sh 'docker push 10.8.0.1:5000/hg-backend:latest'

                }
            }
   		}

    	stage('Deploy to Prod') {
			steps {
				script {
                    sh 'sudo systemctl restart hg-docker-compose.service'

                    sh 'sudo systemctl status hg-docker-compose.service --no-pager'
                }
            }
        }

    }
}
