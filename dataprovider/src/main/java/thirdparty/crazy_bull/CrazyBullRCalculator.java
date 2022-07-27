package thirdparty.crazy_bull;

public class CrazyBullRCalculator {
	public static double XLo, XHi, YLo, YHi;

	private CrazyBullRCalculator() {}

	static {
		init();
	}

	public static void init() {
		XLo = 5.7;
		XHi = 17;
		YLo = 0.935403105;
		YHi = 1;
	}

	/**
	 *
	 * @param x D
	 * @param y ACC
	 * @return The R score of this gameplay
	 */
	public static double eval(double x, double y) {
		double[] c = {
				279.8797946871640,
				528.3010777498843,
				422.1282045808567,
				138.5807381040514,
				318.2521180218479,
				-21.36049015563466,
				89.79916598438198,
				256.7692542088084,
				110.0442908135585,
				25.68573628812841,
				-3.455751278271652,
				-11.24395409094263,
				-46.03561353299639,
				-42.40553252921365,
				-12.24599454333353,
				8.917718764126615,
				33.54545384331117,
				33.21600510917200,
				29.97371473065533,
				21.75010922773462,
				4.694350847362575,
				-2.073168425985943,
				-8.527466920637410,
				-11.41739617279520,
				-13.75800798367046,
				-14.30516423357537,
				-8.346583655814630,
				-0.8935352407485705,
				0.7149903885144406,
				3.578455242068043,
				3.792436829426481,
				4.081955340549154,
				6.230838409222139,
				5.493788245974849,
				1.699357971629093,
				-0.07954682336265180,
				-0.09289519073554239,
				-0.8852091399790804,
				-0.9656173948291340,
				-0.6199915055987623,
				-1.480634925070910,
				-2.211984841680285,
				-1.159184044952558,
				0.06908243025789186,
				0.07864886989688262,
				0.005595506761079347,
				0.1204755520362488,
				0.1940419763008610,
				0.02524697925679845,
				0.1287906618910464,
				0.4217782149408803,
				0.3857828065570721,
				0.07513286986808960,
				-0.08026294653892107,
				-0.01314000678177232,
		};
		CEvalChebPoly.init(54, 1, 1, c,
				11.35000000000000, 5.650000000000000,
				2.286839759448360, 0.5463735846078558,
				0.9677015525000000, 0.03229844749999999,
				-0.03338885713016246, 0.03338885713016246);
		return CEvalChebPoly.eval(x, y);
	}

}

class CEvalChebPoly {

	private CEvalChebPoly() {}

	public static void init(int Order, int LogX, int LogY, double[] parms, double Scale0, double Scale1,
							double Scale2, double Scale3, double Scale4, double Scale5, double Scale6, double Scale7) {
		order = Order;
		logx = LogX;
		logy = LogY;
		p = parms;
		s0 = Scale0;
		s1 = Scale1;
		s2 = Scale2;
		s3 = Scale3;
		s4 = Scale4;
		s5 = Scale5;
		s6 = Scale6;
		s7 = Scale7;
	}

	public static double eval(double x, double y) {
		int tcnt, j, m, iv;
		double[] tx;
		double[] ty;
		double[] v;
		double ans;
		tx = new double[12];
		ty = new double[12];
		v = new double[70];
		if(logx == 0) x = (x - s0) / s1;
		else x = (Math.log(x) - s2) / s3;
		if(logy == 0) y = (y - s4) / s5;
		else y = (Math.log(y) - s6) / s7;
		switch(order) {
			case 5:
				tcnt = 3;
				break;
			case 9:
				tcnt = 4;
				break;
			case 14:
				tcnt = 5;
				break;
			case 20:
				tcnt = 6;
				break;
			case 27:
				tcnt = 7;
				break;
			case 35:
				tcnt = 8;
				break;
			case 44:
				tcnt = 9;
				break;
			case 54:
				tcnt = 10;
				break;

			case 65:
				tcnt = 11;
				break;
			default:
				return 0.0;
		}
		if(tcnt > 6) {
			if(x < -1.0) x = -1.0;
			if(x > 1.0) x = 1.0;
			if(y < -1.0) y = -1.0;
			if(y > 1.0) y = 1.0;
		}
		tx[0] = ty[0] = 1.0;
		tx[1] = x;
		ty[1] = y;
		for(j = 2; j < tcnt; j++) {
			tx[j] = 2 * x * tx[j - 1] - tx[j - 2];
			ty[j] = 2 * y * ty[j - 1] - ty[j - 2];
		}
		iv = 0;
		for(j = 0; j < tcnt; j++) {
			for(m = j; m >= 0; m--)
				v[iv++] = tx[m] * ty[j - m];
		}
		ans = 0.0;
		for(j = 0; j <= order; j++)
			ans += p[j] * v[j];
		return ans;
	}

	private static int order, logx, logy;
	private static double[] p;
	private static double s0;
	private static double s1;
	private static double s2;
	private static double s3;
	private static double s4;
	private static double s5;
	private static double s6;
	private static double s7;
}











