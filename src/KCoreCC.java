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

import java.io.PrintStream;

import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.util.Properties;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.algo.ConnectedComponents;

public class KCoreCC {

	// private static final Logger LOGGER = LoggerFactory.getLogger("it/unimi/dsi/webgraph/algo/ConnectedComponents");

	public static void main(String[] args) throws Exception {
		if(args.length < 2) {
			System.err.println("Usage: java KCoreCC basename format [layerbase layers...]");
			System.exit(1);
		}

		String basename = args[0];
		String format = args[1];


		FileInputStream propFile = new FileInputStream(basename+".properties");
		Properties prop = new Properties();
		prop.load(propFile);
		propFile.close();

		int nodes = Integer.parseInt(prop.getProperty("nodes").toString());
		int[] gcccomponents = new int[nodes+1];

		if (format.equals("ascii")) {
			try(FileInputStream inputStream = new FileInputStream(basename+".cc.txt")) {
				String[] arr = IOUtils.toString(inputStream).split("\n");
				gcccomponents[nodes] = Integer.parseInt(arr[0]);
				for (int i = 0; i < arr.length-1; i++) {
					gcccomponents[i] = Integer.parseInt(arr[i+1]);
				}
			} catch (IOException e) {
				//nothing
			}
		} else {
			try {
				BinIO.loadInts(basename+".cc", gcccomponents);
			} catch (IOException e) {
				//nothing
			}
		}

		if (gcccomponents[nodes] == 0) {
			ImmutableGraph G = ImmutableGraph.load(basename);
			ConnectedComponents cc = ConnectedComponents.compute(G,0,null);
			System.arraycopy(cc.component, 0, gcccomponents, 0, cc.component.length);
			gcccomponents[nodes] = cc.numberOfComponents;
			if (format.equals("ascii")) {
				PrintStream ps = new PrintStream(new File(basename+".cc.txt"));
				ps.println(gcccomponents[nodes]);
				for (int i = 0; i < gcccomponents.length-1; i++) {
					ps.println(gcccomponents[i]);
				}
				ps.close();
			} else {
				BinIO.storeInts(gcccomponents, basename+".cc");
				BinIO.storeInts(cc.computeSizes(), basename+".ccsizes");
			}
		}

		// System.out.println(gcccomponents[nodes]);


		if (args.length > 3) {
			String baselayers = args[2];
			ConnectedComponents ccC;
			ImmutableGraph C;
			int[] sizes;
			if (format.equals("ascii")) {
				try(FileInputStream inputStream = new FileInputStream(basename+".cc-layers.txt")) {
					String[] arr = IOUtils.toString(inputStream).split("\n");
					for (int i = 0; i < arr.length; i++) {
						if (arr[i].contains("cc-layer")) {
							gcccomponents[nodes] += Integer.parseInt(arr[i].split(" ")[1]);
							// System.out.println(gcccomponents[nodes]);
						}
					}
				} catch (IOException e) {
					//nothing
				}
				PrintStream ps = new PrintStream(new FileOutputStream(new File(basename+".cc-layers.txt"), true));
				for (int i = 3; i < args.length; i++) {
					C = ImmutableGraph.load(baselayers+".layer"+args[i]);
					ccC = ConnectedComponents.compute(C,0,null);
					sizes = ccC.computeSizes();
					ps.println("cc-layer"+args[i]+": "+ccC.numberOfComponents);
					for (int j = 0; j < ccC.component.length; j++) {
						if (sizes[ccC.component[j]] != 1) {
							ps.println(j+","+(ccC.component[j]+gcccomponents[nodes])+","+gcccomponents[j]);
						}
					}
					gcccomponents[nodes] += ccC.numberOfComponents;
				}
				ps.close();
			} else {
				try(DataInputStream inputStream = new DataInputStream(new FileInputStream(basename+".cc-layers"))) {
					int count;
					while (inputStream.available()>12) {
						// System.out.println(inputStream.available());
						inputStream.skipBytes(4);
						gcccomponents[nodes] += inputStream.readInt();
						// System.out.println(gcccomponents[nodes]);
						count = inputStream.readInt();
						inputStream.skipBytes(count*12);
					}
				} catch (IOException e) {
					//nothing
				}
				DataOutputStream os = new DataOutputStream(new FileOutputStream(new File(basename+".cc-layers"), true));
				for (int i = 3; i < args.length; i++) {
					C = ImmutableGraph.load(baselayers+".layer"+args[i]);
					ccC = ConnectedComponents.compute(C,0,null);
					sizes = ccC.computeSizes();
					int count = ccC.component.length;
					for (int j = 0; j < sizes.length; j++) {
						if (sizes[j] == 1) {
							count--;
						}
					}
					os.writeInt(-Integer.parseInt(args[i]));
					os.writeInt(ccC.numberOfComponents);
					os.writeInt(count);
					// System.out.print(-Integer.parseInt(args[i])+" ");
					// System.out.print(ccC.numberOfComponents+" ");
					// System.out.print(count+"\n");
					for (int j = 0; j < ccC.component.length; j++) {
						if (sizes[ccC.component[j]] != 1) {
							os.writeInt(j);
							os.writeInt(ccC.component[j]+gcccomponents[nodes]);
							os.writeInt(gcccomponents[j]);
							// System.out.print(j+" ");
							// System.out.print(ccC.component[j]+gcccomponents[nodes]+" ");
							// System.out.print(gcccomponents[j]+"\n");
						}
					}
					gcccomponents[nodes] += ccC.numberOfComponents;
				}
				os.close();
			}
		}
	}
}
