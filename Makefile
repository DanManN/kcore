DATA := simplegraph
TYPE := bvgraph
CORE := 1
RAM  := 4G
SRC  := $(wildcard src/*.java)
OUT  := bin
LIST := $(SRC:src/%.java=$(OUT)/%.class)

all: $(LIST)

$(OUT)/%.class: src/%.java | $(OUT)
	javac -cp "lib/*" -d $| $<

$(OUT):
	@mkdir $@

kcoreStats: all #$(DATA)/$(DATA)-$(CORE)core.nodes
	java -cp "bin:lib/*" KCoreStats $(DATA)/$(DATA)-$(CORE)core

.PHONY: kcoreStats

kdecompBZ: all $(DATA)/$(DATA).offsets
	java -Xms$(RAM) -Xmx$(RAM) -cp "bin:lib/*" KCoreDecompBZ $(DATA)/$(DATA) $(TYPE)

.PHONY: kdecompBZ

kdecompM: all $(DATA)/$(DATA).offsets
	java -Xms$(RAM) -Xmx$(RAM) -cp "bin:lib/*" KCoreDecompM $(DATA)/$(DATA) $(TYPE)

.PHONY: kdecompM

kcoreBZ: all $(DATA)/$(DATA).offsets
	java -cp "bin:lib/*" KCoreWG_BZ $(DATA)/$(DATA)

.PHONY: kcoreBZ

kcoreM: all $(DATA)/$(DATA).offsets
	java -cp "bin:lib/*" KCoreWG_M $(DATA)/$(DATA)

.PHONY: kcoreM

union: all $(DATA)/$(DATA).offsets $(DATA)/$(DATA)-t.offsets

	java -cp "lib/*" it.unimi.dsi.webgraph.Transform union $(DATA)/$(DATA) $(DATA)/$(DATA)-t $(DATA)/$(DATA)-sym

.PHONY: union

eunion: $(DATA)/$(DATA).txt
	mv $(DATA)/$(DATA).txt $(DATA)/$(DATA)-dir.txt
	./sym.sh $(DATA)/$(DATA)-dir.txt | sort -nk 1 | uniq > $(DATA)/$(DATA).txt

.PHONY: eunion

sanitize:
	mv $(DATA)/$(DATA).txt $(DATA)/$(DATA)_orig.txt
	cat $(DATA)/$(DATA)_orig.txt | grep -v '#' | sort -nk 1 | uniq > $(DATA)/$(DATA).txt

.PHONY: sanitize

$(DATA)/$(DATA).graph: $(DATA)/$(DATA).txt
	java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -1 -g ArcListASCIIGraph dummy $(DATA)/$(DATA) < $(DATA)/$(DATA).txt

$(DATA)/$(DATA).offsets: $(DATA)/$(DATA).graph
	java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L $(DATA)/$(DATA)

$(DATA)/$(DATA)-t.offsets: $(DATA)/$(DATA)-t.graph
	java -cp "lib/*" it.unimi.dsi.webgraph.BVGraph -o -O -L $(DATA)/$(DATA)-t

$(DATA)/$(DATA).txt:
	7z x $(DATA)/$(DATA).txt.gz -o$(DATA)
	mv $(DATA)/`7z l $(DATA)/$(DATA).txt.gz | tail -n 3 | head -n 1 | tr -s ' ' | cut -d ' ' -f6` $(DATA)/$(DATA)_orig.txt
	cat $(DATA)/$(DATA)_orig.txt | grep -v '#' | sort -nk 1 | uniq > $(DATA)/$(DATA).txt

# $(DATA)/$(DATA)-$(CORE)core.nodes: $(DATA)/$(DATA).offsets kdecompBZ

clean-bin:
	rm -rf bin/*
clean-graphs:
	rm -rf $(DATA)/*.obl $(DATA)/*.cores $(DATA)/*core.*

.PHONY: clean
