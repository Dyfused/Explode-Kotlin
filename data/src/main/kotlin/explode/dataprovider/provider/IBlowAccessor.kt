package explode.dataprovider.provider

import explode.dataprovider.model.database.*
import explode.dataprovider.model.game.*
import explode.dataprovider.provider.DifficultyUtils.toDifficultyClassNum

interface IBlowAccessor : IBlowReadOnly {

	val gameSetting: GameSettingModel

	fun loginUser(username: String, password: String): MongoUser

	fun registerUser(username: String, password: String): MongoUser

	/**
	 * Get Chart Sets
	 *
	 * Priority: showHidden > showReview > showOfficial > showRanked,
	 * which means if showHidden is true, it will ignore all other filters.
	 */
	fun getSets(
		limit: Int,
		skip: Int,
		searchedName: String,
		onlyRanked: Boolean,
		onlyOfficial: Boolean,
		onlyReview: Boolean,
		onlyHidden: Boolean,
		playCountOrder: Boolean,
		publishTimeOrder: Boolean
	): List<MongoSet>

	fun getSets(limit: Int? = null, skip: Int? = null): Iterable<MongoSet>


	// updaters
	fun updateUser(mongoUser: MongoUser): MongoUser
	fun updateSet(mongoSet: MongoSet): MongoSet
	fun updateChart(mongoChart: MongoChart): MongoChart

	/**
	 * Create and store a new [User][UserModel].
	 *
	 * Note: [password] is plain text but not encrypted value. This is the security problem of Dynamite itself.
	 *
	 * @param username username
	 * @param password password
	 */
	fun createUser(username: String, password: String): MongoUser

	/**
	 * Create a Chart Set and store.
	 *
	 * @param musicName The title of the set showed in Dynamite like PUPA.
	 * @param composerName The music composer's name like モリモリあつし
	 * @param noterId The noter's display name, not have to be username.
	 * @param charts The charts of the set.
	 * @param status The status of the set.
	 * @param introduction Introduction texts. No usage in Dynamite.
	 * @param price The price to buy in the store.
	 */
	fun createSet(
		musicName: String,
		composerName: String,
		noterId: String?,
		charts: List<MongoChart>,
		id: String? = null,
		introduction: String? = null,
		status: SetStatus = SetStatus.NEED_REVIEW,
		price: Int = 0
	): MongoSet

	/**
	 * Create a Chart. Any chart required to be included in a [Set][createSet] to be available to Dynamite.
	 *
	 * @param difficultyClass The difficulty class, from 1 to 6 referring from CASUAL to TERA.
	 * @param difficultyValue The numeric value of difficulty.
	 */
	fun createChart(
		difficultyClass: Int, difficultyValue: Int, id: String? = null, D: Double? = null
	): MongoChart

	/**
	 * Build a set with charts with [ChartSetBuilder].
	 *
	 * @see createSet
	 * @see createChart
	 */
	fun buildChartSet(
		setTitle: String,
		composerName: String,
		noterUser: MongoUser,
		isRanked: Boolean,
		coinPrice: Int,
		introduction: String = "",
		needReview: Boolean = true,
		defaultId: String? = null,
		block: ChartSetBuilder.() -> Unit
	) = ChartSetBuilder(
		this, setTitle, composerName, noterUser, isRanked, coinPrice, introduction, needReview, defaultId
	).apply(block).buildSet()

	/**
	 * Use [IBlowAccessor.buildChartSet][buildChartSet] instead.
	 */
	class ChartSetBuilder internal constructor(
		private val accessor: IBlowAccessor,

		private val musicName: String,
		private val composerName: String,
		private val noterUser: MongoUser,
		private val isRanked: Boolean,
		private val price: Int,
		private val introduction: String = "",
		private val needReview: Boolean = true,
		private val defaultId: String? = null
	) {

		private val charts = mutableMapOf<Int, MongoChart>()

		fun addChart(
			difficultyClass: String, difficultyValue: Int, D: Double? = null, defaultId: String? = null
		): MongoChart = addChart(difficultyClass.toDifficultyClassNum(), difficultyValue, D, defaultId)

		fun addChart(
			difficultyClass: Int, difficultyValue: Int, D: Double? = null, defaultId: String? = null
		): MongoChart {
			// Unranked charts should have no D value
			val d = if(isRanked) D else null

			// Check difficulty duplication
			if(difficultyClass in charts.keys) {
				error("Duplicated Difficulty Class: $difficultyClass")
			}

			return accessor.createChart(difficultyClass, difficultyValue, defaultId, d).apply {
				charts[difficultyClass] = this
			}
		}

		fun buildSet(): MongoSet {
			if(charts.isEmpty()) error("Must include at least one chart.")
			return accessor.createSet(
				musicName = musicName,
				composerName = composerName,
				noterId = noterUser._id,
				charts = charts.values.toList(),
				id = defaultId,
				introduction = introduction,
				status = SetStatus.NEED_REVIEW,
				price = price
			)
		}
	}

	fun MongoUser.buySet(id: String): ExchangeSetModel

	fun MongoUser.submitBeforeAssessment(assessmentId: String, medal: Int): BeforePlaySubmitModel

	fun MongoUser.submitBeforePlay(chartId: String, ppCost: Int, eventArgs: String): BeforePlaySubmitModel

	fun MongoUser.submitAfterAssessment(records: List<PlayRecordInput>, randomId: String): AfterAssessmentModel

	fun MongoUser.submitAfterPlay(record: PlayRecordInput, randomId: String): AfterPlaySubmitModel

	fun MongoSet.addReviewResult(review: MongoReviewResult)

	fun MongoSet.getReview(): MongoReview?

	fun MongoSet.startReview(expectStatus: SetStatus)

	/**
	 * End a review on the specific Set.
	 */
	fun MongoSet.endReview(pass: Boolean)

//	fun addAssessmentGroup(
//		name: String,
//		assessmentCharts: List<String>
//	)
}