import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for ScenariosComparator to store Scenario info as object
 */
public class Scenario {

    private String name =null;
    private List<String> lines;

    /**
     * Create empty scenario
     */
    public Scenario() {
        lines = new ArrayList<>();
    }

    /**
     * Create scenario with name
     * @param name Scenario name
     */
    public Scenario(String name) {
        lines = new ArrayList<>();
        this.name = name;
    }

    /**
     * Create scenario with name and lines
     * @param name Scenario name
     * @param lines List of scenario lines (steps)
     */
    public Scenario(String name, List<String> lines) {
        lines = new ArrayList<>();
        this.name = name;
        this.lines = lines;
    }

    /**
     * Set the name of the scenario
     * @param name Scenario name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the scenario lines (steps)
     * @param lines List of scenario lines (steps)
     */
    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    /**
     * Add a line (step) to the scenario
     * @param line Step line
     */
     public void addLine(String line) {
        lines.add(line);
     }

    /**
     * Returns the name of the scenario
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns a list with each line (step) of the scenario
     * @return lines
     */
    public List<String> getLines() {
        return this.lines;
    }

    /**
     * Returns the line (step) at 'n' position
     * @param n line number (starting at 0)
     * @return line
     */
    public String getLine(int n) {
        return this.lines.get(n);
    }

    /**
     * Returns the number of lines of the scenario
     * @return size
     */
    public int getLength() {
        return lines.size();
    }
}
