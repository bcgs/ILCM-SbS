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
		int[] e_ = new int[(int)(maxTags/numTags)];
		int[] c_ = new int[(int)(maxTags/numTags)];
		int[] nTags = new int[(int)(maxTags/numTags)];
		int[] cComm = new int[(int)(maxTags/numTags)];
		int[] timeM = new int[(int)(maxTags/numTags)];
		int[] nSlots = new int[(int)(maxTags/numTags)];
		int index = 0;
		comm = 0;

		// We need to test for 100, 200, 300, ..., 1000 tags
		for (double env = 1; env <= maxTags/numTags; env+=incr/numTags) {
			
			int totalOffset = 0;
			int totalEmpty = 0;
			int totalSuccess = 0;
			int totalCollision = 0;
			long totalTime = 0;
			int totalError = 0;
			int countCommands = 0;
			int prevOffset=0;

			for (int i = 0; i < eval; i++) {
				long start = System.currentTimeMillis();
				
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

					// ACK for each sucess slot
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
					int f_ = (int) Math.ceil(estimateFunction(protocol,qc,collision, slots));

					if(pot2) L = (int) Math.pow(2,(Math.ceil(log2(f_))));
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
			cComm[index] = countCommands/eval;
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

	public double log2(double x) {
		return (Math.log(x)/Math.log(2));
	}

	public double _N(double k, int s, double l) {
		return k * s + l;
	}

	public double _R(double N, int qc, int i) {
		return (N * Math.pow(2, qc))/i;
	}

	public double estimateFunction(String function, int qc, int C, int[] frame) {
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
		limit = scanner.nextInt();

		System.out.println("Insira a quantidade de avaliacoes:");
		evalLimit = scanner.nextInt();

		System.out.println("Limitar o tamanho do frame a 2^Q? (true/false)");
		frameLimited = scanner.nextBoolean();

		System.out.println("Escolha a simulacao"
			+ "\n 1) Lower Bound & Schoute"
			+ "\n 2) ICML-SbS"
			+ "\n 3) Todos os estimadores"
			+ "\n OBS: A simulacao 3 e bastante demorada por que"
			+ " \nusa os mesmo parametros para todos os estimadores.");

		int choice = scanner.nextInt();
		
		List<int[]> lowerbound = new ArrayList<int[]>();
		List<int[]> schoute = new ArrayList<int[]>();
		List<int[]> ilcmsbs = new ArrayList<int[]>();
		
		switch (choice) {
		case (1):
		schoute = simulator.simulate("Schoute", numTags, pace, limit, evalLimit, frameInit, frameLimited);
		lowerbound = simulator.simulate("Lower-Bound", numTags, pace, limit, evalLimit, frameInit, frameLimited);	
		int emptylb[]= lowerbound.get(0);
		int emptysc[]= schoute.get(0);
		int collisionlb[]= lowerbound.get(1);
		int collisionsc[]= schoute.get(1);
		int commandlb[]= lowerbound.get(3);
		int commandsc[]= schoute.get(3);
		int timelb[]= lowerbound.get(4);
		int timesc[]= schoute.get(4);
		int slotlb[]= lowerbound.get(5);
		int slotsc[]= schoute.get(5);
		grafico.gerarDupla("DSlots", "Tags", "Slots",
				slotlb, slotsc,
				"LoweBound", "Schoute");	
		grafico.gerarDupla("DSlotsVazios", "Tags", "Empty", 
				emptylb, emptysc, 
				"LoweBound", "Schoute");
		grafico.gerarDupla("DColisoes", "Tags", "Collision",
				collisionlb, collisionsc, 
				"LoweBound", "Schoute");
		grafico.gerarDupla("DComandos", "Tags", "Commands", 
				commandlb, commandsc, 
				"LoweBound", "Schoute");
		grafico.gerarDupla("DTempo", "Tags", "Time",
				timelb, timesc,
				"LoweBound", "Schoute");
		break;

		case (2):
		ilcmsbs = simulator.simulate("ILCM-sbs", numTags, pace, limit, evalLimit, frameInit, frameLimited);				
		grafico.gerar("ILCM_e", "Tags", "Empty", ilcmsbs.get(0), ilcmsbs.get(2));
		grafico.gerar("ILCM_c", "Tags", "Collision", ilcmsbs.get(1), ilcmsbs.get(2));
		grafico.gerar("ILCM_comm", "Tags", "Commands", ilcmsbs.get(3), ilcmsbs.get(2));
		grafico.gerar("ILCM_time", "Tags", "Time", ilcmsbs.get(4), ilcmsbs.get(2));
		grafico.gerar("ILCM_slots", "Tags", "Slots", ilcmsbs.get(5), ilcmsbs.get(2));
		break;

		case (3):
		ilcmsbs = simulator.simulate("ILCM-sbs", numTags, pace, limit, evalLimit, frameInit, frameLimited);
		schoute = simulator.simulate("Schoute", numTags, pace, limit, evalLimit, frameInit, frameLimited);
		lowerbound = simulator.simulate("Lower-Bound", numTags, pace, limit, evalLimit, frameInit, frameLimited);
		
		int emptylb2[]= lowerbound.get(0);
		int emptysc2[]= schoute.get(0);
		int emptyil2[]= ilcmsbs.get(0);
		int collisionlb2[]= lowerbound.get(1);
		int collisionsc2[]= schoute.get(1);
		int collisionil2[]= ilcmsbs.get(1);
		int commandlb2[]= lowerbound.get(3);
		int commandsc2[]= schoute.get(3);
		int commandil2[]= ilcmsbs.get(3);
		int timelb2[]= lowerbound.get(4);
		int timesc2[]= schoute.get(4);
		int timeil2[]= ilcmsbs.get(4);	
		int slotlb2[]= lowerbound.get(5);
		int slotsc2[]= schoute.get(5);
		int slotil2[]= ilcmsbs.get(5);

		grafico.gerarEstimadores("Slots", "Tags", "Slots",
				slotlb2, slotsc2, slotil2, 
				"LoweBound", "Schoute", "ILCM-SbS");
		
		grafico.gerarEstimadores("SlotsVazios", "Tags", "Empty", 
				emptylb2, emptysc2,emptyil2, 
				"LoweBound", "Schoute", "ILCM-SbS");
		
		grafico.gerarEstimadores("Colisoes", "Tags", "Collision",
				collisionlb2, collisionsc2,collisionil2, 
				"LoweBound", "Schoute", "ILCM-SbS");
		
		grafico.gerarEstimadores("Comandos", "Tags", "Commands", 
				commandlb2, commandsc2,commandil2, 
				"LoweBound", "Schoute", "ILCM-SbS");
		
		grafico.gerarEstimadores("Tempo", "Tags", "Time",
				timelb2, timesc2,timeil2, 
				"LoweBound", "Schoute", "ILCM-SbS");		
		break;

		default:
			break;
		};
	}
}
