package com.aliyun.sso.samldemo.controller;

import static org.opensaml.saml.common.xml.SAMLConstants.SAML2_POST_BINDING_URI;
import static org.springframework.util.StringUtils.hasText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.joda.time.DateTime;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.messaging.context.SAMLProtocolContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.binding.security.impl.SAML2HTTPRedirectDeflateSignatureSecurityHandler;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.NameIDFormat;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.credential.impl.CollectionCredentialResolver;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.KeyInfoProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.DSAKeyValueProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.RSAKeyValueProvider;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.aliyun.sso.samldemo.configuration.Env;
import com.aliyun.sso.samldemo.util.EncodingUtils;
import com.aliyun.sso.samldemo.util.IDPCredentials;
import com.aliyun.sso.samldemo.util.SamlUtils;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

@Controller
@RequestMapping("/saml")
public class SAMLController {
    /**
     * ?????????host
     */
    @Value("${iot.saml.host}")
    private volatile String host;

    @Value("${server.port}")
    private String port;

    /**
     * ?????? ID
     */
    @Value("${iot.saml.companyId}")
    private String companyId;
    /**
	 * openssl ?????? ???????????????????????????
	 */
	@Value("${iot.key.password}")
	private String password;

    /**
     * ????????????????????????????????????key
     */
    private String sessionKey = "loginKey";

    /**
     * ????????????????????????
     */
    private static final String TEMPLATE_PATH = "/templates/saml2-post-binding.vm";
    /**
     * ????????????????????????
     */
    @Resource
    private SamlUtils samlUtils;

    private Env env = Env.ONLINE;

    @PostConstruct
    public void init(){
        if(!"80".equalsIgnoreCase(port)){
            host = host + ":" + port;
        }
    }


    /**
     * ??????, ????????????????????????,??? EntityID ????????? Issuer ??????????????????,??????????????????!
     * @return
     * @throws MarshallingException
     * @throws SignatureException
     * @throws SecurityException
     */
    @RequestMapping("/metadata")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> metadata()
        throws MarshallingException, SignatureException, SecurityException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + System.currentTimeMillis() + ".xml");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Last-Modified", new Date().toString());
        headers.add("ETag", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity
            .ok()
            .headers(headers)
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(new ByteArrayResource(this.generateIDPMetadataXML().getBytes()));


    }

    public String generateIDPMetadataXML() throws MarshallingException, SignatureException, SecurityException {
        EntityDescriptor entityDescriptor = samlUtils.buildSAMLObject(EntityDescriptor.class);
        //EntityId???metadata??????
        String idpEntityId = host + "/saml/metadata";
        entityDescriptor.setEntityID(idpEntityId);
        //IDP??????SSO????????????
        IDPSSODescriptor idpssoDescriptor = samlUtils.buildSAMLObject(IDPSSODescriptor.class);
        //?????????
        idpssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        //?????????
        idpssoDescriptor.setWantAuthnRequestsSigned(false);
        //?????????????????? Key ????????????
        KeyDescriptor keyDescriptor = samlUtils.buildSAMLObject(KeyDescriptor.class);
        KeyInfoGenerator keyInfoGenerator = samlUtils.getKeyInfoGenerator(samlUtils.getSelfCredential());
        keyDescriptor.setUse(UsageType.SIGNING);
        keyDescriptor.setKeyInfo(keyInfoGenerator.generate(samlUtils.getSelfCredential()));
        idpssoDescriptor.getKeyDescriptors().add(keyDescriptor);
        //IDP?????????NameIDFormat
        NameIDFormat nameIDFormat = samlUtils.buildSAMLObject(NameIDFormat.class);
        nameIDFormat.setFormat(NameIDType.UNSPECIFIED);
        idpssoDescriptor.getNameIDFormats().add(nameIDFormat);
        //SSO????????????
        SingleSignOnService singleSignOnService = samlUtils.buildSAMLObject(SingleSignOnService.class);
        singleSignOnService.setBinding(SAML2_POST_BINDING_URI);
        //??????????????????URL???????????????
        singleSignOnService.setLocation(host + "/saml/sso");

        idpssoDescriptor.getSingleSignOnServices().add(singleSignOnService);
        entityDescriptor.getRoleDescriptors().add(idpssoDescriptor);

        return samlUtils.transformSAMLObject2String(entityDescriptor);
    }


    /**
     * ?????????????????????,????????????????????????,?????????login???????????????,????????? iot ???????????????????????????(????????????)
     * ??????????????? httpPostBinding
     *
     * ??????????????? HTTPS ?????????????????????,??????????????????
     *
     * @param request
     * @param response
     * @param username
     * @return
     * @throws MessageEncodingException
     * @throws ComponentInitializationException
     */
    @RequestMapping("/login")
    public ModelAndView login(HttpServletRequest request , HttpServletResponse response,
                        @RequestParam(value = "username", required = false) String username)
        throws MessageEncodingException, ComponentInitializationException {

        if(StringUtils.isNotBlank(username)){
            request.getSession().setAttribute(sessionKey, username);
            
            Response samlResponse = buildResponse_old( username,companyId, null
                ,SamlUtils.generateSecureRandomId());
            
            //???Reponse??????
            samlUtils.signObject(samlResponse );
            
            //???????????????????????????IOT??????,??????IOT????????????????????????,??????????????????,??????IDP???????????? saml call back ??????
            httpPostBinding("https://workbench.aliyun.com/application", response, env.getAcsUrl(), samlResponse);
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("msg","Welcome");
        return modelAndView;
    }

    /**
     * ???????????????????????????
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    @RequestMapping("/list")
    public Object list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(request.getSession().getAttribute(sessionKey) != null){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("/list");
            modelAndView.addObject("user",request.getSession().getAttribute(sessionKey));
            return modelAndView;
        }else{
            response.sendRedirect("/saml/login");
            return null;
        }
    }

    /**
     * ???????????????????????????,?????? IOT ????????? IDP ??????????????????,????????? SP ???????????? AuthnRequest
     * @param saml
     * @param relayState
     * @param httpServletRequest
     * @param httpSevletResponse
     * @throws MessageEncodingException
     * @throws ComponentInitializationException
     */
    @RequestMapping("/sso")
    public void sso(@RequestParam(value = "SAMLRequest") String saml,
                    @RequestParam(value = "RelayState",required = false)String relayState,
                    HttpServletRequest httpServletRequest,
                    HttpServletResponse httpSevletResponse
    ) throws MessageEncodingException, ComponentInitializationException, IOException, MessageHandlerException {
        String samlRequest = EncodingUtils.inflate(EncodingUtils.decode(saml));
        AuthnRequest authnRequest = (AuthnRequest)samlUtils.transferXML2SAMLObject(samlRequest);
        validateSignature(httpServletRequest, authnRequest , authnRequest.getIssuer().getValue());

        String acsUrl = authnRequest.getAssertionConsumerServiceURL();
        String reqId = authnRequest.getID();
        String messageId = SamlUtils.generateSecureRandomId();
        //??????????????????????????????????????????
        String defaultName = "name";
        Response response = buildResponse_old(defaultName,this.companyId, reqId, messageId);
        httpServletRequest.getSession().setAttribute(sessionKey, "??????SP??????????????????????????????????????????");
        samlUtils.signObject( response );
        httpPostBinding(relayState, httpSevletResponse, acsUrl, response);

    }


    /**
     * ???????????? SAML2HTTPRedirectDeflate?????????,??????POST?????????
     * @param httpServletRequest
     * @param authnRequest
     * @param spEntityId
     * @throws ComponentInitializationException
     * @throws MessageHandlerException
     * @throws IOException
     */
    public void validateSignature(HttpServletRequest httpServletRequest, AuthnRequest authnRequest,String spEntityId)
        throws ComponentInitializationException, MessageHandlerException, IOException {
        MessageContext context = new MessageContext();
        context.setMessage(authnRequest);

        SAMLMessageInfoContext samlMessageInfoContext = context.getSubcontext(SAMLMessageInfoContext.class,true);
        samlMessageInfoContext.setMessageIssueInstant(authnRequest.getIssueInstant());


        SAMLPeerEntityContext samlPeerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        samlPeerEntityContext.setRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);


        SAMLProtocolContext samlProtocolContext = context.getSubcontext(SAMLProtocolContext.class, true);
        samlProtocolContext.setProtocol(SAMLConstants.SAML2_REDIRECT_BINDING_URI);

        SecurityParametersContext securityParametersContext = context.getSubcontext(SecurityParametersContext.class,true);

        SignatureValidationParameters signatureValidationParameters = new SignatureValidationParameters();

        //TODO ????????????????????? SP ?????????????????????????????? Credential

        ArrayList<KeyInfoProvider> providers = new ArrayList<KeyInfoProvider>();
        providers.add( new RSAKeyValueProvider() );
        providers.add( new DSAKeyValueProvider() );
        providers.add( new InlineX509DataProvider() );
        BasicProviderKeyInfoCredentialResolver resolver = new BasicProviderKeyInfoCredentialResolver(providers);
        Credential signCer = readSPCredential();
        ((BasicCredential)signCer).setUsageType(UsageType.SIGNING);
        ((BasicCredential)signCer).setEntityId(spEntityId);
        CollectionCredentialResolver credentialResolver = new CollectionCredentialResolver(Arrays.asList(signCer));

        //MetadataCredentialResolver metadataCredentialResolver = new MetadataCredentialResolver();
        //metadataCredentialResolver.setRoleDescriptorResolver();
        //metadataCredentialResolver.setKeyInfoCredentialResolver(resolver);

        SignatureTrustEngine signatureTrustEngine = new ExplicitKeySignatureTrustEngine(credentialResolver, resolver);
        signatureValidationParameters.setSignatureTrustEngine(signatureTrustEngine);
        securityParametersContext.setSignatureValidationParameters(signatureValidationParameters);

        SAML2HTTPRedirectDeflateSignatureSecurityHandler securityHandler = new SAML2HTTPRedirectDeflateSignatureSecurityHandler();
        securityHandler.setHttpServletRequest(httpServletRequest);
        securityHandler.initialize();
        securityHandler.invoke(context);
    }

    /**
     * ????????????IOT?????????,??????????????????????????? ?????? https://account.aliplus.com/saml/sp/metadata
     * @return
     * @throws IOException
     */
    public Credential readSPCredential() throws IOException {
        return IDPCredentials.readCredential(SAMLController.class.getResourceAsStream("/spcredential"));
    }

    /**
     * ????????????????????????
     *
     * @param reqId
     * @param messageId
     * @return
     */
    private Response buildResponse_old(String loginName,String companyId, String reqId, String messageId) {
        Assertion assertion = samlUtils.buildSAMLObject(Assertion.class);
        DateTime now = new DateTime();
        // ????????????,????????????????????????
        assertion.setID(messageId);
        //????????????,???????????????iot?????????????????????
        Subject subject = samlUtils.buildSAMLObject(Subject.class);
        //????????????,??????????????????????????????????????????
        NameID nameID = samlUtils.buildSAMLObject(NameID.class);
        nameID.setValue(loginName);
        nameID.setFormat(NameIDType.PERSISTENT);
        subject.setNameID(nameID);
        //???????????? SubjectConfirmationData ??? Method????????? METHOD_BEARER
        SubjectConfirmation subjectConfirmation = samlUtils.buildSAMLObject(SubjectConfirmation.class);
        SubjectConfirmationData subjectConfirmationData = samlUtils.buildSAMLObject(SubjectConfirmationData.class);
        if(StringUtils.isNotBlank(reqId)) {
            subjectConfirmationData.setInResponseTo(reqId);
        }
        subjectConfirmationData.setNotOnOrAfter(now.plusMinutes(5));
        //Recipient?????????IOT?????????
        subjectConfirmationData.setRecipient(env.getAcsUrl());
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        assertion.setSubject(subject);
        assertion.getAuthnStatements().add(getAuthnStatement(messageId));
        assertion.setIssueInstant(now);
        //issuer?????????entityId?????? ????????????
        assertion.setIssuer(idpBuildIssuer());
        assertion.setIssueInstant(now);
        //????????????
        Conditions conditions = samlUtils.buildSAMLObject(Conditions.class);
        conditions.setNotBefore(now);
        conditions.setNotOnOrAfter(now.plusSeconds(5));
        AudienceRestriction audienceRestriction = samlUtils.buildSAMLObject(AudienceRestriction.class);
        //????????????
        Audience audience = samlUtils.buildSAMLObject(Audience.class);
        //??????
        audience.setAudienceURI(env.getAudience());
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        assertion.setConditions(conditions);

        //????????? companyId ??? Attribute ???????????????,????????????ID
        AttributeStatement attributeStatement =  samlUtils.buildSAMLObject(AttributeStatement.class);
        Attribute attribute = samlUtils.buildSAMLObject(Attribute.class);
        attribute.setName("companyId");
        XSAny attributeValue =  new XSAnyBuilder().buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        attributeValue.setTextContent(companyId);
        attribute.getAttributeValues().add(attributeValue);
        attributeStatement.getAttributes().add(attribute);

        assertion.getAttributeStatements().add(attributeStatement);

        Response response = samlUtils.buildSAMLObject(Response.class);
        response.setID(SamlUtils.generateSecureRandomId());
        Status status = samlUtils.buildSAMLObject(Status.class);
        StatusCode statusCode = samlUtils.buildSAMLObject(StatusCode.class);
        //Status Code ????????????SUCEESS
        statusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(statusCode);

        response.setStatus(status);
        //DESTION?????????IOT???ACS
        response.setDestination(env.getAcsUrl());
        response.getAssertions().add(assertion);
        response.setIssueInstant(now);
        response.setIssuer(this.idpBuildIssuer());
        response.setVersion(SAMLVersion.VERSION_20);
        //???????????????
        samlUtils.signObject(assertion );
        
        return response;
    }

    private AuthnStatement getAuthnStatement(String msgId){
        AuthnStatement authnStatement = samlUtils.buildSAMLObject(AuthnStatement.class);
        AuthnContext authnContext = samlUtils.buildSAMLObject(AuthnContext.class);
        AuthnContextClassRef authnContextClassRef = samlUtils.buildSAMLObject(AuthnContextClassRef.class);
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        authnStatement.setAuthnInstant(new DateTime());
        //?????? SP ????????? ???????????? SessionIndex ??????????????????
        authnStatement.setSessionIndex(msgId);

        return authnStatement;
    }

    public Issuer idpBuildIssuer() {
        Issuer issuer = samlUtils.buildSAMLObject(Issuer.class);
        String idpEntityId = host + "/saml/metadata";
        issuer.setValue(idpEntityId);
        return issuer;
    }

    /**
     * HTTP POST BINDING ????????????????????????????????????????????????
     * ???????????????????????????????????????
     * ??????iot????????? HTTPPostEncoder
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPArtifactEncoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPSOAP11Encoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HttpClientRequestSOAP11Encoder}
     * ???????????????
     * @param relayState
     * @param res
     * @param acsUrl
     * @param response
     * @throws ComponentInitializationException
     * @throws MessageEncodingException
     */
    private void httpPostBinding(String relayState,
                                 HttpServletResponse res, String acsUrl, Response response)
        throws ComponentInitializationException, MessageEncodingException {
        // HTTP????????????????????? openSamlImplementation ???
        MessageContext messageContext = new MessageContext();
        messageContext.setMessage(response);
        if(hasText(relayState)) {
            messageContext.getSubcontext(SAMLBindingContext.class,true).setRelayState(relayState);
        }
        SAMLEndpointContext samlEndpointContext = messageContext.getSubcontext(SAMLPeerEntityContext.class,true).getSubcontext(SAMLEndpointContext.class,true);
        Endpoint endpoint = samlUtils.buildSAMLObject(AssertionConsumerService.class);
        endpoint.setLocation(acsUrl);
        samlEndpointContext.setEndpoint(endpoint);
        //openSamlImplementation.
        HTTPPostEncoder httpPostEncoder = new HTTPPostEncoder();
        httpPostEncoder.setMessageContext(messageContext);
        httpPostEncoder.setVelocityEngine(velocityEngine);
        httpPostEncoder.setVelocityTemplateId(TEMPLATE_PATH);
        httpPostEncoder.setHttpServletResponse(res);
        httpPostEncoder.initialize();
        httpPostEncoder.encode();
    }

    /**
     * Velocity ??????
     */
    private VelocityEngine velocityEngine;

    public SAMLController() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT,
            "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.OUTPUT_ENCODING,
            "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER,
            "classpath");
        velocityEngine
            .setProperty("classpath.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();
    }


}
