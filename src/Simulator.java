import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Simulator {

	/* Generate tag's random number
	 * inside a given range (offset~limit)
	 * Project specs require using double
	 * refer to ~pasg/if740/Projeto-RFID.pdf
	 * for more information
	 */
	
	static int numTags, frameInit, pace, limit, evalLimit;
	static boolean icml, lbsc, all, frameLimited;

	int[] slots;
	int comm;
	
	public int[] genRandom(double n, int frameSize){
		
		slots = new int[frameSize];

		// status[] represents [e|s|c]
		// e = number of empty slots
		// s = number of success slots
		// c = number of collision slots
		int[] status = new int[3];

		// Each tag picks a random number [0,framesize]
		for (int i = 0; i < n; i++) {
			slots[(int)(Math.random()*frameSize)]++;
		}

		// Now we check each slot for its value.
		for (int slot : slots) {
			if(slot==0) status[0]++;
			if(slot==1) status[1]++;
			if(slot>1)  status[2]++;
		}
		return status;
	}

	@SuppressWarnings("unused")
	public List<int[]> simulate(String protocol, double numTags, double incr, 
		int maxTags, int eval, int fmSize, boolean pot2) {
		System.out.println("===== "+protocol+" =====");
		System.out.println("TAGS\tAV[e|s|c]\tSLOTS\tCMD\tTIME");

		// PLOTING
		List<int[]> esc = new ArrayList<int[]>();
		int[] e_ = new int[(int)Math.ceil(maxTags/incr)];
		int[] c_ = new int[(int)Math.ceil(maxTags/incr)];
		int[] nTags = new int[(int)Math.ceil(maxTags/incr)];
		int[] cComm = new int[(int)Math.ceil(maxTags/incr)];
		int[] timeM = new int[(int)Math.ceil(maxTags/incr)];
		int[] nSlots = new int[(int)Math.ceil(maxTags/incr)];
		int index = 0;

		// We need to test for 100, 200, 300, ..., 1000 tags
		for (double env = 1; env <= maxTags/numTags; env+=incr/numTags) {
			
			int totalOffset = 0;
			int totalEmpty = 0;
			int totalSuccess = 0;
			int totalCollision = 0;
			long start = 0, totalTime = 0;
			int totalError = 0;
			int countCommands = 0;
			comm = 0;

			for (int i = 0; i < eval; i++) {
				start = System.currentTimeMillis();
				
				double n = numTags*env; // Number of tags
				int L = fmSize; 		// Current frame size

				// Tell tags they can choose their own random number
				int[] status = genRandom(n,L);
				int success = status[0];
				int empty = status[1];
				int collision = status[2];

				// Each new frame opened
				countCommands++;

				/* Control the frame offset.
				 * At first offset is 0, on the
				 * next frame, then it is a sum
				 * of the size of each frame before.
				 */				
				int offset = 0;
				int c = 0, s = 0, e = 0;
				
				/* Every while iteration is equivalent to
				 * a frame being generated. Frames will
				 * keep being generated until the number
				 * of tags in collision equals zero.
				 */
				while (collision != 0) {
					/* Success slot means we are not
					 * gonna handle it anymore then
					 * please kick them out of n.
					 */
					n -= success;

					// ACK for each success slot
					countCommands += success;

					/* offset is now holding the total frame size
					 * just to be used in the final average calculation.
					 */
					offset += L;
					e += empty;
					s += success;
					c += collision;

					// Required by ILCM. 2^qc = L
					int qc = (int) Math.ceil(log2(L));

					/* The estimators are going to estimate
					 * how many tags (n_) would have been involved
					 * in collisions. It returns the probability
					 * of number of tags which should taken as the
					 * next frame size. Quite obvious, isn't it?
					 */
					int f_ = (int) Math.ceil(estimateFunction(protocol,qc,collision,success,slots,L));

					// n_ = f_ + success
					if(pot2) L = get2Q(f_ + success);
					else L = f_;
					
					status = genRandom(n,L);
					empty = status[0];
					success = status[1];
					collision = status[2];

					// Each call to estimator
					countCommands++;
				}
				totalTime += System.currentTimeMillis() - start;
				totalOffset += offset;
				totalEmpty += e;
				totalSuccess += s;
				totalCollision += c;
				totalError = (int) Math.abs(n-offset);
			}

			nTags[index] = (int)(numTags*env);
			e_[index] = totalEmpty/eval;
			c_[index] = totalCollision/eval;
			cComm[index] = (countCommands+comm)/eval;
			timeM[index] = (int)totalTime;
			nSlots[index] = totalOffset/eval;
			index++;

			System.out.print((int)(numTags*env)+
					"\t["+totalEmpty/eval+"|"+totalSuccess/eval+"|"+totalCollision/eval+"]\t"
					+totalOffset/eval+"\t"+(countCommands+comm)/eval+"\t");
			System.out.printf("%.3f\n",totalTime/((double)eval));
			
		}
		esc.add(e_); esc.add(c_); esc.add(nTags); esc.add(cComm); esc.add(timeM); esc.add(nSlots);
		return esc;
	}

	public int get2Q(int n_) {
		if(n_ >= 1 && n_ <= 5) return 4;
		if(n_ >= 6 && n_ <= 11) return 8;
		if(n_ >= 12 && n_ <= 22) return 16;
		if(n_ >= 23 && n_ <= 44) return 32;
		if(n_ >= 45 && n_ <= 89) return 64;
		if(n_ >= 90 && n_ <= 177) return 128;
		if(n_ >= 178 && n_ <= 355) return 256;
		if(n_ >= 356 && n_ <= 710) return 512;
		if(n_ >= 711 && n_ <= 1420) return 1024;
		if(n_ >= 1421 && n_ <= 2840) return 2048;
		if(n_ >= 2841 && n_ <= 5680) return 4096;
		if(n_ >= 5681 && n_ <= 11360) return 8192;
		return 0;
	}

	public double log2(double x) {
		return (Math.log(x)/Math.log(2));
	}

	public double _N(double k, int s, double l) {
		return k * s + l;
	}

	public double _R(double N, int qc, int i) {
		return (N * Math.pow(2, qc))/i;
	}

	public double estimateFunction(String function, int qc, int C, int S, int[] frame, int L) {
		switch (function) {
		case "Lower-Bound":
			return C*2;

		case "Schoute":
			return C*2.39;

		case "ILCM-sbs":
			int i = 0;
			double qn = -1;
			double Rant = 0;
			double N = 0;
			double R = 0;
			int c, s, status;
			double l, k, L1, L2, ps1, ps2;
			double pot = Math.pow(2, qc);
			
			while(qn == -1 && i < (int)pot) {	
				i++;		
				c = 0;
				s = 0;
				status = frame[i-1];
				comm++;
				if (status > 1){
					c = 1;
				} else if (status == 1) {
					s = 1;
				}
				k = c/((4.344 * i - 16.28) + (i/(-2.282 - 0.273 * i)) * c + 0.2407 * Math.log(i + 42.56));
				l = (1.2592 + 1.513 * i) * Math.tan((Math.pow(1.234 *i, -0.9907)) * c);
				if (k < 0) k = 0;
				N = (k*s) + l;
				R = (N*pot)/i;
				
				/*if (c==0){
					R = (pot)/i;
				}*/	
				
				if (i > 1) {
					L1 = pot;
					L2 = Math.pow(2, Math.round(log2(R)));
					ps1 = (R/L1) * Math.pow((1 - (1/L1)), R-1);
					ps2 = (R/L2) * Math.pow((1 - (1/L2)), R-1);

					if((R - Rant) <= 1 && ((L1 * ps1-s) < (L2 * ps2))){
						qn = (int) Math.ceil(log2(R));
					}
				}
			}
			Rant = R;
			if (qn != -1) return Math.pow(2, qn);
			else {
				qn = qc;
				return Math.pow(2, qn);
			}

		case "Eom-Lee":
			double Bk, gamaK = 2, gamaK_prev;			
			do {
				gamaK_prev = gamaK;
				Bk = L/(gamaK_prev * C + S);
				gamaK = (1 - Math.pow(Math.E,-1/Bk)) / (Bk * (1 - (1 + 1/Bk) * Math.pow(Math.E,-1/Bk)));
			} while (Math.abs(gamaK_prev - gamaK) >= 0.001);
			return gamaK * C; 

		default:
			return 15;
		}
	}

	public static void main(String[] args) {
		
		Simulator simulator = new Simulator();
		Grafico grafico = new Grafico();
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Insira o numero total de tags da simulacao:");
		numTags = scanner.nextInt();

		System.out.println("Insira o tamanho do frame inicial:");
		frameInit = scanner.nextInt();
		
		System.out.println("Insira a quantidade tags por acressimo:");
		pace = scanner.nextInt();
		
		System.out.println("Insira o limite de acressimos:");
		limit = scanner.nextInt() + 1;

		System.out.println("Insira a quantidade de avaliacoes:");
		evalLimit = scanner.nextInt();

		System.out.println("Limitar o tamanho do frame a 2^Q? (true/false)");
		frameLimited = scanner.nextBoolean();

		System.out.println("Escolha a simulacao"
			+ "\n 1) Lower Bound, Schoute & Eom-Lee"
			+ "\n 2) ICML-SbS"
			+ "\n 3) Todos os estimadores"
			+ "\n OBS: A simulacao 3 e bastante demorada por que"
			+ " \nusa os mesmo parametros para todos os estimadores.");

		int choice = scanner.nextInt();
		
		List<int[]> lowerbound = new ArrayList<int[]>();
		List<int[]> schoute = new ArrayList<int[]>();
		List<int[]> ilcmsbs = new ArrayList<int[]>();
		List<int[]> eomlee = new ArrayList<int[]>();
		
		switch (choice) {
		case (1):
			schoute = simulator.simulate("Schoute", numTags, pace, limit, evalLimit, frameInit, frameLimited);
			lowerbound = simulator.simulate("Lower-Bound", numTags, pace, limit, evalLimit, frameInit, frameLimited);
			eomlee = simulator.simulate("Eom-Lee", numTags, pace, limit, evalLimit, frameInit, frameLimited);	
			String[] curves = {"LoweBound", "Schoute", "Eom-Lee"};

			int empty[][]= {lowerbound.get(0),schoute.get(0),eomlee.get(0)};
			int collision[][]= {lowerbound.get(1),schoute.get(1),eomlee.get(1)};
			int command[][]= {lowerbound.get(3),schoute.get(3),eomlee.get(3)};
			int time[][]= {lowerbound.get(4),schoute.get(4),eomlee.get(4)};
			int slot[][]= {lowerbound.get(5),schoute.get(5),eomlee.get(5)}; 

			grafico.gerarN("Slots", curves, "Tags", "Slots", slot, schoute.get(2));
			grafico.gerarN("SlotsVazios", curves, "Tags", "Empty", empty, schoute.get(2));
			grafico.gerarN("Colisoes", curves, "Tags", "Collision", collision, schoute.get(2));
			grafico.gerarN("Comandos", curves, "Tags", "Commands", command, schoute.get(2));
			grafico.gerarN("Tempo", curves, "Tags", "Time 10^-3(ms)", time, schoute.get(2));
			break;

		case (2):
			ilcmsbs = simulator.simulate("ILCM-sbs", numTags, pace, limit, evalLimit, frameInit, frameLimited);
			String[] curves2 = {"ILCM-SbS"};

			int empty2[][]= {ilcmsbs.get(0)};
			int collision2[][]= {ilcmsbs.get(1)};
			int command2[][]= {ilcmsbs.get(3)};
			int time2[][]= {ilcmsbs.get(4)};
			int slot2[][]= {ilcmsbs.get(5)};

			grafico.gerarN("Slots", curves2, "Tags", "Slots", slot2, ilcmsbs.get(2));
			grafico.gerarN("SlotsVazios", curves2, "Tags", "Empty", empty2, ilcmsbs.get(2));
			grafico.gerarN("Colisoes", curves2, "Tags", "Collision", collision2, ilcmsbs.get(2));
			grafico.gerarN("Comandos", curves2, "Tags", "Commands", command2, ilcmsbs.get(2));
			grafico.gerarN("Tempo", curves2, "Tags", "Time 10^-4(ms)", time2, ilcmsbs.get(2));
			break;

		case (3):
			ilcmsbs = simulator.simulate("ILCM-sbs", numTags, pace, limit, evalLimit, frameInit, frameLimited);
			schoute = simulator.simulate("Schoute", numTags, pace, limit, evalLimit, frameInit, frameLimited);
			lowerbound = simulator.simulate("Lower-Bound", numTags, pace, limit, evalLimit, frameInit, frameLimited);
			String[] curves3 = {"LoweBound", "Schoute", "Eom-Lee", "ILCM-SbS"};

			int empty3[][]= {lowerbound.get(0),schoute.get(0),eomlee.get(0),ilcmsbs.get(0)};
			int collision3[][]= {lowerbound.get(1),schoute.get(1),eomlee.get(1),ilcmsbs.get(1)};
			int command3[][]= {lowerbound.get(3),schoute.get(3),eomlee.get(3),ilcmsbs.get(3)};
			int time3[][]= {lowerbound.get(4),schoute.get(4),eomlee.get(4),ilcmsbs.get(4)};
			int slot3[][]= {lowerbound.get(5),schoute.get(5),eomlee.get(5),ilcmsbs.get(5)}; 

			grafico.gerarN("Slots", curves3, "Tags", "Slots", slot3, schoute.get(2));
			grafico.gerarN("SlotsVazios", curves3, "Tags", "Empty", empty3, schoute.get(2));
			grafico.gerarN("Colisoes", curves3, "Tags", "Collision", collision3, schoute.get(2));
			grafico.gerarN("Comandos", curves3, "Tags", "Commands", command3, schoute.get(2));
			grafico.gerarN("Tempo", curves3, "Tags", "Time 10^-3(ms)", time3, schoute.get(2));		
			break;

		default:
			break;
		};
	}
}
