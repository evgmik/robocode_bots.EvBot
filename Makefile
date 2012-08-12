# -*- make -*-
# FILE: "/home/evmik/src/my_src/robocode/Makefile"
# LAST MODIFICATION: "Sun, 12 Aug 2012 01:00:13 -0400 (evmik)"
# (C) 2012 by Eugeniy Mikhailov, <evgmik@gmail.com>
# $Id:$

OUTDIR=out
SUPERPACKADE=eem
JFLAGS=-d $(OUTDIR) -classpath /usr/share/java/robocode.jar:

VERSION:=$(shell git describe --tags --abbrev=0)
UUID:=$(shell uuid)

ROBOTS_DIR=~/.robocode/robots/
TESTJAR=EvBot_vtest.jar 
RELEASEJAR=$(SUPERPACKADE).EvBot_$(VERSION).jar

SRC=eem/EvBot.java eem/misc/PaintRobotPath.java
CLASSES=$(SRC:%.java=$(OUTDIR)/%.class)

.SUFFIXES: .java .class 

all: $(CLASSES) $(TESTJAR) copy-jar-test

upload: $(RELEASEJAR)
	 rsync -rvze ssh $(RELEASEJAR) beamhome.dyndns.org:public_html/robocode/	

copy-jar-test: $(ROBOTS_DIR)/$(TESTJAR)

$(ROBOTS_DIR)/$(TESTJAR): $(TESTJAR)
	cp $(TESTJAR) $(ROBOTS_DIR)/$(TESTJAR)

$(TESTJAR): $(CLASSES)
	cp EvBot.properties.test $(OUTDIR)/$(SUPERPACKADE)/EvBot.properties
	cd $(OUTDIR); jar cvfM  ../$@  `find $(SUPERPACKADE) -type f`

$(RELEASEJAR): $(CLASSES)
	cat EvBot.properties.template \
		| sed s'/^uuid=.*$$'/uuid=$(UUID)/ \
		| sed s'/^robot.version=.*$$'/robot.version=$(VERSION)/ \
		> $(OUTDIR)/$(SUPERPACKADE)/EvBot.properties
	cd $(OUTDIR); jar cvfM  ../$@  `find $(SUPERPACKADE) -type f`


out:
	mkdir -p $(OUTDIR)

$(CLASSES):$(OUTDIR)/%.class : %.java $(OUTDIR)
	javac $(JFLAGS) $<

clean:
	rm -f $(CLASSES)
	rm -f *jar
