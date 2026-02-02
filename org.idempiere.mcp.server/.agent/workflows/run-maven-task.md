---
description: How to run long-running Maven build and test tasks reliably
---

// turbo-all
# Running Long-Running Maven Tasks

Use this workflow for Maven tasks that are expected to take more than 60 seconds (e.g., full project compilation, integration tests).

## Configuration
1. **Batch Mode**: Always use `-B` to ensure clean, line-oriented output.
2. **Java Home**: Set `JAVA_HOME` to `/usr/lib/jvm/java-17-temurin-jdk`.
3. **Workspace**: Run from the relevant project or workspace root folder.

## Execution Pattern

1. **Start Task with Redirection**:
   Run the command in the background, redirecting both stdout and stderr to `build.log`. Use a significant `WaitMsBeforeAsync` (e.g., 5000ms) to ensure the file is created.
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-temurin-jdk && mvn -B [goals] > build.log 2>&1
   ```

2. **Poll Progress**:
   Instead of waiting for the terminal tool to return, poll the `build.log` file using `view_file` or `run_command` with `tail`.
   ```bash
   tail -n 20 build.log
   ```

3. **Verify Completion**:
   Check for `BUILD SUCCESS` or `BUILD FAILURE` in the log.
   ```bash
   grep -E "BUILD SUCCESS|BUILD FAILURE" build.log
   ```

## Handling Timeouts
- If the `run_command` tool itself returns a timeout, do not assume the build stopped. The background process will continue writing to `build.log`.
- Simply use `view_file` on `build.log` to check the current status and whether the process has finished.
