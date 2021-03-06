package tech.aroma.data.sql

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
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcOperations
import tech.aroma.data.doesNotExist
import tech.aroma.data.invalidArg
import tech.aroma.data.operationError
import tech.aroma.data.sql.SQLStatements.Deletes
import tech.aroma.data.sql.SQLStatements.Inserts
import tech.aroma.data.sql.SQLStatements.Queries
import tech.sirwellington.alchemy.generator.BooleanGenerators.Companion.booleans
import tech.sirwellington.alchemy.generator.one
import tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat
import tech.sirwellington.alchemy.test.junit.runners.GenerateString
import tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.ALPHABETIC
import tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.ALPHANUMERIC
import tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID
import tech.sirwellington.alchemy.test.junit.runners.Repeat

@RunWith(AlchemyTestRunner::class)
@Repeat
class SQLCredentialRepositoryTest
{

    @Mock
    private lateinit var database: JdbcOperations

    @GenerateString(UUID)
    private lateinit var userId: String

    @GenerateString(ALPHANUMERIC)
    private lateinit var encryptedPassword: String

    @GenerateString(ALPHABETIC)
    private lateinit var badId: String

    private lateinit var instance: SQLCredentialRepository

    @Before
    fun setUp()
    {
        setupData()
        setupMocks()

        instance = SQLCredentialRepository(database)
    }

    @Test
    fun testSaveEncryptedPassword()
    {
        instance.saveEncryptedPassword(userId, encryptedPassword)

        val sql = Inserts.CREDENTIAL

        verify(database).update(sql, userId.toUUID(), encryptedPassword)
    }

    @DontRepeat
    @Test
    fun testSaveEncryptedPasswordWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.saveEncryptedPassword(userId, encryptedPassword) }.operationError()
    }

    @DontRepeat
    @Test
    fun testSaveEncryptedPasswordWithBadArgs()
    {
        assertThrows { instance.saveEncryptedPassword("", encryptedPassword) }.invalidArg()
        assertThrows { instance.saveEncryptedPassword(badId, encryptedPassword) }.invalidArg()
        assertThrows { instance.saveEncryptedPassword(userId, "") }.invalidArg()
    }

    @Test
    fun testContainsEncryptedPassword()
    {
        val sql = Queries.CHECK_CREDENTIAL
        val expected = one(booleans())

        whenever(database.queryForObject(sql, Boolean::class.java, userId.toUUID()))
                .thenReturn(expected)

        val result = instance.containsEncryptedPassword(userId)

        assertThat(result, equalTo(expected))
    }


    @DontRepeat
    @Test
    fun testContainsEncryptedPasswordWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.containsEncryptedPassword(userId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testContainsEncryptedPasswordWithBadArgs()
    {
        assertThrows { instance.containsEncryptedPassword("") }.invalidArg()
        assertThrows { instance.containsEncryptedPassword(badId) }.invalidArg()
    }

    @Test
    fun testGetEncryptedPassword()
    {
        val sql = Queries.SELECT_CREDENTIAL

        whenever(database.queryForObject(sql, String::class.java, userId.toUUID()))
                .thenReturn(encryptedPassword)

        val result = instance.getEncryptedPassword(userId)
        assertThat(result, equalTo(encryptedPassword))
    }

    @DontRepeat
    @Test
    fun testGetEncryptedPasswordWhenNotExists()
    {
        val sql = Queries.SELECT_CREDENTIAL

        whenever(database.queryForObject(sql, String::class.java, userId.toUUID()))
                .thenThrow(EmptyResultDataAccessException(1))

        assertThrows { instance.getEncryptedPassword(userId) }.doesNotExist()

    }

    @DontRepeat
    @Test
    fun testGetEncryptedPasswordWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.getEncryptedPassword(userId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testGetEncryptedPasswordWithBadArgs()
    {
        assertThrows { instance.getEncryptedPassword("") }.invalidArg()
        assertThrows { instance.getEncryptedPassword(badId) }.invalidArg()
    }

    @Test
    fun testDeleteEncryptedPassword()
    {
        val sql = Deletes.CREDENTIAL

        instance.deleteEncryptedPassword(userId)

        verify(database).update(sql, userId.toUUID())
    }

    @DontRepeat
    @Test
    fun testDeleteEncryptedPasswordWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.deleteEncryptedPassword(userId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testDeleteEncryptedPasswordWithBadArgs()
    {
        assertThrows { instance.deleteEncryptedPassword("") }.invalidArg()
        assertThrows { instance.deleteEncryptedPassword(badId) }.invalidArg()
    }

    private fun setupData()
    {

    }

    private fun setupMocks()
    {
    }

}