/*
 * Copyright 2011 Glenn Maynard
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class TestPipelining extends AbstractBOSHTest {
    @Test(timeout=5000)
    public void testSendWindow() throws Exception {
        // Send session initialization.
        session.send(ComposableBody.builder().build());

        StubConnection conn = cm.awaitConnection();
        AbstractBody req = conn.getRequest().getBody();

        // This test only makes sense if BOSHClient supports pipelining, which
        // is indicated by @hold being greater than 1.
        int hold = Integer.parseInt(req.getAttribute(Attributes.HOLD));
        assertTrue("Receive windowing is not enabled", hold > 1);

        int requests = hold + 1;

        // Send a session creation request acknowledging the higher hold size.
        AbstractBody scr = getSessionCreationResponse(req)
                .setAttribute(Attributes.HOLD, new Integer(hold).toString())
                .setAttribute(Attributes.REQUESTS, new Integer(requests).toString())
                .build();
        conn.sendResponse(scr);

        // We should be able to pipeline up to "requests" requests without blocking.
        for(int i = 0; i < requests; ++i)
            session.send(ComposableBody.builder().build());

        // The CM should receive all of the requests, without having to respond to
        // any of them.
        int expectedResponses = requests;
        while(expectedResponses > 0) {
            cm.awaitConnection();
            --expectedResponses;
        }
    }

    /**
     * If 'hold' is set to a value greater than 1, the client can hold more
     * than one connection idle at a time.  This increases the number of
     * responses the server can send to the client without waiting for a
     * new empty request.  This is analogous to the receive window for TCP.
     */
    // @Test
    public void testReceiveWindow() throws Exception {
        // Send session initialization.
        session.send(ComposableBody.builder().build());

        StubConnection conn = cm.awaitConnection();
        AbstractBody req = conn.getRequest().getBody();

        // This test only makes sense if BOSHClient supports pipelining, which
        // is indicated by @hold being greater than 1.
        int hold = Integer.parseInt(req.getAttribute(Attributes.HOLD));
        assertTrue("Receive windowing is not enabled", hold > 1);

        // Send a session creation request acknowledging the higher hold size,
        // and setting POLLING to 0, so no limits are placed on how quickly the
        // empty requests can be sent.
        AbstractBody scr = getSessionCreationResponse(req)
                .setAttribute(Attributes.HOLD, new Integer(hold).toString())
                .setAttribute(Attributes.POLLING, "0") // enable empty requests
                .setAttribute(Attributes.REQUESTS, new Integer(hold+1).toString())
                .build();
        conn.sendResponse(scr);

        // Send a request; the response will be held by the CM.  This tests that
        // the total number of empty requests is enough to fill @hold, even when
        // existing requests are already waiting.
        session.send(ComposableBody.builder().build());

        // Give BOSHClient a chance to send the empty requests.
        Thread.sleep(250);

        // 'hold' requests should now be waiting.
        assertEquals(hold, cm.pendingConnectionCount());
    }
};