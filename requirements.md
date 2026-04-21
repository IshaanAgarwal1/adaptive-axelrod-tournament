# Requirements

## Language
- Java 17

## Build Tool
- Maven

## Dependencies
- JFreeChart (for visualization)

## How to Run

1. Clone the repository
2. Navigate to the project root
3. Run:

   mvn clean compile
   mvn exec:java -Dexec.mainClass="com.axelrod.adaptive.Main"

## Notes
- All experiments are configurable inside Main.java
- Results are exported to the /results folder