# Cucumber Scenarios Comparator


##### Java tool to compare two directories containing cucumber feature files.


This tool was designed to check differences between Jira Xray test sets or test plans and their repository counterparts.

If you are using the last version of our Adidas TAS SerenityBDD (FE or BE) seeds as base for your TAS project and your
test scenarios are being automatically updated to the  Xray Cucumber test importing endpoint when merging any changes,
this tool can be used as a complement to check if any scenario is not synchronized. This can happen in the following cases:

- A test scenario was modified in Jira and the changes has not been replicated in the repository.
- A test scenario was deleted from the repository but it remains in Jira.

The tool will also detect if there are any repeated scenario names either in Jira or the repository.  

Otherwise, if your tests are not being automatically updated to Xray, the tool would also point out any other difference
in the scenarios.

***
#### How to run as a Jenkins Job

You can set a Jenkins job to run periodically the Scenarios Comparator, so it notifies you when any difference between Xray
and repository scenarios gets detected.

To do so, you need to edit the following parameters in the Scenarios Comparator jenkinsfile:

- ```repoScenarios:``` Url to git repository that contains the tas project with scenarios to check.
- ```gitScenariosCredentials:``` Git credentials for the given repository.
- ```testSet:``` Id of the Jira test set (or test plan) that contains the scenarios to check.
- ```xrayCredentials:``` Xray credentials to download the test set.
- ```notifyByEmail:``` The emails of the members of the team that you want to notify when a difference gets detected.
  You can add multiple emails separated by a comma.
- ```ignoreTableWhitespace:``` This parameter controls whether if the whitespace between bars ( | ) in gherkin tables is 
ignored or not in the scenario comparison. This parameter was added as differences in this whitespace is a common issue 
caused when uploading scenarios by copying and pasting them in Jira web page. You can set it to 'true' to get a cleaner
tool output. However, we **do not recommend** ignoring whitespace differences by using the tool this way by default and
the correct approach is to fix the whitespace in Jira so it matches your repository.

When the automated execution finish, it will send an email to the given email addresses with the results:

```Test scenarios are synchronized :)``` = All test scenarios from Xray an the repository match (but there may be duplicates).
 
```TEST SCENARIOS NOT SYNCHRONIZED :(``` = There are differences between Xray and repository scenarios.

```NO TEST SCENARIOS WERE FOUND :(``` = The tool couldn't find any matching scenarios between Xray and the repository.

The email also contains 2 attached files:

- ```compartorOutput.txt :``` Detailed execution output. This is the file where you can find each difference found by 
  the tool. Check the "Understanding the tool results" section for more information about it.
  
- ```build.log :``` Jenkins console output for the given execution. You can use it to check for any errors or execution
  failures.
  
***
#### How to run locally

In order to use this tool locally you should have installed [Java JDK](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) in your machine.
1. Compile the app:
    ```
    javac ScenariosComparator.java
    javac Scenario.java
    ```
2. Download feature files from Jira test set or Jira test plan:
    ```
    1- Navigate to the Jira test set or test plan
    2- Click in More > Export to Cucumber
    3- Unzip the FeatureBundle.zip file into a folder
    ```
3. Download repository feature files as project folder (or use local project instead):

4. Run the tool:
    ```
    java ScenariosComparator path-to-xray-folder path-to-repository-folder
    ```

To create an executable JAR file from ScenariosComaprator app, you can use the GenerateJar script:
```
chmod  +x GenerateJar.sh && ./GenerateJar.sh
```

***
#### Understanding the tool results

This is an example output from the tool:
 ```
  Scenario: Example_scenario_1 | Scenario OK |
  -----------------------------------------------------------------------------------------------------------
  Scenario: Example_scenario_2 | DIFF LENGTH |
  -----------------------------------------------------------------------------------------------------------
  Scenario: Example_scenario_3 | DIFF CONTENT |
    --> Xray: And this line is different
    --> Repo: And this line is not the same
  -----------------------------------------------------------------------------------------------------------
  Scenario: Example_scenario_4 | NOT FOUND IN REPOSITORY |
  -----------------------------------------------------------------------------------------------------------
  Scenario: Example_scenario_5 | DUPLICATED NAME IN JIRA |
  -----------------------------------------------------------------------------------------------------------
  Total found: 4
  Tests with different length: 1
  Tests with different content: 1
  Tests not found: 1
  ```

The tool prints a line for each test scenario from Jira (1st argument) it finds in the repository (2nd argument). 
These are the possible results for each scenario:

- ```| Scenario OK |``` : The test scenario in the repository matches the one with the same name in Jira.
- ```| DIFF LENGTH |``` : The number of steps in the scenario are different. This may also hide some other difference, as 
the tool won't do more checks in this case.
- ```| DIFF CONTENT |``` : There are differences in some of the steps. In this case, the output will show each step that 
differs between Jira and the repository.
- ```| NOT FOUND IN REPOSITORY |``` : This scenario is present in Jira (1st argument), but not in the repo (2nd argument).
- ```| DUPLICATED NAME IN JIRA |``` : There are two scenarios in Jira with this same name.

At the end of the file you can find a summary of the run with the following parameters:

- ```Total found```: Number of tests scenarios found in **both** Jira and the repository. 
- ```Tests with different length```: Number of tests that differ in length between Jira and the repository.
- ```Tests with different content```: Number of tests with differences in their steps between Jira and the repository.
- ```Test not found```: Number of tests in Jira that don't exist in the repository.

***
#### Technical information and limitations.

- The tool compares the scenario folders unidirectionally. That is, it searches for all scenario occurrences in the Xray
folder (1st argument) into the repository folder (2nd argument). You can run a full comparison locally by swapping the 
order of the two arguments.

- The tool checks the test scenarios by their name, and **ignores all tags**. Any changes 
made to the tags locally or in Xray will not be detected, therefore you should be careful when changing them or recreating
scenarios.

- The tool trims the steps before comparing them, so any whitespace at the start and at the end of the line is ignored.

- Tags and comments should always be written **in separate lines** to be correctly detected by the tool. Not doing so can
 also cause trouble to the Xray Cucumber test importing endpoint, so it's better to keep them that way.

- The tool also ignores **feature descriptions**, **rules**, **backgrounds** and **comments** that may be written in the
 feature files. This is because test sets and test plans from Jira can only contain test scenarios.

- If you are using the  Xray Cucumber test importing endpoint and your feature files contain any of the above, note that
 they are being updated, but the tool won't find their differences.

- You should also have in mind that Xray limits the Gherkin syntax you can use (see [Xray Supported Gherkin keywords](https://confluence.xpand-it.com/pages/releaseview.action?pageId=46858243#TestinginBDDwithGherkinbasedframeworks(e.g.Cucumber)-SupportedGherkinkeywords)).
 
To find more information about how Xray manages Cucumber tests, see the links below:

[Testing in BDD with Gherkin based frameworks](https://confluence.xpand-it.com/pages/releaseview.action?pageId=46858243)

[Export Cucumber Features](https://confluence.xpand-it.com/display/public/XRAY/Export+Cucumber+Features)

[Importing Cucumber Tests - REST](https://confluence.xpand-it.com/display/public/XRAY/Importing+Cucumber+Tests+-+REST)


***
#### Testing the tool and feedback

This tool was developed as a solo side project. Therefore, it's testing has been limited.

How different teams write their test scenarios can also differ widely, so you might find some situations that were not 
considered for the tool.

***

Developed by Mario Torbado

Based in previous work from Sergio Esteban in GTS project (ex co-worker)