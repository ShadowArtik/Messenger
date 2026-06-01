package org.example.messengerserver.websocket;

import org.example.messengerserver.dto.MessageResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class XmlProtocol {

    private XmlProtocol() {
    }

    // =================== Incoming (parsing) ===================

    public static XmlMessage parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element root = document.getDocumentElement();
            XmlMessage message = new XmlMessage();

            message.setType(root.getAttribute("type"));
            message.setChatId(getInt(root, "chatId"));
            message.setSenderId(getInt(root, "senderId"));
            message.setReceiverId(getInt(root, "receiverId"));
            message.setUserId(getInt(root, "userId"));
            message.setReceiverIds(getIntList(root, "receiverIds"));
            message.setMemberIds(getIntList(root, "memberIds"));
            message.setText(getText(root, "text"));
            message.setSystemMessageText(getText(root, "systemMessageText"));
            message.setMsgId(getText(root, "msgId"));

            return message;
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse XML WebSocket message", e);
        }
    }

    // =================== Outgoing (builders) ===================

    public static String connectSuccess() {
        return """
                <message type="CONNECT_SUCCESS"></message>
                """;
    }

    public static String onlineUsers(Set<Integer> userIds) {
        return """
                <message type="ONLINE_USERS">
                    %s
                </message>
                """.formatted(intList("userIds", userIds));
    }

    public static String history(int chatId, List<MessageResponse> messages) {
        StringBuilder msgs = new StringBuilder("<messages>");

        for (MessageResponse msg : messages) {
            msgs.append("<msg>")
                .append("<msgId>").append(escape(msg.getClientId())).append("</msgId>")
                .append("<senderId>").append(msg.getSenderId()).append("</senderId>")
                .append("<senderUsername>").append(escape(msg.getSenderUsername())).append("</senderUsername>")
                .append("<senderDisplayName>").append(escape(msg.getSenderDisplayName())).append("</senderDisplayName>")
                .append("<text>").append(escape(msg.getText())).append("</text>")
                .append("<time>").append(escape(msg.getTime())).append("</time>")
                .append("<date>").append(escape(msg.getDate())).append("</date>")
                .append("<read>").append(msg.isRead()).append("</read>")
                .append("<edited>").append(msg.isEdited()).append("</edited>")
                .append("</msg>");
        }

        msgs.append("</messages>");

        return """
                <message type="HISTORY">
                    <chatId>%d</chatId>
                    %s
                </message>
                """.formatted(chatId, msgs);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String messageDeleted(int chatId, String msgId) {
        return """
                <message type="MESSAGE_DELETED">
                    <chatId>%d</chatId>
                    <msgId>%s</msgId>
                </message>
                """.formatted(chatId, escape(msgId));
    }

    public static String messageEdited(int chatId, String msgId, String newText) {
        return """
                <message type="MESSAGE_EDITED">
                    <chatId>%d</chatId>
                    <msgId>%s</msgId>
                    <text>%s</text>
                </message>
                """.formatted(chatId, escape(msgId), escape(newText));
    }

    public static String groupDeleted(int chatId) {
        return """
                <message type="GROUP_DELETED">
                    <chatId>%d</chatId>
                </message>
                """.formatted(chatId);
    }

    public static String messagesRead(int chatId) {
        return """
                <message type="MESSAGES_READ">
                    <chatId>%d</chatId>
                </message>
                """.formatted(chatId);
    }

    public static String userStatus(String type, int userId) {
        return """
                <message type="%s">
                    <userId>%d</userId>
                </message>
                """.formatted(type, userId);
    }

    // =================== XML helpers ===================

    private static String intList(String tagName, Iterable<Integer> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(tagName).append(">");

        for (Integer value : values) {
            builder.append("<id>").append(value).append("</id>");
        }

        builder.append("</").append(tagName).append(">");
        return builder.toString();
    }

    private static String getText(Element root, String tagName) {
        NodeList nodes = root.getElementsByTagName(tagName);

        if (nodes.getLength() == 0) {
            return null;
        }

        return nodes.item(0).getTextContent();
    }

    private static int getInt(Element root, String tagName) {
        String text = getText(root, tagName);

        if (text == null || text.isBlank()) {
            return 0;
        }

        return Integer.parseInt(text.trim());
    }

    private static List<Integer> getIntList(Element root, String tagName) {
        NodeList containers = root.getElementsByTagName(tagName);

        if (containers.getLength() == 0) {
            return List.of();
        }

        Element container = (Element) containers.item(0);
        NodeList ids = container.getElementsByTagName("id");
        List<Integer> values = new ArrayList<>();

        for (int index = 0; index < ids.getLength(); index++) {
            values.add(Integer.parseInt(ids.item(index).getTextContent().trim()));
        }

        return values;
    }
}
