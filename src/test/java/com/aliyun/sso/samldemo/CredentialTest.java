package com.aliyun.sso.samldemo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.security.credential.Credential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.aliyun.sso.samldemo.util.EncodingUtils;
import com.aliyun.sso.samldemo.util.IDPCredentials;

/**
 * @author @aliababa-inc.com
 * @date 2019/3/1
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CredentialTest {
    @Value("${iot.key.password}")
    private String password;
    @Test
    public void readCred() {
        Credential credential = IDPCredentials.getCredential(password);
        System.out.println(EncodingUtils.encode(credential.getPrivateKey().getEncoded()));
        System.out.println(EncodingUtils.encode(credential.getPublicKey().getEncoded()));
    }
}
