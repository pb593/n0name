BRIEF MAP OF THIS PROJECT

src/ 		– Java code with numerous packages
python/ 	- server-side code, evaluation scripts
docs/		- all the write-up, from proposal to dissertation
res/ 		– icons and rington for GUI
lib/		- third-party libs used by Java


The compiled JAR file is in out/artifacts/part2proj_jar

Here are some ways to run to run it from the terminal.

java -jar part2proj.jar  				--- default, runs the GUI with patching every 3 seconds
java -jar part2proj.jar -gui 1500 		--- also GUI, but with custom patching (once every 1.5 sec)
java -jar part2pror.jar -cli 			--- CLI with default patching every 3s
java -jar part2proj.jar -m 1200 pb593	--- super-minimalistc machine interface, with custom patching period and name

If do not need anything in particular and just want to see a working instance of the application, just double click the file. This should trigger the default above.