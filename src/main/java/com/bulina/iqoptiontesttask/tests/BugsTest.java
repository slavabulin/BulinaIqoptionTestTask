package com.bulina.iqoptiontesttask.tests;

import com.bulina.iqoptiontesttask.framework.dataModel.TestAccount;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by user on 15.11.2016.
 */
public class BugsTest extends TestBase {

    @Test(groups = {"P1"}, description = "Max-Age cannot be less than 0")
    public void testIncorrectMaxAge() throws InterruptedException, IOException {
        httpLoginRestAssured(TestAccount.getInstance().email, TestAccount.getInstance().password);
    }


    private void httpLoginRestAssured(String email, String password) throws IOException {

        given()
                .header("TestAccount-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36")
                .header("Accept-Encoding", "gzip, deflate, sdch, br")
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4")
                .header("Cache-Control", "no-cache")
                .queryParam("email", email)
                .queryParam("password", password)
                .when().
                post("https://iqoption.com/api/login/v2");
    }
}
