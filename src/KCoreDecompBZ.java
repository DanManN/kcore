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

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;

import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;

public class KCoreDecompBZ {
	ImmutableGraph G;
	boolean printprogress = false;
	long E=0;
	int n;
	int md; //max degree

	HashMap<String, Integer> blackedges;
	int[] decdeg;

	// int[] vert;
	// int[] pos;
	// int[] deg;
	// int[] bin;

	public KCoreDecompBZ(String basename) throws Exception {
		G = ImmutableGraph.load(basename);

		n = G.numNodes();

		blackedges = new HashMap<String, Integer>(n);
		decdeg = new int[n];

		md = 0;
		for(int v=0; v<n; v++) {
			int v_deg = G.outdegree(v);
			E += v_deg;
			if(md < v_deg)
				md = v_deg;
		}

		// vert = new int[n];
		// pos = new int[n];
		// deg = new int[n];
		// bin = new int[md+1]; //md+1 because we can have zero degree

		// for(int v=0; v<n; v++) {
		// 	deg[v] = G.outdegree(v);// - decdeg[v];		//************MODIFIED
		// }

	}


	public int[] KCoreCompute () {

		int[] vert = new int[n];
		int[] pos = new int[n];
		int[] deg = new int[n];
		int[] bin = new int[md+1]; //md+1 because we can have zero degree

		for(int d=0; d<=md; d++)
			bin[d] = 0;
		for(int v=0; v<n; v++) {
			deg[v] = G.outdegree(v) - decdeg[v];		//************MODIFIED
			// System.out.println(deg[v]);
			bin[ deg[v] ]++;
		}

		int start = 0; //start=1 in original, but no problem
		for(int d=0; d<=md; d++) {
			int num = bin[d];
			bin[d] = start;
			start += num;
		}

		//bin-sort vertices by degree
		for(int v=0; v<n; v++) {
			pos[v] = bin[ deg[v] ];
			vert[ pos[v] ] = v;
			bin[ deg[v] ]++;
		}
		//recover bin[]
		for(int d=md; d>=1; d--)
			bin[d] = bin[d-1];
		bin[0] = 0; //1 in original

		//main algorithm
		// long pctDoneLastPrinted = 0;
		for(int i=0; i<n; i++) {

			int v = vert[i]; //smallest degree vertex
			int v_deg = G.outdegree(v);
			int[] N_v = G.successorArray(v);
			for(int j=0; j<v_deg; j++) {
				int u = N_v[j];

				if (blackedges.containsKey(v+","+u)) {	//***************ADDED
					continue;							//***************ADDED
				}										//***************ADDED

				if(deg[u] > deg[v]) {
					int du = deg[u]; int pu = pos[u];
					int pw = bin[du]; int w = vert[pw];
					if(u!=w) {
						pos[u] = pw; vert[pu] = w;
						pos[w] = pu; vert[pw] = u;
					}
					bin[du]++;
					deg[u]--;
				}
			}


			// long pctDone = Math.round( (100.0*(i+1))/n );
			// if ( pctDone >= pctDoneLastPrinted + 10 || pctDone == 100) {
			// 	System.out.println("pctDone=" + pctDone + "%");
			// 	pctDoneLastPrinted = pctDone;
			// }
		}

		return deg;
	}
	
	final int BUF_SIZE = 1024*1024*512;

	public long[] kcoredecomp(Boolean debug, String type, String savename) throws Exception {
		int kmax;
		long temptime;
		long iotime;
		long decomptime;
		long[] io_d_time = {0,0};

		do {
			temptime = System.currentTimeMillis();
			
			PipedInputStream cedges = new PipedInputStream(BUF_SIZE);
			BufferedWriter wedges = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(cedges)),BUF_SIZE);

			iotime = System.currentTimeMillis() - temptime;

			temptime = System.currentTimeMillis();

			int[] res = KCoreCompute();
			int[] freq = new int[md+1];
			kmax = -1;
			for (int v=0; v<n; v++) {
				if(res[v] > kmax)
					kmax = res[v];
				freq[res[v]]++;
			}
			// IntSet core = new IntArraySet(n/kmax);
			int numedges = 0;
			while (numedges == 0) {
				for (int v=0; v<n; v++) {
					int[] ngbs = G.successorArray(v);
					int v_d = G.outdegree(v);
					for (int i=0; i<v_d; i++) {
						int u = ngbs[i];
						if (blackedges.containsKey(v+","+u)) {
							continue;
						}
						if (res[v] >= kmax && res[v] <= res[u]) {
							// core.add(v);
							blackedges.put(v+","+u,kmax);
							numedges++;
							wedges.write(v+"\t"+u+"\n");
							// System.out.println(decdeg[v]);
							decdeg[v]++;
							// System.out.println(v+"\t"+u+"\t"+kmax);
						}
					}
				}
				if (numedges == 0) {
					do {
						kmax--;
					} while (freq[kmax] == 0);
				}
			}

			decomptime = System.currentTimeMillis() - temptime;
			io_d_time[0] += decomptime;

			temptime = System.currentTimeMillis();

			ArcListASCIIGraph kcore = ArcListASCIIGraph.loadOnce(cedges);
			wedges.close();
			if (type.equals("ascii"))
				ImmutableGraph.store(ArcListASCIIGraph.class, kcore, savename+".layer"+kmax+".txt");
			else
				ImmutableGraph.store(BVGraph.class, kcore, savename+".layer"+kmax);
			// ImmutableSubgraph kcore = new ImmutableSubgraph(G,core);
			// kcore.save(G.basename()+"-"+kmax+"core");
			cedges.close();

			iotime += System.currentTimeMillis() - temptime;
			io_d_time[1] += iotime;

			System.out.println("Computed " + kmax + " core in " + decomptime/1000.0 + " sec and stored in " + iotime/1000.0 + " sec.");
		} while (kmax > 1);
		return io_d_time;
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

		//args = new String[] {"simplegraph"};

		if (args.length != 3) {
			System.err.println("Usage: java KCoreDecomp basename savename format");
			System.exit(1);
		}

		String basename = args[0];
		String savename = args[1];
		String gtype = args[2];

		System.out.println("Starting " + basename);
		KCoreDecompBZ kc = new KCoreDecompBZ(basename);

		long[] times = kc.kcoredecomp(false,gtype,savename);

		//storing the core value for each node in a file.
		// PrintStream ps = new PrintStream(new File(basename+".cores"));

		// int[] res = kc.KCoreCompute();
		// int kmax = -1;
		// double sum = 0;
		// int cnt = 0;
		// for(int i=0; i<res.length; i++) {
		// 	ps.println(i+":" + res[i] + " ");
		// 	if(res[i] > kmax)
		// 		kmax = res[i];
		// 	sum += res[i];
		// 	if(res[i] > 0) cnt++;
		// }
		// System.out.println("|V|\t|E|\tdmax\tkmax\tkavg");
		// System.out.println(cnt + "\t" + (kc.E/2) + "\t" + kc.md + "\t" + kmax + "\t" + (sum/cnt) );

		System.out.println(args[0] + ": kcore time (sec) = " + times[0]/1000.0);
		System.out.println(args[0] + ": I/O   time (sec) = " + times[1]/1000.0);
		System.out.println(args[0] + ": Total Runtime elapsed (sec) = " + (System.currentTimeMillis() - startTime)/1000.0);
	}
}
