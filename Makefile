# -*- make -*-
# FILE: "/home/evmik/src/my_src/robocode/Makefile"
# LAST MODIFICATION: "Sat, 11 Aug 2012 21:41:50 -0400 (evmik)"
# (C) 2012 by Eugeniy Mikhailov, <evgmik@gmail.com>
# $Id:$

OUTDIR=out
SUPERPACKADE=eem
JFLAGS=-d $(OUTDIR) -classpath /usr/share/java/robocode.jar:

ROBOTS_DIR=~/.robocode/robots/
TESTJAR=EvBot_vtest.jar

SRC=eem/EvBot.java eem/misc/PaintRobotPath.java
CLASSES=$(SRC:%.java=$(OUTDIR)/%.class)

.SUFFIXES: .java .class 

all: $(CLASSES) $(TESTJAR) copy-jar-test

copy-jar-test: $(ROBOTS_DIR)/$(TESTJAR)

$(ROBOTS_DIR)/$(TESTJAR): $(TESTJAR)
	cp $(TESTJAR) $(ROBOTS_DIR)/$(TESTJAR)

$(TESTJAR): $(CLASSES)
	cp EvBot.properties.test $(OUTDIR)/$(SUPERPACKADE)/EvBot.properties
	cd $(OUTDIR); jar cvfM  ../EvBot_vtest.jar  `find $(SUPERPACKADE) -type f`

out:
	mkdir -p $(OUTDIR)

$(CLASSES):$(OUTDIR)/%.class : %.java $(OUTDIR)
	javac $(JFLAGS) $<

clean:
	rm -f $(CLASSES)
