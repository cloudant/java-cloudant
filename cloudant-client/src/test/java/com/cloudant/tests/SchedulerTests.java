package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cloudant.client.api.scheduler.SchedulerDocsResponse;
import com.cloudant.client.api.scheduler.SchedulerJobsResponse;
import com.cloudant.tests.base.TestWithMockedServer;

import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;

public class SchedulerTests extends TestWithMockedServer {

    @Test
    public void schedulerJobsTest() {
        String schedulerJobsPayload = "{\"total_rows\":1,\"offset\":0," +
                "\"jobs\":[{\"database\":null," +
                "\"id\":\"f11105eaaded4981d21ff8ebf846f48b+create_target\"," +
                "\"pid\":\"<0.5866.6800>\"," +
                "\"source\":\"https://clientlibs-test:*****@clientlibs-test.cloudant" +
                ".com/largedb1g/\",\"target\":\"https://tomblench:*****@tomblench.cloudant" +
                ".com/largedb1g/\",\"user\":\"tomblench\",\"doc_id\":null," +
                "\"history\":[{\"timestamp\":\"2018-04-12T13:06:20Z\",\"type\":\"started\"}," +
                "{\"timestamp\":\"2018-04-12T13:06:20Z\",\"type\":\"added\"}]," +
                "\"node\":\"dbcore@db2.bigblue.cloudant.net\"," +
                "\"start_time\":\"2018-04-12T13:06:20Z\"}]}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(schedulerJobsPayload));
        SchedulerJobsResponse response = client.schedulerJobs();
        assertNotNull(response.getJobs());
        assertThat(response.getTotalRows(), is(1L));
        assertThat(response.getJobs(), hasSize(1));
        for (SchedulerJobsResponse.Job job : response.getJobs()) {
            assertNotNull(job.getHistory());
            assertNotNull(job.getId());
            assertNotNull(job.getNode());
            assertNotNull(job.getPid());
            assertNotNull(job.getSource());
            assertNotNull(job.getStartTime());
            assertNotNull(job.getTarget());
            assertNotNull(job.getUser());
        }
    }

    @Test
    public void schedulerDocsTest() {
        String schedulerDocsPayload = "{\"total_rows\":6,\"offset\":0,\"docs\":[\n" +
                "{\"database\":\"tomblench/_replicator\"," +
                "\"doc_id\":\"296e48244e003eba8764b2156b3bf302\",\"id\":null," +
                "\"source\":\"https://tomblench.cloudant.com/animaldb/\"," +
                "\"target\":\"https://tomblench.cloudant.com/animaldb_copy/\"," +
                "\"state\":\"completed\",\"error_count\":0,\"info\":{\"revisions_checked\":15," +
                "\"missing_revisions_found\":2,\"docs_read\":2,\"docs_written\":2," +
                "\"changes_pending\":null,\"doc_write_failures\":0," +
                "\"checkpointed_source_seq\":\"19" +
                "-g1AAAAGjeJyVz10KwjAMB_BoJ4KX8AZF2tWPJ3eVpqnO0XUg27PeTG9Wa_VhwmT6kkDIPz_iACArGcGS0DRnWxDmHE9HdJ3lxjUdad9yb1sXF6cacB9CqEqmZ3UczKUh2uGhHxeD8U9i_Z3AIla8vJVJUlBIZYTqX5A_KMM7SfFZrHCNLUK3p7RIkl5tSRD-K6kx6f6S0k8sScpYJTb5uFQ9AI9Ch9c\"},\"start_time\":null,\"last_updated\":\"2017-04-13T14:53:50+00:00\"},\n" +
                "{\"database\":\"tomblench/_replicator\"," +
                "\"doc_id\":\"3b749f320867d703550b0f758a4000ae\",\"id\":null," +
                "\"source\":\"https://examples.cloudant.com/animaldb/\"," +
                "\"target\":\"https://tomblench.cloudant.com/animaldb/\",\"state\":\"completed\"," +
                "\"error_count\":0,\"info\":{\"revisions_checked\":15," +
                "\"missing_revisions_found\":15,\"docs_read\":15,\"docs_written\":15," +
                "\"changes_pending\":null,\"doc_write_failures\":0," +
                "\"checkpointed_source_seq\":\"56" +
                "-g1AAAAGveJzLYWBgYMlgTmFQSElKzi9KdUhJstDLTS3KLElMT9VLzskvTUnMK9HLSy3JAapkSmRIsv___39WBnMiby5QgN04JS3FLDUJWb8Jdv0gSxThigyN8diS5AAkk-qhFvFALEo2MTEwMSXGDDSbTPHYlMcCJBkagBTQsv0g28TBtpkbGCQapaF4C4cxJFt2AGIZ2GscYMuMDEzMUizMkC0zw25MFgBKoovi\"},\"start_time\":null,\"last_updated\":\"2017-04-27T12:28:44+00:00\"},\n" +
                "{\"database\":\"tomblench/_replicator\"," +
                "\"doc_id\":\"ad8f7896480b8081c8f0a2267ffd1859\",\"id\":null," +
                "\"source\":\"https://tortytherlediffecareette:*****@mikerhodestesty008.cloudant" +
                ".com/moviesdb/\",\"target\":\"https://tomblench.cloudant.com/moviesdb_rep/\"," +
                "\"state\":\"completed\",\"error_count\":0,\"info\":{\"revisions_checked\":5997," +
                "\"missing_revisions_found\":5997,\"docs_read\":5997,\"docs_written\":5997," +
                "\"changes_pending\":null,\"doc_write_failures\":0," +
                "\"checkpointed_source_seq\":\"5997" +
                "-g1AAAANreJy10UEKwjAQAMBgBcVP2BeUpEm1PdmfaDYJSKkVtB486U_0J_oBTz5AHyAI3jxIjUml1x7ayy67LDssmyKE-nNHIleCWK5ULIF6uVrnW4xDT6TLjeRZ7mUqT_VkhyMYFkWRzB3Q1XOhez3iczKKghor6jvg6giTiroYiuNQYYqbpeIfNa2oh72KhQGosFlq9qN2FfUyFPgUCKONUllXR7TXSWuHkvsYjjEWjQVvgTta7lRyV_szKgmRbVx3ttzNcs7AcEoKCHAb3N1y_9-9DYeBYzEiNTYlX3EcE0s\"},\"start_time\":null,\"last_updated\":\"2016-08-23T13:11:26+00:00\"},\n" +
                "{\"database\":\"tomblench/_replicator\"," +
                "\"doc_id\":\"b63c053ecd95a4047b55ed8847b046f1\",\"id\":null," +
                "\"source\":\"https://tomblench.cloudant.com/atestdb2/\"," +
                "\"target\":\"https://tomblench.cloudant.com/atestdb1/\",\"state\":\"completed\"," +
                "\"error_count\":0,\"info\":{\"revisions_checked\":1," +
                "\"missing_revisions_found\":1,\"docs_read\":1,\"docs_written\":1," +
                "\"changes_pending\":null,\"doc_write_failures\":0," +
                "\"checkpointed_source_seq\":\"2" +
                "-g1AAAAFHeJyNjkEOgjAQRSdAYjyFN2jSFCtdyVU6nSKQWhJC13ozvVktsoEF0c2fTPL_" +
                "-98BQNHmBCdCM4y2JuQMuxu6YJlxQyDtJ-bt5JIx04DXGGOvYRsR-xGsk" +
                "-JjTrW5hnv6Dg0XplRngmPwZJvOW9ry5D7PF0nhmU5CvmZm9mVKVVacLr8pfy9fmt5L02q9qEhJbtbr" +
                "-w-AQmfD\"},\"start_time\":null,\"last_updated\":\"2017-05-16T16:25:22+00:00\"}," +
                "\n" +
                "{\"database\":\"tomblench/_replicator\"," +
                "\"doc_id\":\"c71c9e69e30a182dc91d8938277bc85e\",\"id\":null," +
                "\"source\":\"https://tomblench.cloudant.com/animaldb/\"," +
                "\"target\":\"https://tomblench.cloudant.com/animaldb_copy/\"," +
                "\"state\":\"completed\",\"error_count\":0,\"info\":{\"revisions_checked\":15," +
                "\"missing_revisions_found\":15,\"docs_read\":15,\"docs_written\":15," +
                "\"changes_pending\":null,\"doc_write_failures\":0," +
                "\"checkpointed_source_seq\":\"14" +
                "-g1AAAAEueJzLYWBgYMlgTmGQSUlKzi9KdUhJMtTLTU1M0UvOyS9NScwr0ctLLckBqmJKZEiy____f1YGUyJrLlCAPdHEPCktJZk43UkOQDKpHmoAI9gAw2STxCTzJOIMyGMBkgwNQApoxv6sDGaoK0yN04wsk80IGEGKHQcgdoAdygxxaIplklFaWhYAu2FdOA\"},\"start_time\":null,\"last_updated\":\"2015-05-12T11:47:33+00:00\"},\n" +
                "{\"database\":\"tomblench/_replicator\"," +
                "\"doc_id\":\"e6242d1e9ce059b0388fc75af3116a39\",\"id\":null," +
                "\"source\":\"https://tomblench.cloudant.com/atestdb1/\"," +
                "\"target\":\"https://tomblench.cloudant.com/atestdb2/\",\"state\":\"completed\"," +
                "\"error_count\":0,\"info\":{\"revisions_checked\":1," +
                "\"missing_revisions_found\":1,\"docs_read\":1,\"docs_written\":1," +
                "\"changes_pending\":null,\"doc_write_failures\":0," +
                "\"checkpointed_source_seq\":\"1" +
                "-g1AAAAFheJyFzkEOgjAQBdBRSIyn8AZNgEJgJVeZ6bQCqSUhdK0305th1Q1dEDYzyWTy_rcAkHYJw4VJjZNumQpB_Y2s10LZ0TO6WTg92_B4RKDrsixDlyDcw-FUVUiFahjO3rE2vdMcY9k2Rm2Y9Ig8bWqspdz25Lbn0jDhGVYgX1_z8DMblnlp8n0lTir3kt7_pFV7NE2WYbluP3wATr5vQA\"},\"start_time\":null,\"last_updated\":\"2017-05-16T16:24:02+00:00\"}\n" +
                "]}\n";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(schedulerDocsPayload));
        SchedulerDocsResponse response = client.schedulerDocs();
        assertNotNull(response.getDocs());
        assertThat(response.getTotalRows(), is(6L));
        assertThat(response.getDocs(), hasSize(6));
        for (SchedulerDocsResponse.Doc doc : response.getDocs()) {
            assertNotNull(doc.getDatabase());
            assertNotNull(doc.getDocId());
            assertNotNull(doc.getInfo());
            assertNotNull(doc.getLastUpdated());
            assertNotNull(doc.getSource());
            assertNotNull(doc.getState());
            assertNotNull(doc.getTarget());
            assertNotNull(doc.getInfo().get("revisions_checked"));
            assertNotNull(doc.getInfo().get("missing_revisions_found"));
            assertNotNull(doc.getInfo().get("docs_read"));
            assertNotNull(doc.getInfo().get("docs_written"));
            assertNotNull(doc.getInfo().get("doc_write_failures"));
            assertNotNull(doc.getInfo().get("checkpointed_source_seq"));
        }

    }

}
