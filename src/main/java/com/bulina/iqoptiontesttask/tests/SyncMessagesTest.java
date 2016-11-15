package com.bulina.iqoptiontesttask.tests;

import com.bulina.iqoptiontesttask.framework.dataModel.Message;
import com.bulina.iqoptiontesttask.framework.httpClient.HttpApi;
import com.bulina.iqoptiontesttask.framework.dataModel.TestAccount;
import com.bulina.iqoptiontesttask.framework.httpClient.LoginResponse;
import com.bulina.iqoptiontesttask.framework.wsClient.WebsocketClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SyncMessagesTest extends TestBase {

    @Test(groups = {"P0"}, description = "Positive scenario")
    public void testAuthorizedWsShouldSendTimeSyncMessagesEachSecond() throws InterruptedException, IOException {

        logAction("Getting test account....");
        TestAccount account = TestAccount.getInstance();
        logPassed();

        logAction("Authenticate account using HTTP API. User creds: %s ... ", account);
        LoginResponse loginResponse =
                new HttpApi().postToLoginV2(account.email, account.password);

        logAction("Check HTTP status code");
        Assert.assertEquals(loginResponse.getStatusCode(), 200,
                "Incorrect HTTP status received");
        logPassed("HTTP 200 OK");

        logAction("Check e-mail from response");
        Assert.assertEquals(account.email.toLowerCase(), loginResponse.getEmail().toLowerCase(),
                "Incorect user authorized, e-mail values from request and response do not match!!!");
        logPassed("E-mail values from request and response match");

        logAction("Check ssids from response heahers...");
        Assert.assertFalse((loginResponse.getSsids() == null || (loginResponse.getSsids().size() == 0)),
                "No ssid found on responce headers");
        logPassed("SSIDs got from cookies: %s", loginResponse.getSsids());

        logAction("Opening web socket connection...");
        WebsocketClient wsClient = new WebsocketClient(WebsocketClient.SNIFFER_MODE.SYNC_AFTER_AUTH);

        logAction("Authorization web socket connection, sending auth message...");
        logPassed();
        wsClient.sendAuthMessage(
                Message.Builder().setName("ssid").setMsg(loginResponse.getSsids().iterator().next()).build());
        logPassed();

        long testDurationInSeconds = 10;

        logAction("Listening and queuing timeSync messages from server and close connection after %s seconds...", testDurationInSeconds);
        Thread.sleep(testDurationInSeconds * 1000);
        wsClient.closeConnection();
        logPassed();

        logAction("Checking syncTime messages count", testDurationInSeconds);
        ConcurrentLinkedQueue<Message> queue = wsClient.getSyncMsgQueue();
        Assert.assertTrue(Math.abs(testDurationInSeconds - queue.size()) <= 1,
                String.format("Received mesages count [%s] is not equal expectation", queue.size()));
        logPassed();

        long timeFrameInMs = 50;
        logAction("Checking that syncTime messages was reseives each second +/- %s ms using as time frame", timeFrameInMs);
        checkMessagesInterval(queue, timeFrameInMs);
        logPassed();
    }

    @Test(groups = {"P1"}, description = "Negative scenario")
    public void testUnauthorizedWsShouldNotSendTimeSyncMessagesEachSecond() throws InterruptedException, IOException {

        logAction("Trying to open web socket connection with incorrect ssid...");
        WebsocketClient wsClient = new WebsocketClient(WebsocketClient.SNIFFER_MODE.ALL_SYNC);

        logAction("Authorization web socket connection, sending auth message...");
        logPassed();
        wsClient.sendAuthMessage(Message.Builder().setName("ssid").setMsg("Bla-bla").build());
        logPassed();

        long testDurationInSeconds = 10;

        logAction("Listening %s seconds, queuing timeSync messages from server, waiting connection closing initiated by server...", testDurationInSeconds);
        Thread.sleep(testDurationInSeconds * 1000);
        Assert.assertFalse(wsClient.isSessionOpen(), "Session was not closed");
        logPassed();

        logAction("Checking syncTime messages count", testDurationInSeconds);

        Assert.assertTrue((wsClient.getSyncMsgQueue().size() <= 1),
                String.format("Received messages count [%s] more then expected", wsClient.getSyncMsgQueue().size()));
        logPassed();
    }

    private void checkMessagesInterval(ConcurrentLinkedQueue<Message> queue, long timeFrameInMs) {
        Message msgLast = queue.poll();
        do {
            Message msg = queue.poll();
            //long intervalInMs = msg.getTimeCreated().getTime() - msgLast.getTimeCreated().getTime();
            long intervalInMs = Long.parseLong(msg.getMsg(), 10) - Long.parseLong(msgLast.getMsg(), 10);
            long delta = Math.abs(intervalInMs - 1000);

            logAction("Comparing receive message timestamps, timeframe %s ms. Interval between messages = [%s], delta = [%s] ", timeFrameInMs, intervalInMs, delta);
            Assert.assertTrue(delta < timeFrameInMs, String.format("Deviation of 1 second more than %s ms", timeFrameInMs));
            msgLast = msg;
        } while (!queue.isEmpty());
    }
}
