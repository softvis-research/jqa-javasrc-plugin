# jQAssistant Java Source Code Plugin

[![GitHub license](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/softvis-research/jqa-javasrc-plugin/blob/master/LICENSE)
[![Build Status](https://travis-ci.com/softvis-research/jqa-javasrc-plugin.svg)](https://travis-ci.com/softvis-research/jqa-javasrc-plugin)

This is a java source code parser for [jQAssistant](https://www.jqassistant.org).
It enables jQAssistant to scan and to analyze Java source code files.

## Getting Started

Download the jQAssistant command line tool for your system: [jQAssistant - Get Started](https://jqassistant.org/get-started/).

Next download the latest version from the release tab. Put the `jqassistant-javasrc-plugin-*.jar` into the plugins folder 
of the jQAssistant commandline tool.

Now scan your java source code and wait for the plugin to finish:

```bash
jqassistant.sh scan -f <java-source-code-file>
```

You can then start a local Neo4j server to start querying the database at [http://localhost:7474](http://localhost:7474):

```bash
jqassistant.sh server
```

## Configuration Parameters for Maven

```
<configuration>
	<scanIncludes>
		<scanInclude>
			<path>[PATH TO SOURCE ROOT FOLDER]</path>
			<scope>java:src</scope>
		</scanInclude>
	</scanIncludes>
	<scanProperties>
		<jqassistant.plugin.javasrc.jar.dirname>[PATH TO FOLDER WITH JAR DEPENDENCIES]</jqassistant.plugin.javasrc.jar.dirname>
	</scanProperties>
</configuration>
```
