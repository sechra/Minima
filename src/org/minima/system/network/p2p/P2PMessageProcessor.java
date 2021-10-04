package org.minima.system.network.p2p;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.minima.objects.greet.Greeting;
import org.minima.system.Main;
import org.minima.system.brains.BackupManager;
import org.minima.system.input.InputHandler;
import org.minima.system.network.NetworkHandler;
import org.minima.system.network.base.MinimaClient;
import org.minima.system.network.p2p.functions.*;
import org.minima.system.network.p2p.messages.*;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageProcessor;
import org.minima.utils.messages.TimerMessage;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.minima.system.network.NetworkHandler.NETWORK_CONNECT;
import static org.minima.system.network.NetworkHandler.NETWORK_DISCONNECT;

@Getter
@Slf4j(topic = "P2P")
public class P2PMessageProcessor extends MessageProcessor {

    /**
     * P2P Functions..
     */

    public static final String P2P_INIT = "P2P_INIT";
    public static final String P2P_SHUTDOWN = "P2P_SHUTDOWN";
    public static final String P2P_ON_GREETED = "P2P_ON_GREETED";
    public static final String P2P_ON_CONNECTED = "P2P_ON_CONNECTED";
    public static final String P2P_ON_DISCONNECTED = "P2P_ON_DISCONNECTED";

    public static final String P2P_LOOP = "P2P_LOOP";

    public static final String P2P_CONNECT = "P2P_CONNECT";
    public static final String P2P_DISCONNECT = "P2P_DISCONNECT";

    public static final String P2P_RENDEZVOUS = "P2P_RENDEZVOUS";
    public static final String P2P_WALK_LINKS = "P2P_WALK_LINKS";
    public static final String P2P_WALK_LINKS_RESPONSE = "P2P_WALK_LINKS_RESPONSE";
    public static final String P2P_SWAP_LINK = "P2P_SWAP_LINK";
    public static final String P2P_DO_SWAP = "P2P_DO_SWAP";
    public static final String P2P_MAP_NETWORK = "P2P_MAP_NETWORK";
    public static final String P2P_PRINT_NETWORK_MAP = "P2P_PRINT_NETWORK_MAP";
    public static final String P2P_PRINT_NETWORK_MAP_RESPONSE = "P2P_PRINT_NETWORK_MAP_RESPONSE";

    public static final String P2P_SEND_MESSAGE = "P2P_SEND_MESSAGE";





    /*
     * Network Messages
     * RENDEZVOUS
     * WALK_LINKS
     * SWAP_LINK
     * MAP_NETWORK
     */

    private static final int CLEANUP_LOOP_DELAY = 60_000;

    private final P2PState state;
    private final int minimaPort;
    Message printNetworkMapRPCReq;
    //The data store
    private InetAddress hostIP;

    public P2PMessageProcessor() {
        super("P2P Message Processor");

        try {
            this.hostIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            log.error("Could not identify the local ip address: " + hostIP);
        }
        state = new P2PState(5, null);
        state.setAddress(new InetSocketAddress(getHostIP(), getMinimaPort()));
        this.minimaPort = 9001;

    }

    public P2PMessageProcessor(String hostIP, int minimaPort) {
        super("P2P Message Processor");

        try {
            this.hostIP = InetAddress.getByName(hostIP);
        } catch (UnknownHostException e) {
            log.error("Could not identify the local ip address: " + hostIP);
        }
        this.minimaPort = minimaPort;


        //Get the BackupManager
        BackupManager backup = Main.getMainHandler().getBackupManager();
        File p2pDataFile = backup.getBackUpFile("p2pdata.json");
        state = new P2PState(5, p2pDataFile);
        state.setAddress(new InetSocketAddress(getHostIP(), getMinimaPort()));
        //Start the Ball rolling..
//        this.setLOG(true);
        PostTimerMessage(new TimerMessage(10_000, P2P_LOOP));

    }

    public void stop() {
        PostMessage(P2P_SHUTDOWN);
    }

    /**
     * You can use this to get your HOST/IP etc
     *
     * @return
     */
    protected NetworkHandler getNetworkHandler() {
        return Main.getMainHandler().getNetworkHandler();
    }

    /**
     * All the current connections
     *
     * @return
     */
    protected ArrayList<MinimaClient> getCurrentMinimaClients() {
        return getNetworkHandler().getNetClients();
    }

    protected void sendMessage(MinimaClient zClient, Message zMessage) {
        zClient.PostMessage(zMessage);
    }


    /**
     * Routes messages to the correct processing function
     *
     * @param zMessage The Full Message
     * @throws Exception
     */
    @Override
    protected void processMessage(Message zMessage) throws Exception {
        if (!zMessage.isMessageType(P2P_WALK_LINKS)) {
            log.debug("[+] P2PMessageProcessor processing: " + zMessage.getMessageType());
        }
        try {
            switch (zMessage.getMessageType()) {
                case P2P_SEND_MESSAGE:
                    processSendMessage(zMessage);
                    break;
                case P2P_SHUTDOWN:
                    processShutdownMsg(zMessage);
                    break;
                case P2P_ON_GREETED:
                    processOnGreetedMsg(zMessage);
                    break;
                case P2P_RENDEZVOUS:
                    processOnRendezvousMsg(zMessage);
                    break;
                case P2P_ON_CONNECTED:
                    processOnConnectedMsg(zMessage);
                    break;
                case P2P_ON_DISCONNECTED:
                    processOnDisconnectedMsg(zMessage);
                    break;
                case P2P_CONNECT:
                    processConnectMsg(zMessage);
                    break;
                case P2P_DISCONNECT:
                    processDisconnectMsg(zMessage);
                    break;
                case P2P_LOOP:
                    processLoopMsg(zMessage);
                    break;
                case P2P_WALK_LINKS:
                    processWalkLinksMsg(zMessage);
                    break;
                case P2P_WALK_LINKS_RESPONSE:
                    processWalkLinksResponseMsg(zMessage);
                    break;
                case P2P_SWAP_LINK:
                    processSwapLinkMsg(zMessage);
                    break;
                case P2P_DO_SWAP:
                    processDoSwapMsg(zMessage);
                    break;
                case P2P_MAP_NETWORK:
                    processNetworkMapMsg(zMessage);
                    break;
                case P2P_PRINT_NETWORK_MAP:
                    processPrintNetworkMapRequestMsg(zMessage);
                    break;
                case P2P_PRINT_NETWORK_MAP_RESPONSE:
                    processPrintNetworkMapResponseMsg(zMessage);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {

            StringBuilder builder = new StringBuilder();
            StackTraceElement[] trace = e.getStackTrace();
            for (StackTraceElement traceElement : trace)
                builder.append("\tat " + traceElement + "\n");

            log.error("[!] Exception in P2P Message Processor: " + e + "\n" + builder);
        }
    }

    private void processSendMessage(Message zMessage) {
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        Message message = (Message) zMessage.getObject("message");
        client.PostMessage(message);
    }

    private void processShutdownMsg(Message zMessage) {
        log.debug("[+] P2PMessageProcessor processing P2P_SHUTDOWN message");
        // Make sure the node list is saved
        StartupFuncs.SaveNodeList(this.state);
        //And stop this Message Processor stack
        stopMessageProcessor();
    }

    private void processOnGreetedMsg(Message zMessage) {
        Greeting greeting = (Greeting) zMessage.getObject("greeting");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");

        GreetingFuncs.onGreetedMsg(state, greeting, client).forEach(this::PostMessage);
    }

    private void processOnRendezvousMsg(Message zMessage) {
        if (state.isRendezvousComplete()) {
            return;
        }

        P2PMsgRendezvous rendezvous = (P2PMsgRendezvous) zMessage.getObject("rendezvous");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        StartupFuncs.processOnRendezvousMsg(state, rendezvous, client);
    }

    private void processConnectMsg(Message zMessage) {
        InetSocketAddress address = (InetSocketAddress) zMessage.getObject("address");
        String reason = zMessage.getString("reason");
        if (!state.getAddress().equals(address)) {

            Message msg = new Message(NETWORK_CONNECT);
            msg.addObject("address", address);
            log.debug("[+] P2P_CONNECT to: " + address + " reason: " + reason);
            Main.getMainHandler().getNetworkHandler().PostMessage(msg);
        } else {
            log.debug("[!] Attempting to connect to self");
            state.removeRandomNodeSet(address);
        }
    }

    private void processOnConnectedMsg(Message zMessage) {
        // Do nothing as we don't have enough info to process - wait until we get the greeting message
//        log.debug("[+] P2PMessageProcessor processing P2P_ON_CONNECTED message");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        if (!client.isIncoming()) {
            state.addOutLink(client.getMinimaAddress());
        }
    }


    private void processOnDisconnectedMsg(Message zMessage) {
        log.debug(this.state.genPrintableState());
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        if (client.isIncoming()) {
        }
        log.debug("[!] P2P_ON_DISCONNECT Disconnected from isInLink? " + client.isIncoming() + " IP: " + client.getMinimaAddress());
        Message walkMsg = null;
        if (client.isIncoming()) {
            walkMsg = DisconnectionFuncs.onInLinkDisconnected(state, client, getCurrentMinimaClients());
        } else {
            walkMsg = DisconnectionFuncs.onOutLinkDisconnected(state, client, getCurrentMinimaClients());
        }
        if (walkMsg != null) {
            PostMessage(walkMsg);
        }
    }


    private void processDisconnectMsg(Message zMessage) {
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        String reason = zMessage.getString("reason");
        log.debug("[!] P2P_DISCONNECT Disconnecting from isInLink? " + client.isIncoming() + " IP: " + client.getMinimaAddress() + " for: " + reason);
        int attempt = zMessage.getInteger("attempt");
        if (this.state.getOutLinks().contains(client.getMinimaAddress()) ||
                this.state.getInLinks().contains(client.getMinimaAddress()) ||
                this.state.getClientLinks().contains(client.getMinimaAddress())) {
            getNetworkHandler().PostMessage(new Message(NETWORK_DISCONNECT).addString("uid", client.getUID()));
        } else {
            if (attempt < 3) {
                TimerMessage shutdownMsg = new TimerMessage(1_000, P2P_DISCONNECT);
                shutdownMsg.addObject("client", client);
                shutdownMsg.addInteger("attempt", attempt + 1);
                shutdownMsg.addString("reason", reason + " attempt: " + attempt);
                PostTimerMessage(shutdownMsg);
            }
        }


    }

    private void processSwapLinkMsg(Message zMessage) {
        P2PMsgSwapLink swapLink = (P2PMsgSwapLink) zMessage.getObject("data");
        Message messageToSend = null;
        if (swapLink.isSwapClientReq()) {
            messageToSend = SwapFuncs.onSwapClientsReq(state, swapLink, getCurrentMinimaClients());
        } else if (swapLink.isConditionalSwapReq() && state.getInLinks().size() > state.getNumLinks()) {
            // Send SwapLink message if we have more inLinks than desired
            messageToSend = SwapFuncs.onSwapReq(state, swapLink, getCurrentMinimaClients());
        } else {
            messageToSend = SwapFuncs.onSwapReq(state, swapLink, getCurrentMinimaClients());
        }
        if (messageToSend != null) {
            PostMessage(messageToSend);
        }

    }

    private void processDoSwapMsg(Message zMessage) {
        P2PMsgDoSwap doSwap = (P2PMsgDoSwap) zMessage.getObject("data");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        SwapFuncs.executeDoSwap(state, doSwap, client).forEach(this::PostMessage);
        state.getConnectionDetailsMap().put(client.getMinimaAddress(), new ConnectionDetails(ConnectionReason.DO_SWAP));
    }

    private void processWalkLinksMsg(Message zMessage) {
        P2PMsgWalkLinks msgWalkLinks = (P2PMsgWalkLinks) zMessage.getObject("data");
        Message message;
        if (msgWalkLinks.isWalkInLinks()) {
            message = WalkLinksFuncs.onInLinkWalkMsg(state, msgWalkLinks, getCurrentMinimaClients());
        } else {
            message = WalkLinksFuncs.onOutLinkWalkMsg(state, msgWalkLinks, getCurrentMinimaClients());
        }
        if (message != null) {
            PostMessage(message);
        }
    }

    public void processWalkLinksResponseMsg(Message zMessage) {
        P2PMsgWalkLinks p2pWalkLinks = (P2PMsgWalkLinks) zMessage.getObject("data");
        Message sendMsg = null;
        if (state.getAddress().equals(p2pWalkLinks.getPathTaken().get(0))) {
            log.debug("[+] P2P_WALK_LINKS_RESPONSE returned to origin node");
            sendMsg = WalkLinksFuncs.onReturnedWalkMsg(state, p2pWalkLinks);
        } else {
            sendMsg = WalkLinksFuncs.onWalkLinkResponseMsg(state, p2pWalkLinks, getCurrentMinimaClients());
        }
        if (sendMsg != null) {
            PostMessage(sendMsg);
        }
    }

    private void processLoopMsg(Message zMessage) {
        Random rand = new Random();
        long loopDelay = 300_000 + rand.nextInt(30_000);

        if (!state.isRendezvousComplete()) {
            JoiningFuncs.joinRendezvousNode(state, getCurrentMinimaClients()).forEach(this::PostMessage);
            loopDelay = 6_000 + rand.nextInt(3_000);
        } else if (state.getOutLinks().size() < 3) {
            JoiningFuncs.joinEntryNode(state, getCurrentMinimaClients()).forEach(this::PostMessage);
            loopDelay = 6_000 + rand.nextInt(3_000);
        } else if (state.getOutLinks().size() < state.getNumLinks()) {
            JoiningFuncs.joinScaleOutLinks(state, getCurrentMinimaClients()).forEach(this::PostMessage);
        }
        ArrayList<ExpiringMessage> expiringMessages = this.state.dropExpiredMessages();
//        log.debug(state.genPrintableState());

        PostTimerMessage(new TimerMessage(loopDelay, P2P_LOOP));
    }


    private void processNetworkMapMsg(Message zMessage) {
        // On getting a network map back
        P2PMsgNetworkMap networkMap = (P2PMsgNetworkMap) zMessage.getObject("data");
        state.getNetworkMap().put(networkMap.getNodeAddress(), networkMap);
        state.getActiveMappingRequests().remove(networkMap.getNodeAddress());
        ArrayList<InetSocketAddress> newAddresses = networkMap.getAddresses().stream()
                .filter(x -> !state.getNetworkMap().containsKey(x))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        log.debug("[+] P2P_MAP_NETWORK node: " + networkMap.toNodeJSON().toString() + " links: " + networkMap.toLinksJSON().toString());
        if (state.getNetworkMap().size() < 1_000 && !newAddresses.isEmpty()) {
            for (InetSocketAddress address: newAddresses){
                // Connect and
                state.getConnectionDetailsMap().put(address, new ConnectionDetails(ConnectionReason.MAPPING));
                PostMessage(new Message(P2PMessageProcessor.P2P_CONNECT).addObject("address", address).addString("reason", "MAPPING connection"));
            }
        }
        if (state.getActiveMappingRequests().isEmpty()){
            PostMessage(new Message(P2P_PRINT_NETWORK_MAP_RESPONSE));
        }

    }

    private void processPrintNetworkMapRequestMsg(Message zMessage) {
        printNetworkMapRPCReq = zMessage;

        ArrayList<InetSocketAddress> addresses = Stream.of(state.getRandomNodeSet(), state.getOutLinks(), state.getInLinks())
                .flatMap(Collection::stream).distinct().collect(Collectors.toCollection(ArrayList::new));
        for (InetSocketAddress address: addresses){
            // Connect and
            state.getConnectionDetailsMap().put(address, new ConnectionDetails(ConnectionReason.MAPPING));
            PostMessage(new Message(P2PMessageProcessor.P2P_CONNECT).addObject("address", address).addString("reason", "MAPPING connection"));
        }

        PostTimerMessage(new TimerMessage(29_000, P2P_PRINT_NETWORK_MAP_RESPONSE));
//        log.error("[-] P2P_PRINT_NETWORK_MAP Request UID " + mapNetwork.getRequestUID());
//        PostMessage(new Message(P2P_MAP_NETWORK).addObject("data", mapNetwork).addString("from_ip", state.getAddress().toString()));

    }


    private void processPrintNetworkMapResponseMsg(Message zMessage) {
        if (printNetworkMapRPCReq != null) {
            JSONObject networkMapJSON = InputHandler.getResponseJSON(printNetworkMapRPCReq);
            // nodes
            JSONArray nodes = new JSONArray();
            JSONArray links = new JSONArray();
            for (P2PMsgNetworkMap value: state.getNetworkMap().values()){
                nodes.add(value.toNodeJSON());
                links.addAll(value.toLinksJSON());
            }
            // links
            networkMapJSON.put("nodes", nodes);
            networkMapJSON.put("links", links);

            //All good
            InputHandler.endResponse(printNetworkMapRPCReq, true, "");
        } else {
            log.warn("[-] Failed to make network map");
        }
    }

}
