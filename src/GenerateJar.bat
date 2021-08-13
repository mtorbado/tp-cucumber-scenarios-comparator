javac ScenariosComparator.java
echo Manifest-Version: 1.0 > ScenariosComparator.mf
echo Main-Class: ScenariosComparator >> ScenariosComparator.mf
jar cmf ScenariosComparator.mf ScenariosComparator.jar ScenariosComparator.class ScenariosComparator.java
rm ScenariosComparator.class
rm ScenariosComparator.mf

