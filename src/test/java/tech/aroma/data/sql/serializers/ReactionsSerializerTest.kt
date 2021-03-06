package tech.aroma.data.sql.serializers

/*
 * Copyright 2017 RedRoma, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.springframework.jdbc.core.JdbcOperations
import tech.aroma.data.notNull
import tech.aroma.data.sql.serializers.Columns.Reactions
import tech.aroma.thrift.generators.ReactionGenerators.reactions
import tech.aroma.thrift.reactions.Reaction
import tech.sirwellington.alchemy.generator.CollectionGenerators.Companion.listOf
import tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat
import tech.sirwellington.alchemy.test.junit.runners.GenerateString
import tech.sirwellington.alchemy.thrift.ThriftObjects
import java.sql.ResultSet

@RunWith(AlchemyTestRunner::class)
class ReactionsSerializerTest
{

    @Mock
    private lateinit var database: JdbcOperations

    @Mock
    private lateinit var row: ResultSet

    @GenerateString
    private lateinit var sql: String

    private lateinit var reactions: MutableList<Reaction>
    private val serializedReactions get() = reactions.map(ThriftObjects::toJson)

    private lateinit var instance: ReactionsSerializer

    @Before
    fun setUp()
    {
        setupData()
        setupMocks()
        instance = ReactionsSerializer()
    }

    @Test
    fun testSave()
    {
        instance.save(reactions, sql, database)

        verifyZeroInteractions(database)
    }

    @DontRepeat
    @Test
    fun testSaveWithBadArgs()
    {
        assertThrows {
            instance.save(reactions, "", database)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun testDeserialize()
    {
        val result = instance.deserialize(row)
        assertThat(result, equalTo(reactions))
    }

    @DontRepeat
    @Test
    fun testDeserializeWhenColumnIsEmpty()
    {
        whenever(row.getString(Reactions.SERIALIZED_REACTIONS))
                .thenReturn("")

        val result = instance.deserialize(row)
        assertThat(result, notNull)
    }

    private fun setupData()
    {
        reactions = listOf(reactions(), 14).toMutableList()
    }

    private fun setupMocks()
    {
        val array = mock<java.sql.Array>()
        whenever(array.array).thenReturn(serializedReactions.toTypedArray())

        whenever(row.getArray(Reactions.SERIALIZED_REACTIONS)).thenReturn(array)
    }

}