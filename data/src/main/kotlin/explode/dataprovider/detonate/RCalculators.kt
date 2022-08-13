package explode.dataprovider.detonate

import thirdparty.crazy_bull.CrazyBullRCalculator

enum class RCalculators : RCalculator {
	Simple {
		override fun calculateRScore(D: Double, perfect: Int, good: Int, miss: Int): Double {
			val acc = (perfect + (good / 2)) / (perfect + good + miss)
			return acc * RCalculator.calculateMaxR(D)
		}
	},
	KesdiaelKen {
		override fun calculateRScore(D: Double, perfect: Int, good: Int, miss: Int): Double {
			val n = perfect + good + miss
			val maxR = RCalculator.calculateMaxR(D)
			val a1 = (maxR * D) / n
			return (maxR - (good * ((a1 / 2) + 2)) - (miss * (a1 + 2))).takeIf { it >= 50 } ?: 0.0
		}
	},
	CrazyBull {
		override fun calculateRScore(D: Double, perfect: Int, good: Int, miss: Int): Double {
			val acc = (perfect + (good.toDouble() / 2)) / (perfect + good + miss)
			return CrazyBullRCalculator.eval(D, acc).takeIf { it >= 50 } ?: 0.0
		}
	}
}