package explode.dataprovider.provider.mongo

import TConfig.Configuration
import explode.dataprovider.model.PlayRecordInput
import explode.dataprovider.provider.BlowFileResourceProvider
import explode.dataprovider.util.ConfigPropertyDelegates.delegateBoolean
import explode.dataprovider.util.ConfigPropertyDelegates.delegateInt
import explode.dataprovider.util.ConfigPropertyDelegates.delegateString
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
object Detonate {

	private const val MongoCategoryName = "Mongo"

	private val c = Configuration(File("./provider.cfg"))

	private val mongoCate = c.getCategory(MongoCategoryName).apply {
		comment = "Configurations for MongoProvider(explode.dataprovider.provider.mongo.MongoProvider)."
	}

	private val allowUnrankedCoin by c.get(
		MongoCategoryName,
		"allow-unranked-coin",
		true,
		"True to enable coin gaining when player finishes a Unranked chart."
	).delegateBoolean()

	private val basicCoinValue by c.get(
		MongoCategoryName,
		"basic-coin-factor",
		5,
		"Value for calculating the gaining coin when player finishes a chart.",
		0,
		Int.MAX_VALUE
	).delegateInt()

	private val superCoinFactor by c.get(
		MongoCategoryName,
		"super-coin-factor",
		3,
		"Value for calculating the gaining coin when player finishes a chart. The Greater this is, the Smaller result is."
	).delegateInt()

	private val resourceDirectory by c.get(
		MongoCategoryName,
		"resource-directory",
		".explode_data",
		"The path to the directory of data."
	).delegateString()

	private val defaultUserAvatar by c.get(
		MongoCategoryName,
		"default-user-avatar",
		"",
		"The ID of the default user avatar. Empty if disable."
	).delegateString()

	private val defaultStorePreview by c.get(
		MongoCategoryName,
		"default-store-preview",
		"",
		"The ID of the default set. Empty if disable."
	).delegateString()

	init {
		c.save()
	}

	private fun levelFactor(level: Int) = exp(level.toFloat() / 4)

	private fun playModeFactor(isBleed: Boolean?, isMirror: Boolean?): Float =
		0.2F + if(isBleed == true) 0.8F else 0F + if(isMirror == true) 0.2F else 0F

	private fun finishRateFactor(rate: Float) = rate.pow(2)

	fun calcGainCoin(isRanked: Boolean, difficultyLevel: Int, record: PlayRecordInput): Int {
		if(!allowUnrankedCoin && !isRanked) {
			return 0
		}

		val perfect = record.perfect?.toFloat() ?: error("Missing 'Perfect' Value")
		val good = record.good?.toFloat() ?: error("Missing 'Good' Value")
		val miss = record.miss?.toFloat() ?: error("Missing 'Miss' value")
		val total = perfect + good + miss

		val rate = (perfect + (good / 2) - miss * 2) / total

		val basicCoin = basicCoinValue
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

		return floor(basicCoin * difficultyFactor * playModeFactor * rateFactor * ( 1F / superCoinFactor )).roundToInt() + 1
	}

	val ResourceProvider: BlowFileResourceProvider by lazy {
		val avatar = defaultUserAvatar.takeIf { it.isNotBlank() }
		val preview = defaultStorePreview.takeIf { it.isNotBlank() }
		BlowFileResourceProvider(File(resourceDirectory), defaultUserAvatar = avatar, defaultStorePreview = preview)
	}

}