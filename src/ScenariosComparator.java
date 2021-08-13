import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * ScenariosComparator is a tool designed to compare 2 folders that contain .feature files with Gherkin scenarios.
 * It uses the scenarios from first folder in the arguments as reference and search for them in the second one.
 * It ignores whitespace, tags (@) and comment lines (#).
 * It also ignores feature descriptions, rules and backgrounds.
 * Check the readme.md file for usage info.
 */
public class ScenariosComparator {

    private static final String LINE_SEPARATOR = "-----------------------------------------------------------------------------------------------------------";
    private static int totalFound = 0;
    private static int diffContentCount = 0;
    private static int diffLengthCount = 0;
    private static int notFoundCount = 0;

    /**
     * This parameter controls if the whitespace between bars ("|") in the Gherkin tables is ignored in the comparison.
     * Depending how you upload your test scenarios to Jira, it might be very common that this whitespace differs,
     * so to make easier to read the tool output, if you get a lot of these differences, you can turn this variable to
     * true. However, we advise you to fix them as a good practise.
     *
     * Copying and pasting the scenarios directly to Jira via web browser is a common source of this issue. Instead,
     * you can use the xray-endpoint-update jenkinsfile that you can find in the last version of FE and BE seeds to
     * ensure that the whitespace is correct when uploading tests.
     */
    private static boolean ignoreTableWhitespace = false;

    /**
     * Scenarios comparator main method.
     * It should be called with 2 command-line arguments containing the paths of the feature folders to compare
     * @param args
     *        args[0] = path_to_xray_scenarios
     *        args[1] = path_to_repository_scenarios
     *        args[2] = ignoreTableWhitespace ("true" to enable)
     */
    public static void main(String[] args) {

        Map<String, Scenario> jiraScenarioList = new HashMap<>();
        Map<String, Scenario> repoScenarioList = new HashMap<>();
        Set<String> jiraScenarioNames = new HashSet<>();
        Set<String> repoScenarioNames = new HashSet<>();

        if (args.length == 3) {
            if (args[2].equals("true")) {
                ignoreTableWhitespace = true;
            }
        }

        try {
            lookForIn(new File(args[0]), jiraScenarioList);
            lookForIn(new  File(args[1]), repoScenarioList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String key : jiraScenarioList.keySet()) {

            if (jiraScenarioNames.contains(key)) {
                System.out.println("Scenario: " + key + " | DUPLICATED NAME IN JIRA |");
            }
            if (repoScenarioNames.contains(key)) {
                System.out.println("Scenario: " + key + " | DUPLICATED NAME IN REPOSITORY |");
            }

            jiraScenarioNames.add(key);
            repoScenarioNames.add(key);

            if (repoScenarioList.containsKey(key)) {
                totalFound ++;
                if (jiraScenarioList.get(key).getLength() != repoScenarioList.get(key).getLength()) {
                    diffLengthCount++;
                    System.out.println(LINE_SEPARATOR);
                    System.out.println("Scenario: " + key + " | DIFF LENGTH |");
                    // TODO: (future improvement) link to jira issue
                }
                else if (!checkLines(jiraScenarioList.get(key), repoScenarioList.get(key))) {
                    diffContentCount++;
                    System.out.println(LINE_SEPARATOR);
                    System.out.println("Scenario: " + key + " | DIFF CONTENT |");
                    scenarioDiff(jiraScenarioList.get(key), repoScenarioList.get(key));
                    // TODO: (future improvement) link to jira issue
                }
                else {
                    System.out.println(LINE_SEPARATOR);
                    System.out.println("Scenario: " + key + " | Scenario OK |");
                }
            }
            else {
                notFoundCount++;
                System.out.println(LINE_SEPARATOR);
                System.out.println("Scenario: " + key + " | NOT FOUND IN REPOSITORY |");
            }
        }
        System.out.println(LINE_SEPARATOR);
        System.out.println("Total found: " + totalFound);
        System.out.println("Tests with different length: " + diffLengthCount);
        System.out.println("Tests with different content: " + diffContentCount);
        System.out.println("Tests not found: " + notFoundCount);

        generateEmailBody(); // You can remove this line for local executions
    }


    /**
     * Recursive method to loop though all directories that contain .feature files
     * @param dir Initial directory
     * @param scenarioMap Map to store the Gherkin scenarios
     * @throws Exception
     */
    private static void lookForIn (File dir, Map<String, Scenario> scenarioMap) throws Exception {

        File fileList[] = dir.listFiles();

        if (fileList != null && fileList.length > 0) {
            for (File file : fileList) {
                if (file.isDirectory()) lookForIn(file, scenarioMap); //recursive call for looping through all folders
                else readFeatureFile(file, scenarioMap);
            }
        }
    }


    /**
     * Method that separates each scenario inside a Cucumber .feature file and stores it into a java map
     * @param file Cucumber .feature file
     * @param scenarioMap Map to store the Gherkin scenarios
     * @throws FileNotFoundException
     */
    private static void readFeatureFile (File file, Map<String, Scenario> scenarioMap) throws FileNotFoundException {

        Scanner scanner = new Scanner(file);
        List<String> lines = new ArrayList<>();
        Scenario scenario = new Scenario();

        boolean isFeatureOrBackground = false;
        boolean isDocString = false;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("\"\"\"")) { //------------------------------------------------------------------------- start or end of doc string
                isDocString = !isDocString;
            }
            if ((line.startsWith("Feature:") || line.startsWith("Background:")) && !isDocString) { //------------------- new feature or background
                isFeatureOrBackground = true;
            }
            else if (line.startsWith("Scenario") && !isDocString) { //---------------------------------------------------- new scenario
                scenario.setName(line.split(":", 2)[1].substring(1).trim());
            }
            else if (isDocString || (isStepLine(line) && !isFeatureOrBackground)) { //---------------------------------- new line
                if( line.startsWith("|") && ignoreTableWhitespace) { // line from table
                    line = line.replaceAll("([ \t])*\\|([ \t])*", "|");
                }
                scenario.addLine(line);
                if (!scanner.hasNextLine() && scenario.getName()!=null) { //-------------------------------------------- end of scenario
                    scenarioMap.put(scenario.getName(), scenario);
                    lines.clear();
                    scenario = new Scenario();
                }
            }

            else if (!isStepLine(line) && !scanner.hasNextLine() && scenario.getName()!= null) { //--------------------- end of scenario
                scenarioMap.put(scenario.getName(), scenario);
                lines.clear();
                scenario = new Scenario();
            }

            else if (isFeatureOrBackground && line.equals("")) { //----------------------------------------------------- end of feature or background
                isFeatureOrBackground = false;
            }

            if (!isStepLine(line) && isNextScenario(scanner) && scenario.getName()!=null) { //----------------------- end of scenario
                scenarioMap.put(scenario.getName(), scenario);
                lines.clear();
                scenario = new Scenario();
            }
        }
    }

    /**
     * Checks if a new scenario (or rule) starts in the next line
     * @param scanner Scanner reading the feature file
     * @return true if scenario fisish,, false otherwise
     */
    private static boolean isNextScenario(Scanner scanner) {
        if (scanner.hasNext("Scenario( Outline)?( )?:.*")) { // next scenario starts (without tags)
            return true;
        }
        else if(scanner.hasNext("Rule( )?:.*")) { // next rule starts
            return true;
        }
        else if (scanner.hasNext("@.*")) { // next scenario starts (with tags)
            return true;
        }
//        else if (!scanner.hasNextLine()) { // end of file
//            return true;
//        }
        return false;
    }

    /**
     * Checks if given string is a regular scenario step.
     * @param line line from feature file
     * @return true if line is a regular step line, false otherwise
     */
    private static boolean isStepLine(String line) {
        if (line.equals("")) return false;
        if (line.startsWith("@")) return false;
        if (line.startsWith("#")) return false;
        if (line.startsWith("Rule:")) return false;
        return true;
    }

    /**
     * Checks that all the lines (steps) from 2 scenarios are equal
     * @param s1 scenario 1
     * @param s2 scenario 2
     * @return true if steps are equal, false otherwise
     */
    private static boolean checkLines (Scenario s1, Scenario s2) {
        for (int i=0 ; i < s1.getLength(); i++) {
            if (!s1.getLines().get(i).equals(s2.getLines().get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prints the lines that are differ betweent the 2 given scenarios
     * @param s_xray scenario from Xray
     * @param s_repo scenario from repository
     */
    private static void scenarioDiff (Scenario s_xray, Scenario s_repo) {
        for (int i=0 ; i < s_xray.getLength(); i++) {
            if (!s_xray.getLines().get(i).equals(s_repo.getLines().get(i))) {
                System.out.println(" --> Xray: " + s_xray.getLine(i));
                System.out.println(" --> Repo: " + s_repo.getLine(i));
            }
        }
    }

    /**
     * Writes an html file with a result summary to send via email with the automated execution in Jenkins
     */
    private static void generateEmailBody()  {

        int totalFailed = diffContentCount + diffLengthCount + notFoundCount;
        try {
            String htmlString = new String(Files.readAllBytes(Paths.get("../jenkins/email_template.html")));

            if (totalFound == 0) {
                htmlString = htmlString.replace("$red_text", "NO TEST SCENARIOS WERE FOUND :(");
                htmlString = htmlString.replace("$green_text", "");
                htmlString = htmlString.replace("$result_summary" , "Please, check that the urls to Xray and repository are set correctly.");
            }
            else if (totalFailed == 0) {
                htmlString = htmlString.replace("$green_text", "Test scenarios are synchronized :)");
                htmlString = htmlString.replace("$red_text", "");
                htmlString = htmlString.replace("$result_summary", "Total scenario count: " + totalFound);
            }
            else {
                htmlString = htmlString.replace("$red_text", "TEST SCENARIOS NOT SYNCHRONIZED :(");
                htmlString = htmlString.replace("$green_text", "");
                htmlString = htmlString.replace("$result_summary", totalFailed + " scenarios do not match.");
            }
            Files.write(Paths.get("../email_body.html"), htmlString.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}