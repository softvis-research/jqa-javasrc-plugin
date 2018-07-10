# jQAssistant Java Source Parser Plugin &middot; [![GitHub license](https://img.shields.io/badge/License-GPL%20v3-blue.svg)] (https://github.com/softvis-research/jqa-javasrc-plugin/blob/master/LICENSE) #

This is a java source parser of [jQAssistant](https://www.jqassistant.org).
It enables jQAssistant to scan and to analyze Java source code files.

## Configuration Parameters for Maven ##

```
<configuration>
	<scanIncludes>
		<scanInclude>
			<scanInclude>
				<path>[PATH TO SOURCE ROOT FOLDER]</path>
				<scope>java:src</scope>
			</scanInclude>
	</scanIncludes>
	<scanProperties>
		<jqassistant.plugin.javasrc.jar.dirname>[PATH TO FOLDER WITH JAR DEPENDENCIES]</jqassistant.plugin.javasrc.jar.dirname>
	<scanProperties>
</configuration>
```

## TODO ##

- add implicit default constructor?
- add enum constructor (not supported in javaparser/symbolsolver v3.6.2)
- add comments
- check effective line count (comments, empty methods)
- handle inner classes? (inner classes' fqn are with . instead of $)
- extract reusable scan method for tests
- Inheritance: Object is not set as super class
- TypeDescriptor: Why does typeDescriptor.getDeclaredFields() return fields, methods, or classes (same for getDeclaredMethods)?
- InterfaceTypeDescriptor: An interface might extend from multiple interfaces, currently there is only one super class possible.