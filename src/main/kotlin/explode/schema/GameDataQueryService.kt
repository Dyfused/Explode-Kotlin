@file:Suppress("UNUSED_PARAMETER")

package explode.schema

import com.expediagroup.graphql.server.operations.Query
import explode.GameVersion
import explode.NNInt
import explode.schema.model.*
import java.time.LocalDate

object GameDataQueryService : Query {

	suspend fun gameSetting(): GameSettingModel {
		return GameSettingModel(GameVersion)
	}

	// TODO: Implement
	// See Also: ReviewerModel
	suspend fun reviewer(): ReviewerModel {
		return ReviewerModel()
	}

	// TODO: Implement
	// FIXME: NNInt
	suspend fun set(playCountOrder: Int?, publishTimeOrder: Int?, limit: NNInt?, skip: NNInt?, isHidden: Int?, musicTitle: String?, isOfficial: Int?, isRanked: Int?): List<SetModel> {
		return listOf(FakeSet)
	}

	// TODO: Implement
	suspend fun self(): OwnSetWrapperModel {
		return OwnSetWrapperModel(listOf(FakeSet))
	}

	// TODO: Implement
	suspend fun ownOrGotChart(): List<ChartExpandedModel> {
		return listOf(ChartExpandedModel(UserMutationService.FakeUserModel, "FakeChartName", 0, MusicModel("UnknownMusician"), 0, 0))
	}

	// TODO: Removal
	data class ChartExpandedModel(
		val charter: UserModel,
		val chartName: String,
		val gcPrice: Int,
		val music: MusicModel,
		val difficultyBase: Int,
		val difficultyValue: Int
	)

	// TODO: Removal
	data class MusicModel(
		val musicianName: String
	)

	// TODO: Implement
	suspend fun assessmentGroup(limit: Int?, skip: Int?): List<AssessmentGroupModel> {
		return listOf(FakeAssessmentGroup, FakeAssessmentGroup2)
	}

	// TODO: Implement
	// FIXME: NNInt
	suspend fun assessmentRank(assessmentGroupId: String?, medalLevel: Int?, skip: NNInt?, limit: NNInt?): AssessmentRecordWithRankModel {
		return AssessmentRecordWithRankModel(
			PlayerModel("10001", "FakeUsername", 100000, 1000),
			1,
			1.0,
			10,
			LocalDate.now()
		)
	}

}