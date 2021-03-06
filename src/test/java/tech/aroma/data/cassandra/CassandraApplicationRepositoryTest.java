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

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import com.datastax.driver.core.*;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import sir.wellington.alchemy.collections.lists.Lists;
import tech.aroma.thrift.Application;
import tech.aroma.thrift.exceptions.InvalidArgumentException;
import tech.sirwellington.alchemy.test.junit.runners.*;

import static java.util.UUID.fromString;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.*;
import static sir.wellington.alchemy.collections.sets.Sets.toSet;
import static tech.aroma.data.cassandra.Tables.Applications.*;
import static tech.aroma.thrift.generators.ApplicationGenerators.applications;
import static tech.sirwellington.alchemy.generator.AlchemyGenerator.Get.one;
import static tech.sirwellington.alchemy.generator.CollectionGenerators.listOf;
import static tech.sirwellington.alchemy.generator.NumberGenerators.longs;
import static tech.sirwellington.alchemy.generator.StringGenerators.alphabeticStrings;
import static tech.sirwellington.alchemy.generator.StringGenerators.uuids;
import static tech.sirwellington.alchemy.test.junit.ThrowableAssertion.*;
import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID;

/**
 *
 * @author SirWellington
 */
@Repeat(100)
@RunWith(AlchemyTestRunner.class)
public class CassandraApplicationRepositoryTest
{

    @Mock(answer = RETURNS_MOCKS)
    private Session session;

    @Captor
    private ArgumentCaptor<Statement> captor;

    private CassandraApplicationRepository instance;

    private Application app;

    @GenerateString(UUID)
    private String appId;

    @GenerateString(UUID)
    private String orgId;

    @GenerateString(UUID)
    private String ownerId;

    private Row mockRow;
    
    @Mock
    private Function<Row, Application> appMapper;

    @Before
    public void setUp()
    {
        app = one(applications());
        appId = app.applicationId;
        app.organizationId = orgId;
        
        mockRow = mockRowFor(app);

        List<String> owners = listOf(uuids, 5);
        app.setOwners(toSet(owners));

        instance = new CassandraApplicationRepository(session, appMapper);
        
        when(appMapper.apply(mockRow))
            .thenReturn(app);
    }

    @DontRepeat
    @Test
    public void testConstructor()
    {
        assertThrows(() -> new CassandraApplicationRepository(session, null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThrows(() -> new CassandraApplicationRepository(null, appMapper))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSaveApplication() throws Exception
    {
        instance.saveApplication(app);
        verify(session).execute(captor.capture());

        Statement statement = captor.getValue();
        assertThat(statement, notNullValue());
        assertThat(statement, is(instanceOf(BatchStatement.class)));

    }
    
    @DontRepeat
    @Test
    public void testSaveApplicationWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.saveApplication(app))
            .isInstanceOf(TException.class);
    }
    
    @Test
    public void testSaveApplicationWhenOrgIdMissing() throws Exception
    {
        app.unsetOrganizationId();
        
        instance.saveApplication(app);
        verify(session).execute(captor.capture());
        
        Statement statement = captor.getValue();
        assertThat(statement, is(instanceOf(BatchStatement.class)));
    }

    @Test
    public void testDeleteApplication() throws Exception
    {
        ResultSet results = mock(ResultSet.class);
        
        when(results.one())
            .thenReturn(mockRow);
        
        when(appMapper.apply(mockRow))
            .thenReturn(app);
        
        when(session.execute(Mockito.any(Statement.class)))
            .thenReturn(results);
        
        instance.deleteApplication(appId);
    }
    
    @DontRepeat
    @Test
    public void testDeleteApplicationWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.deleteApplication(appId))
            .isInstanceOf(TException.class);
    }

    @DontRepeat
    @Test
    public void testDeleteApplicationWithBadArgs() throws Exception
    {
        String empty = "";
        assertThrows(() -> instance.deleteApplication(empty))
            .isInstanceOf(InvalidArgumentException.class);

        String badId = one(alphabeticStrings());
        assertThrows(() -> instance.deleteApplication(badId))
            .isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    public void testGetById() throws Exception
    {
        ResultSet results = mock(ResultSet.class);
        when(results.one()).thenReturn(mockRow);
        when(session.execute(Mockito.any(Statement.class)))
            .thenReturn(results);

        Application result = instance.getById(appId);

        verify(session).execute(captor.capture());

        Statement statementMade = captor.getValue();
        assertThat(statementMade, notNullValue());

        assertThat(result.applicationId, is(appId));
    }
    
    @DontRepeat
    @Test
    public void testGetByIdWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.getById(appId))
            .isInstanceOf(TException.class);
    }

    @Test
    public void testContainsApplication() throws Exception
    {
        ResultSet results = mock(ResultSet.class);
        when(session.execute(Mockito.any(Statement.class)))
            .thenReturn(results);

        Row fakeRow = mock(Row.class);
        when(results.one()).thenReturn(fakeRow);

        long count = one(longs(0, 2));
        when(fakeRow.getLong(0)).thenReturn(count);

        boolean result = instance.containsApplication(appId);
        boolean expected = count > 0L;
        assertThat(result, is(expected));
    }

    @DontRepeat
    @Test
    public void testContainsApplicationWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.containsApplication(appId))
            .isInstanceOf(TException.class);
    }

    @Test
    public void testGetApplicationsOwnedBy() throws Exception
    {
        setupWithEmptyResults();

        List<Application> apps = instance.getApplicationsOwnedBy(ownerId);
        assertThat(apps, notNullValue());
        assertThat(apps, is(empty()));

    }
    
    @DontRepeat
    @Test
    public void testGetApplicationsOwnedByWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.getApplicationsOwnedBy(ownerId))
            .isInstanceOf(TException.class);
    }

    @Test
    public void testGetApplicationsByOrg() throws Exception
    {
        setupWithEmptyResults();

        List<Application> apps = instance.getApplicationsByOrg(orgId);
        assertThat(apps, notNullValue());
        assertThat(apps, is(empty()));
    }

    @DontRepeat
    @Test
    public void testGetApplicationsByOrgWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.getApplicationsByOrg(orgId))
            .isInstanceOf(TException.class);
    }

    @Test
    public void testSearchByName() throws Exception
    {
        setupWithEmptyResults();

        List<Application> apps = instance.getApplicationsByOrg(orgId);
        assertThat(apps, notNullValue());
        assertThat(apps, is(empty()));
    }

    @Test
    public void testGetRecentlyCreated() throws Exception
    {
        setupWithEmptyResults();
        
        List<Application> apps = instance.getRecentlyCreated();
        assertThat(apps, notNullValue());
        assertThat(apps, is(empty()));
    }

    @DontRepeat
    @Test
    public void testGetRecentlyCreatedWhenFails() throws Exception
    {
        setupForFailure();
        
        assertThrows(() -> instance.getRecentlyCreated())
            .isInstanceOf(TException.class);
    }

    private Row mockRowFor(Application app)
    {
        Row row = mock(Row.class);

        when(row.getUUID(APP_ID))
            .thenReturn(fromString(app.applicationId));
        
        when(row.getUUID(ICON_MEDIA_ID))
            .thenReturn(fromString(app.applicationIconMediaId));

        when(row.getUUID(ORG_ID))
            .thenReturn(fromString(app.organizationId));

        when(row.getString(APP_NAME))
            .thenReturn(app.name);

        when(row.getString(APP_DESCRIPTION))
            .thenReturn(app.applicationDescription);

        when(row.getTimestamp(TIME_PROVISIONED))
            .thenReturn(new Date(app.timeOfProvisioning));

        return row;
    }

    private void setupWithEmptyResults()
    {
        ResultSet results = mock(ResultSet.class);
        List<Row> list = Lists.emptyList();
        when(results.iterator()).thenReturn(list.iterator());

        when(session.execute(Mockito.any(Statement.class)))
            .thenReturn(results);
    }

    private void setupForFailure()
    {
        when(session.execute(Mockito.any(Statement.class)))
            .thenThrow(new IllegalArgumentException());
    }

}
