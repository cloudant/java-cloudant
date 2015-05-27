package com.cloudant.test.main;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.cloudant.tests.AttachmentsTest;
import com.cloudant.tests.BulkDocumentTest;
import com.cloudant.tests.ChangeNotificationsTest;
import com.cloudant.tests.CloudantClientTests;
import com.cloudant.tests.ClientLoadTest;
import com.cloudant.tests.DBServerTest;
import com.cloudant.tests.DatabaseTest;
import com.cloudant.tests.DesignDocumentsTest;
import com.cloudant.tests.DocumentsCRUDTest;
import com.cloudant.tests.IndexTests;
import com.cloudant.tests.ReplicationTest;
import com.cloudant.tests.ReplicatorTest;
import com.cloudant.tests.SearchTests;
import com.cloudant.tests.SslAuthenticationTest;
import com.cloudant.tests.UnicodeTest;
import com.cloudant.tests.UpdateHandlerTest;
import com.cloudant.tests.ViewsTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	   AttachmentsTest.class,
	   BulkDocumentTest.class,
	   ChangeNotificationsTest.class,
	   CloudantClientTests.class,
	   ClientLoadTest.class,
	   DatabaseTest.class,
	   DBServerTest.class,
	   DesignDocumentsTest.class,
	   DocumentsCRUDTest.class,
	   IndexTests.class,
	   ReplicationTest.class,
	   ReplicatorTest.class,
	   SearchTests.class,
       SslAuthenticationTest.class,
	   UpdateHandlerTest.class,
	   UnicodeTest.class,
	   ViewsTest.class
	})
public class CloudantTestSuite {

}
