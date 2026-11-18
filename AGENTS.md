# AGENTS.md

Instructions for any AI coding agent working in this repository.

## Build and test environment
- JoshHack is a Java 17 Maven project. Always use the Maven Wrapper (`./mvnw`), not a global `mvn`.
- Production code is in `src/main/java` (`game` and `ui` packages). Tests are in `src/test/java` and use JUnit 5.
- GitHub Actions (`.github/workflows/quality.yml`) runs JUnit, then PIT, then SpotBugs and uploads the SARIF results to Code Scanning.

## Quality checks before work is considered complete
- Run the full JUnit suite: `./mvnw test`. Every test must pass.
- If you changed or added tests, run PIT: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`.
- If you changed production code, run SpotBugs: `./mvnw spotbugs:spotbugs`.
- Report the results of the checks you ran.

## Rules
- Preserve existing behavior unless a change is explicitly requested.
- Never weaken, remove, skip, or disable tests just to make the build pass.
- Any production-code change (anything in `src/main/java`, `pom.xml`, or the workflow) must be clearly identified for human review. Ask before making it.
- Do not commit, push, or merge changes. A human reviews and commits all work.
- Keep changes small and focused on the task you were given.
