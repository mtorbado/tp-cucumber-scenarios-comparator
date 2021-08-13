javac ScenariosComparator.java
javac Scenario.java
echo Manifest-Version: 1.0 > ScenariosComparator.mf
echo Main-Class: ScenariosComparator >> ScenariosComparator.mf
jar cmf ScenariosComparator.mf ScenariosComparator.jar *.class *.java
rm ScenariosComparator.class
rm Scenario.class
rm ScenariosComparator.mf

