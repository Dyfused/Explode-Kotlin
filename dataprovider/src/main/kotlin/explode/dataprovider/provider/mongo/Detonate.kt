package explode.dataprovider.provider.mongo

import explode.dataprovider.detonate.RCalculator
import explode.dataprovider.model.game.PlayRecordInput
import explode.dataprovider.provider.BlowFileResourceProvider
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.*

/**
 * This class is used to handle coin, diamond changes.
 *
 * Like whenever a new play record is uploaded, here returns a change of the coin or diamond,
 * and return to provider to calculate the exact coin, diamond value then return to the client.
 *
 * Current Coin Gaining Formula:
 * 	P, G, M - Perfect, Good, Miss
 * 	H, S    - Hardness, Super Coin Factor
 * 	B       - Basic Coin Factor
 *
 * 	FinishRate: F(P, G, M) = (P+G/2-2M) / (P+G+M)
 * 	GainingCoinFactor: G(F, H) = exp(H/4) * F^2 * (1/S)
 * 	GainingCoin: C(B) = G*B + 1
 */
class Detonate(private val config: MongoExplodeConfig) {

	private val logger = LoggerFactory.getLogger("Mongo-Detonate")

	private val rCalc: RCalculator

	init {
		rCalc = runCatching { RCalcAlgorithms.valueOf(config.rScoreAlgorithm) }.getOrElse {
			logger.warn("Cannot find R-Calc-Algorithm: ${config.rScoreAlgorithm}, fallback to 'Simple'.")
			RCalcAlgorithms.Simple
		}
	}

	private fun levelFactor(level: Int) = exp(level.toFloat() / 4)

	private fun playModeFactor(isBleed: Boolean?, isMirror: Boolean?): Float =
		0.2F + if(isBleed == true) 0.8F else 0F + if(isMirror == true) 0.2F else 0F

	private fun finishRateFactor(rate: Float) = rate.pow(2)

	fun calcGainCoin(isRanked: Boolean, difficultyLevel: Int, record: PlayRecordInput): Int {
		if(!config.allowUnrankedCoin && !isRanked) {
			return 0
		}

		val perfect = record.perfect?.toFloat() ?: error("Missing 'Perfect' Value")
		val good = record.good?.toFloat() ?: error("Missing 'Good' Value")
		val miss = record.miss?.toFloat() ?: error("Missing 'Miss' value")
		val total = perfect + good + miss

		val rate = (perfect + (good / 2) - miss * 2) / total

		val basicCoin = config.basicCoinValue
		val rateFactor = finishRateFactor(rate)
		val difficultyFactor = levelFactor(difficultyLevel)
		val playModeFactor = playModeFactor(record.mod?.isBleed, record.mod?.isMirror)

//		println("""
//			Basic:            $basicCoin
//			DifficultyFactor: $difficultyFactor
//			PlayModeFactor:   $playModeFactor
//			FinishRateFactor: $rateFactor
//			SuperFactor:      ${ 1F / superCoinFactor }
//			TotalFactor:      ${difficultyFactor * playModeFactor * rateFactor * ( 1F / superCoinFactor ) }
//		""".trimIndent())

		return floor(basicCoin * difficultyFactor * playModeFactor * rateFactor * (1F / config.superCoinFactor)).roundToInt() + 1
	}

	val resourceProvider: BlowFileResourceProvider by lazy {
		val avatar = config.defaultUserAvatar.takeIf { it.isNotBlank() }
		val preview = config.rScoreAlgorithm.takeIf { it.isNotBlank() }
		BlowFileResourceProvider(
			File(config.resourceDirectory),
			defaultUserAvatar = avatar,
			defaultStorePreview = preview
		)
	}

	/**
	 * Calculate the Current R score
	 */
	fun calcR(d: Double, record: PlayRecordInput): Double {
		val p = record.perfect!!
		val g = record.good!!
		val m = record.miss!!

		return rCalc.calculateRScore(d, p, g, m)
	}

}