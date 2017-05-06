package tech.aroma.data.sql;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import tech.sirwellington.alchemy.test.junit.runners.AlchemyTestRunner;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

/**
 *
 * @author SirWellington
 */
@RunWith(AlchemyTestRunner.class)
public class SQLStatementsTest
{


    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public void testDeletes() throws Exception
    {
        assertThat(SQLStatements.Deletes.MESSAGE, not(isEmptyOrNullString()));
    }

    @Test
    public void testInserts() throws Exception
    {
        assertThat(SQLStatements.Inserts.MESSAGE, not(isEmptyOrNullString()));
    }

    @Test
    public void testQueries() throws Exception
    {
        assertThat(SQLStatements.Queries.SELECT_MESSAGE, not(isEmptyOrNullString()));
        assertThat(SQLStatements.Queries.SELECT_APP_MESSAGES, not(isEmptyOrNullString()));
        assertThat(SQLStatements.Queries.SELECT_MESSAGES_BY_HOSTNAME, not(isEmptyOrNullString()));
        assertThat(SQLStatements.Queries.CHECK_MESSAGE, not(isEmptyOrNullString()));
    }

}