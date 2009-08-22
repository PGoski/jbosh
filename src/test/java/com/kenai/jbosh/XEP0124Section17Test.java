/*
 * Copyright 2009 Mike Cumings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kenai.jbosh;

import org.junit.Test;
import static junit.framework.Assert.*;

/**
 * BOSH XEP-0124 specification section 17 tests: Error and Status Codes.
 */
public class XEP0124Section17Test extends AbstractBOSHTest {

    ///////////////////////////////////////////////////////////////////////////
    // XEP-0124 Section 17.1: HTTP Conditions

    /*
     * Non-legacy connection managers SHOULD NOT send HTTP error codes unless
     * they are communicating with a legacy client.
     */
    // BOSH CM functionality not supported.

    /*
     * Upon receiving an HTTP error (400, 403, 404), a legacy client or any
     * client that is communicating with a legacy connection manager MUST
     * consider the HTTP session to be null and void.
     */
    @Test(timeout=5000)
    public void interpretLegacyHTTPCodes() throws Exception {
        logTestStart();
        // Initiate a new session with a legacy response
        session.send(ComposableBody.builder().build());
        StubConnection conn = cm.awaitConnection();
        AbstractBody scr = ComposableBody.builder()
                .setAttribute(Attributes.SID, "123XYZ")
                .setAttribute(Attributes.WAIT, "1")
                .build();
        conn.sendResponseWithStatus(scr, 400);

        // This should not work, since the ver attr indicated legacy CM
        try {
            session.send(ComposableBody.builder().build());
            conn = cm.awaitConnection();
            conn.sendResponse(ComposableBody.builder()
                    .setAttribute(Attributes.SID, "123XYZ")
                    .build());
            fail("Did not catch legacy CM terminal binding error");
        } catch (BOSHException boshx) {
            // Good.
        }

        assertValidators(scr);
    }

    /*
     * A non-legacy client that is communicating with a non-legacy connection
     * manager MAY consider that the session is still active.
     */
    @Test(timeout=5000)
    public void ignoreNonLegacyHTTPCodes() throws Exception {
        logTestStart();
        // Initiate a new session
        session.send(ComposableBody.builder().build());
        StubConnection conn = cm.awaitConnection();
        AbstractBody scr = ComposableBody.builder()
                .setAttribute(Attributes.SID, "123XYZ")
                .setAttribute(Attributes.WAIT, "1")
                .setAttribute(Attributes.VER, "1.8")
                .build();
        conn.sendResponseWithStatus(scr, 400);

        // This should work, since the ver attr indicated non-legacy CM
        try {
            session.send(ComposableBody.builder().build());
            conn = cm.awaitConnection();
            conn.sendResponse(ComposableBody.builder()
                    .setAttribute(Attributes.SID, "123XYZ")
                    .build());
        } catch (BOSHException boshx) {
            fail("Caught boshx: " + boshx.getMessage());
        }

        assertValidators(scr);
    }

    ///////////////////////////////////////////////////////////////////////////
    // XEP-0124 Section 17.2: Terminal Binding Conditions

    /*
     * In any response it sends to the client, the connection manager MAY
     * return a fatal error by setting a 'type' attribute of the <body/>
     * element to "terminate".
     */
    @Test(timeout=5000)
    public void testTerminalBindingError() throws Exception {
        logTestStart();
        // Initiate a new session
        session.send(ComposableBody.builder().build());
        StubConnection conn = cm.awaitConnection();
        AbstractBody scr = ComposableBody.builder()
                .setAttribute(Attributes.SID, "123XYZ")
                .setAttribute(Attributes.WAIT, "1")
                .setAttribute(Attributes.VER, "1.8")
                .setAttribute(Attributes.TYPE, "terminate")
                .setAttribute(Attributes.CONDITION, "bad-request")
                .build();
        conn.sendResponse(scr);

        // TODO: should receive close event
        // Attempts to send anything else should fail
        try {
            session.send(ComposableBody.builder().build());
            conn = cm.awaitConnection();
            conn.sendResponse(ComposableBody.builder()
                    .setAttribute(Attributes.SID, "123XYZ")
                    .build());
            fail("Shouldn't be able to send after terminal binding error");
        } catch (BOSHException boshx) {
            // Good
        }
        assertValidators(scr);
    }

    /*
     * In cases where BOSH is being used to transport XMPP, any fatal XMPP
     * stream error conditions experienced between the connection manager and
     * the XMPP server SHOULD only be reported using the "remote-stream-error"
     * condition.
     */
    // Application considerations are out of slope for this library

    /*
     * If the client did not include a 'ver' attribute in its session creation
     * request then the connection manager SHOULD send a deprecated HTTP Error
     * Condition instead of this terminal binding condition.
     */
    // BOSH CM functionality not supported.

    /*
     * If the connection manager did not include a 'ver' attribute in its
     * session creation response then the client SHOULD expect it to send a
     * deprecated HTTP Error Condition instead of this terminal binding
     * condition.
     */
    @Test(timeout=5000)
    public void testDeprecatedHTTPError() throws Exception {
        logTestStart();
        // Initiate a new session
        session.send(ComposableBody.builder().build());
        StubConnection conn = cm.awaitConnection();
        AbstractBody scr = ComposableBody.builder()
                .setAttribute(Attributes.SID, "123XYZ")
                .setAttribute(Attributes.WAIT, "1")
                .build();
        conn.sendResponseWithStatus(scr, 400);

        // Attempts to send anything else should fail
        try {
            session.send(ComposableBody.builder().build());
            conn = cm.awaitConnection();
            conn.sendResponse(ComposableBody.builder()
                    .setAttribute(Attributes.SID, "123XYZ")
                    .build());
            fail("Shouldn't be able to send after terminal binding error");
        } catch (BOSHException boshx) {
            // Good
        }
        assertValidators(scr);
    }

    /*
     * The client MAY report binding errors to the connection manager as well,
     * although this is unlikely
     */
    // Not supported

    ///////////////////////////////////////////////////////////////////////////
    // XEP-0124 Section 17.3: Recoverable Binding Conditions

    /*
     * In any response it sends to the client, the connection manager MAY
     * return a recoverable error by setting a 'type' attribute of the <body/>
     * element to "error".
     */
    // BOSH CM functionality not supported.

    /*
     * If it decides to recover from the error, then the client MUST repeat the
     * HTTP request that resulted in the error, as well as all the preceding
     * HTTP requests that have not received responses.
     */
    // TODO: Retry recoverable errors

    /*
     * The content of these requests MUST be identical to the <body/> elements
     * of the original requests.
     */
    // TODO: Test that there are no changes made to the messages of retries

    ///////////////////////////////////////////////////////////////////////////
    // XEP-0124 Section 17.4: XML Payload Conditions

}
