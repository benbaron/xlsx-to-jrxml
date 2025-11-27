Perfect—here’s a tiny Maven project scaffold that builds a **single runnable fat-jar** for the converter you approved.

# Project layout

```
xlsx2jrxml/
├─ pom.xml
└─ src/
   └─ main/
      └─ java/
         └─ XlsxToJrxml.java   ← paste the full Java file I gave you earlier
```

# `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>org.scaledger.tools</groupId>
  <artifactId>xlsx2jrxml</artifactId>
  <version>1.0.0</version>
  <name>xlsx2jrxml</name>
  <description>XLSX/XLSM → JRXML (detail-only bands, blue-field naming, optional beans)</description>
  <properties>
    <!-- set your JDK; 17 is a good default for current Eclipse/IDE setups -->
    <maven.compiler.release>17</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <main.class>XlsxToJrxml</main.class>
    <poi.version>5.2.5</poi.version>
  </properties>

  <dependencies>
    <!-- Apache POI: brings poi-ooxml and its transitives (xmlbeans, commons-*, curvesapi) -->
    <dependency>
      <groupId>org.apache.poi</groupId>
      <artifactId>poi-ooxml</artifactId>
      <version>${poi.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- compile against chosen Java release -->
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
          <release>${maven.compiler.release}</release>
          <encoding>${project.build.sourceEncoding}</encoding>
        </configuration>
      </plugin>

      <!-- build a single fat-jar with Main-Class set -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.5.3</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
              <createDependencyReducedPom>true</createDependencyReducedPom>
              <shadedArtifactAttached>true</shadedArtifactAttached>
              <shadedClassifierName>all</shadedClassifierName>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>${main.class}</mainClass>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

# Build

```bash
cd xlsx2jrxml
mvn -q -DskipTests clean package
# Fat jar: target/xlsx2jrxml-1.0.0-all.jar
```

# Run (examples)

```bash
# Basic: JRXML only
java -jar target/xlsx2jrxml-1.0.0-all.jar \
  --in SCAFinancialReportv6_LARGE_NOLONGERLOCKED.xlsm \
  --out outdir

# With beans (default package org.scaledger.reports.AccountBeans)
java -jar target/xlsx2jrxml-1.0.0-all.jar \
  --in SCAFinancialReportv6_LARGE_NOLONGERLOCKED.xlsm \
  --out outdir \
  --beans \
  --beans-package org.scaledger.reports.AccountBeans
```

# Notes you can tweak

* **JDK version:** change `<maven.compiler.release>` to 21 if you prefer Java 21.
* **Main class name/location:** the POM assumes `XlsxToJrxml` is in the default package. If you move it into a package (e.g., `org.scaledger.tools`), update:

  * The first line of the `.java` (`package …;`), and
  * `<main.class>` in the POM to that FQCN.
* **Offline builds:** since you often vendor deps, the shaded jar already contains everything. If you need a local repo freeze, I can add a `dependency:go-offline` step or a script to populate a local Maven cache.

Want me to generate a ready-to-import ZIP with this layout and the Java file pre-dropped in?


  

