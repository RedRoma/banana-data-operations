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

import java.util.*;
import java.util.function.Function;
import javax.inject.Inject;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sir.wellington.alchemy.collections.lists.Lists;
import sir.wellington.alchemy.collections.sets.Sets;
import tech.aroma.data.OrganizationRepository;
import tech.aroma.data.cassandra.Tables.Organizations;
import tech.aroma.data.cassandra.Tables.Users;
import tech.aroma.thrift.*;
import tech.aroma.thrift.exceptions.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static tech.aroma.data.assertions.RequestAssertions.*;
import static tech.aroma.data.cassandra.Tables.Organizations.*;
import static tech.sirwellington.alchemy.arguments.Arguments.*;
import static tech.sirwellington.alchemy.arguments.assertions.Assertions.notNull;

/**
 *
 * @author SirWellington
 */
final class CassandraOrganizationRepository implements OrganizationRepository
{
    
    private final static Logger LOG = LoggerFactory.getLogger(CassandraOrganizationRepository.class);
    
    private final Session cassandra;
    private final Function<Row, Organization> organizationMapper;
    private final Function<Row, User> userMapper;
    
    @Inject
    CassandraOrganizationRepository(Session cassandra,
                                    Function<Row, Organization> organizationMapper,
                                    Function<Row, User> userMapper)
    {
        checkThat(cassandra, organizationMapper, userMapper)
            .are(notNull());
        
        this.cassandra = cassandra;
        this.organizationMapper = organizationMapper;
        this.userMapper = userMapper;
    }
    
    @Override
    public void saveOrganization(Organization organization) throws TException
    {
        checkThat(organization)
            .throwing(InvalidArgumentException.class)
            .is(validOrganization());
        
        Statement insertStatement = createStatementToInsert(organization);
        
        try
        {
            cassandra.execute(insertStatement);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to save Organization in Cassandra: [{}]", organization, ex);
            throw new OperationFailedException("Failed to save Organization: " + ex.getMessage());
        }
    }
    
    @Override
    public Organization getOrganization(String organizationId) throws TException
    {
        checkOrganizationId(organizationId);
        
        Statement query = createQueryToGetOrganization(organizationId);
        
        ResultSet results;
        
        try
        {
            results = cassandra.execute(query);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to query for Organization [{}]", organizationId, ex);
            throw new OperationFailedException("Could not get Organization: " + ex.getMessage());
        }
        
        Row row = results.one();
        checkRowIsPresent(row);
        
        Organization org = organizationMapper.apply(row);
        return org;
    }
    
    @Override
    public void deleteOrganization(String organizationId) throws TException
    {
        deleteAllMembers(organizationId);
        
        Statement deleteStatement = createStatementToDelete(organizationId);
        
        try
        {
            cassandra.execute(deleteStatement);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to delete Organization in Cassandra: [{}]", organizationId, ex);
            throw new OperationFailedException("Failed to delete Organization: " + ex.getMessage());
        }
    }
    
    @Override
    public boolean containsOrganization(String organizationId) throws TException
    {
        checkOrganizationId(organizationId);
        
        Statement query = createQueryToCheckIfOrgExists(organizationId);
        
        ResultSet results;
        
        try
        {
            results = cassandra.execute(query);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to query for existence of Organization [{}]", organizationId, ex);
            throw new OperationFailedException("Could not query for Organization: " + ex.getMessage());
        }
        
        Row row = results.one();
        checkRowIsPresent(row);
        
        long count = row.getLong(0);
        return count > 0L;
    }
    
    @Override
    public List<User> getOrganizationMembers(String organizationId) throws TException
    {
        checkOrganizationId(organizationId);
        
        Statement query = createQueryToGetOrganizationMembers(organizationId);
        
        ResultSet results;
        
        try
        {
            results = cassandra.execute(query);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to query for Organization Members: [{}]", organizationId, ex);
            throw new OperationFailedException("Could not query for Organization Members: " + ex.getMessage());
        }
        
        List<User> members = Lists.create();
        
        for (Row row : results)
        {
            User member = userMapper.apply(row);
            members.add(member);
        }
        
        LOG.debug("Found {} members in Org [{]]", members.size(), organizationId);
        return members;
        
    }
    
    @Override
    public List<Organization> searchByName(String searchTerm) throws TException
    {
        throw new OperationFailedException("Organization Search not supported yet.");
    }
    
    @Override
    public void saveMemberInOrganization(String organizationId, User user) throws TException
    {
        checkOrganizationId(organizationId);
        
        checkThat(user)
            .throwing(InvalidArgumentException.class)
            .is(validUser());
        
        Statement insertStatement = createStatementToSaveMember(organizationId, user);
        
        try
        {
            cassandra.execute(insertStatement);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to save Member [{}] in Organization [{}] in Cassandra: [{}]", user, organizationId, ex);
            throw new OperationFailedException("Failed to save Member in Organization: " + ex.getMessage());
        }
        
    }
    
    @Override
    public boolean isMemberInOrganization(String organizationId, String userId) throws TException
    {
        checkOrganizationId(organizationId);
        checkUserId(userId);

        Statement query = createQueryToSeeIfMemberOfOrg(organizationId, userId);

        ResultSet results;

        try
        {
            results = cassandra.execute(query);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to query for existence of Member [{]] in Organization [{}]", userId, organizationId, ex);
            throw new OperationFailedException("Could not query for membership in Organization: " + ex.getMessage());
        }

        Row row = results.one();
        checkRowIsPresent(row);

        long count = row.getLong(0);
        return count > 0L;

    }
    
    @Override
    public List<User> getOrganizationOwners(String organizationId) throws TException
    {
        Organization org = this.getOrganization(organizationId);
        Set<String> owners = Sets.copyOf(org.owners);
        
        return owners
            .stream()
            .map(id -> new User().setUserId(id))
            .collect(toList());
    }
    
    @Override
    public void deleteMember(String organizationId, String userId) throws TException
    {
        checkOrganizationId(organizationId);
        checkUserId(userId);
        
        Statement deleteStatement = createStatmentToDeleteMember(organizationId, userId);
        
        try
        {
            cassandra.execute(deleteStatement);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to delete Member [{}] in Organization [{}]", userId, organizationId, ex);
            throw new OperationFailedException("Failed to delete Member in Organization: " + ex.getMessage());
        }
    }
    
    @Override
    public void deleteAllMembers(String organizationId) throws TException
    {
        checkOrganizationId(organizationId);
        
        Statement deleteStatement = createStatementToDeleteAllMembers(organizationId);
        
        try
        {
            cassandra.execute(deleteStatement);
        }
        catch (Exception ex)
        {
            LOG.error("Failed to delete all Members in Organization [{}]", organizationId, ex);
            throw new OperationFailedException("Failed to delete all members in Organization: " + ex.getMessage());
        }
    }
    
    private Statement createStatementToInsert(Organization org)
    {
        UUID orgUuid = UUID.fromString(org.organizationId);
        
        Set<UUID> owners = Lists.nullToEmpty(org.owners)
            .stream()
            .map(UUID::fromString)
            .collect(toSet());
        
        
        //Enums
        String industry = org.industry != null ? org.industry.toString() : null;
        String tier = org.tier != null ? org.tier.toString() : null;
        
        return QueryBuilder
            .insertInto(Organizations.TABLE_NAME)
            .value(ORG_ID, orgUuid)
            .value(ORG_NAME, org.organizationName)
            .value(OWNERS, owners)
            .value(ICON_LINK, org.logoLink)
            .value(INDUSTRY, industry)
            .value(EMAIL, org.organizationEmail)
            .value(GITHUB_PROFILE, org.githubProfile)
            .value(STOCK_NAME, org.stockMarketSymbol)
            .value(TIER, tier)
            .value(DESCRIPTION, org.organizationDescription)
            .value(WEBSITE, org.website);
    }
    
    private Statement createQueryToGetOrganization(String organizationId)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        
        return QueryBuilder
            .select()
            .all()
            .from(Organizations.TABLE_NAME)
            .where(eq(ORG_ID, orgUuid));
    }
    
    private void checkRowIsPresent(Row row) throws OperationFailedException, OrganizationDoesNotExistException
    {
        checkThat(row)
            .throwing(OrganizationDoesNotExistException.class)
            .usingMessage("Org Does not exist")
            .is(notNull());
    }
    
    private Statement createStatementToDelete(String organizationId)
    {
        UUID orgUuid = UUID.fromString(organizationId);

        return QueryBuilder
            .delete()
            .all()
            .from(Organizations.TABLE_NAME)
            .where(eq(ORG_ID, orgUuid));
    }
    
    private Statement createQueryToCheckIfOrgExists(String organizationId)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        
        return QueryBuilder
            .select()
            .countAll()
            .from(Organizations.TABLE_NAME)
            .where(eq(ORG_ID, orgUuid));
    }
    
    private Statement createQueryToGetOrganizationMembers(String organizationId)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        
        return QueryBuilder
            .select()
            .column(USER_ID).as(Users.USER_ID)
            .column(USER_FIRST_NAME).as(Users.FIRST_NAME)
            .column(USER_MIDDLE_NAME).as(Users.MIDDLE_NAME)
            .column(USER_LAST_NAME).as(Users.LAST_NAME)
            .column(USER_ROLES).as(Users.ROLES)
            .column(USER_EMAIL).as(Users.EMAIL)
            .from(Organizations.TABLE_NAME_MEMBERS)
            .where(eq(ORG_ID, orgUuid));
    }
    
    private Statement createStatementToSaveMember(String organizationId, User user)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        UUID userUuid = UUID.fromString(user.userId);
        
        //Enums
        Set<String> roles = Sets.nullToEmpty(user.roles)
            .stream()
            .map(Role::toString)
            .collect(toSet());
        
        return QueryBuilder
            .insertInto(Organizations.TABLE_NAME_MEMBERS)
            .value(ORG_ID, orgUuid)
            .value(USER_ID, userUuid)
            .value(USER_FIRST_NAME, user.firstName)
            .value(USER_LAST_NAME, user.lastName)
            .value(USER_ROLES, roles)
            .value(USER_EMAIL, user.email);
    }
    
    private Statement createQueryToSeeIfMemberOfOrg(String organizationId, String userId)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        UUID userUuid = UUID.fromString(userId);
        
        return QueryBuilder
            .select()
            .countAll()
            .from(Organizations.TABLE_NAME_MEMBERS)
            .where(eq(ORG_ID, orgUuid))
            .and(eq(USER_ID, userUuid));
    }
    
    private Statement createStatmentToDeleteMember(String organizationId, String userId)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        UUID userUuid = UUID.fromString(userId);
        
        return QueryBuilder
            .delete()
            .all()
            .from(Organizations.TABLE_NAME_MEMBERS)
            .where(eq(ORG_ID, orgUuid))
            .and(eq(USER_ID, userUuid));
    }
    
    private Statement createStatementToDeleteAllMembers(String organizationId)
    {
        UUID orgUuid = UUID.fromString(organizationId);
        
        return QueryBuilder
            .delete()
            .all()
            .from(Organizations.TABLE_NAME_MEMBERS)
            .where(eq(ORG_ID, orgUuid));
    }
    
    private void checkOrganizationId(String organizationId) throws InvalidArgumentException
    {
        checkThat(organizationId)
            .throwing(InvalidArgumentException.class)
            .is(validOrgId());
    }
    
    private void checkUserId(String userId) throws InvalidArgumentException
    {
        checkThat(userId)
            .throwing(InvalidArgumentException.class)
            .is(validUserId());
    }

    
}
