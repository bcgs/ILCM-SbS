
public class Simulator {
	public int readSlot() {
		return 0; // implement later!
	}
	
	public double findOptimalQ(double R) {
		return 0.0; // implement later!
	}
	
	public double _N(double k, int s, double l) {
		return k * s + l;
	}
	
	public double _R(double N, int qc, int i) {
		return (N * Math.pow(2, qc)) / i;
	}
	
	private int ilcmSbS(int qc) {
		int i = 0;
		double qn = -1;
		
		while(qn == -1 && i <= Math.pow(2, qc)) {
			int e, s, c = readSlot();
			i++;
			
			double k = c/((4.344 * i - 16.28) + (i/(-2.282 - 0.273 * i) * c) + 0.2407 * Math.log(i + 42.56));
			double l = (1.2592 + 1.513 * i) * Math.tan((Math.pow(1.234 * i, -0.9907)) * c);
			
			if (k < 0)
				k = 0;
			
			double N = _N(k, s, l);
			double R = _R(N, qc, i);
			
			if (c == 0) { // check if C is really c
				R = (s * Math.pow(2, qc) / i);
			}
			
			if (i > 1 && R - _R(N, qc, i - 1) <= 1) {
				double L1 = Math.pow(2, qc);
				double Qt = findOptimalQ(R);
				double L2 = Math.pow(2, Qt);
				double ps1 = (R/L1) * Math.pow((1 - (1/L1)), _R(N, qc, i - 1));
				double ps2 = (R/L2) * Math.pow((1 - (1/L2)), _R(N, qc, i - 1));
				
				if ((L1 * ps1 - s) < (L2 * ps2)) {
					qn = Qt;
				}
			}
		}
		
		if (qn != -1) {
			broadcast(qn);
		} else {
			qn = qc;
			broadcast(qn);
		}
	}

	private void broadcast(double qn) {
		// TODO Auto-generated method stub
		
	}
}
