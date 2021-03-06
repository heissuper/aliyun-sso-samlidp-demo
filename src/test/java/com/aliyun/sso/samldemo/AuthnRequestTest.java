package com.aliyun.sso.samldemo;

import org.joda.time.DateTime;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.saml2.binding.security.impl.SAML2HTTPPostSimpleSignSecurityHandler;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.security.credential.CredentialResolver;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.aliyun.sso.samldemo.constants.AlgorithmMethod;
import com.aliyun.sso.samldemo.constants.DigestMethod;
import com.aliyun.sso.samldemo.util.SamlUtils;

import javax.annotation.Resource;

import java.util.Arrays;

import static org.opensaml.saml.common.xml.SAMLConstants.SAML2_POST_BINDING_URI;

/**
 * @author @aliababa-inc.com
 * @date 2019/3/2
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuthnRequestTest {
    String dest = "http://localhost:8888/saml/sso";
    String spAcsUrl = "http://localhost:8888/saml/acs";
    String spEntityId = "entityId";
    @Resource
    SamlUtils implementation;
    private static AuthnRequest request;

    @Test
    public void aTest() throws MarshallingException {
        AuthnRequest authnRequest = implementation.buildSAMLObject(AuthnRequest.class);
        //???????????????????????????????????????????????????????????????
        authnRequest.setIssueInstant(new DateTime());
        //??????URL??????????????????IDP??????
        authnRequest.setDestination(dest);
        //??????SAML?????????????????????????????????????????????????????????Artifact?????????????????????????????????
        authnRequest.setProtocolBinding(SAML2_POST_BINDING_URI);
        //SP????????? ?????????SAML?????????????????????
        authnRequest.setAssertionConsumerServiceURL(this.spAcsUrl);
        //?????????ID????????????????????????ID?????????????????????
        authnRequest.setID(SamlUtils.generateSecureRandomId());
        //Issuer??? ???????????????????????????SP???ID????????????SP???URL
        authnRequest.setIssuer(spBuildIssuer());
        //NameID???IDP??????????????????????????????NameID policy???SP??????NameID????????????????????????
        authnRequest.setNameIDPolicy(buildNameIdPolicy());
        // ????????????????????????requested Authentication Context???:
        // SP??????????????????????????????SP??????IDP??????????????????????????????IDP???????????????????????????????????????
        authnRequest.setRequestedAuthnContext(buildRequestedAuthnContext());


        implementation.signObject(authnRequest);

        request = authnRequest;
        System.out.println(implementation.transformSAMLObject2String(authnRequest));
    }

    @Test
    public void bValidate9(){
        //MessageContext context = new MessageContext<Response>();
        //context.setMessage(request);
        //
        //
        //SAML2HTTPPostSimpleSignSecurityHandler securityHandler = new SAML2HTTPPostSimpleSignSecurityHandler();
        //securityHandler.setHttpServletRequest(request);
        //securityHandler.setParser(implementation.getParserPool());
        //InlineX509DataProvider inlineX509DataProvider = new InlineX509DataProvider();
        //BasicProviderKeyInfoCredentialResolver basicProviderKeyInfoCredentialResolver =
        //    new BasicProviderKeyInfoCredentialResolver(Arrays.asList(inlineX509DataProvider));
        //securityHandler.setKeyInfoResolver(basicProviderKeyInfoCredentialResolver);
        //
        //SecurityParametersContext securityParametersContext = context.getSubcontext(SecurityParametersContext.class,true);
        //SignatureValidationParameters signatureValidationParameters = new SignatureValidationParameters();
        //KeyInfoCredentialResolver keyInfoCredentialResolver =
        //    new StaticKeyInfoCredentialResolver(openSamlImplementation.resolveCredentialByCertificate(extractX509StringFromSignable(response)));
        //CredentialResolver credentialResolver = new StaticCredentialResolver(openSamlImplementation.spCredential());
        //ExplicitKeySignatureTrustEngine signatureTrustEngine = new ExplicitKeySignatureTrustEngine(credentialResolver, keyInfoCredentialResolver);
        //signatureValidationParameters.setSignatureTrustEngine(signatureTrustEngine);
        //securityParametersContext.setSignatureValidationParameters(signatureValidationParameters);

    }

    public Issuer spBuildIssuer() {
        Issuer issuer = implementation.buildSAMLObject(Issuer.class);
        issuer.setValue(spEntityId);
        return issuer;
    }

    private NameIDPolicy buildNameIdPolicy() {
        NameIDPolicy nameIDPolicy = implementation.buildSAMLObject(NameIDPolicy.class);
        nameIDPolicy.setAllowCreate(true);
        nameIDPolicy.setFormat(NameIDType.TRANSIENT);
        return nameIDPolicy;
    }

    /**
     * SP??????Authn???????????????
     * @return
     */
    private RequestedAuthnContext buildRequestedAuthnContext() {
        RequestedAuthnContext requestedAuthnContext = implementation.buildSAMLObject(RequestedAuthnContext.class);
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);

        AuthnContextClassRef passwordAuthnContextClassRef = implementation.buildSAMLObject(AuthnContextClassRef.class);
        passwordAuthnContextClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);

        requestedAuthnContext.getAuthnContextClassRefs().add(passwordAuthnContextClassRef);

        return requestedAuthnContext;

    }

}
