import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.icerockdev.common.database.execAndMap
import com.icerockdev.common.database.prepare
import com.icerockdev.common.database.prepareParameterized
import com.icerockdev.common.database.sql.jsonb
import com.icerockdev.common.database.sql.postgresEnumColumn
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class PreparedStatementTest {
    @Test
    fun testPrepare() {
        transaction {
            val sql = """
            SELECT 
                integer_value,
                NULLIF(REGEXP_REPLACE(varchar_value, '[[:alpha:].\s]', '', 'g'), '') AS numeric_value,
                varchar_value,
                json_value,
                postgres_enum_value
            FROM test
            WHERE integer_value=?
            AND varchar_value LIKE ?
            AND (json_value ->> 'name'=? OR json_value::jsonb @> ?::jsonb)
            ORDER BY numeric_value;
        """.trimIndent()

            val result = prepare(
                connection,
                sql,
                listOf(1, "Test String%", "Test name", "{\"name\": \"Test name\"}")
            ).execAndMap {
                var numeric: Int? = it.getInt("numeric_value")
                if (it.wasNull()) {
                    numeric = null
                }
                listOf(
                    it.getInt("integer_value"),
                    numeric,
                    it.getString("varchar_value"),
                    it.getString("json_value"),
                    it.getString("postgres_enum_value")
                )
            }

            result.forEach {
                println(
                    "integer_value: ${it[0]}, numeric_value: ${it[1]}, varchar_value: ${it[2]}, " +
                            "json_value: ${it[3]}, postgres_enum_value: ${it[4]}"
                )
            }

            assertEquals(
                result,
                listOf(
                    listOf(
                        1,
                        3000,
                        "Test String with numeric 3000",
                        "{\"name\": \"Test name\", \"value\": 22}",
                        "ENUM_ONE"
                    ),
                    listOf(1, null, "Test String", "{\"name\": \"Test name\", \"value\": 22}", "ENUM_ONE")
                )
            )
        }
    }

    @Test
    fun testPrepareParameterized() {
        transaction {
            val sql = """
            SELECT 
                integer_value, 
                varchar_value,
                json_value,
                postgres_enum_value
            FROM test
            WHERE integer_value=:value 
            ORDER BY integer_value
        """.trimIndent()

            val result = prepareParameterized(connection, sql, mapOf("value" to 2)).execAndMap {
                listOf(
                    it.getInt("integer_value"),
                    it.getString("varchar_value"),
                    it.getString("json_value"),
                    it.getString("postgres_enum_value")
                )
            }

            result.forEach {
                println("integer_value: ${it[0]}, varchar_value: ${it[1]}, json_value: ${it[2]}, postgres_enum_value: ${it[3]}")
            }


            assertEquals(
                result,
                listOf(
                    listOf(2, "Test String with numeric 7000", "{\"name\": \"Test name 3\", \"value\": 34}", "ENUM_TWO")
                )
            )
        }
    }

    companion object {
        private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.start()
        private val dataSource: DataSource = embeddedPostgres.postgresDatabase

        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            Database.connect(dataSource)
            transaction {
                SchemaUtils.create(TestTable)

                TestTable.insert {
                    it[integerValue] = 1
                    it[varcharValue] = "Test String"
                    it[jsonValue] = JsonValueDto("Test name", 22)
                    it[postgresEnumValue] = PostgresEnumValue.ENUM_ONE
                }

                TestTable.insert {
                    it[integerValue] = 1
                    it[varcharValue] = "Test String with numeric 3000"
                    it[jsonValue] = JsonValueDto("Test name", 22)
                    it[postgresEnumValue] = PostgresEnumValue.ENUM_ONE
                }

                TestTable.insert {
                    it[integerValue] = 2
                    it[varcharValue] = "Test String with numeric 7000"
                    it[jsonValue] = JsonValueDto("Test name 3", 34)
                    it[postgresEnumValue] = PostgresEnumValue.ENUM_TWO
                }
            }
        }

        @JvmStatic
        @AfterAll
        fun shutdown() {
            embeddedPostgres.close()
        }
    }
}

val jsonMapper = jacksonObjectMapper()

object TestTable : IntIdTable() {
    var integerValue = integer("integer_value")
    var varcharValue = varchar("varchar_value", 50)
    var jsonValue: Column<JsonValueDto> = jsonb(
        name = "json_value",
        clazz = JsonValueDto::class.java,
        jsonMapper = jsonMapper
    )
    var postgresEnumValue: Column<PostgresEnumValue> = postgresEnumColumn("postgres_enum_value", "varchar")
}

data class JsonValueDto(var name: String, var value: Int)

enum class PostgresEnumValue {
    ENUM_ONE,
    ENUM_TWO
}
