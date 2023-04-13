package mobi.sevenwinds.app.budget

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
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse =
        withContext(Dispatchers.IO) {
            transaction {
                val pageQuery = BudgetTable
                    .select { BudgetTable.year eq param.year }
                    .orderBy(BudgetTable.month to SortOrder.ASC)
                    .orderBy(BudgetTable.amount to SortOrder.DESC)
                    .limit(param.limit, param.offset)

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

                val pageData = BudgetEntity.wrapRows(pageQuery).map { it.toResponse() }

                return@transaction BudgetYearStatsResponse(
                    total = total,
                    totalByType = sumByType,
                    items = pageData
                )
            }

        }

}


