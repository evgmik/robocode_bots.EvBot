# -*- make -*-
# FILE: "/home/evmik/.robocode/robots/eem/Makefile"
# LAST MODIFICATION: "Sat, 14 Jul 2012 18:30:10 -0400 (evmik)"
# (C) 2012 by Eugeniy Mikhailov, <evgmik@gmail.com>
# $Id:$


all: EvBot.class

EvBot.class: EvBot.java
	javac EvBot.java -classpath classes:/usr/share/java/robocode.jar

clean:
	rm EvBot.class
