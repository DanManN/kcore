Forked from https://github.com/athomo/kcore

Added are wrappers for the KCoreWG\_BZ and KCoreWG\_M to produce the decomposition of graphs into fixed points of graph peeling as described in:

*Abello, James, and François Queyroi. "Fixed points of graph peeling." Proceedings of the 2013 IEEE/ACM International Conference on Advances in Social Networks Analysis and Mining. ACM, 2013.*


I also include a Makefile to automatically compile the java code and a sample dataset called simplegraph.
The Makefile assumes all graph files for a dataset are in a subdirectory of the same name as the basename of the dataset.

Makefile Parameters:

- DATA = (the data set basename, default: simplegraph)
- TYPE = (the output format for the core decompositions, either edgelist or bvgraph, default: bvgraph)
- CORE = (the core number for the kcoreStats command or ALL, if 0 the stats of entire graph are printed, default: 0)
- RAM  = (amount of ram to allocate to the jvm, default: 4G)

Makefile Commands:

- kcoreStats: run the kcoreBZ algorithm on core layer CORE of data set DATA and get its basic stats
- kdecompBZ: run the core decomposition on DATA using the kcoreBZ implementation
- kdecompM: run the core decomposition on DATA using the kcoreM implementation
- kcoreBZ: run the kcoreBZ algorithm on DATA
- kcoreM: run the kcoreM algorithm on DATA
- union: peform a graph union of DATA and DATA-t (the transpose)
- eunion: perform a graph union of DATA using just an edgelist text file (much slower than union)
- sanitize: sort and filter an edgelist file for DATA so that it can be converted to webgraph format
- clean-bin: delete class files
- clean-graphs: delete generated core layer graphs
