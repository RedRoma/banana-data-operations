/*
 * Copyright 2016 Aroma Tech.
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

package tech.aroma.banana.data.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.util.List;
import java.util.function.Function;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import sir.wellington.alchemy.collections.sets.Sets;
import tech.aroma.banana.thrift.User;
import tech.aroma.banana.thrift.exceptions.UserDoesNotExistException;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;
import tech.sirwellington.alchemy.test.junit.runners.GenerateList;
import tech.sirwellington.alchemy.test.junit.runners.GeneratePojo;
import tech.sirwellington.alchemy.test.junit.runners.GenerateString;
import tech.sirwellington.alchemy.test.junit.runners.Repeat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows;
import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID;

/**
 *
 * @author SirWellington
 */
@Repeat(10)
@RunWith(AlchemyTestRunner.class)
public class CassandraUserRepositoryIT
{

    private static Cluster cluster;
    private static Session session;
    private static QueryBuilder queryBuilder;

    @BeforeClass
    public static void begin()
    {
        cluster = TestSessions.createTestCluster();
        session = TestSessions.createTestSession(cluster);
        queryBuilder = TestSessions.createQueryBuilder(cluster);
    }

    @AfterClass
    public static void end()
    {
        session.close();
        cluster.close();
    }

    @GeneratePojo
    private User user;

    @GenerateString(UUID)
    private String userId;

    @GenerateList(User.class)
    private List<User> users;

    private final Function<Row, User> userMapper = Mappers.userMapper();
    private CassandraUserRepository instance;

    @Before
    public void setUp()
    {
        user.userId = userId;

        instance = new CassandraUserRepository(session, queryBuilder, userMapper);
    }

    @After
    public void cleanUp() throws TException
    {
        try
        {
            instance.deleteUser(userId);
        }
        catch (Exception ex)
        {
            System.out.println("Could not delete User: " + userId);
        }
    }

    private void saveUsers(List<User> users) throws TException
    {
        for (User user : users)
        {
            instance.saveUser(user);
        }
    }

    private void deleteUsers(List<User> users) throws TException
    {
        for (User user : users)
        {
            try
            {
                instance.deleteUser(user.userId);
            }
            catch (Exception ex)
            {
                System.out.println("Could not delete User: " + user.userId);
            }
        }
    }

    @Test
    public void testSaveUser() throws Exception
    {
        instance.saveUser(user);

        assertThat(instance.containsUser(userId), is(true));
    }

    @Test
    public void testGetUser() throws Exception
    {
        instance.saveUser(user);
        
        User result = instance.getUser(userId);
        
        assertMostlyMatch(result, user);
    }
    
    @Test
    public void testGetUserWhenNotExist() throws Exception
    {
        assertThrows(() -> instance.getUser(userId))
            .isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    public void testDeleteUser() throws Exception
    {
        instance.saveUser(user);
        
        instance.deleteUser(userId);
        
        assertThat(instance.containsUser(userId), is(false));
    }
    
    @Test
    public void testDeleteUserWhenNotExists() throws Exception
    {
        assertThrows(() -> instance.deleteUser(userId))
            .isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    public void testContainsUser() throws Exception
    {
        boolean result = instance.containsUser(userId);
        assertThat(result, is(false));
        
        instance.saveUser(user);
        
        result = instance.containsUser(userId);
        assertThat(result, is(true));
    }

    @Test
    public void testGetUserByEmail() throws Exception
    {
        instance.saveUser(user);
        
        User result = instance.getUserByEmail(user.email);
        assertMostlyMatch(result, user);
    }

    @Test
    public void testFindByGithubProfile() throws Exception
    {
    }

    private void assertMostlyMatch(User result, User user)
    {
        assertThat(result, notNullValue());
        assertThat(result.userId, is(user.userId));
        assertThat(result.email, is(user.email));
        assertThat(result.firstName, is(user.firstName));
        assertThat(result.middleName, is(user.middleName));
        assertThat(result.lastName, is(user.lastName));
        
        if(!Sets.isEmpty(result.roles))
        {
            assertThat(result.roles, is(user.roles));
        }
        
    }

}
