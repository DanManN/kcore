/**
 * K-core decomposition algorithm.
 * This is an implementation of the algorithm given in:
 * V. Batagelj and M. Zaversnik. An o(m) algorithm for cores decomposition of networks. CoRR, 2003.
 *
 * Outputs: array "int[] res" containing the core values for each vertex.
 * The cores are stored in the <basename>.cores file.
 * This is a text file where each line is of the form <vertex-id>:<core number>
 *
 * The graph is stored using Webgraph
 * (see P. Boldi and S. Vigna. The Webgraph framework I: compression techniques. WWW'04.)
 *
 * @author Alex Thomo, thomo@uvic.ca, 2015
 */

// import java.io.File;
// import java.io.PrintStream;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.io.File;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.algo.ConnectedComponents;

public class KCoreCC {

	// private static final Logger LOGGER = LoggerFactory.getLogger("it/unimi/dsi/webgraph/algo/ConnectedComponents");

	public static void main(String[] args) throws Exception {
		if(args.length > 2 || args.length < 1) {
			System.err.println("Usage: java KCoreCC basename [savefile_name]");
			System.exit(1);
		}

		String basename = args[0];
		String filename = null;
		if (args.length == 2)
			filename = args[1];

		// System.out.println("Starting ConnectedComponents: " + basename);
		ImmutableGraph G = ImmutableGraph.load(basename);
		// ProgressLogger pl = new ProgressLogger(LOGGER, 10000, TimeUnit.MILLISECONDS);
		ConnectedComponents cc = ConnectedComponents.compute(G,0,null);
		// cc.sortBySize(cc.component);

		PrintStream ps = System.out;
		if (filename != null)
			ps = new PrintStream(new File(filename));

		int i;
		for (i = 0; i < cc.component.length-1; i++)
			ps.print(cc.component[i]+",");
		ps.println(cc.component[i]);
		// System.out.println();
	}
}
