#!groovy

// Cucumber Scenarios Comparator jenkinsfile
// Tool repository: https://tools.adidas-group.com/bitbucket/projects/TE/repos/tp-cucumber-scenarios-comparator/browse

@Library(["GlobalJenkinsLibrary@master"]) _

final def testNode = "serenity" //DO NOT MODIFY


// == git repository with the scenarios to check ==
final def repoScenarios = ''
final def gitScenariosBranch = ''
final def gitCredentials = ''

// == git repository with the scenarios comparator ==
final def repoScenariosComparator = 'https://tools.adidas-group.com/bitbucket/scm/te/tp-cucumber-scenarios-comparator.git' //DO NOT MODIFY
final def gitComparatorBranch = 'master' //DO NOT MODIFY

// == Jira test set with the scenarios to check ==
final def xrayExportFeaturesUrl = 'https://tools.adidas-group.com/jira/rest/raven/1.0/export/test?keys=' //DO NOT MODIFY
final def xrayCredentials = ''
final def testSet = '' // (or test plan)
final def featuresUrl = xrayExportFeaturesUrl + testSet //DO NOT MODIFY
final def featureDir = 'xray_features' //DO NOT MODIFY
final def outputFile = 'features.zip' //DO NOT MODIFY

final def notifyByEmail = ''

// set to 'true' to ignore whitespace between bars ("|") in Gherkin tables. Check  the tool readme for more information on the topic.
final def ignoreTableWhitespace = 'false'

node(testNode) {
    try {
        currentBuild.result = 'SUCCESS'

        stage('Download scenarios code from remote repository') {
            flows.git.clone(repositoryUrl: repoScenarios, branch: gitScenariosBranch,  credentials: gitCredentials, dir: 'scenarios')
        }

        stage('Download scenarios from xray') {
            sh "mkdir -p ${featureDir}"
            flows.xray.getFeatures(featureDir: featureDir, credentials: xrayCredentials, outputFile: outputFile, url: featuresUrl)
        }

        stage('Download scenarios comparator') {
            flows.git.clone(repositoryUrl: repoScenariosComparator ,  branch: gitComparatorBranch, credentials: gitCredentials, dir: 'scenarios-comparator')
        }

        stage('Compile scenarios comparator') {
            sh 'cd scenarios-comparator/src && ' + 'chmod  +x GenerateJar.sh && ' + './GenerateJar.sh'
        }

        stage('Run scenarios comparator') {
            comparatorCommand = 'cd scenarios-comparator/src &&' + 'java -jar ScenariosComparator.jar ../../xray_features/ ../../scenarios/src/test/resources/features ' + ignoreTableWhitespace + ' > comparatorOutput.txt 2>&1'
            sh comparatorCommand
            sh 'cat scenarios-comparator/src/comparatorOutput.txt'
        }

        stage('Send Email notifications') {

            emailext attachLog: true, attachmentsPattern: 'scenarios-comparator/src/comparatorOutput.txt',
                    body: '''${FILE, path="scenarios-comparator/email_body.html"}''',
                    subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!',
                    to: notifyByEmail,
                    mimeType: 'text/html'
        }



    } catch(e) {
        echo "Caught: ${e}"
        currentBuild.result = 'FAILURE'
    }
}