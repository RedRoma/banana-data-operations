package tech.aroma.data.sql.serializers

/**
 * @author SirWellington
 */

import com.google.inject.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tech.aroma.data.sql.DatabaseSerializer
import tech.aroma.thrift.*
import tech.aroma.thrift.authentication.AuthenticationToken
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner
import kotlin.test.assertTrue

@RunWith(AlchemyTestRunner::class)
class ModuleSerializersTest
{

    private lateinit var instance: ModuleSerializers
    private lateinit var injector: Injector

    @Before
    fun setup()
    {
        instance = ModuleSerializers()

        injector = Guice.createInjector(instance)
    }

    @Test
    fun testHasMessageSerializer()
    {
        assertTrue { injector.hasInstance<DatabaseSerializer<Message>>() }
    }

    @Test
    fun testHasOrganizationSerializer()
    {
        assertTrue { injector.hasInstance<DatabaseSerializer<Organization>>() }
    }

    @Test
    fun testHasTokenSerializer()
    {
        assertTrue { injector.hasInstance<DatabaseSerializer<AuthenticationToken>>() }
    }

    @Test
    fun testHasUserSerializer()
    {
        assertTrue { injector.hasInstance<DatabaseSerializer<User>>() }
    }

    private inline fun <reified T> Injector.hasInstance(): Boolean
    {
        val literal = object : TypeLiteral<T>()
        {}

        val result = injector.getInstance(Key.get(literal)) ?: null

        return result != null
    }
}