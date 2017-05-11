package tech.aroma.data.sql

import com.nhaarman.mockito_kotlin.whenever
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.springframework.jdbc.core.JdbcTemplate
import sir.wellington.alchemy.collections.lists.Lists
import tech.aroma.thrift.*
import tech.aroma.thrift.exceptions.*
import tech.aroma.thrift.functions.TimeFunctions
import tech.sirwellington.alchemy.generator.AlchemyGenerator.one
import tech.sirwellington.alchemy.generator.BooleanGenerators.booleans
import tech.sirwellington.alchemy.generator.CollectionGenerators.listOf
import tech.sirwellington.alchemy.generator.NumberGenerators.positiveLongs
import tech.sirwellington.alchemy.generator.ObjectGenerators.pojos
import tech.sirwellington.alchemy.generator.StringGenerators.alphabeticString
import tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows
import tech.sirwellington.alchemy.test.junit.runners.*
import java.time.Duration
import java.util.*

/**
 * @author SirWellington
 */
@RunWith(AlchemyTestRunner::class)
@Repeat(50)
class SQLMessageRepositoryTest
{

    @Mock
    private lateinit var database: JdbcTemplate

    @Mock
    private lateinit var serializer: DatabaseSerializer<Message>

    private lateinit var instance: SQLMessageRepository

    @GeneratePojo
    private lateinit var message: Message

    @GenerateString(GenerateString.Type.UUID)
    private lateinit var appId: String

    @GenerateString(GenerateString.Type.UUID)
    private lateinit var messageId: String

    @GeneratePojo
    private lateinit var lifetime: LengthOfTime

    @GenerateString(GenerateString.Type.ALPHABETIC)
    private lateinit var alphabetic: String

    @Before
    @Throws(Exception::class)
    fun setUp()
    {
        instance = SQLMessageRepository(database, serializer)

        lifetime.unit = TimeUnit.SECONDS
        message.applicationId = appId
        message.messageId = messageId
    }

    @Test
    @Throws(Exception::class)
    fun testSaveMessage()
    {
        val expectedStatement = SQLStatements.Inserts.MESSAGE

        val duration = TimeFunctions.lengthOfTimeToDuration().apply(lifetime)
        instance.saveMessage(message, lifetime)


        verify<DatabaseSerializer<Message>>(serializer).save(message, duration, expectedStatement, database)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testSaveMessageWithNoDuration()
    {
        val expectedStatement = SQLStatements.Inserts.MESSAGE

        instance.saveMessage(message, null)

        verify<DatabaseSerializer<Message>>(serializer).save(message, null, expectedStatement, database)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testSaveMessageWithBadArguments()
    {
        assertThrows { instance.saveMessage(null) }
                .isInstanceOf(InvalidArgumentException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testSaveWhenSerializerFails()
    {
        doThrow(RuntimeException())
                .whenever(serializer)
                .save(any(), any(), any(), any())

        assertThrows { instance.saveMessage(message) }
                .isInstanceOf(OperationFailedException::class.java)
    }


    @Test
    @Throws(Exception::class)
    fun testGetMessage()
    {
        val expectedQuery = SQLStatements.Queries.SELECT_MESSAGE

        whenever(database.queryForObject(expectedQuery, serializer, appId.asUUID(), messageId.asUUID()))
                .thenReturn(message)

        val result = instance.getMessage(appId, messageId)
        assertThat(result, `is`<Message>(message))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetMessageWhenDatabaseFails()
    {
        val expectedQuery = SQLStatements.Queries.SELECT_MESSAGE

        whenever(database.queryForObject(expectedQuery, serializer, appId.asUUID(), messageId.asUUID()))
                .thenThrow(RuntimeException())

        assertThrows { instance.getMessage(appId, messageId) }
                .isInstanceOf(OperationFailedException::class.java)

    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetMessageWhenMessageDoesNotExist()
    {
        val expectedQuery = SQLStatements.Queries.SELECT_MESSAGE

        whenever(database.queryForObject(expectedQuery, serializer, appId.asUUID(), messageId.asUUID()))
                .thenReturn(null)

        assertThrows { instance.getMessage(appId, messageId) }
                .isInstanceOf(DoesNotExistException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetMessageWithBadArgs()
    {
        assertThrows { instance.getMessage(null, messageId) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getMessage(appId, null) }.isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.getMessage("", messageId) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getMessage(appId, "") }.isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.getMessage(alphabetic, messageId) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getMessage(appId, alphabetic) }.isInstanceOf(InvalidArgumentException::class.java)

    }

    @Test
    @Throws(Exception::class)
    fun testDeleteMessage()
    {
        val expectedStatement = SQLStatements.Deletes.MESSAGE

        instance.deleteMessage(appId, messageId)

        verify<JdbcTemplate>(database).update(expectedStatement, appId.asUUID(), messageId.asUUID())
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testDeleteMessageWithInvalidArgs()
    {
        val alphabetic = one(alphabeticString())

        assertThrows { instance.deleteMessage(alphabetic, messageId) }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.deleteMessage(appId, alphabetic) }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.deleteMessage("", messageId) }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.deleteMessage(alphabetic, "") }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.deleteMessage(null, messageId) }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.deleteMessage(appId, null) }
                .isInstanceOf(InvalidArgumentException::class.java)

    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testDeleteWhenOperationFails()
    {
        val expectedStatement = SQLStatements.Deletes.MESSAGE

        whenever(database.update(expectedStatement, appId.asUUID(), messageId.asUUID()))
                .thenThrow(RuntimeException())

        assertThrows { instance.deleteMessage(appId, messageId) }
                .isInstanceOf(OperationFailedException::class.java)

    }


    @Test
    @Throws(Exception::class)
    fun testContainsMessage()
    {
        val query = SQLStatements.Queries.CHECK_MESSAGE
        val expected = one(booleans())

        whenever(database.queryForObject(query, Boolean::class.java, appId.asUUID(), messageId.asUUID()))
                .thenReturn(expected)

        val result = instance.containsMessage(appId, messageId)
        assertThat(result, `is`(expected))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testContainsMessageWhenOperationFails()
    {
        val query = SQLStatements.Queries.CHECK_MESSAGE

        whenever(database.update(eq(query), eq(Boolean::class.java), any(), any()))
                .thenThrow(RuntimeException())

        assertThrows { instance.containsMessage(appId, messageId) }
                .isInstanceOf(OperationFailedException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testContainsMessageWithBadArgs()
    {
        assertThrows { instance.containsMessage(null, messageId) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.containsMessage(appId, null) }.isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.containsMessage("", messageId) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.containsMessage(appId, "") }.isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.containsMessage(alphabetic, messageId) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.containsMessage(appId, alphabetic) }.isInstanceOf(InvalidArgumentException::class.java)

    }


    @Test
    @Throws(Exception::class)
    fun testGetByHostname()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_HOSTNAME
        val hostname = alphabetic

        val messages = listOf(pojos(Message::class.java))

        whenever(database.query(query, serializer, hostname)).thenReturn(messages)

        val result = instance.getByHostname(hostname)

        assertThat(result, `is`(messages))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByHostnameWhenDatabaseFails()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_HOSTNAME
        val hostname = alphabetic

        whenever(database.query(query, serializer, hostname))
                .thenThrow(RuntimeException())

        assertThrows { instance.getByHostname(hostname) }
                .isInstanceOf(OperationFailedException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByHostnameWithBadArgs()
    {
        assertThrows { instance.getByHostname(null) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getByHostname("") }.isInstanceOf(InvalidArgumentException::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByApplication()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_APPLICATION
        val messages = listOf(pojos(Message::class.java))

        whenever(database.query(query, serializer, appId.asUUID()))
                .thenReturn(messages)

        val results = instance.getByApplication(appId)
        assertThat(results, `is`(messages))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByApplicationWhenNoMessages()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_APPLICATION

        whenever(database.query(query, serializer, appId.asUUID()))
                .thenReturn(Lists.emptyList<Message>())

        val results = instance.getByApplication(appId)
        assertThat(results, notNullValue())
        assertThat(results, `is`(empty<Message>()))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByApplicationWhenDatabaseFails()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_APPLICATION

        whenever(database.query(query, serializer, appId.asUUID()))
                .thenThrow(RuntimeException())

        assertThrows { instance.getByApplication(appId) }
                .isInstanceOf(OperationFailedException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByApplicationWithBadArgs()
    {
        assertThrows { instance.getByApplication(null) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getByApplication("") }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getByApplication(alphabetic) }.isInstanceOf(InvalidArgumentException::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByTitle()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_TITLE
        val title = alphabetic
        val messages = listOf(pojos(Message::class.java))

        whenever(database.query(query, serializer, appId.asUUID(), title))
                .thenReturn(messages)

        val results = instance.getByTitle(appId, title)
        assertThat(results, `is`(messages))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByTitleWhenNoMessages()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_TITLE
        val title = alphabetic

        whenever(database.query(query, serializer, appId.asUUID(), title))
                .thenReturn(Lists.emptyList())

        val results = instance.getByTitle(appId, title)
        assertThat(results, notNullValue())
        assertThat(results, `is`(empty<Message>()))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByTitleWhenDatabaseFails()
    {
        val query = SQLStatements.Queries.SELECT_MESSAGES_BY_TITLE
        val title = alphabetic

        whenever(database.query(query, serializer, appId.asUUID(), title))
                .thenThrow(RuntimeException())

        assertThrows { instance.getByTitle(appId, title) }
                .isInstanceOf(OperationFailedException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetByTitleWithBadArgs()
    {
        assertThrows { instance.getByTitle("", alphabetic) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getByTitle(appId, "") }.isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.getByTitle(null, alphabetic) }.isInstanceOf(InvalidArgumentException::class.java)
        assertThrows { instance.getByTitle(appId, null) }.isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.getByTitle(alphabetic, alphabetic) }.isInstanceOf(InvalidArgumentException::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCountByApplication()
    {
        val query = SQLStatements.Queries.COUNT_MESSAGES
        val count = one(positiveLongs())

        whenever(database.queryForObject(query, Long::class.java, appId.asUUID()))
                .thenReturn(count)

        val result = instance.getCountByApplication(appId)
        assertThat(result, `is`(count))
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetCountWhenDatabaseFails()
    {
        val query = SQLStatements.Queries.COUNT_MESSAGES

        whenever(database.queryForObject(query, Long::class.java, appId.asUUID()))
                .thenThrow(RuntimeException())

        assertThrows { instance.getCountByApplication(appId) }
                .isInstanceOf(OperationFailedException::class.java)
    }

    @DontRepeat
    @Test
    @Throws(Exception::class)
    fun testGetCountWithBadArgs()
    {
        assertThrows { instance.getCountByApplication("") }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.getCountByApplication(null) }
                .isInstanceOf(InvalidArgumentException::class.java)

        assertThrows { instance.getCountByApplication(alphabetic) }
                .isInstanceOf(InvalidArgumentException::class.java)
    }
}

