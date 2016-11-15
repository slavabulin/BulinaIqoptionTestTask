package com.bulina.iqoptiontesttask.framework.dataModel;

import static java.lang.String.format;

/**
 * Created by user on 13.11.2016.
 */
public class TestAccount {
    public final String email;
    public final String password;

    private TestAccount(){
        email = "vapupkina@ya.ru";
        password = "Pup-31415926";
    }

    public static TestAccount getInstance() {return new TestAccount();}

    @Override
    public String toString() {
        return format("[email: '%s', password: '%s']", email, password);
    }
}
