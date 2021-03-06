package tech.aroma.data.sql

import tech.sirwellington.alchemy.annotations.access.NonInstantiable
import tech.sirwellington.alchemy.arguments.AlchemyAssertion
import tech.sirwellington.alchemy.arguments.Arguments.checkThat
import tech.sirwellington.alchemy.arguments.FailedAssertionException
import tech.sirwellington.alchemy.arguments.assertions.*
import java.sql.ResultSet

/**
 *
 * @author SirWellington
 */
@NonInstantiable
class SQLAssertions
{
    companion object
    {
        /**
         * Creates an [Assertion][AlchemyAssertion] that checks whether the [ResultSet]
         * contains the given [column].
         */
        @JvmStatic fun resultSetWithColumn(column: String): AlchemyAssertion<ResultSet>
        {
            checkThat(column).isA(nonEmptyString())

            return AlchemyAssertion { results ->

                checkThat(results).isA(notNull())

                if (!results.hasColumn(column))
                {
                    throw FailedAssertionException("Expected ResultSet to have Column [$column]")
                }
            }
        }
    }

}