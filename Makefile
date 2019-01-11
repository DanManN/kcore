DATA := simplegraph
TYPE := bin
CORE := 0
RAM  := 4G
ARGS := min
SRC  := $(wildcard src/*.java)
OUT  := bin
LIST := $(SRC:src/%.java=$(OUT)/%.class)

all: $(LIST)

$(OUT)/%.class: src/%.java | $(OUT)
	javac -cp "lib/*" -d $| $<

$(OUT):
	@mkdir $@

vdvec:
	@echo -n "$(ARGS) " ;\
	for x in $$(ls -v $(DATA)/edgedecomp | grep 'layer.*nd'); do \
			NAME=$(DATA) ;\
			DEG=$$(($$(./bindump.sh $(DATA)/edgedecomp/$$x -N4 -j $$[4*$(ARGS)] 2> /dev/null))) ;\
			if (( $$DEG != 0 )) ; then \
				printf "%s,%s " "$${x:$${#NAME}+6:-3}" "$$DEG" ;\
			fi ;\
	done

ccdist:
	@./bindump.sh $(DATA)/$(DATA).ccsizes -w4 -v | nl -v 0 | grep -v ' 0$$' | grep -v ' 1$$'

ifeq ($(CORE), 0)
degdist:
	@./bindump.sh $(DATA)/$(DATA).dd -w4 -v | nl -v 0 | grep -v ' 0$$'
else
degdist:
	@./bindump.sh $(DATA)/edgedecomp/$(DATA).layer$(CORE).dd -w4 -v | nl -v 0 | grep -v ' 0$$'
endif

.PHONY: degdist

ifeq ($(CORE), ALL)
kcoreCC: all
	@java -cp "bin:lib/*" KCoreCC $(DATA)/$(DATA) $(TYPE) $(DATA)/edgedecomp/$(DATA) $$( \
		for x in $$(ls -v $(DATA)/edgedecomp | grep 'layer.*graph' | tac) ; do \
			NAME=$(DATA) ;\
			echo -n "$${x:$${#NAME}+6:-6} " ;\
		done \
	)
else
ifeq ($(CORE), 0)
kcoreCC: all
	java -cp "bin:lib/*" KCoreCC $(DATA)/$(DATA) $(TYPE)
else
kcoreCC: all
	java -cp "bin:lib/*" KCoreCC $(DATA)/$(DATA) $(TYPE) $(DATA)/edgedecomp/$(DATA) $(CORE)
endif
endif

.PHONY: kcoreCC

ifeq ($(CORE), ALL)
kcoreStats: all
	@echo -e "|V|\t|E|\tdmax\tdavg\tkmax\tkavg"
	@java -cp "bin:lib/*" KCoreStats $(DATA)/$(DATA) | grep -v $(DATA) | grep -v kmax
	@for x in $$(ls -v $(DATA)/edgedecomp | grep 'layer.*graph') ; do \
		java -cp "bin:lib/*" KCoreStats $(DATA)/edgedecomp/$${x%??????} | grep -v $(DATA) | grep -v kmax ;\
	done
else
ifeq ($(CORE), 0)
kcoreStats: all
	@java -cp "bin:lib/*" KCoreStats $(DATA)/$(DATA)
else
kcoreStats: all
	@java -cp "bin:lib/*" KCoreStats $(DATA)/edgedecomp/$(DATA).layer$(CORE)
endif
endif

.PHONY: kcoreStats

kdecompCC: all $(DATA)/$(DATA).cc
	java -Xms$(RAM) -Xmx$(RAM) -cp "bin:lib/*" KCoreDecompCC $(DATA)/$(DATA) $(DATA)/edgedecomp/$(DATA) $(TYPE) $(ARGS)

.PHONY: kdecompCC

kdecompBZ: all $(DATA)/$(DATA).offsets
	java -Xms$(RAM) -Xmx$(RAM) -cp "bin:lib/*" KCoreDecompBZ $(DATA)/$(DATA) $(DATA)/edgedecomp/$(DATA) $(TYPE)

.PHONY: kdecompBZ

kdecompM: all $(DATA)/$(DATA).offsets
	java -Xms$(RAM) -Xmx$(RAM) -cp "bin:lib/*" KCoreDecompM $(DATA)/$(DATA) $(DATA)/edgedecomp/$(DATA) $(TYPE)

.PHONY: kdecompM

kcoreBZ: all $(DATA)/$(DATA).offsets
	@java -cp "bin:lib/*" KCoreWG_BZ $(DATA)/$(DATA)

.PHONY: kcoreBZ

kcoreM: all $(DATA)/$(DATA).offsets
	@java -cp "bin:lib/*" KCoreWG_M $(DATA)/$(DATA)

.PHONY: kcoreM

union: $(DATA)/$(DATA).offsets $(DATA)/$(DATA)-t.offsets
	mv $(DATA)/$(DATA).graph $(DATA)/$(DATA)-tt.graph
	mv $(DATA)/$(DATA).offsets $(DATA)/$(DATA)-tt.offsets
	mv $(DATA)/$(DATA).properties $(DATA)/$(DATA)-tt.properties
	java -Xms$(RAM) -Xmx$(RAM) -cp "lib/*" it.unimi.dsi.webgraph.Transform union $(DATA)/$(DATA)-tt $(DATA)/$(DATA)-t $(DATA)/$(DATA)

.PHONY: union

eunion: $(DATA)/$(DATA).txt
	mv $(DATA)/$(DATA).txt $(DATA)/$(DATA)-dir.txt
	cat $(DATA)/$(DATA)-dir.txt | awk '{print $$0"\n"$$2"\t"$$1}' | sort -nk 1 | uniq > $(DATA)/$(DATA).txt

.PHONY: eunion

sanitize:
	mv $(DATA)/$(DATA).txt $(DATA)/$(DATA)_orig.txt
	cat $(DATA)/$(DATA)_orig.txt | grep -v '#' | sort -nk 1 | uniq | tr -d '\r' | awk '$$1 != $$2' > $(DATA)/$(DATA).txt

.PHONY: sanitize

ifeq (,$(wildcard $(DATA)/$(DATA).properties))
$(DATA)/$(DATA).graph: $(DATA)/$(DATA).txt
	java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -1 -g ArcListASCIIGraph dummy $(DATA)/$(DATA) < $(DATA)/$(DATA).txt
endif

$(DATA)/$(DATA).offsets: $(DATA)/$(DATA).graph
	java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L $(DATA)/$(DATA)

$(DATA)/$(DATA)-t.offsets: $(DATA)/$(DATA)-t.graph
	java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L $(DATA)/$(DATA)-t

$(DATA)/$(DATA).txt:
	7z x $(DATA)/$(DATA).txt.gz -o$(DATA)
	mv $(DATA)/$$(7z l $(DATA)/$(DATA).txt.gz | tail -n 3 | head -n 1 | tr -s ' ' | cut -d ' ' -f6) $(DATA)/$(DATA)_orig.txt
	cat $(DATA)/$(DATA)_orig.txt | grep -v '#' | sort -nk 1 | uniq | tr -d '\r' | awk '$$1 != $$2' > $(DATA)/$(DATA).txt

$(DATA)/$(DATA).cc: $(DATA)/$(DATA).offsets
	java -cp "bin:lib/*" KCoreCC $(DATA)/$(DATA) bin

clean-bin:
	rm -rf bin/*

.PHONY: clean-bin

clean-graphs:
	rm -rf $(DATA)/*.obl $(DATA)/edgedecomp/*

.PHONY: clean-graphs

clean-meta:
	rm -rf $(DATA)/*.cc* $(DATA)/*.cores

.PHONY: clean-meta
