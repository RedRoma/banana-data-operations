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
import org.springframework.jdbc.core.JdbcOperations
import tech.aroma.data.AromaGenerators.Applications
import tech.aroma.data.invalidArg
import tech.aroma.data.operationError
import tech.aroma.data.sql.SQLStatements.Deletes
import tech.aroma.data.sql.SQLStatements.Inserts
import tech.aroma.data.sql.SQLStatements.Queries
import tech.aroma.thrift.Application
import tech.aroma.thrift.User
import tech.aroma.thrift.generators.UserGenerators.users
import tech.sirwellington.alchemy.generator.BooleanGenerators.Companion.booleans
import tech.sirwellington.alchemy.generator.CollectionGenerators.Companion.listOf
import tech.sirwellington.alchemy.generator.StringGenerators.Companion.uuids
import tech.sirwellington.alchemy.generator.one
import tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat
import tech.sirwellington.alchemy.test.junit.runners.GenerateString
import tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.ALPHABETIC
import tech.sirwellington.alchemy.test.junit.runners.Repeat

@RunWith(AlchemyTestRunner::class)
@Repeat
class SQLFollowerRepositoryTest
{
    @Mock
    private lateinit var database: JdbcOperations

    @Mock
    private lateinit var userSerializer: DatabaseSerializer<User>

    @Mock
    private lateinit var appSerializer: DatabaseSerializer<Application>


    private lateinit var user: User
    private lateinit var app: Application

    private val userId get() = user.userId
    private val appId get() = app.applicationId

    private lateinit var appIds: List<String>
    private lateinit var userIds: List<String>

    private val apps get() = appIds.map { Application().setApplicationId(it) }
    private val users get() = userIds.map { User().setUserId(it) }

    @GenerateString(ALPHABETIC)
    private lateinit var badId: String

    private lateinit var instance: SQLFollowerRepository

    @Before
    fun setUp()
    {

        setupData()
        setupMocks()

        instance = SQLFollowerRepository(database, appSerializer, userSerializer)
    }

    @Test
    fun testSaveFollowing()
    {
        val sql = Inserts.FOLLOWING

        instance.saveFollowing(user, app)

        verify(database).update(sql, appId.toUUID(), userId.toUUID())
    }

    @DontRepeat
    @Test
    fun testSaveFollowingWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.saveFollowing(user, app) }.operationError()
    }

    @DontRepeat
    @Test
    fun testSaveFollowingWithBadArgs()
    {
        assertThrows {
            instance.saveFollowing(User(), app)
        }.invalidArg()

        assertThrows {
            instance.saveFollowing(user, Application())
        }.invalidArg()

        assertThrows {
            val badUser = User().setUserId(badId)
            instance.saveFollowing(badUser, app)
        }.invalidArg()

        assertThrows {
            val badApp = Application().setApplicationId(badId)
            instance.saveFollowing(user, badApp)
        }.invalidArg()

    }

    @Test
    fun testDeleteFollowing()
    {
        val sql = Deletes.FOLLOWING

        instance.deleteFollowing(userId, appId)

        verify(database).update(sql, appId.toUUID(), userId.toUUID())
    }

    @DontRepeat
    @Test
    fun testDeleteFollowingWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.deleteFollowing(userId, appId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testDeleteFollowingWithBadArgs()
    {
        assertThrows { instance.deleteFollowing("", appId) }.invalidArg()
        assertThrows { instance.deleteFollowing(badId, appId) }.invalidArg()
        assertThrows { instance.deleteFollowing(userId, "") }.invalidArg()
        assertThrows { instance.deleteFollowing(userId, badId) }.invalidArg()
    }

    @Test
    fun testFollowingExists()
    {
        val sql = Queries.CHECK_FOLLOWING_EXISTS
        val expected = one(booleans())

        whenever(database.queryForObject(sql, Boolean::class.java, appId.toUUID(), userId.toUUID()))
                .thenReturn(expected)

        val result = instance.followingExists(userId, appId)
        assertThat(result, equalTo(expected))
    }

    @DontRepeat
    @Test
    fun testFollowingExistsWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.followingExists(userId, appId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testFollowingExistsWithBadArgs()
    {
        assertThrows { instance.followingExists("", appId) }.invalidArg()
        assertThrows { instance.followingExists(badId, appId) }.invalidArg()
        assertThrows { instance.followingExists(userId, "") }.invalidArg()
        assertThrows { instance.followingExists(userId, badId) }.invalidArg()
    }

    @Test
    fun testGetApplicationsFollowedBy()
    {
        val sql = Queries.SELECT_APPS_FOLLOWING

        whenever(database.query(sql, appSerializer, userId.toUUID()))
                .thenReturn(apps)

        val results = instance.getApplicationsFollowedBy(userId)
        assertThat(results, equalTo(apps))
    }

    @DontRepeat
    @Test
    fun testGetApplicationsFollowedByWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.getApplicationsFollowedBy(userId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testGetApplicationsFollowedByWithBadArgs()
    {
        assertThrows { instance.getApplicationsFollowedBy("") }.invalidArg()
        assertThrows { instance.getApplicationsFollowedBy(badId) }.invalidArg()
    }

    @Test
    fun testGetApplicationFollowers()
    {
        val sql = Queries.SELECT_APP_FOLLOWERS

        whenever(database.query(sql, userSerializer, appId.toUUID()))
                .thenReturn(users)

        val results = instance.getApplicationFollowers(appId)
        assertThat(results, equalTo(users))
    }

    @DontRepeat
    @Test
    fun testGetApplicationFollowersWhenFails()
    {
        database.setupForFailure()

        assertThrows { instance.getApplicationFollowers(appId) }.operationError()
    }

    @DontRepeat
    @Test
    fun testGetApplicationFollowersWithBadArgs()
    {
        assertThrows { instance.getApplicationFollowers("") }.invalidArg()
        assertThrows { instance.getApplicationFollowers(badId) }.invalidArg()
    }

    private fun setupData()
    {
        user = one(users())
        app = Applications.application

        userIds = listOf(uuids(), 10)
        appIds = listOf(uuids(), 10)
    }

    private fun setupMocks()
    {

    }

}