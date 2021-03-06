package tech.aroma.data.sql

import tech.sirwellington.alchemy.arguments.Arguments.checkThat
import tech.sirwellington.alchemy.arguments.assertions.*
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 *
 * @author SirWellington
 */

/**
 * Checks whether a [ResultSet] has a column present or not.
 */
public fun ResultSet.hasColumn(column: String): Boolean
{
    checkThat(column).isA(nonEmptyString())

    try
    {
        this.findColumn(column)
        return true
    }
    catch (ex: SQLException)
    {
        return false
    }
}

public fun Long.toTimestamp(): Timestamp
{
    return Timestamp(this)
}

public fun Long.toDate(): java.sql.Date
{
    return java.sql.Date(this)
}

public fun String?.toUUID(): UUID?
{
    val string = this ?: return null

    return try
    {
        UUID.fromString(string)
    }
    catch (ex: Exception)
    {
        return null
    }
}

internal fun <T> Iterable<T>.toCommaSeparatedList() = joinToString(separator = ",")

internal object Timestamps
{
    fun now(): Timestamp
    {
        return Timestamp.from(Instant.now())
    }
}