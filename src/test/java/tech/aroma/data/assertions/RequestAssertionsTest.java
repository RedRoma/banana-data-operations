
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

package tech.aroma.data.assertions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import sir.wellington.alchemy.collections.lists.Lists;
import tech.aroma.thrift.Application;
import tech.aroma.thrift.Message;
import tech.aroma.thrift.Organization;
import tech.aroma.thrift.User;
import tech.aroma.thrift.authentication.ApplicationToken;
import tech.aroma.thrift.authentication.AuthenticationToken;
import tech.aroma.thrift.authentication.UserToken;
import tech.sirwellington.alchemy.arguments.AlchemyAssertion;
import tech.sirwellington.alchemy.arguments.FailedAssertionException;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;
import tech.sirwellington.alchemy.test.junit.runners.DontRepeat;
import tech.sirwellington.alchemy.test.junit.runners.GeneratePojo;
import tech.sirwellington.alchemy.test.junit.runners.GenerateString;
import tech.sirwellington.alchemy.test.junit.runners.Repeat;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static tech.sirwellington.alchemy.generator.AlchemyGenerator.one;
import static tech.sirwellington.alchemy.generator.CollectionGenerators.listOf;
import static tech.sirwellington.alchemy.generator.StringGenerators.alphabeticString;
import static tech.sirwellington.alchemy.generator.StringGenerators.uuids;
import static tech.sirwellington.alchemy.test.junit.ThrowableAssertion.assertThrows;
import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.ALPHABETIC;
import static tech.sirwellington.alchemy.test.junit.runners.GenerateString.Type.UUID;

/**
 *
 * @author SirWellington
 */
@Repeat(100)
@RunWith(AlchemyTestRunner.class)
public class RequestAssertionsTest
{

    @GenerateString(UUID)
    private String validId;

    @GenerateString(ALPHABETIC)
    private String invalidId;

    @GeneratePojo
    private ApplicationToken appToken;

    @GeneratePojo
    private UserToken userToken;

    @GeneratePojo
    private AuthenticationToken authToken;
    
    @GeneratePojo
    private Application app;

    @GeneratePojo
    private Message message;

    @GeneratePojo
    private Organization organization;

    @GeneratePojo
    private User user;

    @GenerateString
    private String string;

    @Before
    public void setUp()
    {
        app.applicationId = validId;
        app.organizationId = validId;
        message.messageId = validId;
        message.applicationId = validId;
        user.userId = validId;
        organization.organizationId = validId;

        organization.owners = Lists.createFrom(validId);
    }

    @DontRepeat
    @Test
    public void testConstuctor()
    {
        assertThrows(() -> new RequestAssertions())
            .isInstanceOf(IllegalAccessException.class);
    }

    @Test
    public void testValidApplication()
    {
        AlchemyAssertion<Application> assertion = RequestAssertions.validApplication();
        assertThat(assertion, notNullValue());

        assertion.check(app);

        assertThrows(() -> assertion.check(null))
            .isInstanceOf(FailedAssertionException.class);

        Application empty = new Application();
        assertThrows(() -> assertion.check(empty))
            .isInstanceOf(FailedAssertionException.class);

        Application appWithInvalidId = new Application(app)
            .setApplicationId(this.invalidId);

        assertThrows(() -> assertion.check(appWithInvalidId))
            .isInstanceOf(FailedAssertionException.class);
    }

    @Test
    public void testValidUser()
    {
        AlchemyAssertion<User> assertion = RequestAssertions.validUser();
        assertThat(assertion, notNullValue());

        assertion.check(user);

        assertThrows(() -> assertion.check(null))
            .isInstanceOf(FailedAssertionException.class);

        assertThrows(() -> assertion.check(new User()))
            .isInstanceOf(FailedAssertionException.class);

        User userWithInvalidId = new User(user)
            .setUserId(invalidId);

        assertThrows(() -> assertion.check(userWithInvalidId))
            .isInstanceOf(FailedAssertionException.class);
    }

    @Test
    public void testIsNullOrEmpty()
    {
        assertThat(RequestAssertions.isNullOrEmpty(string), is(false));
        assertThat(RequestAssertions.isNullOrEmpty(""), is(true));
        assertThat(RequestAssertions.isNullOrEmpty(null), is(true));
    }

    @Test
    public void testValidMessage()
    {
        AlchemyAssertion<Message> assertion = RequestAssertions.validMessage();
        assertThat(assertion, notNullValue());

        assertion.check(message);
    }

    @DontRepeat
    @Test
    public void testValidMessageWithBadMessages()
    {
        AlchemyAssertion<Message> assertion = RequestAssertions.validMessage();

        assertThrows(() -> assertion.check(null))
            .isInstanceOf(FailedAssertionException.class);

        Message emptyMessage = new Message();
        assertThrows(() -> assertion.check(emptyMessage))
            .isInstanceOf(FailedAssertionException.class);

        Message messageWithoutTitle = emptyMessage.setMessageId(one(uuids));
        assertThrows(() -> assertion.check(messageWithoutTitle))
            .isInstanceOf(FailedAssertionException.class);

        Message messageWithInvalidId = new Message(message)
            .setMessageId(invalidId);
        assertThrows(() -> assertion.check(messageWithInvalidId))
            .isInstanceOf(FailedAssertionException.class);
        
        Message messageWithInvalidAppId = new Message(message)
        .setApplicationId(invalidId);
        assertThrows(() -> assertion.check(messageWithInvalidAppId))
            .isInstanceOf(FailedAssertionException.class);

    }

    @Test
    public void testValidApplicationId()
    {
        AlchemyAssertion<String> assertion = RequestAssertions.validApplicationId();
        assertThat(assertion, notNullValue());

        assertion.check(validId);

        assertThrows(() -> assertion.check(invalidId))
            .isInstanceOf(FailedAssertionException.class);
    }

    @Test
    public void testValidMessageId()
    {
        AlchemyAssertion<String> assertion = RequestAssertions.validMessageId();
        assertThat(assertion, notNullValue());

        assertion.check(validId);

        assertThrows(() -> assertion.check(invalidId))
            .isInstanceOf(FailedAssertionException.class);
    }

    @Test
    public void testValidUserId()
    {
        AlchemyAssertion<String> assertion = RequestAssertions.validUserId();
        assertThat(assertion, notNullValue());

        assertion.check(validId);

        assertThrows(() -> assertion.check(invalidId))
            .isInstanceOf(FailedAssertionException.class);
    }

    @Test
    public void testValidOrganization()
    {
        AlchemyAssertion<Organization> assertion = RequestAssertions.validOrganization();

        assertThrows(() -> assertion.check(null))
            .isInstanceOf(FailedAssertionException.class);

        Organization emptyOrg = new Organization();
        assertThrows(() -> assertion.check(emptyOrg))
            .isInstanceOf(FailedAssertionException.class);

        Organization orgWithoutName = emptyOrg.setOrganizationId(validId);
        assertThrows(() -> assertion.check(orgWithoutName))
            .isInstanceOf(FailedAssertionException.class);

        Organization orgWithInvalidId = new Organization(organization)
            .setOrganizationId(invalidId);
        assertThrows(() -> assertion.check(orgWithInvalidId))
            .isInstanceOf(FailedAssertionException.class);

        Organization orgWithBadOwners = new Organization(organization)
            .setOwners(listOf(alphabeticString(2)));
        assertThrows(() -> assertion.check(orgWithBadOwners))
            .isInstanceOf(FailedAssertionException.class);

    }

    @Test
    public void testValidOrgId()
    {
        AlchemyAssertion<String> assertion = RequestAssertions.validOrgId();
        assertThat(assertion, notNullValue());

        assertion.check(validId);

        assertThrows(() -> assertion.check(invalidId))
            .isInstanceOf(FailedAssertionException.class);
    }

    @Test
    public void testTokenContainingOwnerId()
    {
        AlchemyAssertion<AuthenticationToken> assertion = RequestAssertions.tokenContainingOwnerId();
        assertThat(assertion, notNullValue());
    
        assertion.check(authToken);
    }

    @Test
    public void testTokenContainingOwnerIdWithBadArgs()
    {
        AlchemyAssertion<AuthenticationToken> assertion = RequestAssertions.tokenContainingOwnerId();
        assertThat(assertion, notNullValue());

        assertThrows(() -> assertion.check(null))
            .isInstanceOf(FailedAssertionException.class);

        AuthenticationToken emptyToken = new AuthenticationToken();
        assertThrows(() -> assertion.check(emptyToken))
            .isInstanceOf(FailedAssertionException.class);

    }

}