package com.bulina.iqoptiontesttask.tests;

import com.bulina.iqoptiontesttask.framework.dataModel.Message;
import com.bulina.iqoptiontesttask.framework.httpClient.HttpApi;
import com.bulina.iqoptiontesttask.framework.dataModel.TestAccount;
import com.bulina.iqoptiontesttask.framework.httpClient.LoginResponse;
import com.bulina.iqoptiontesttask.framework.wsClient.WebsocketClient;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Timer;
import java.util.TimerTask;


import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncMessagesPositiveTest extends TestBase {

    LoginResponse loginResponse;
    WebsocketClient wsClient;

    @BeforeMethod
    public void beforeTest() throws IOException {

        logAction("Getting test account....");
        TestAccount account = TestAccount.getInstance();
        logPassed();

        logAction("Authenticate account using HTTP API. User creds: %s ... ", account);
        loginResponse = new HttpApi().postToLoginV2(account.email, account.password);

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
        wsClient = new WebsocketClient(WebsocketClient.SNIFFER_MODE.SYNC_AFTER_AUTH);

        logAction("Authorization web socket connection, sending auth message...");
        logPassed();
        wsClient.sendAuthMessage(
                Message.Builder().setName("ssid").setMsg(loginResponse.getSsids().iterator().next()).build());
        logPassed();
    }

    @AfterTest
    public void afterTest() throws IOException {
        if (wsClient != null) {
            wsClient.closeConnection();
        }
        wsClient = null;
    }

    //---------------------------------------------------------------------------------------------------

    AtomicBoolean isTestPassed;

    @Test(groups = {"P0"}, description = "Positive scenario, ws client receive sync messages each eacond")
    public void testWsClientShouldRecieveTimeSyncMessagesEachSecond() throws InterruptedException, IOException {

        long testDurationInSeconds = 10;
        long waitTimeFrameInMs = 50;

        AtomicBoolean isFirstMsg = new AtomicBoolean(true);
        isTestPassed = new AtomicBoolean(true);
        TimerTask timerTask = new QueueChecker();
        Timer timer = new Timer(true);

        // add messages listener
        wsClient.addMessageHandler(new WebsocketClient.MessageHandler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.getType() != Message.MESSAGE_TYPE.SYNC) {
                    return;
                } else if (isFirstMsg.get()) {
                    // let's give 'waitTimeFrameInMs' ms for message waiting
                    timer.scheduleAtFixedRate(timerTask, 1000 + waitTimeFrameInMs, 1000);
                    //System.out.println("TimerTask started as demon at: " + new Date().getTime());
                    isFirstMsg.getAndSet(false);
                    wsClient.getSyncMsgQueue().poll();
                }
            }
        });

        logAction("Listening and queuing timeSync messages from server and close connection after %s seconds...", testDurationInSeconds);
        try {
            Thread.sleep(testDurationInSeconds * 1000);
        } finally {
            wsClient.closeConnection();
            timer.cancel();
        }

        Assert.assertTrue(isTestPassed.get(), "Test failed");
        logPassed();
    }

    public class QueueChecker extends TimerTask {

        @Override
        public void run() {
            logAction("Checking for new message at %s ...", new Date().getTime());

            ConcurrentLinkedQueue<Message> queue = wsClient.getSyncMsgQueue();
            if (queue.isEmpty()) {
                Assert.assertFalse(true, "No new sync messages received, messages count: " + queue.size());
                isTestPassed.getAndSet(false);

            } else if (queue.size() > 1) {
                Assert.assertFalse(true, "More than one new sync messages received, messages count: " + queue.size());
                isTestPassed.getAndSet(false);

            } else {
                logAction("Message received: " + queue.poll().toJsonString());
            }

            //System.out.println("Timer task completed at: " + new Date().getTime());
        }
    }

    //---------------------------------------------------------------------------------------------------

    @Test(groups = {"P0"}, description = "Positive scenario, ws server receive sync messages each eacond")
    public void testAuthorizedWsShouldSendTimeSyncMessagesEachSecond() throws InterruptedException, IOException {

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
