/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.conn.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpHost;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.localserver.LocalServerTestBase;
import org.apache.http.localserver.SSLTestContexts;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for {@link SSLConnectionSocketFactory}.
 */
public class TestSSLSocketFactory {

    private HttpServer server;

    @After
    public void shutDown() throws Exception {
        if (this.server != null) {
            this.server.shutdown(10, TimeUnit.SECONDS);
        }
    }

    static class TestX509HostnameVerifier implements X509HostnameVerifier {

        private boolean fired = false;

        @Override
        public boolean verify(final String host, final SSLSession session) {
            return true;
        }

        @Override
        public void verify(final String host, final SSLSocket ssl) throws IOException {
            this.fired = true;
        }

        @Override
        public void verify(final String host, final String[] cns, final String[] subjectAlts) throws SSLException {
        }

        @Override
        public void verify(final String host, final X509Certificate cert) throws SSLException {
        }

        public boolean isFired() {
            return this.fired;
        }

    }

    @Test
    public void testBasicSSL() throws Exception {
        this.server = ServerBootstrap.bootstrap()
                .setServerInfo(LocalServerTestBase.ORIGIN)
                .setSslContext(SSLTestContexts.createServerSSLContext())
                .create();
        this.server.start();

        final HttpContext context = new BasicHttpContext();
        final TestX509HostnameVerifier hostVerifier = new TestX509HostnameVerifier();
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLTestContexts.createClientSSLContext(), hostVerifier);
        final Socket socket = socketFactory.createSocket(context);
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", this.server.getLocalPort());
        final HttpHost target = new HttpHost("localhost", this.server.getLocalPort(), "https");
        final SSLSocket sslSocket = (SSLSocket) socketFactory.connectSocket(0, socket, target, remoteAddress, null, context);
        try {
            final SSLSession sslsession = sslSocket.getSession();

            Assert.assertNotNull(sslsession);
            Assert.assertTrue(hostVerifier.isFired());
        } finally {
            sslSocket.close();
        }
    }

    @Test
    public void testClientAuthSSL() throws Exception {
        this.server = ServerBootstrap.bootstrap()
                .setServerInfo(LocalServerTestBase.ORIGIN)
                .setSslContext(SSLTestContexts.createServerSSLContext())
                .create();
        this.server.start();

        final HttpContext context = new BasicHttpContext();
        final TestX509HostnameVerifier hostVerifier = new TestX509HostnameVerifier();
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLTestContexts.createClientSSLContext(), hostVerifier);
        final Socket socket = socketFactory.createSocket(context);
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", this.server.getLocalPort());
        final HttpHost target = new HttpHost("localhost", this.server.getLocalPort(), "https");
        final SSLSocket sslSocket = (SSLSocket) socketFactory.connectSocket(0, socket, target, remoteAddress, null, context);
        try {
            final SSLSession sslsession = sslSocket.getSession();

            Assert.assertNotNull(sslsession);
            Assert.assertTrue(hostVerifier.isFired());
        } finally {
            sslSocket.close();
        }
    }

    @Ignore("There is no way to force client auth with HttpServer in 4.4a1")
    @Test(expected=IOException.class)
    public void testClientAuthSSLFailure() throws Exception {
        this.server = ServerBootstrap.bootstrap()
                .setServerInfo(LocalServerTestBase.ORIGIN)
                .setSslContext(SSLTestContexts.createServerSSLContext())
                .create();
        this.server.start();

        final HttpContext context = new BasicHttpContext();
        final TestX509HostnameVerifier hostVerifier = new TestX509HostnameVerifier();
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLTestContexts.createClientSSLContext(), hostVerifier);
        final Socket socket = socketFactory.createSocket(context);
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", this.server.getLocalPort());
        final HttpHost target = new HttpHost("localhost", this.server.getLocalPort(), "https");
        final SSLSocket sslSocket = (SSLSocket) socketFactory.connectSocket(0, socket, target, remoteAddress, null, context);
        try {
            final SSLSession sslsession = sslSocket.getSession();

            Assert.assertNotNull(sslsession);
            Assert.assertTrue(hostVerifier.isFired());
        } finally {
            sslSocket.close();
        }
    }

    @Test
    public void testClientAuthSSLAliasChoice() throws Exception {
        // TODO unused - is there a bug in the test?
        final PrivateKeyStrategy aliasStrategy = new PrivateKeyStrategy() {

            @Override
            public String chooseAlias(
                    final Map<String, PrivateKeyDetails> aliases, final Socket socket) {
                Assert.assertEquals(2, aliases.size());
                Assert.assertTrue(aliases.containsKey("hc-test-key-1"));
                Assert.assertTrue(aliases.containsKey("hc-test-key-2"));
                return "hc-test-key-2";
            }

        };

        this.server = ServerBootstrap.bootstrap()
                .setServerInfo(LocalServerTestBase.ORIGIN)
                .setSslContext(SSLTestContexts.createServerSSLContext())
                .create();
        this.server.start();

        final HttpContext context = new BasicHttpContext();
        final TestX509HostnameVerifier hostVerifier = new TestX509HostnameVerifier();
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLTestContexts.createClientSSLContext(), hostVerifier);
        final Socket socket = socketFactory.createSocket(context);
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", this.server.getLocalPort());
        final HttpHost target = new HttpHost("localhost", this.server.getLocalPort(), "https");
        final SSLSocket sslSocket = (SSLSocket) socketFactory.connectSocket(0, socket, target, remoteAddress, null, context);
        try {
            final SSLSession sslsession = sslSocket.getSession();

            Assert.assertNotNull(sslsession);
            Assert.assertTrue(hostVerifier.isFired());
        } finally {
            sslSocket.close();
        }
    }

    @Test(expected=SSLHandshakeException.class)
    public void testSSLTrustVerification() throws Exception {
        this.server = ServerBootstrap.bootstrap()
                .setServerInfo(LocalServerTestBase.ORIGIN)
                .setSslContext(SSLTestContexts.createServerSSLContext())
                .create();
        this.server.start();

        final HttpContext context = new BasicHttpContext();
        // Use default SSL context
        final SSLContext defaultsslcontext = SSLContexts.createDefault();

        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(defaultsslcontext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final Socket socket = socketFactory.createSocket(context);
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", this.server.getLocalPort());
        final HttpHost target = new HttpHost("localhost", this.server.getLocalPort(), "https");
        final SSLSocket sslSocket = (SSLSocket) socketFactory.connectSocket(0, socket, target, remoteAddress, null, context);
        sslSocket.close();
    }

    @Test
    public void testSSLTrustVerificationOverride() throws Exception {
        this.server = ServerBootstrap.bootstrap()
                .setServerInfo(LocalServerTestBase.ORIGIN)
                .setSslContext(SSLTestContexts.createServerSSLContext())
                .create();
        this.server.start();

        final HttpContext context = new BasicHttpContext();

        final TrustStrategy trustStrategy = new TrustStrategy() {

            @Override
            public boolean isTrusted(
                    final X509Certificate[] chain, final String authType) throws CertificateException {
                return chain.length == 1;
            }

        };
        final SSLContext sslcontext = SSLContexts.custom()
            .loadTrustMaterial(null, trustStrategy)
            .build();
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslcontext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        final Socket socket = socketFactory.createSocket(context);
        final InetSocketAddress remoteAddress = new InetSocketAddress("localhost", this.server.getLocalPort());
        final HttpHost target = new HttpHost("localhost", this.server.getLocalPort(), "https");
        final SSLSocket sslSocket = (SSLSocket) socketFactory.connectSocket(0, socket, target, remoteAddress, null, context);
        sslSocket.close();
    }

    @Test
    public void testDefaultHostnameVerifier() throws Exception {
        final SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLContexts.createDefault(),
                null);
        Assert.assertNotNull(socketFactory.getHostnameVerifier());
    }

}
