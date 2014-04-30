# -*- make -*-
# FILE: "/home/evmik/src/my_src/robocode_bots/Makefile"
# LAST MODIFICATION: "Wed, 30 Apr 2014 09:57:00 -0400 (evmik)"
# (C) 2012 by Eugeniy Mikhailov, <evgmik@gmail.com>
# $Id:$

SUPERPACKADE = eem

ROBOCODE_VERSION_TO_COMPILE = ~/misc/robocode-1.8.2.0
ROBOCODE_VERSION_TO_RUN = ~/misc/robocode-1.9.1.0
ROBOTS_DIR   = $(ROBOCODE_VERSION_TO_RUN)/robots
ROBOCODEJAR  = $(ROBOCODE_VERSION_TO_COMPILE)/libs/robocode.jar

TESTVERSION := vtest
VERSION     := $(shell git describe --tags --abbrev=0)
UUID        := $(shell uuid)

TESTJAR    = $(SUPERPACKADE).EvBot_$(TESTVERSION).jar 
RELEASEJAR = $(SUPERPACKADE).EvBot_$(VERSION).jar

OUTDIR = out
JFLAGS = -d $(OUTDIR) -classpath $(ROBOCODEJAR): -Xlint:unchecked

SRC = eem/EvBot.java eem/botVersion.java \
    $(wildcard eem/misc/*.java) \
    $(wildcard eem/radar/*.java) \
    $(wildcard eem/motion/*.java) \
    $(wildcard eem/bullets/*.java) \
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
	echo $(TESTVERSION)
	cat EvBot.properties.template \
		| sed s'/^uuid=.*$$'/uuid=$(UUID)/ \
		| sed s'/^robot.version=.*$$'/robot.version=$(TESTVERSION)/ \
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
