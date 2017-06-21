/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.testsuite.telephony;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.sip.Dialog;
import javax.sip.address.SipURI;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.CreateClientsTool;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
import org.restcomm.connect.testsuite.http.RestcommConferenceParticipantsTool;
import org.restcomm.connect.testsuite.http.RestcommConferenceTool;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Test for Dial Action attribute for organization
 *
 * @author maria
 *
 */
@RunWith(Arquillian.class)
public class DialActionTestOrganization {

    private final static Logger logger = Logger.getLogger(DialActionTestOrganization.class.getName());

    private static final String version = Version.getVersion();
    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
        53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
        48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
        13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
        86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;
    private static SipStackTool tool5;
    private static SipStackTool tool6;
    private static SipStackTool tool7;

    private SipStack bobSipStackOrg2;
    private SipPhone bobPhoneOrg2;
    private String bobContactOrg2 = "sip:bob@org2.restcomm.com";

    private SipStack bobSipStackOrg3;
    private SipPhone bobPhoneOrg3;
    private String bobContactOrg3 = "sip:bob@org3.restcomm.com";

    private SipStack bobSipStackDefaultOrg;
    private SipPhone bobPhoneDefaultOrg;
    private String bobContactDefaultOrg = "sip:bob@127.0.0.1:5096";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed. Alice belong to organization org2.restcomm.com
    private SipStack aliceSipStackOrg2;
    private SipPhone alicePhoneOrg2;
    private String aliceContactOrg2 = "sip:alice@org2.restcomm.com";

    // Maria is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack mariaSipStackOrg2;
    private SipPhone mariaPhoneOrg2;
    private String mariaContactOrg2 = "sip:maria@org2.restcomm.com";

    // Shoaibs is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack shoaibSipStackOrg2;
    private SipPhone shoaibPhoneOrg2;
    private String shoaibContactOrg2 = "sip:shoaib@org2.restcomm.com";

    private String mariaRestcommClientSidOrg2;
    private String shoaibRestcommClientSidOrg2;
    private String bobRestcommClientSidOrg2;
    private String bobRestcommClientSidOrg3;
    private String clientPassword = "qwerty1234RT";

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed. Alice belong to organization: org3.restcomm.com
    private SipStack aliceSipStackOrg3;
    private SipPhone alicePhoneOrg3;
    private String aliceContactOrg3 = "sip:alice@org3.restcomm.com";

    private String providerNumberOrg2 = "sip:+12223334467@org2.restcomm.com"; // Application: dial-client-entry_wActionUrl.xml
    private String pureSipNumberOrg3 = "sip:+12223334467@org3.restcomm.com"; // Application: dial-client-entry_wActionUrl.xml
    private String numberWithDefaultDomain = "sip:+12223334467@127.0.0.1:5080"; // Application: dial-client-entry_wActionUrl.xml
    private String dialClientWithActionUrlOrg2 = "sip:+12223334455@org2.restcomm.com"; // Application: dial-client-entry_wActionUrl.xml
    private String dialClientWithActionUrlOrg3 = "sip:+12223334455@org3.restcomm.com"; // Application: dial-client-entry_wActionUrl.xml of organization: org3.restcomm.com
    
    private String dialRestcommConferenceOrg2 = "sip:1111@org2.restcomm.com";
    private final String confRoom2 = "confRoom2";
    private String dialConfernceRcml = "<Response><Dial><Conference>"+confRoom2+"</Conference></Dial></Response>";
    

    private String superAdminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAccountSidOrg2 = "ACae6e420f425248d6a26948c17a9e2acg";
    private String adminAccountSidOrg3 = "ACae6e420f425248d6a26948c17a9e2ach";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("DialActionTest1");
        tool2 = new SipStackTool("DialActionTest2");
        tool3 = new SipStackTool("DialActionTest3");
        tool4 = new SipStackTool("DialActionTest4");
        tool5 = new SipStackTool("DialActionTest5");
        tool6 = new SipStackTool("DialActionTest6");
        tool7 = new SipStackTool("DialActionTest7");
    }

    @Before
    public void before() throws Exception {
        bobSipStackOrg2 = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhoneOrg2 = bobSipStackOrg2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContactOrg2);

        aliceSipStackOrg2 = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5091", "127.0.0.1:5080");
        alicePhoneOrg2 = aliceSipStackOrg2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContactOrg2);

        bobSipStackOrg3 = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        bobPhoneOrg3 = bobSipStackOrg3.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContactOrg3);

        mariaSipStackOrg2 = tool5.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5093", "127.0.0.1:5080");
        mariaPhoneOrg2 = mariaSipStackOrg2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaContactOrg2);

        shoaibSipStackOrg2 = tool6.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5080");
        shoaibPhoneOrg2 = shoaibSipStackOrg2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, shoaibContactOrg2);

        aliceSipStackOrg3 = tool7.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5095", "127.0.0.1:5080");
        alicePhoneOrg3 = aliceSipStackOrg3.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, aliceContactOrg3);

        bobSipStackDefaultOrg = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5096", "127.0.0.1:5080");
        bobPhoneDefaultOrg = bobSipStackDefaultOrg.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContactDefaultOrg);
        
        mariaRestcommClientSidOrg2 = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), adminAccountSidOrg2, adminAuthToken, "maria", clientPassword, null);
        shoaibRestcommClientSidOrg2 = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), adminAccountSidOrg2, adminAuthToken, "shoaib", clientPassword, null);
        bobRestcommClientSidOrg2 = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), adminAccountSidOrg2, adminAuthToken, "bob", clientPassword, null);
        bobRestcommClientSidOrg3 = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), adminAccountSidOrg3, adminAuthToken, "bob", clientPassword, null);

    }

    /**
     * testDialNumberExistingInMultipleOrganization:
     * 
     * given we have 5 clients:
     * 1. alice @ org2.
     * 2. alice @ org3.
     * 3. bob @ org2.
     * 4. bob @ org3.
     * 5. alice @ defaultOrg.
     * 
     * we have 2 number:
     * +12223334467@org2.restcomm.com is provider number and mapped on dial action to call alice@org2.
     * +12223334467@org3.restcomm.com is pure sip number and mapped on dial action to call alice@org3.
     * 
     * test case 1: bob@org2 created INVITE - sip:+12223334467@org3.restcomm.com -> call should NOT go to alice@org3 (bcz 12223334467@org3.restcomm.com is pure sip) - instead call should FAIL
     * test case 2: bob@org2 created INVITE - sip:+12223334467@default.restcomm.com -> call should NOT go to alice@org2 (bcz number does not exist in default.restcomm.com) - so call should FAIL
     * test case 3: bob@org3 created INVITE - sip:+12223334467@org2.restcomm.com(a conference in org2) -> able to join conference (bcz 12223334467@org2.restcomm.com is provider number)
     * test case 4: bob@org3 created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org3
     * test case 5: alice@defaultOrg created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org2
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialNumberExistingInMultipleOrganizationCase1() throws ParseException, InterruptedException, UnknownHostException {
    	stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                    .withStatus(200)));
    	/*
    	 * test case 1 - bob@org2 created INVITE - sip:+12223334467@org3.restcomm.com -> call should NOT go to alice@org3 (bcz 12223334467@org3.restcomm.com is pure sip) - instead call should FAIL
    	 */

    	assertNotNull(bobRestcommClientSidOrg2);
        SipURI uri = bobSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg2.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
        Credential c = new Credential("org3.restcomm.com", "bob", clientPassword);
        bobPhoneOrg2.addUpdateCredential(c);

        //register as alice@org2.restcomm.com
        uri = aliceSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg2.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));
        SipCall aliceCallOrg2 = alicePhoneOrg2.createSipCall();
        aliceCallOrg2.listenForIncomingCall();

        // bob@org2.restcomm.com - dials a pure sip number in org3.
        final SipCall bobCallOrg2 = bobPhoneOrg2.createSipCall();
        bobCallOrg2.initiateOutgoingCall(bobContactOrg2, pureSipNumberOrg3, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg2);
        assertTrue(bobCallOrg2.waitForAuthorisation(3000));
        
        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCallOrg2.getLastReceivedResponse().getStatusCode();
        logger.info("bobCallOrg2 response: "+response);
        assertTrue(response == Response.NOT_FOUND);
    }

    /**
     * testDialNumberExistingInMultipleOrganization:
     * 
     * given we have 5 clients:
     * 1. alice @ org2.
     * 2. alice @ org3.
     * 3. bob @ org2.
     * 4. bob @ org3.
     * 5. alice @ defaultOrg.
     * 
     * we have 2 number:
     * +12223334467@org2.restcomm.com is provider number and mapped on dial action to call alice@org2.
     * +12223334467@org3.restcomm.com is pure sip number and mapped on dial action to call alice@org3.
     * 
     * test case 1: bob@org2 created INVITE - sip:+12223334467@org3.restcomm.com -> call should NOT go to alice@org3 (bcz 12223334467@org3.restcomm.com is pure sip) - instead call should FAIL
     * test case 2: bob@org2 created INVITE - sip:+12223334467@default.restcomm.com -> call should FAIL (bcz defaulOrg does not have that number)
     * test case 3: bob@org3 created INVITE - sip:+12223334467@org2.restcomm.com(a conference in org2) -> able to join conference (bcz 12223334467@org2.restcomm.com is provider number)
     * test case 4: bob@org3 created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org3
     * test case 5: alice@defaultOrg created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org2
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialNumberExistingInMultipleOrganizationCase2() throws ParseException, InterruptedException, UnknownHostException {
    	stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                    .withStatus(200)));
    	/*
    	 * test case 2: bob@org2 created INVITE - sip:+12223334467@default.restcomm.com -> call should FAIL (bcz defaulOrg does not have that number)
    	 */

    	assertNotNull(bobRestcommClientSidOrg2);
        SipURI uri = bobSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg2.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
        Credential c = new Credential("127.0.0.1", "bob", clientPassword);
        bobPhoneOrg2.addUpdateCredential(c);

        //register as alice@org2.restcomm.com
        uri = aliceSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg2.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));
        SipCall aliceCallOrg2 = alicePhoneOrg2.createSipCall();
        aliceCallOrg2.listenForIncomingCall();

        // bob@org2.restcomm.com - dials a pure sip number in org3.
        final SipCall bobCallOrg2 = bobPhoneOrg2.createSipCall();
        bobCallOrg2.initiateOutgoingCall(bobContactOrg2, numberWithDefaultDomain, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg2);
        assertTrue(bobCallOrg2.waitForAuthorisation(3000));
        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCallOrg2.getLastReceivedResponse().getStatusCode();
        logger.info("bobCallOrg2 response: "+response);
        assertTrue(response == Response.NOT_FOUND);
    }

    /**
     * testDialNumberExistingInMultipleOrganization:
     * 
     * given we have 5 clients:
     * 1. alice @ org2.
     * 2. alice @ org3.
     * 3. bob @ org2.
     * 4. bob @ org3.
     * 5. alice @ defaultOrg.
     * 
     * we have 2 number:
     * +12223334467@org2.restcomm.com is provider number and mapped on dial action to call alice@org2.
     * +12223334467@org3.restcomm.com is pure sip number and mapped on dial action to call alice@org3.
     * 
     * test case 1: bob@org2 created INVITE - sip:+12223334467@org3.restcomm.com -> call should NOT go to alice@org3 (bcz 12223334467@org3.restcomm.com is pure sip) - instead call should FAIL
     * test case 2: bob@org2 created INVITE - sip:+12223334467@default.restcomm.com -> call should FAIL (bcz defaulOrg does not have that number)
     * test case 3: bob@org3 created INVITE - sip:+12223334467@org2.restcomm.com(a conference in org2) -> able to join conference (bcz 12223334467@org2.restcomm.com is provider number)
     * test case 4: bob@org3 created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org3
     * test case 5: alice@defaultOrg created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org2
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialNumberExistingInMultipleOrganizationJoinConferenceOfDifferentOrgCase3() throws ParseException, InterruptedException, UnknownHostException {
    	stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialConfernceRcml)));
    	/*
    	 * test case 3: bob@org3 created INVITE - sip:+12223334467@org2.restcomm.com(a conference in org2) -> able to join conference (bcz 12223334467@org2.restcomm.com is provider number)
    	 */

    	//bob@org3 joins conference via sip:+12223334467@org2.restcomm.com
    	assertNotNull(bobRestcommClientSidOrg3);
    	SipURI uri = bobSipStackOrg3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg3.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5092", 3600, 3600));
        Credential c = new Credential("org2.restcomm.com", "bob", clientPassword);
        bobPhoneOrg3.addUpdateCredential(c);

        final SipCall bobCallOrg3 = bobPhoneOrg3.createSipCall();
        bobCallOrg3.initiateOutgoingCall(bobContactOrg3, dialRestcommConferenceOrg2, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg3);
        assertTrue(bobCallOrg3.waitForAuthorisation(3000));
        assertTrue(bobCallOrg3.waitOutgoingCallResponse(5 * 1000));
        int responseBobOrg3 = bobCallOrg3.getLastReceivedResponse().getStatusCode();
        logger.info("responseBobOrg3: "+responseBobOrg3);
        assertTrue(responseBobOrg3 == Response.TRYING || responseBobOrg3 == Response.RINGING);

        if (responseBobOrg3 == Response.TRYING) {
            assertTrue(bobCallOrg3.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCallOrg3.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCallOrg3.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCallOrg3.getLastReceivedResponse().getStatusCode());
        bobCallOrg3.sendInviteOkAck();
        assertTrue(!(bobCallOrg3.getLastReceivedResponse().getStatusCode() >= 400));

    	//bob@org2 joins conference via sip:+12223334467@org2.restcomm.com
    	assertNotNull(bobRestcommClientSidOrg2);
        uri = bobSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg2.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
        c = new Credential("org2.restcomm.com", "bob", clientPassword);
        bobPhoneOrg2.addUpdateCredential(c);

    	final SipCall bobCallOrg2 = bobPhoneOrg2.createSipCall();
        bobCallOrg2.initiateOutgoingCall(bobContactOrg2, dialRestcommConferenceOrg2, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg2);
        assertTrue(bobCallOrg2.waitForAuthorisation(3000));
        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        int responseBobOrg2 = bobCallOrg2.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBobOrg2 == Response.TRYING || responseBobOrg2 == Response.RINGING);

        if (responseBobOrg2 == Response.TRYING) {
            assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCallOrg2.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCallOrg2.getLastReceivedResponse().getStatusCode());
        bobCallOrg2.sendInviteOkAck();
        assertTrue(!(bobCallOrg2.getLastReceivedResponse().getStatusCode() >= 400));

        Thread.sleep(2000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), superAdminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), superAdminAccountSid, adminAuthToken);
        logger.info("&&&&& LiveCalls: "+liveCalls);
        logger.info("&&&&& LiveCallsArraySize: "+liveCallsArraySize);
        assertTrue(liveCalls == 2);
        assertTrue(liveCallsArraySize == 2);
        assertTrue(getConferencesSize()>=1);
        int numOfParticipants = getParticipantsSize(confRoom2);
        logger.info("Number of participants: "+numOfParticipants);
        assertTrue(numOfParticipants==2);

        Thread.sleep(3000);

        bobCallOrg3.disconnect();
        bobCallOrg2.disconnect();

        Thread.sleep(5000);
        liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), superAdminAccountSid, adminAuthToken);
        liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), superAdminAccountSid, adminAuthToken);
        logger.info("&&&&& LiveCalls: "+liveCalls);
        logger.info("&&&&& LiveCallsArraySize: "+liveCallsArraySize);
        assertTrue(liveCalls == 0);
        assertTrue(liveCallsArraySize == 0);
        assertTrue(getConferencesSize()>=1);
        int confRoom2Participants = getParticipantsSize(confRoom2);
        logger.info("&&&&& ConfRoom2Participants: "+confRoom2Participants);
        assertTrue(confRoom2Participants==0);
    }

    private int getConferencesSize() {
        JsonObject conferences = RestcommConferenceTool.getInstance().getConferences(deploymentUrl.toString(),superAdminAccountSid, adminAuthToken);
        JsonArray conferenceArray = conferences.getAsJsonArray("conferences");
        return conferenceArray.size();
    }

    private int getParticipantsSize(final String name) {
        JsonObject conferences = RestcommConferenceTool.getInstance().getConferences(deploymentUrl.toString(),superAdminAccountSid, adminAuthToken);
        JsonArray conferenceArray = conferences.getAsJsonArray("conferences");
        String confSid = null;
        for(int i = 0; i < conferenceArray.size(); i++) {
            JsonObject confObj = conferenceArray.get(i).getAsJsonObject();
            String confName = confObj.get("friendly_name").getAsString();
            if (confName.equalsIgnoreCase(name)) {
                confSid = confObj.get("sid").getAsString();
                break;
            }
        }
        JsonObject participants = RestcommConferenceParticipantsTool.getInstance().getParticipants(deploymentUrl.toString(), superAdminAccountSid, adminAuthToken, confSid);
        JsonArray participantsArray = participants.getAsJsonArray("calls");
        return participantsArray.size();
    }


    /**
     * testDialNumberExistingInMultipleOrganization:
     * 
     * given we have 5 clients:
     * 1. alice @ org2.
     * 2. alice @ org3.
     * 3. bob @ org2.
     * 4. bob @ org3.
     * 5. alice @ defaultOrg.
     * 
     * we have 2 number:
     * +12223334467@org2.restcomm.com is provider number and mapped on dial action to call alice@org2.
     * +12223334467@org3.restcomm.com is pure sip number and mapped on dial action to call alice@org3.
     * 
     * test case 1: bob@org2 created INVITE - sip:+12223334467@org3.restcomm.com -> call should NOT go to alice@org3 (bcz 12223334467@org3.restcomm.com is pure sip) - instead call should FAIL
     * test case 2: bob@org2 created INVITE - sip:+12223334467@default.restcomm.com -> call should FAIL (bcz defaulOrg does not have that number)
     * test case 3: bob@org3 created INVITE - sip:+12223334467@org2.restcomm.com(a conference in org2) -> able to join conference (bcz 12223334467@org2.restcomm.com is provider number)
     * test case 4: bob@org3 created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org3
     * test case 5: alice@defaultOrg created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org2
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialNumberExistingInMultipleOrganizationCase4() throws ParseException, InterruptedException, UnknownHostException {
    	stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                    .withStatus(200)));
    	/*
    	 * test case 4: bob@org3 created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org3
    	 */

    	assertNotNull(bobRestcommClientSidOrg3);
        SipURI uri = bobSipStackOrg3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg3.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
        Credential c = new Credential("org3.restcomm.com", "bob", clientPassword);
        bobPhoneOrg3.addUpdateCredential(c);

        //register as alice@org3.restcomm.com
        uri = aliceSipStackOrg3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg3.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));
        SipCall aliceCallOrg3 = alicePhoneOrg3.createSipCall();
        aliceCallOrg3.listenForIncomingCall();

        // bob@org3.restcomm.com - dials a provider number in org2.
        final SipCall bobCallOrg3 = bobPhoneOrg3.createSipCall();
        bobCallOrg3.initiateOutgoingCall(bobContactOrg3, numberWithDefaultDomain, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg3);
        assertTrue(bobCallOrg3.waitOutgoingCallResponse(5 * 1000));
        int responseFotini = bobCallOrg3.getLastReceivedResponse().getStatusCode();
        assertTrue(responseFotini == Response.TRYING || responseFotini == Response.RINGING);

        if (responseFotini == Response.TRYING) {
            assertTrue(bobCallOrg3.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCallOrg3.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCallOrg3.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCallOrg3.getLastReceivedResponse().getStatusCode());
        bobCallOrg3.sendInviteOkAck();
        assertTrue(!(bobCallOrg3.getLastReceivedResponse().getStatusCode() >= 400));

        // Wait for the media to play and the call to hangup.
        bobCallOrg3.listenForDisconnect();

        // Start a new thread for bob to wait disconnect
        new Thread(new Runnable() {
            @Override
            public void run() {
                assertTrue(bobCallOrg3.waitForDisconnect(30 * 1000));
            }
        }).start();
    }


    /**
     * testDialNumberExistingInMultipleOrganization:
     * 
     * given we have 5 clients:
     * 1. alice @ org2.
     * 2. alice @ org3.
     * 3. bob @ org2.
     * 4. bob @ org3.
     * 5. alice @ defaultOrg.
     * 
     * we have 2 number:
     * +12223334467@org2.restcomm.com is provider number and mapped on dial action to call alice@org2.
     * +12223334467@org3.restcomm.com is pure sip number and mapped on dial action to call alice@org3.
     * 
     * test case 1: bob@org2 created INVITE - sip:+12223334467@org3.restcomm.com -> call should NOT go to alice@org3 (bcz 12223334467@org3.restcomm.com is pure sip) - instead call should FAIL
     * test case 2: bob@org2 created INVITE - sip:+12223334467@default.restcomm.com -> call should FAIL (bcz defaulOrg does not have that number)
     * test case 3: bob@org3 created INVITE - sip:+12223334467@org2.restcomm.com(a conference in org2) -> able to join conference (bcz 12223334467@org2.restcomm.com is provider number)
     * test case 4: bob@org3 created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org3
     * test case 5: alice@defaultOrg created INVITE - sip:+12223334467@default.restcomm.com -> call should go to alice@org2
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialNumberExistingInMultipleOrganizationCase5() throws ParseException, InterruptedException, UnknownHostException {
    	stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                    .withStatus(200)));
    	/*
    	 * test case 1
    	 */

    	assertNotNull(bobRestcommClientSidOrg2);
        SipURI uri = bobSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg2.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
        Credential c = new Credential("org3.restcomm.com", "bob", clientPassword);
        bobPhoneOrg2.addUpdateCredential(c);

        //register as alice@org2.restcomm.com, alice@org3.restcomm.com and alice@default.restcomm.com
        uri = aliceSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg2.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));
        SipCall aliceCallOrg2 = alicePhoneOrg2.createSipCall();
        aliceCallOrg2.listenForIncomingCall();
        uri = aliceSipStackOrg3.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg3.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5095", 3600, 3600));
        SipCall aliceCallOrg3 = alicePhoneOrg3.createSipCall();
        aliceCallOrg3.listenForIncomingCall();

        // bob@org2.restcomm.com - dials a pure sip number in org3.
        final SipCall bobCallOrg2 = bobPhoneOrg2.createSipCall();
        bobCallOrg2.initiateOutgoingCall(bobContactOrg2, pureSipNumberOrg3, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg2);
        assertTrue(bobCallOrg2.waitForAuthorisation(3000));
        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCallOrg2.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);
        if (response == Response.TRYING) {
            assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCallOrg2.getLastReceivedResponse().getStatusCode());
        }
        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCallOrg2.getLastReceivedResponse().getStatusCode());

        bobCallOrg2.sendInviteOkAck();
        assertTrue(!(bobCallOrg2.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCallOrg2.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCallOrg2.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCallOrg2.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCallOrg2.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCallOrg2.waitForAck(50 * 1000));
        Thread.sleep(3000);
        // hangup.
        aliceCallOrg2.disconnect();
        bobCallOrg2.listenForDisconnect();
        assertTrue(bobCallOrg2.waitForDisconnect(30 * 1000));
        assertTrue(bobCallOrg2.respondToDisconnect());
        try {
            Thread.sleep(50 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        Thread.sleep(3000);

        logger.info("About to check the DialAction Requests");
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/DialAction.*")));
        assertEquals(1, requests.size());
        String requestBody = requests.get(0).getBodyAsString();
        String[] params = requestBody.split("&");
        assertTrue(requestBody.contains("DialCallStatus=completed"));
        assertTrue(requestBody.contains("To=%2B12223334467"));
        assertTrue(requestBody.contains("From=bob"));
        assertTrue(requestBody.contains("DialCallDuration=3"));
        Iterator iter = Arrays.asList(params).iterator();
        String dialCallSid = null;
        while (iter.hasNext()) {
            String param = (String) iter.next();
            if (param.startsWith("DialCallSid")) {
                dialCallSid = param.split("=")[1];
                break;
            }
        }
        assertNotNull(dialCallSid);
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSidOrg2, adminAuthToken, dialCallSid);
        assertNotNull(cdr);
    }
    /**
     * testClientsCallEachOtherSameOrganization
     * given clients:
     * 
     * maria belongs to org: org2.restcomm.com
     * shoaib belong to org: org2.restcomm.com
     * 
     * test case: maria calls shoaib.
     * 
     * result: call goes through
     * 
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testClientsCallEachOtherSameOrganization() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSidOrg2);
        assertNotNull(shoaibRestcommClientSidOrg2);

        SipURI uri = mariaSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(mariaPhoneOrg2.register(uri, "maria", clientPassword, "sip:maria@127.0.0.1:5093", 3600, 3600));
        assertTrue(shoaibPhoneOrg2.register(uri, "shoaib", clientPassword, "sip:shoaib@127.0.0.1:5094", 3600, 3600));

        Credential c = new Credential("org2.restcomm.com", "maria", clientPassword);
        mariaPhoneOrg2.addUpdateCredential(c);

        final SipCall shoaibCall = shoaibPhoneOrg2.createSipCall();
        shoaibCall.listenForIncomingCall();

        Thread.sleep(1000);

        // Maria initiates a call to Shoaib
        long startTime = System.currentTimeMillis();
        final SipCall mariaCall = mariaPhoneOrg2.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContactOrg2, shoaibContactOrg2, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        assertTrue(shoaibCall.waitForIncomingCall(5000));
        assertTrue(shoaibCall.sendIncomingCallResponse(100, "Trying-Shoaib", 1800));
        assertTrue(shoaibCall.sendIncomingCallResponse(180, "Ringing-Shoaib", 1800));
        String receivedBody = new String(shoaibCall.getLastReceivedRequest().getRawContent());
        assertTrue(shoaibCall.sendIncomingCallResponse(Response.OK, "OK-Shoaib", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 2000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
        }

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
        assertTrue(mariaCall.getDialog().equals(mariaDialog));
        mariaCall.sendInviteOkAck();
        assertTrue(mariaCall.getDialog().equals(mariaDialog));

        assertTrue(!(mariaCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(shoaibCall.waitForAck(3000));

        //Talk time ~ 3sec
        Thread.sleep(3000);
        shoaibCall.listenForDisconnect();
        assertTrue(mariaCall.disconnect());

        assertTrue(shoaibCall.waitForDisconnect(5 * 1000));
        assertTrue(shoaibCall.respondToDisconnect());
        long endTime   = System.currentTimeMillis();

        double totalTime = (endTime - startTime)/1000.0;
        assertTrue(3.0 <= totalTime);
        assertTrue(totalTime <= 4.0);

        Thread.sleep(3000);

        //Check CDR
        JsonObject cdrs = RestcommCallsTool.getInstance().getCalls(deploymentUrl.toString(), adminAccountSidOrg2, adminAuthToken);
        assertNotNull(cdrs);
        JsonArray cdrsArray = cdrs.get("calls").getAsJsonArray();
        System.out.println("cdrsArray.size(): "+cdrsArray.size());
        assertTrue(cdrsArray.size() == 1);

    }

    /**
     * testClientsCallEachOtherDifferentOrganization
     * given clients:
     * 
     * maria belongs to org: org2.restcomm.com
     * alice belong to org: org3.restcomm.com
     * 
     * test case: maria calls alice.
     * 
     * result: call do not go through
     * 
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testClientsCallEachOtherDifferentOrganization() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSidOrg2);

        SipURI uri = mariaSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(mariaPhoneOrg2.register(uri, "maria", clientPassword, "sip:maria@127.0.0.1:5093", 3600, 3600));
        assertTrue(alicePhoneOrg3.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5095", 3600, 3600));
        //following realm is a cheat/hack to get through authorization and test organization related p2p logic.
        Credential c = new Credential("org3.restcomm.com", "maria", clientPassword);
        mariaPhoneOrg2.addUpdateCredential(c);

        final SipCall aliceCallOrg3 = alicePhoneOrg3.createSipCall();
        aliceCallOrg3.listenForIncomingCall();

        Thread.sleep(1000);

        // Maria initiates a call to Alice
        long startTime = System.currentTimeMillis();
        final SipCall mariaCall = mariaPhoneOrg2.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContactOrg2, aliceContactOrg3, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        //alice should not get the call
        assertTrue(!aliceCallOrg3.waitForIncomingCall(5000));
    }

    /**
     * testDialActionAliceAnswers:
     * given: 
     * clients:
     * bob@org2, alice@org2.
     * 
     * given numbers:
     * 12223334455@org2
     * 12223334455@org3
     * 
     * test case: bob@org2 INVITE 12223334455@org2
     * result: call goes to alice@org2.
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialActionAliceAnswers() throws ParseException, InterruptedException, UnknownHostException {

       stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                    .withStatus(200)));



   	    assertNotNull(bobRestcommClientSidOrg2);
   	    SipURI uri = bobSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
   	    assertTrue(bobPhoneOrg2.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
   	    Credential c = new Credential("org2.restcomm.com", "bob", clientPassword);
   	    bobPhoneOrg2.addUpdateCredential(c);
       
   	    //register as alice@org2
        uri = aliceSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg2.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));
        // Prepare alice's phone to receive call
        SipCall aliceCall = alicePhoneOrg2.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with bob@org
        final SipCall bobCall = bobPhoneOrg2.createSipCall();
        bobCall.initiateOutgoingCall(bobContactOrg2, dialClientWithActionUrlOrg2, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCall);
        assertTrue(bobCall.waitForAuthorisation(3000));
        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());

        bobCall.sendInviteOkAck();
        assertTrue(!(bobCall.getLastReceivedResponse().getStatusCode() >= 400));

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        Thread.sleep(3000);

        // hangup.
        aliceCall.disconnect();

        bobCall.listenForDisconnect();
        assertTrue(bobCall.waitForDisconnect(30 * 1000));
        assertTrue(bobCall.respondToDisconnect());
        try {
            Thread.sleep(50 * 1000);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }

        Thread.sleep(3000);

        logger.info("About to check the DialAction Requests");
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/DialAction.*")));
        assertEquals(1, requests.size());
        String requestBody = requests.get(0).getBodyAsString();
        String[] params = requestBody.split("&");
        assertTrue(requestBody.contains("DialCallStatus=completed"));
        assertTrue(requestBody.contains("To=%2B12223334455"));
        assertTrue(requestBody.contains("From=bob"));
        assertTrue(requestBody.contains("DialCallDuration=3"));
        Iterator iter = Arrays.asList(params).iterator();
        String dialCallSid = null;
        while (iter.hasNext()) {
            String param = (String) iter.next();
            if (param.startsWith("DialCallSid")) {
                dialCallSid = param.split("=")[1];
                break;
            }
        }
        assertNotNull(dialCallSid);
        JsonObject cdr = RestcommCallsTool.getInstance().getCall(deploymentUrl.toString(), adminAccountSidOrg2, adminAuthToken, dialCallSid);
        assertNotNull(cdr);
    }


    /**
     * testDialActionHangupWithLCM:
     * given: 
     * clients:
     * bob@org2, alice@org2.
     * 
     * given numbers:
     * 12223334455@org2
     * 12223334455@org3
     * 
     * test case1: bob@org2 INVITE 12223334455@org2
     * result: call goes to alice@org2.
     * 
     * test case2: hangup using  LCM
     * result: call completes
     * 
     * @throws ParseException
     * @throws InterruptedException
     * @throws UnknownHostException
     */
    @Test
    public void testDialActionHangupWithLCM() throws Exception {

        stubFor(post(urlPathMatching("/DialAction.*"))
                .willReturn(aResponse()
                        .withStatus(200)));



    	assertNotNull(bobRestcommClientSidOrg2);
        SipURI uri = bobSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(bobPhoneOrg2.register(uri, "bob", clientPassword, "sip:bob@127.0.0.1:5090", 3600, 3600));
        Credential c = new Credential("org3.restcomm.com", "bob", clientPassword);
        bobPhoneOrg2.addUpdateCredential(c);

        // Phone2 register as alice
        uri = aliceSipStackOrg2.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(alicePhoneOrg2.register(uri, "alice", "1234", "sip:alice@127.0.0.1:5091", 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCallOrg2 = alicePhoneOrg2.createSipCall();
        aliceCallOrg2.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall bobCallOrg2 = bobPhoneOrg2.createSipCall();
        bobCallOrg2.initiateOutgoingCall(bobContactOrg2, dialClientWithActionUrlOrg2, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(bobCallOrg2);
        assertTrue(bobCallOrg2.waitForAuthorisation(3000));
        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        final int response = bobCallOrg2.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCallOrg2.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(bobCallOrg2.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCallOrg2.getLastReceivedResponse().getStatusCode());

        String callSid = bobCallOrg2.getLastReceivedResponse().getMessage().getHeader("X-RestComm-CallSid").toString().split(":")[1].trim().split("-")[1];

        bobCallOrg2.sendInviteOkAck();
        assertTrue(!(bobCallOrg2.getLastReceivedResponse().getStatusCode() >= 400));
        bobCallOrg2.listenForDisconnect();

        assertTrue(aliceCallOrg2.waitForIncomingCall(30 * 1000));
        aliceCallOrg2.listenForCancel();

        logger.info("About to execute LCM to hangup the call");
        RestcommCallsTool.getInstance().modifyCall(deploymentUrl.toString(),adminAccountSidOrg2,adminAuthToken,callSid,"completed", null);

        assertTrue(bobCallOrg2.waitForDisconnect(50 * 1000));
        assertTrue(bobCallOrg2.respondToDisconnect());

//        logger.info("&&&&&&&&&&&&&&&&&&&&&& Alice about to listen for CANCEL");
//        SipTransaction sipTransaction = aliceCall.waitForCancel(50 * 1000);
//        assertNotNull(sipTransaction);
//        aliceCall.respondToCancel(sipTransaction,200,"Alice-OK-To-Cancel",3600);
//        aliceCall.respondToCancel(sipTransaction,487,"Alice-Request-Terminated",3600);

        Thread.sleep(10 * 1000);

        logger.info("About to check the DialAction Requests");
        List<LoggedRequest> requests = findAll(postRequestedFor(urlPathMatching("/DialAction.*")));
        assertEquals(1, requests.size());
        String requestBody = requests.get(0).getBodyAsString();
        String[] params = requestBody.split("&");
        assertTrue(requestBody.contains("DialCallStatus=completed"));
        assertTrue(requestBody.contains("To=%2B12223334455"));
        assertTrue(requestBody.contains("From=bob"));
        assertTrue(requestBody.contains("DialCallDuration=0"));
        assertTrue(requestBody.contains("CallStatus=completed"));
        Iterator iter = Arrays.asList(params).iterator();
    }

    @After
    public void after() throws Exception {
        if (bobPhoneOrg2 != null) {
            bobPhoneOrg2.dispose();
        }
        if (bobSipStackOrg2 != null) {
            bobSipStackOrg2.dispose();
        }
        if (bobPhoneOrg3 != null) {
            bobPhoneOrg3.dispose();
        }
        if (bobSipStackOrg3 != null) {
            bobSipStackOrg3.dispose();
        }
        if (bobPhoneDefaultOrg != null) {
            bobPhoneDefaultOrg.dispose();
        }
        if (bobSipStackDefaultOrg != null) {
            bobSipStackDefaultOrg.dispose();
        }
        if (mariaPhoneOrg2 != null) {
            mariaPhoneOrg2.dispose();
        }
        if (mariaSipStackOrg2 != null) {
            mariaSipStackOrg2.dispose();
        }

        if (aliceSipStackOrg2 != null) {
            aliceSipStackOrg2.dispose();
        }
        if (alicePhoneOrg2 != null) {
            alicePhoneOrg2.dispose();
        }

        if (aliceSipStackOrg3 != null) {
            aliceSipStackOrg3.dispose();
        }
        if (alicePhoneOrg3 != null) {
            alicePhoneOrg3.dispose();
        }

        if (shoaibSipStackOrg2 != null) {
            shoaibSipStackOrg2.dispose();
        }
        if (shoaibPhoneOrg2 != null) {
        	shoaibPhoneOrg2.dispose();
        }

        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    @Deployment(name = "DialAction", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialActionTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-client-entry_wActionUrl.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}