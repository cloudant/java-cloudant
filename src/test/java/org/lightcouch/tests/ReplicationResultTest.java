package org.lightcouch.tests;

import com.google.gson.Gson;
import org.junit.Test;
import org.lightcouch.ReplicationResult;

import static org.junit.Assert.assertEquals;

public class ReplicationResultTest {

    @Test
    public void testWithCloudantResult() throws Exception {
        ReplicationResult replicationResult=new Gson().fromJson("{\"ok\":true,\n" +
                "\"session_id\":\"b062b02fccd6dbdf1a818b29a7dbe185\",\n" +
                "\"source_last_seq\":\"9-g1AAAADdeJzLYWBgYMlgTmGQS0lKzi9KdUhJMjTWyyrNSS3QS87JL01JzCvRy0styQGqY0pkSLL___9_ViIzSIcsXIclLg1JDkAyqR6sh4lYW_JYgCRDA5AC6tsP1UiEZRB9ByD6QBZmAQD8g0jz\",\n" +
                "\"history\":[{\"session_id\":\"b062b02fccd6dbdf1a818b29a7dbe185\",\n" +
                "            \"start_time\":\"Sat, 14 Apr 2012 11:49:00 GMT\",\n" +
                "            \"end_time\":\"Sat, 14 Apr 2012 11:49:01 GMT\",\n" +
                "            \"start_last_seq\":0,\n" +
                "            \"end_last_seq\":\"9-g1AAAADdeJzLYWBgYMlgTmGQS0lKzi9KdUhJMjTWyyrNSS3QS87JL01JzCvRy0styQGqY0pkSLL___9_ViIzSIcsXIclLg1JDkAyqR6sh4lYW_JYgCRDA5AC6tsP1UiEZRB9ByD6QBZmAQD8g0jz\",\n" +
                "            \"recorded_seq\":\"9-g1AAAADdeJzLYWBgYMlgTmGQS0lKzi9KdUhJMjTWyyrNSS3QS87JL01JzCvRy0styQGqY0pkSLL___9_ViIzSIcsXIclLg1JDkAyqR6sh4lYW_JYgCRDA5AC6tsP1UiEZRB9ByD6QBZmAQD8g0jz\",\n" +
                "            \"missing_checked\":0,\n" +
                "            \"missing_found\":34,\n" +
                "            \"docs_read\":34,\n" +
                "            \"docs_written\":34,\n" +
                "            \"doc_write_failures\":0}]}\n" +
                "", ReplicationResult.class);

        final ReplicationResult.ReplicationHistory firstHistory = replicationResult.getHistories().get(0);
        assertEquals("9-g1AAAADdeJzLYWBgYMlgTmGQS0lKzi9KdUhJMjTWyyrNSS3QS87JL01JzCvRy0styQGqY0pkSLL___9_ViIzSIcsXIclLg1JDkAyqR6sh4lYW_JYgCRDA5AC6tsP1UiEZRB9ByD6QBZmAQD8g0jz", firstHistory.getEndLastSeq());
        assertEquals("9-g1AAAADdeJzLYWBgYMlgTmGQS0lKzi9KdUhJMjTWyyrNSS3QS87JL01JzCvRy0styQGqY0pkSLL___9_ViIzSIcsXIclLg1JDkAyqR6sh4lYW_JYgCRDA5AC6tsP1UiEZRB9ByD6QBZmAQD8g0jz", firstHistory.getRecordedSeq());
    }
}
