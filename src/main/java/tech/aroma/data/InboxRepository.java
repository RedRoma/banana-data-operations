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


package tech.aroma.data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import tech.aroma.thrift.*;
import tech.aroma.thrift.service.AromaServiceConstants;
import tech.sirwellington.alchemy.annotations.arguments.Required;


/**
 * The Inbox repository is responsible for storage and retrieval of messages that are stored
 * for an Applications followers in their own provide "Inbox".
 * 
 * @author SirWellington
 */
public interface InboxRepository 
{
    default void saveMessageForUser(@Required User user, @Required Message message) throws TException
    {
        this.saveMessageForUser(user, message, AromaServiceConstants.DEFAULT_INBOX_LIFETIME);
    }
    
    void saveMessageForUser(@Required User user, @Required Message message, @Required LengthOfTime lifetime) throws TException;

    List<Message> getMessagesForUser(@Required String userId) throws TException;
    
    default List<Message> getMessagesForUser(@Required String userId, @Required String applicationId) throws TException
    {
        return getMessagesForUser(userId)
            .stream()
            .filter(msg -> Objects.equals(msg.applicationId, applicationId))
            .collect(Collectors.toList());
    }
    
    boolean containsMessageInInbox(@Required String userId, @Required Message message) throws TException;

    void deleteMessageForUser(@Required String userId, @Required String messageId) throws TException;
    
    void deleteAllMessagesForUser(@Required String userId) throws TException;
    
    long countInboxForUser(@Required String userId) throws TException;
    
}
