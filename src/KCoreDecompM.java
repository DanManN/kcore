import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;

import it.unimi.dsi.webgraph.ArcListASCIIGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;

/**
 * K-core decomposition algorithm
 *
 * Outputs: array "int[] res" containing the core values for each vertex.
 * The cores are stored in the <basename>.cores file.
 * This is a text file where each line is of the form <vertex-id>:<core number>
 *
 * This is an implementation (with optimizations) of the algorithm given in:
 * A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition.
 * Parallel and Distributed Systems, IEEE Trans., 24(2), 2013.
 *
 * The graph is stored using Webgraph
 * (see P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04.)
 *
 * @author Alex Thomo, thomo@uvic.ca, 2015
 */

public class KCoreDecompM {
	ImmutableGraph G;
	int n;
	int E=0;
	int md; //max degree

	int[] core;
	boolean[] scheduled;
	boolean printprogress = false;
	int iteration = 0;
	boolean change = false;

	HashMap<String, Integer> blackedges;
	int[] decdeg;

	public KCoreDecompM(String basename) throws Exception {
		G = ImmutableGraph.loadMapped(basename);

		n = G.numNodes();
		core = new int[n];

		blackedges = new HashMap<String, Integer>(n);
		decdeg = new int[n];

		md = 0;
		scheduled = new boolean[n];
		for(int v=0; v<n; v++) {

			int degree = G.outdegree(v);
			E += degree;
			if(degree > md)
				md = degree;

			scheduled[v] = true;
		}
	}

	void update(int v) {
		if(iteration == 0) {
			core[v] = G.outdegree(v);// - decdeg[v];			//**************ADDED
			scheduled[v] = true;
			change = true;
		}
		else {
			int d_v = G.outdegree(v);
			int[] N_v = G.successorArray(v);
			int localEstimate = computeUpperBound(v,d_v,N_v);
			if(localEstimate < core[v]) {
				// System.out.println("UB: " + localEstimate);
				core[v] = localEstimate;
				change = true;

				for(int i=0; i<d_v; i++) {
					int u = N_v[i];

					if (blackedges.containsKey(v+","+u)) {	//***************ADDED
						continue;							//***************ADDED
					}										//***************ADDED

					if(core[v]<=core[u])
						scheduled[u] = true;
				}
			}
		}
	}

	int computeUpperBound(int v, int d_v, int[] N_v) {
		int[] c = new int[core[v]+1];
		for(int i=0; i<d_v; i++) {
			int u = N_v[i];

			if (blackedges.containsKey(v+","+u)) {			//***************ADDED
				continue;									//***************ADDED
			}												//***************ADDED

			int j = Math.min(core[v], core[u]);
			c[j]++;
		}

		int cumul = 0;
		for(int i=core[v]; i>=1; i--) {
			// System.out.println("cumul: " + i + ", " + cumul);
			cumul = cumul + c[i];
			if (cumul >= i)
				return i;
		}

		return d_v - decdeg[v];
	}


	public int[] KCoreCompute () {
		while(true) {
			// System.out.print("Iteration " + iteration);

			// int num_scheduled=0;
			boolean[] scheduledNow = scheduled.clone();
			for(int v=0; v<n; v++)
				scheduled[v] = false;

			for(int v=0; v<n; v++) {
				if(scheduledNow[v] == true) {
					// num_scheduled++;
					update(v);
				}
			}
			// System.out.println( "\t\t" + ((100.0*num_scheduled)/n) + "%\t of nodes were scheduled this iteration.");
			iteration++;
			if(change == false)
				break;
			else
				change = false;
		}

		return core;
	}

	final int BUF_SIZE = 1024*1024*512;

	public long[] kcoredecomp(Boolean debug, String type) throws Exception {
		int kmax;
		int previter;
		long temptime;
		long iotime;
		long decomptime;
		long[] io_d_time = {0,0};

		do {
			previter = iteration;
			temptime = System.currentTimeMillis();

			PipedInputStream cedges = new PipedInputStream(BUF_SIZE);
			BufferedWriter wedges = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(cedges)),BUF_SIZE);

			iotime = System.currentTimeMillis() - temptime;

			temptime = System.currentTimeMillis();

			int[] res = KCoreCompute().clone();
			int[] freq = new int[md+1];
			kmax = -1;
			for (int v=0; v<res.length; v++) {
				if(res[v] > kmax)
					kmax = res[v];
				freq[res[v]]++;
			}
			// IntSet ccore = new IntArraySet(n/kmax);
			int numedges = 0;
			while (numedges == 0) {
				for (int v=0; v<res.length; v++) {
					int[] ngbs = G.successorArray(v);
					int v_d = G.outdegree(v);
					for (int i=0; i<v_d; i++) {
						int u = ngbs[i];
						if (blackedges.containsKey(v+","+u)) {
							continue;
						}
						if (res[v] == kmax && res[v] == res[u]) {
							// ccore.add(v);
							blackedges.put(v+","+u,kmax);
							numedges++;
							wedges.write(v+"\t"+u+"\n");
							// core[v] -= 1;
							decdeg[v] += 1;
							// System.out.println(v+"\t"+u+"\t"+kmax);
							scheduled[v] = true;
						}
					}
					for (int i=0; i<v_d; i++) {
						int u = ngbs[i];
						if (blackedges.containsKey(v+","+u)) {
							continue;
						}
						if (scheduled[v])
							scheduled[u] = true;
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

			// iteration = 0;
			// System.out.println(iteration + ", " + previter);
			ArcListASCIIGraph kcore = ArcListASCIIGraph.loadOnce(cedges);
			wedges.close();
			if (type.equals("edgelist"))
				ImmutableGraph.store(ArcListASCIIGraph.class, kcore, G.basename()+"-"+kmax+"core.txt");
			else
				ImmutableGraph.store(BVGraph.class, kcore, G.basename()+"-"+kmax+"core");
			// ImmutableSubgraph kcore = new ImmutableSubgraph(G,core);
			// kcore.save(G.basename()+"-"+kmax+"core");
			cedges.close();

			iotime += System.currentTimeMillis() - temptime;
			io_d_time[1] += iotime;

			System.out.println("Computed " + kmax + " core in " + (iteration-previter) + " iterations (" + decomptime/1000.0 + " sec) and stored in " + iotime/1000.0 + " sec.");
		} while (kmax > 1);
		return io_d_time;
	}

	public static void main(String[] args) throws Exception {

		long startTime = System.currentTimeMillis();

		//args = new String[] {"simplegraph"};

		if(args.length > 2 || args.length < 1) {
			System.err.println("Usage: java KCoreDecompM basename [type]");
			System.exit(1);
		}

		String basename = args[0];
		String gtype = "bvgraph";
		if (args.length == 2)
			gtype = "edgelist";

		System.out.println("Starting " + basename);
		KCoreDecompM kc = new KCoreDecompM(basename);

		long[] times = kc.kcoredecomp(false,gtype);

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
		System.out.println(args[0] + ": Time elapsed (sec) = " + (System.currentTimeMillis() - startTime)/1000.0);
	}
}
