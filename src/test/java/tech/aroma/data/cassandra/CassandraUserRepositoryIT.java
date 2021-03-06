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

package tech.aroma.data.cassandra;

 import java.util.List;
 import java.util.Map;
 import java.util.function.Function;

 import com.datastax.driver.core.Row;
 import com.datastax.driver.core.Session;
 import org.apache.thrift.TException;
 import org.junit.*;
 import org.junit.runner.RunWith;
 import sir.wellington.alchemy.collections.maps.Maps;
 import sir.wellington.alchemy.collections.sets.Sets;
 import tech.aroma.thrift.User;
 import tech.aroma.thrift.exceptions.UserDoesNotExistException;
 import tech.sirwellington.alchemy.annotations.testing.IntegrationTest;
 import tech.sirwellington.alchemy.annotations.testing.TimeSensitive;
 import tech.sirwellington.alchemy.test.junit.runners.*;

 import static java.util.stream.Collectors.toList;
 import static org.hamcrest.Matchers.*;
 import static org.junit.Assert.assertThat;
 import static tech.aroma.data.assertions.RequestAssertions.isNullOrEmpty;
 import static tech.sirwellington.alchemy.generator.AlchemyGenerator.Get.one;
 import static tech.sirwellington.alchemy.generator.StringGenerators.uuids;
 import static tech.sirwellington.alchemy.test.junit.ThrowableAssertion.*;
 import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID;

/**
 *
 * @author SirWellington
 */
@IntegrationTest
@Repeat(10)
@RunWith(AlchemyTestRunner.class)
public class CassandraUserRepositoryIT
{
    
    private static Session session;
    
    @BeforeClass
    public static void begin()
    {
        session = TestCassandraProviders.getTestSession();
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
        
        instance = new CassandraUserRepository(session, userMapper);
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
        
        deleteUsers(users);
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
    
    @DontRepeat
    @TimeSensitive
    @Test
    public void testSaveUserTwice() throws Exception
    {
        instance.saveUser(user);
        
        Thread.sleep(5);
        instance.saveUser(user);
    }
    
    @Test
    public void testSaveUserWithoutGithub() throws Exception
    {
        user.unsetGithubProfile();
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
        instance.saveUser(user);
        
        User result = instance.findByGithubProfile(user.githubProfile);
        assertMostlyMatch(result, user);
    }
    
    @DontRepeat
    @Test
    public void testWithMultipleUsers() throws Exception
    {
        
        users = users.stream()
            .map(u -> u.setUserId(one(uuids)))
            .collect(toList());
        
        saveUsers(users);
    }
    
    private void assertMostlyMatch(User result, User user)
    {
        assertThat(result, notNullValue());
        assertThat(result.userId, is(user.userId));
        assertThat(result.email, is(user.email));
        assertThat(result.firstName, is(user.firstName));
        assertThat(result.middleName, is(user.middleName));
        assertThat(result.lastName, is(user.lastName));
        
        if (!Sets.isEmpty(result.roles))
        {
            assertThat(result.roles, is(user.roles));
        }
        
        if (!isNullOrEmpty(result.githubProfile))
        {
            assertThat(result.githubProfile, is(user.githubProfile));
        }
        
    }

    @Test
    public void testGetRecentlyCreatedUsers() throws Exception
    {
        instance.saveUser(user);
        
        List<User> result = instance.getRecentlyCreatedUsers();
        
        Map<String, User> mapping = result.stream()
            .collect(() -> Maps.<String, User>create(), 
                     (map, user) -> map.put(user.userId, user),
                     (left, right) -> left.putAll(right));
        
        assertThat(result, notNullValue());
        assertThat(result, not(empty()));
        
        if (mapping.containsKey(user.userId))
        {
            User savedUser = mapping.get(user.userId);
            assertMostlyMatch(savedUser, user);
        }
        
    }
    
}
