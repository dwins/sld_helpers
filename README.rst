sld helpers
===========

This project hopes to provide a few basic utility operations for modifying map styles that use the Styled Layer Descriptor standard.
We assume the user will have some familiarity with the XML structure, and automate some common operations that require widespread modifications.

Running
-------

The sld helper utilities are bundled together in a single executable Jar file; you will need a Java Virtual Machine in order to run them.
In many systems it is possible to simple double-click the .jar file in a file browser to run the program, but if that doesn't work you may need to fall back to running the Java command by hand::

   $ java -jar sld_helper-assembly-0.1.jar

A pre-built copy of the project is available from http://dev.opengeo.org/~dwinslow/sld-helper/ .
See below for info on building your own.

Utilities
---------

* **Saturation** Scan a style for constant colors (not derived from expressions) and modify them by adjusting the saturation by a percentage.
  If you're not familiar with saturation, see http://en.wikipedia.org/wiki/HSL_and_HSV for info.

Building
--------

This project uses the SBT build tool; information on installing it is available at https://github.com/harrah/xsbt/wiki/Setup

Once the ``sbt`` command is available, you can build the project with::

    $ sbt assembly

(The first time you run it, it will download a few dependencies.)
The executable Jar ends up in :file:`target/sld_helper-assembly-0.1.jar` and can be used as described above.
If you're planning on developing this tool the SBT usage instructions at https://github.com/harrah/xsbt/wiki/Running will probably come in handy.
