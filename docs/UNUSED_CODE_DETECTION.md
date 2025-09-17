# Unused Code Detection for Grails

This document describes the unused code and dead file detection capabilities added to the Grails framework build system.

## Overview

The unused code detection system provides comprehensive analysis of:
- **Dead files**: Files that are not referenced anywhere in the codebase
- **Unused code elements**: Variables, methods, fields, imports, and other code constructs detected by CodeNarc
- **Comprehensive reporting**: HTML and text reports with actionable insights

## Available Tasks

### Root Project Tasks

```bash
# Run unused code detection across ALL Grails modules
./gradlew detectUnusedCodeAll

# Run analysis on key Grails framework modules only  
./gradlew detectUnusedCodeGrailsModules
```

### Individual Module Tasks

```bash
# Run dead file detection on a specific module
./gradlew :grails-core:detectDeadFiles

# Run complete unused code analysis with CodeNarc
./gradlew :grails-core:unusedCodeReport

# Run just CodeNarc analysis
./gradlew :grails-core:codenarcMain
```

## Configuration

### Applying to New Modules

To enable unused code detection on a Grails module, add this to the module's `build.gradle`:

```gradle
// Apply centralized unused code detection
apply from: rootProject.file('gradle/codenarc-config.gradle')
apply from: rootProject.file('gradle/unused-code-detection.gradle')
```

### Customizing Analysis

The CodeNarc rules can be customized by editing `gradle/codenarc-rules.groovy`. Key configuration options:

```gradle
// In the module's build.gradle, add exclusions:
codenarcMain {
    exclude '**/SpecificFileToExclude.groovy'
    exclude '**/test/**'
}

// Adjust violation thresholds:
codenarc {
    maxPriority1Violations = 0  // High priority (errors)
    maxPriority2Violations = 5  // Medium priority (warnings) 
    maxPriority3Violations = 10 // Low priority (info)
}
```

## Report Outputs

### Summary Reports (Root Level)
- `build/reports/unused-code-all/grails-unused-code-summary.html` - Interactive HTML overview
- `build/reports/unused-code-all/grails-unused-code-detailed.txt` - Detailed text report

### Module Reports  
- `build/reports/unused-code/dead-files-report.txt` - Dead file analysis
- `build/reports/unused-code/unused-code-summary.html` - Module summary
- `build/reports/codenarc/main.html` - CodeNarc detailed findings

## Understanding the Analysis

### Dead File Detection
The dead file detector analyzes source files and looks for:
- Import statements
- Class references and inheritance
- Method calls and instantiations  
- Annotations usage
- Generic type usage

**Note**: Files are marked as "potentially dead" - manual review is recommended before deletion.

### CodeNarc Analysis
CodeNarc detects:
- Unused variables, methods, and fields
- Unnecessary imports and code constructs
- Empty methods and classes
- Duplicate code patterns
- Dead code blocks

### False Positives
Common scenarios that may cause false positives:
- **Reflection usage**: Files accessed via reflection may appear unused
- **Spring/Grails conventions**: Convention-based usage may not be detected
- **Test utilities**: Test helper classes may appear unused
- **Configuration files**: Framework configuration files are often not directly referenced
- **DSL usage**: Groovy DSL patterns may not be detected

## Best Practices

1. **Review Before Deletion**: Always manually verify that identified dead files are truly unused
2. **Run Tests**: Execute the full test suite after removing unused code
3. **Check Conventions**: Verify that removed code doesn't break Grails conventions
4. **Update Dependencies**: Remove unused dependencies from `build.gradle` files
5. **Incremental Cleanup**: Remove unused code in small, reviewable chunks

## Examples

### Finding Unused Imports
```bash
./gradlew :grails-web-core:codenarcMain
# Check build/reports/codenarc/main.html for "UnusedImport" violations
```

### Detecting Dead Configuration Files
```bash  
./gradlew :grails-core:detectDeadFiles
# Review build/reports/unused-code/dead-files-report.txt
```

### Project-wide Analysis
```bash
./gradlew detectUnusedCodeAll
# Open build/reports/unused-code-all/grails-unused-code-summary.html
```