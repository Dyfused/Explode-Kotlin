package explode.dataprovider.provider

import explode.dataprovider.model.*
import explode.dataprovider.provider.DifficultyUtils.toDifficultyClassNum

interface IBlowAccessor {

	fun getUser(userId: String): UserModel?

	fun getUserByName(username: String): UserModel?

	fun getUserByToken(soudayo: String): UserModel?

	fun getSet(setId: String): SetModel?

	fun getChart(chartId: String): DetailedChartModel?

	fun updateUser(userModel: UserModel): UserModel

	fun updateSet(setModel: SetModel): SetModel

	fun updateChart(detailedChartModel: DetailedChartModel): DetailedChartModel

	/**
	 * Read [password] of [User][UserModel]s from the database.
	 * Password is stored separatedly.
	 */
	val UserModel.password: String

	/**
	 * Write [password] of [User][UserModel] to the database.
	 * Password should be plain text without encryption.
	 * Password is stored separatedly.
	 *
	 * @param password The new password
	 */
	fun UserModel.setPassword(password: String): UserModel

	/**
	 * Create and store a new [User][UserModel].
	 *
	 * Note: [password] is plain text but not encrypted value. This is the security problem of Dynamite itself.
	 *
	 * @param username username
	 * @param password password
	 */
	fun createUser(username: String, password: String): UserModel

	/**
	 * Create a Chart Set and store.
	 *
	 * @param setTitle The title of the set showed in Dynamite like PUPA.
	 * @param composerName The music composer's name like モリモリあつし
	 * @param noterName The noter's display name, not have to be username.
	 * @param chart The charts of the set.
	 * @param isRanked If true, the set will be listed on Ranked category in the store. Normally charts of ranked set make more sense in gameplay.
	 * @param introduction Introduction texts. No usage in Dynamite.
	 * @param coinPrice The price to buy in the store.
	 * @param needReview If true, the set will be listed on Review category instead of others. Default set true for new sets.
	 */
	fun createSet(
		setTitle: String,
		composerName: String,
		noterName: String,
		chart: List<ChartModel>,
		isRanked: Boolean,
		introduction: String = "",
		coinPrice: Int = 0,
		OverridePriceStr: String = "",
		needReview: Boolean = true
	): SetModel

	/**
	 * Create a Chart. Any chart required to be included in a [Set][createSet] to be available to Dynamite.
	 *
	 * @param difficultyClass The difficulty class, from 1 to 6 referring from CASUAL to TERA.
	 * @param difficultyValue The numeric value of difficulty.
	 */
	fun createChart(
		chartName: String,
		charterUser: UserModel,
		musicianName: String,
		difficultyClass: Int,
		difficultyValue: Int,
		gcPrice: Int = 0,
		D: Double? = null
	): DetailedChartModel

	/**
	 * Build a set with charts with [ChartSetBuilder].
	 *
	 * @see createSet
	 * @see createChart
	 */
	fun buildChartSet(
		setTitle: String,
		composerName: String,
		noterUser: UserModel,
		isRanked: Boolean,
		coinPrice: Int,
		introduction: String = "",
		needReview: Boolean = true,
		block: ChartSetBuilder.() -> Unit
	) = ChartSetBuilder(
		this,
		setTitle,
		composerName,
		noterUser,
		isRanked,
		coinPrice,
		introduction,
		needReview
	).apply(block).buildSet()

	/**
	 * Use [IBlowAccessor.buildChartSet][buildChartSet] instead.
	 */
	class ChartSetBuilder internal constructor(
		private val accessor: IBlowAccessor,

		private val setTitle: String,
		private val composerName: String,
		private val noterUser: UserModel,
		private val isRanked: Boolean,
		private val coinPrice: Int,
		private val introduction: String = "",
		private val needReview: Boolean = true
	) {

		private val chart = mutableSetOf<DetailedChartModel>()

		fun addChart(
			difficultyClass: String,
			difficultyValue: Int,
			D: Double? = null
		): DetailedChartModel =
			addChart(difficultyClass.toDifficultyClassNum(), difficultyValue, D)

		fun addChart(
			difficultyClass: Int,
			difficultyValue: Int,
			D: Double? = null
		): DetailedChartModel {
			// Unranked charts should have no D value
			val d = if(isRanked) D else null

			// Check difficulty duplication
			if(difficultyClass in chart.map(DetailedChartModel::difficultyBase)) {
				error("Duplicated Difficulty Class: $difficultyClass")
			}

			return accessor.createChart(
				"${setTitle}_${difficultyClass}",
				noterUser, composerName,
				difficultyClass, difficultyValue,
				0,
				D = d
			).apply(chart::add)
		}

		fun buildSet(): SetModel {
			if(chart.isEmpty()) error("Must include charts")
			return accessor.createSet(
				setTitle,
				composerName, noterUser.username,
				chart.map(DetailedChartModel::minify),
				isRanked, introduction, coinPrice,
				"", needReview
			)
		}
	}
}