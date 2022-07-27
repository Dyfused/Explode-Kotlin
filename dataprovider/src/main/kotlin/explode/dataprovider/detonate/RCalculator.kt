package explode.dataprovider.detonate

import kotlin.math.pow
import kotlin.math.round

interface RCalculator {

	/**
	 * Get the R score of certain gameplay result.
	 */
	fun calculateRScore(D: Double, perfect: Int, good: Int, miss: Int): Double

	companion object {
		/**
		 * The Max R score calculation.
		 *
		 * This algorithm is published in DraXon's article(https://www.bilibili.com/read/cv17024921).
		 *
		 * Notice: when D is bigger than 17, the value is incorrect, which is not patched in this method.
		 */
		fun calculateMaxR(D: Double): Double {
			return if(D <= 5.5) {
				50.0
			} else {
				round((0.5813 * D.pow(3) - (3.28 * D.pow(2) + (14.43 * D) - 29.3)))
			}
		}
	}

}