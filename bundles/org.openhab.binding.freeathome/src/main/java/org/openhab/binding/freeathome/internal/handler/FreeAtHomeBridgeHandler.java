/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.freeathome.internal.handler;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.freeathome.internal.FreeAtHomeUpdateHandler;
import org.openhab.binding.freeathome.internal.config.FreeAtHomeBridgeConfig;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extension.abb.com.protocol.update.Update;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extension.abb.com.protocol.update.UpdateEvent;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extension.abb.com.protocol.update.UpdateManager;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extensions.abb.com.protocol.data.Channel;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extensions.abb.com.protocol.data.DataPoint;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extensions.abb.com.protocol.data.Device;
import org.openhab.binding.freeathome.internal.xmpp.rocks.extensions.abb.com.protocol.data.Project;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.net.ChannelEncryption;
import rocks.xmpp.core.session.Extension;
import rocks.xmpp.core.session.SessionStatusEvent;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.session.debug.ConsoleDebugger;
import rocks.xmpp.core.stanza.IQEvent;
import rocks.xmpp.core.stanza.MessageEvent;
import rocks.xmpp.core.stanza.PresenceEvent;
import rocks.xmpp.core.stanza.model.Message;
import rocks.xmpp.core.stanza.model.Presence;
import rocks.xmpp.extensions.component.accept.model.Handshake;
import rocks.xmpp.extensions.pubsub.PubSubManager;
import rocks.xmpp.extensions.pubsub.model.Item;
import rocks.xmpp.extensions.pubsub.model.event.Event;
import rocks.xmpp.extensions.rpc.RpcManager;
import rocks.xmpp.extensions.rpc.model.Value;
import rocks.xmpp.im.subscription.PresenceManager;
import rocks.xmpp.websocket.net.client.WebSocketConnectionConfiguration;

/**
 * Handler that connects to FreeAtHome gateway.
 *
 * @author Stian Kjoglum - Initial contribution
 *
 */
public class FreeAtHomeBridgeHandler extends BaseBridgeHandler {

    public static FreeAtHomeBridgeHandler g_freeAtHomeBridgeHandler = null;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(FreeAtHomeBridgeHandler.class);

    private WebSocketConnectionConfiguration m_WebSocketConfiguration;
    private XmppSessionConfiguration m_XmppConfiguration;
    private XmppClient m_XmppClient = null;
    private RpcManager m_RpcManager;
    private final Properties httpHeader = new Properties();
    private static final int REQUEST_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(20);

    /*
     * Store bridge configuration
     */
    protected FreeAtHomeBridgeConfig m_Configuration;

    public FreeAtHomeUpdateHandler m_UpdateHandler;

    public FreeAtHomeBridgeHandler(Bridge bridge) {
        super(bridge);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {

        FreeAtHomeBridgeConfig configuration = getConfigAs(FreeAtHomeBridgeConfig.class);

        m_Configuration = configuration;

        logger.debug("Gateway IP            {}.", m_Configuration.host);
        logger.debug("Port                  {}.", m_Configuration.port);
        logger.debug("Login                 {}.", m_Configuration.login);
        logger.debug("Password              {}.", m_Configuration.password);
        logger.debug("dummy_things_enabled  {}.", m_Configuration.dummy_things_enabled);

        m_UpdateHandler = new FreeAtHomeUpdateHandler();

        connectGateway();

        g_freeAtHomeBridgeHandler = this;

    }

    public boolean dummyThingsEnabled() {
        return m_Configuration.dummy_things_enabled;
    }

    @Override
    public void dispose() {

        onConnectionLost(ThingStatusDetail.CONFIGURATION_ERROR, "Bridge removed");

        m_UpdateHandler = null;

        try {
            m_XmppClient.close();
        } catch (XmppException e) {
            logger.error("Problems closing XMPP Client: {}", e);
        }
    }

    // @Override
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            handleCommand(channelUID, command);
        } catch (Exception e) {
            logger.warn("No bridge commands defined. Cannot process: '{}'.", command);
        }
    }

    /*
     * Call set data point via XMPP service
     */
    public void setDataPoint(String adress, String value) {

        try {

            Value response = m_RpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.setDatapoint",
                    Value.of(adress), Value.of(value)).getResult();
            logger.debug("Datapoint Result: {}", response.getAsString());
            response = null;

        } catch (XmppException e) {
            logger.debug("XMPP Exception: {}", e);
        }
    }

    public String getAll() {
        try {

            Value response = m_RpcManager.call(Jid.of("mrha@busch-jaeger.de/rpc"), "RemoteInterface.getAll",
                    Value.of("de"), Value.of(4), Value.of(0), Value.of(0)).getResult();
            logger.debug("Message {} ", response.getAsString());
            String resp = response.getAsString();

            return resp;

        } catch (XmppException e) {
            logger.debug("XMPP Exception: {} ", e);
        }
        return null;
    }

    private void connectGateway() {

        // If old session is still connected -> close xmpp session
        if (m_XmppClient != null) {
            try {
                m_XmppClient.close();
            } catch (XmppException e) {
                logger.error("Error closing existing Client Connection: {}", e);
            }
        }

        m_WebSocketConfiguration = WebSocketConnectionConfiguration.builder().hostname(m_Configuration.host)
                .port(m_Configuration.port).path("/xmpp-websocket/").channelEncryption(ChannelEncryption.DISABLED)
                .connectTimeout(30000).build();

        m_XmppConfiguration = XmppSessionConfiguration.builder()
                .extensions(Extension.of("http://abb.com/protocol/update", null, true, true, Update.class),
                        Extension.of("http://abb.com/protocol/update", null, true, Update.class),
                        Extension.of(Handshake.class))
                .debugger(ConsoleDebugger.class).authenticationMechanisms("SCRAM-SHA-1")
                .defaultResponseTimeout(Duration.ofSeconds(30)).build();

        m_XmppClient = XmppClient.create("busch-jaeger.de", m_XmppConfiguration, m_WebSocketConfiguration);

        // Listen for inbound messages.
        m_XmppClient.addInboundMessageListener(e -> onMessageEvent(e));
        m_XmppClient.addOutboundMessageListener(e -> onMessageEvent(e));
        m_XmppClient.addInboundIQListener(e -> onIQEvent(e));

        // Listen for inbound presence.
        m_XmppClient.addInboundPresenceListener(e -> onPresenceEvent(e));

        m_XmppClient.addSessionStatusListener(e -> onUpdateXMPPStatus(e));

        PubSubManager pubSubManager = m_XmppClient.getManager(PubSubManager.class);
        pubSubManager.setEnabled(true);

        UpdateManager updateManager = m_XmppClient.getManager(UpdateManager.class);
        updateManager.setEnabled(true);

        updateManager.addUpdateListener(e -> onUpdateEvent(e));

        // Connect XMPP client over websocket layer and login to SysAp
        try {
            String jid = getJid(m_Configuration.login);
            logger.debug("Found JID: {}", Jid.of(jid));

            if (jid != null) {
                Jid From = Jid.of(jid);

                try {
                    logger.info("Connecting to XMPP Client");
                    m_XmppClient.connect(From);

                    onConnectionEstablished();
                } catch (Exception e) {
                    logger.warn("Problems connecting to SysAp: {}", e);
                    onConnectionLost(ThingStatusDetail.COMMUNICATION_ERROR,
                            "Can not connect to SysAP with address: " + m_Configuration.host);
                    return;
                }
                try {
                    logger.info("Logging in to SysAp");
                    String id = From.toString().split("@")[0];
                    m_XmppClient.login(id, m_Configuration.password);

                    Presence presence = new Presence(Jid.of("mrha@busch-jaeger.de/rpc"), Presence.Type.SUBSCRIBE, null,
                            null, null, null, Jid.of(id), null, null, null);
                    logger.debug("Presence update: {}", presence);

                    m_RpcManager = m_XmppClient.getManager(RpcManager.class);

                    m_XmppClient.send(presence);

                } catch (Exception e) {
                    logger.warn("Problems logging in to SysAp: {}", e);
                    onConnectionLost(ThingStatusDetail.CONFIGURATION_ERROR,
                            "Login on SysAP with login name: " + m_Configuration.login);

                    try {
                        m_XmppClient.close();
                    } catch (XmppException e2) {
                        logger.error("Ops!", e2);
                    }
                    return;
                }
            } else {
                logger.warn("No SysAp account (JID) found for provided username: Verify username");
            }

        } catch (Exception e) {
            logger.warn("Problems getting JID: {}", e);
        }
    }

    private String getJid(String userName) throws Exception {

        String foundJid = null;
        /*
         * Read settings.json from SysAP
         */
        String url = "http://" + m_Configuration.host + "/settings.json"; // settings stores mapping to jid
        String USER_AGENT = "Mozilla/5.0";

        httpHeader.put("User-Agent", USER_AGENT);
        String jsonResponse = HttpUtil.executeUrl("GET", url, httpHeader, null, null, REQUEST_TIMEOUT);
        logger.debug("JSON Response (JID): {}", jsonResponse);

        /*
         * Parse json to find mapping to jid
         */
        JsonObject myObject = (JsonObject) new JsonParser().parse(jsonResponse);
        JsonArray myArray = myObject.getAsJsonArray("users");

        for (int i = 0; i < myArray.size(); i++) {
            String login = myArray.get(i).getAsJsonObject().get("name").toString().replaceAll("\"", "");
            String jid = myArray.get(i).getAsJsonObject().get("jid").toString().replaceAll("\"", "");
            if (login.equals(userName)) {
                foundJid = jid;
            }
        }
        return foundJid;
    }

    private void onConnectionEstablished() {
        logger.debug("Bridge connected. Updating thing status to ONLINE.");
        updateStatus(ThingStatus.ONLINE);
    }

    private void onConnectionLost(ThingStatusDetail detail, String msg) {
        logger.debug("Bridge connection lost. Updating thing status to OFFLINE.");
        updateStatus(ThingStatus.OFFLINE, detail, msg);
    }

    private void onUpdateXMPPStatus(SessionStatusEvent e) {
        if (e.getStatus() == XmppClient.Status.DISCONNECTED) {
            onConnectionLost(ThingStatusDetail.BRIDGE_OFFLINE, "XMPP connection lost");
            logger.debug("Connection lost {}", e);
        }

        if (e.getStatus() == XmppClient.Status.AUTHENTICATED) {
            onConnectionEstablished();
            logger.debug("Connection authenticated: {}", e);
        }
    }

    /*
     * XMPP Event handlers
     */
    private void onPresenceEvent(PresenceEvent e) {
        Presence presence = e.getPresence();
        if (presence.getType() == Presence.Type.SUBSCRIBE) {
            logger.debug("Presence subscribed: {}", presence);
        }
        m_XmppClient.getManager(PresenceManager.class).approveSubscription(presence.getFrom());
        logger.debug("Presence {}", presence);

    }

    /**
     * When an XMPP message is received from the bridge. It will be parsed to an XML string.
     *
     * TODO can be used to react on pressed switches and status update of raffstores.
     *
     * @param e
     */
    private void onMessageEvent(MessageEvent e) {
        logger.debug("MessageEvent Handler called");
        Message message = e.getMessage();
        Event event = message.getExtension(Event.class);

        logger.debug("Namespace {}", event.getNode());
        if (Update.NAMESPACE.equals(event.getNode())) {
            for (Item item : event.getItems()) {

                logger.debug("Payload of Item {}", item.getPayload());

                if (item.getPayload() instanceof org.openhab.binding.freeathome.internal.xmpp.rocks.extension.abb.com.protocol.update.Update) {
                    org.openhab.binding.freeathome.internal.xmpp.rocks.extension.abb.com.protocol.update.Update updateData = (org.openhab.binding.freeathome.internal.xmpp.rocks.extension.abb.com.protocol.update.Update) item
                            .getPayload();
                    String data = updateData.getData().replace("&amp;", "&").replace("&apos;", "'").replace("&lt;", "<")
                            .replace("&gt;", ">").replace("&quot;", "\"");
                    logger.debug("UpdateEvent {}", data);

                    try {
                        Project p = org.openhab.binding.freeathome.internal.xmpp.rocks.extensions.abb.com.protocol.data.Project
                                .builder().build(data);

                        // create JAXB context and instantiate marshaller
                        JAXBContext context = JAXBContext.newInstance(
                                org.openhab.binding.freeathome.internal.xmpp.rocks.extensions.abb.com.protocol.data.Project.class);
                        Marshaller m = context.createMarshaller();
                        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                        try {
                            List<Device> devices = p.getDevices();
                            for (int i = 0; i < devices.size(); i++) {
                                Device currentDevice = devices.get(i);

                                logger.debug("Update From {}", currentDevice.getSerialNumber());
                                List<Channel> channels = currentDevice.getChannels();

                                for (int j = 0; j < channels.size(); j++) {
                                    Channel channel = channels.get(j);

                                    /*
                                     * Outputs
                                     */
                                    List<DataPoint> dataPointsOut = channel.getOutputs();
                                    for (int d = 0; d < dataPointsOut.size(); d++) {
                                        DataPoint datapoint = dataPointsOut.get(d);
                                        String dataPoint = "Serial: " + currentDevice.getSerialNumber() + " Channel: "
                                                + channel.getI() + " DataPoint: " + datapoint.getI() + " Value: "
                                                + datapoint.getValue();
                                        logger.debug("Datapoint {}", dataPoint);

                                        m_UpdateHandler.NotifyThing(currentDevice.getSerialNumber(), channel.getI(),
                                                datapoint.getI(), datapoint.getValue());
                                    }
                                }
                            }

                        } catch (Exception e2) {
                            logger.error("Ops!", e2);
                            logger.error("Exception {}", e2.getMessage());
                        }

                    } catch (JAXBException e1) {
                        // TODO Auto-generated catch block
                        logger.error("Ops!", e1);
                        logger.error("JaxbException {}", e1.getMessage());
                    } catch (Exception ex) {
                        logger.error("General Exception {}", ex.getMessage());
                    }

                    // ...
                } else {
                    logger.debug("Payload is not instance of extension.abb.com.protocol.update.Update");
                }
            }
        } else {
            logger.debug("Message does not have namespace" + Update.NAMESPACE);
        }

    }

    private void onIQEvent(IQEvent e) {
        logger.debug("IQEvent Handler called: {}", e);
    }

    private void onUpdateEvent(UpdateEvent e) {
        logger.debug("UpdateEvent: {}", e);
    }

}
