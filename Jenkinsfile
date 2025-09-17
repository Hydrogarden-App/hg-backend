pipeline {
	agent any
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
					sh 'mvn clean install -B'
                }
            }
        }

        stage('Hello') {
			steps {
				echo 'Hello World'
            }
        }
    }
}
