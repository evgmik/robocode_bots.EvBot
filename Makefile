# -*- make -*-
# FILE: "/home/evmik/src/my_src/robocode/Makefile"
# LAST MODIFICATION: "Mon, 16 Sep 2013 18:30:34 -0400 (evmik)"
# (C) 2012 by Eugeniy Mikhailov, <evgmik@gmail.com>
# $Id:$

OUTDIR=out
SUPERPACKADE=eem

#ROBOTS_DIR=~/.robocode/robots/
ROBOTS_DIR= ~/misc/rumble-1.7.3.0/robots/
# ROBOCODEJAR=/usr/share/java/robocode.jar
# ROBOCODEJAR=~/misc/robocode-1.8.1/libs/robocode.jar
ROBOCODEJAR=~/misc/rumble-1.7.3.0/libs/robocode.jar

JFLAGS=-d $(OUTDIR) -classpath $(ROBOCODEJAR):

TESTVERSION:=$(shell date +%H:%M)
VERSION:=$(shell git describe --tags --abbrev=0)
UUID:=$(shell uuid)

TESTJAR=EvBot_vtest.jar 
RELEASEJAR=$(SUPERPACKADE).EvBot_$(VERSION).jar

SRC=eem/EvBot.java eem/botVersion.java \
    $(wildcard eem/misc/*.java) \
    $(wildcard eem/radar/*.java) \
    $(wildcard eem/gun/*.java) \
    $(wildcard eem/target/*.java)

CLASSES=$(SRC:%.java=$(OUTDIR)/%.class)

.SUFFIXES: .java .class 

all: $(CLASSES) $(TESTJAR) copy-jar-test

release: $(RELEASEJAR)
	cp $(RELEASEJAR) $(ROBOTS_DIR)/$(RELEASEJAR)

upload: $(RELEASEJAR)
	 rsync -rvze ssh $(RELEASEJAR) beamhome.dyndns.org:public_html/robocode/	

copy-jar-test: $(ROBOTS_DIR)/$(TESTJAR)

$(ROBOTS_DIR)/$(TESTJAR): $(TESTJAR)
	cp $(TESTJAR) $(ROBOTS_DIR)/$(TESTJAR)

$(TESTJAR): $(CLASSES)
	cat EvBot.properties.template \
		| sed s'/^uuid=.*$$'/uuid=$(UUID)/ \
		| sed s'/^robot.version=.*$$'/robot.version=vtest_$(TESTVERSION)/ \
		> $(OUTDIR)/$(SUPERPACKADE)/EvBot.properties
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
