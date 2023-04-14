package mobi.sevenwinds.app.budget

import AuthorTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.authorId = body.authorId
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse {
        return withContext(Dispatchers.IO) {
            transaction {
                val (total, sumByType) = queryStatsData(param)

                return@transaction BudgetYearStatsResponse(
                    total = total,
                    totalByType = sumByType,
                    items = queryBudgetPage(param)
                )
            }
        }
    }

    private fun queryStatsData(param: BudgetYearParam): Pair<Int, Map<String, Int>> {
        val countColumn = BudgetTable.year.count()
        val sumAmount = BudgetTable.amount.sum();

        val statData = BudgetTable
            .slice(BudgetTable.type, countColumn, sumAmount)
            .select(BudgetTable.year eq param.year)
            .groupBy(BudgetTable.type)
            .toList()

        val total = statData.map { it[countColumn] }.sum()

        val sumByType = statData
            .groupBy { it[BudgetTable.type].name }
            .mapValues { it.value.first()[sumAmount]!! }
        return Pair(total, sumByType)
    }

    private fun queryBudgetPage(param: BudgetYearParam): List<BudgetRecord> {
        val pageQuery = Join(
            BudgetTable, AuthorTable,
            onColumn = BudgetTable.authorId, otherColumn = AuthorTable.id,
            joinType = JoinType.LEFT
        )
            .select {
                (BudgetTable.year eq param.year) and (
                        if (param.authorName != null)
                            (AuthorTable.name.lowerCase() like "%${param.authorName}%".toLowerCase())
                        else Op.TRUE
                        )
            }
            .orderBy(BudgetTable.month to SortOrder.ASC)
            .orderBy(BudgetTable.amount to SortOrder.DESC)
            .limit(param.limit, param.offset)

        val pageData = pageQuery.toList().map {
            BudgetRecord(
                it[BudgetTable.year],
                it[BudgetTable.month],
                it[BudgetTable.amount],
                it[BudgetTable.type],
                it[BudgetTable.authorId],
                it[AuthorTable.name]
            )
        }
        return pageData
    }
}


