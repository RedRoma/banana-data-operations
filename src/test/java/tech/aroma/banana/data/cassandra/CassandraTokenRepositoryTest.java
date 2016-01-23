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
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import tech.aroma.banana.thrift.authentication.AuthenticationToken;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat;
import tech.sirwellington.alchemy.test.junit.runners.GeneratePojo;
import tech.sirwellington.alchemy.test.junit.runners.GenerateString;
import tech.sirwellington.alchemy.test.junit.runners.Repeat;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.when;
import static tech.sirwellington.alchemy.generator.AlchemyGenerator.one;
import static tech.sirwellington.alchemy.generator.NumberGenerators.positiveLongs;
import static tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows;
import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID;

/**
 *
 * @author SirWellington
 */
@Repeat(10)
@RunWith(AlchemyTestRunner.class)
public class CassandraTokenRepositoryTest 
{

    @Mock
    private Session cassandra;
    
    @Mock(answer = RETURNS_MOCKS)
    private Cluster cluster;
    
    private QueryBuilder queryBuilder;
    
    @Mock
    private Function<Row, AuthenticationToken> tokenMapper;
    
    private CassandraTokenRepository instance;
    
    @GeneratePojo
    private AuthenticationToken token;
    
    @GenerateString(UUID)
    private String ownerId;
    
    @GenerateString(UUID)
    private String orgId;
    
    @GenerateString(UUID)
    private String tokenId;
    
    @Mock
    private ResultSet results;
    
    @Mock
    private Row row;
    
    
    @Before
    public void setUp()
    {
        queryBuilder = new QueryBuilder(cluster);
        
        instance = new CassandraTokenRepository(cassandra, queryBuilder, tokenMapper);
        
        token.tokenId = tokenId;
        token.ownerId = ownerId;
        token.organizationId = orgId;
        
        when(cassandra.execute(Mockito.any(Statement.class))).thenReturn(results);
        when(results.one()).thenReturn(row);
    }
    
    @DontRepeat
    @Test
    public void testConstructor()
    {
        assertThrows(() -> new CassandraTokenRepository(null, queryBuilder, tokenMapper));
        assertThrows(() -> new CassandraTokenRepository(cassandra, null, tokenMapper));
        assertThrows(() -> new CassandraTokenRepository(cassandra, queryBuilder, null));
    }

    @Test
    public void testContainsToken() throws Exception
    {
        when(row.getLong(0)).thenReturn(0L);
        
        assertThat(instance.containsToken(tokenId), is(false));
        
        long count = one(positiveLongs());
        when(row.getLong(0)).thenReturn(count);
        
        assertThat(instance.containsToken(tokenId), is(true));
    }

    @Test
    public void testGetToken() throws Exception
    {
    }

    @Test
    public void testSaveToken() throws Exception
    {
    }

    @Test
    public void testGetTokensBelongingTo() throws Exception
    {
    }

    @Test
    public void testDeleteToken() throws Exception
    {
    }

}