public class Simulator {

	/* Generate tag's random number
	 * inside a given range (offset~limit)
	 * Project specs require using double
	 * refer to ~pasg/if740/Projeto-RFID.pdf
	 * for more information
	 */
	
	int[] slots;
	
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
			if(slot==0) status[0]++;	// No tags = EMPTY SLOT
			if(slot==1) status[1]++;	// Just one tag	= SUCCESS SLOT
			if(slot>1)  status[2]++;	// More than one tag = COLLISION SLOT
		}
		return status;
	}

	public void simulate(String protocol, double numTags, double incr, int maxTags, int eval, int fmSize) {
		System.out.println("===== "+protocol+" =====");
		System.out.println("TAGS\tAV[e|s|c]\tTIME\tSLOTS\tERROR\n");

		// We need to test for 100, 200, 300, ..., 1000 tags
		for (double env = 1; env <= maxTags/numTags; env+=incr/numTags) {
			
			int totalOffset = 0;
			int totalEmpty = 0;
			int totalSuccess = 0;
			int totalCollision = 0;
			long totalTime = 0;
			int totalError = 0;
			
			for (int i = 0; i < eval; i++) {
				long start = System.currentTimeMillis();
				
				double n = numTags*env; //Number of tags
				
				
				int L = fmSize; //scurrent frame size

				// Tell tags they can choose their own random number
				int[] status = genRandom(n,L);
				int success = status[0];
				int empty = status[1];
				int collision = status[2];

				/* Controls the frame offset.
				 * At firs offset is 0, on the
				 * next frame, then it is a sum
				 * of the size of each frame before.
				 */
				int offset = 0;
				int c = 0, s = 0, e = 0;
				boolean flag = false;	// <<error>>
				
				/* Every while iteration is equivalent to
				 * a frame being generated. Frames will
				 * keep being generated until the number
				 * of tags in collision equals zero.
				 */
				while (collision != 0){
					/* Success slot means we are not
					 * gonna handle it anymore then
					 * please kick them out of n.
					 */
					n -= success;

					// offset is now holding the total frame size
					// just to be used in the final average calculation.
					offset += L;
					e += empty;
					s += success;
					c += collision;

					// Required by ILCM. 2^qc = L
					int qc = (int) Math.ceil(log2(L));
					//System.out.println("QCurrent= "+ qc);

					/* The estimators are going to estimate
					 * how many tags (n_) would have been involved
					 * in collisions. It returns the probability
					 * of number of tags which should taken as the
					 * next frame size. Quite obvious, isn't it?
					 */
					int f_ = (int) Math.ceil(estimateFunction(protocol,qc,collision, slots));
					L = f_;

					if(!flag) {
						totalError += Math.abs(offset - f_);
						flag = true;
					}
					
					status = genRandom(n,L);
					empty = status[0];
					success = status[1];
					collision = status[2];
				}
				totalTime += System.currentTimeMillis() - start;
				totalOffset += offset;
				totalEmpty += e;
				totalSuccess += s;
				totalCollision += c;
			}
			System.out.println((int)(numTags*env)+
					"\t["+totalEmpty/eval+"|"+totalSuccess/eval+"|"+totalCollision/eval+"]\t"
					+totalTime+"\t"+totalOffset/eval+"\t"+totalError/eval);
		}
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
		case "lower-bound":
			return C*2;
		case "schoute":
			return C*2.39;
		case "ilcm-sbs":
			int i = 0;
			double qn = -1;
			double Rant = 0;
			double N = 0;
			double R = 0;
			int c, s;
			double l, k;
			double pot = Math.pow(2, qc);
			//System.out.println("Limit is= "+ (int) Math.pow(2, qc));
			while(qn == -1 && i < (int)pot) {	
				i++;		
				c = 0;
				s = 0;
				//System.out.println("Fetching slot number "+ (i-1));
				if (frame[i-1] > 1){
					c = 1;
				} else if (frame[i-1] == 1) {
					s = 1;
				}
				k = c/((4.344 * i - 16.28) + (i/(-2.282 - 0.273 * i)) * c + 0.2407 * Math.log(i + 42.56));
				l = (1.2592 + 1.513 * i) * Math.tan((Math.pow(1.234 *i, -0.9907)) * c);

				if (k < 0) k = 0;

				N = (k*s) + l;
				R = (N*pot)/i;
				
				/*if (c==0){
					R = (s*pot)/i;
				}*/
				
				if (i > 1) {
					double L1 = pot;
					double L2 = Math.pow(2, Math.round(log2(R)));
					double ps1 = (R/L1) * Math.pow((1 - (1/L1)), R-1);
					double ps2 = (R/L2) * Math.pow((1 - (1/L2)), R-1);

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
		simulator.simulate("ilcm-sbs", 1, 50, 251, 10000, 128);
	}
}
