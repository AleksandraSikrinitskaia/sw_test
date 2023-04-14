
import mobi.sevenwinds.app.author.AuthorResponse

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable


object AuthorTable : IntIdTable("author") {
    val name = text("name")
    val createdDate = datetime("created_date")
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var name by AuthorTable.name
    var createdDate by AuthorTable.createdDate

    fun toResponse(): AuthorResponse {
        return AuthorResponse(name, id.value, createdDate)
    }
}