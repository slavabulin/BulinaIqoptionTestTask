package com.bulina.iqoptiontesttask.tests;

import com.bulina.iqoptiontesttask.framework.dataModel.Message;
import com.bulina.iqoptiontesttask.framework.wsClient.WebsocketClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

public class SyncMessagesNegativeTest extends TestBase{

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

}
