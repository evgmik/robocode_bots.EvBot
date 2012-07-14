# -*- make -*-
# FILE: "/home/evmik/.robocode/robots/eem/Makefile"
# LAST MODIFICATION: "Sat, 14 Jul 2012 14:46:58 -0400 (evmik)"
# (C) 2012 by Eugeniy Mikhailov, <evgmik@gmail.com>
# $Id:$


all: MyFirstRobot.class

MyFirstRobot.class: MyFirstRobot.java
	javac MyFirstRobot.java -classpath classes:/usr/share/java/robocode.jar

clean:
	rm MyFirstRobot.class
