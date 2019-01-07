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

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.algo.ConnectedComponents;

public class KCoreCC {

	// private static final Logger LOGGER = LoggerFactory.getLogger("it/unimi/dsi/webgraph/algo/ConnectedComponents");

	public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			System.err.println("Usage: java KCoreCC basename [cores]");
			System.exit(1);
		}

		String basename = args[0];
		ImmutableGraph G = ImmutableGraph.load(basename);
		ConnectedComponents cc = ConnectedComponents.compute(G,0,null);
		System.out.print("G0: ");
		for (int i = 0; i < cc.component.length; i++) {
			System.out.print(cc.component[i]+" ");
		}
		System.out.println();
		if (args.length > 1) {
			ConnectedComponents ccC;
			ImmutableGraph C;
			int[] sizes;
			for (int i = 1; i < args.length; i++) {
				C = ImmutableGraph.load(basename+"-"+args[i]+"core");
				ccC = ConnectedComponents.compute(C,0,null);
				sizes = ccC.computeSizes();
				System.out.print("C"+i+": ");
				for (int j = 0; j < ccC.component.length; j++) {
					if (sizes[ccC.component[j]] != 1)
						System.out.print(ccC.component[j]+","+cc.component[j]+" ");
				}
				System.out.println();
				// for (int j = 0;j < sizes.length; j++) {
				// 	System.out.print(sizes[j]+" ");
				// }
				// System.out.println();
			}
		}
	}
}
