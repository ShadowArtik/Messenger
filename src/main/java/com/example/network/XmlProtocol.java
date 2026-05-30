package com.example.network;

import com.example.model.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class XmlProtocol {

    private XmlProtocol() {
    }

    public static String loadHistory(int chatId) {
        return """
                <message type="LOAD_HISTORY">
                    <chatId>%d</chatId>
                </message>
                """.formatted(chatId);
    }

    public static String clearChat(int chatId, int senderId) {
        return """
                <message type="CLEAR_CHAT">
                    <chatId>%d</chatId>
                    <senderId>%d</senderId>
                </message>
                """.formatted(chatId, senderId);
    }

    public static String saveMessage(int chatId, int senderId, String text) {
        return """
                <message type="SAVE_MESSAGE">
                    <chatId>%d</chatId>
                    <senderId>%d</senderId>
                    <text>%s</text>
                </message>
                """.formatted(chatId, senderId, escape(text));
    }

    public static String connect(int userId, String username, String displayName) {
        return """
                <message type="CONNECT">
                    <userId>%d</userId>
                    <username>%s</username>
                    <displayName>%s</displayName>
                </message>
                """.formatted(userId, escape(username), escape(displayName));
    }

    public static String privateMessage(
            int chatId,
            int senderId,
            int receiverId,
            String senderUsername,
            String senderDisplayName,
            String text
    ) {
        return """
                <message type="PRIVATE_MESSAGE">
                    <chatId>%d</chatId>
                    <senderId>%d</senderId>
                    <receiverId>%d</receiverId>
                    <senderUsername>%s</senderUsername>
                    <senderDisplayName>%s</senderDisplayName>
                    <text>%s</text>
                </message>
                """.formatted(
                chatId,
                senderId,
                receiverId,
                escape(senderUsername),
                escape(senderDisplayName),
                escape(text)
        );
    }

    public static String groupMessage(
            int chatId,
            int senderId,
            String senderUsername,
            String senderDisplayName,
            String text
    ) {
        return """
                <message type="GROUP_MESSAGE">
                    <chatId>%d</chatId>
                    <senderId>%d</senderId>
                    <senderUsername>%s</senderUsername>
                    <senderDisplayName>%s</senderDisplayName>
                    <text>%s</text>
                </message>
                """.formatted(
                chatId,
                senderId,
                escape(senderUsername),
                escape(senderDisplayName),
                escape(text)
        );
    }

    public static String typing(
            int chatId,
            int senderId,
            List<Integer> receiverIds,
            String senderDisplayName
    ) {
        return """
                <message type="TYPING">
                    <chatId>%d</chatId>
                    <senderId>%d</senderId>
                    %s
                    <senderDisplayName>%s</senderDisplayName>
                </message>
                """.formatted(
                chatId,
                senderId,
                intList("receiverIds", receiverIds),
                escape(senderDisplayName)
        );
    }

    public static String profileUpdated(int userId, String displayName) {
        return """
                <message type="USER_PROFILE_UPDATED">
                    <userId>%d</userId>
                    <displayName>%s</displayName>
                </message>
                """.formatted(userId, escape(displayName));
    }

    public static String groupCreated(
            int senderId,
            int chatId,
            String chatName,
            List<Integer> memberIds
    ) {
        return """
                <message type="GROUP_CREATED">
                    <senderId>%d</senderId>
                    <chatId>%d</chatId>
                    <chatName>%s</chatName>
                    %s
                </message>
                """.formatted(senderId, chatId, escape(chatName), intList("memberIds", memberIds));
    }

    public static String groupMembersUpdated(
            int senderId,
            int chatId,
            List<Integer> memberIds,
            String systemMessageText
    ) {
        return """
                <message type="GROUP_MEMBERS_UPDATED">
                    <senderId>%d</senderId>
                    <chatId>%d</chatId>
                    %s
                    <systemMessageText>%s</systemMessageText>
                </message>
                """.formatted(senderId, chatId, intList("memberIds", memberIds), escape(systemMessageText));
    }

    public static String groupRenamed(
            int senderId,
            int chatId,
            String oldName,
            String newName,
            List<Integer> memberIds,
            String systemMessageText
    ) {
        return """
                <message type="GROUP_RENAMED">
                    <senderId>%d</senderId>
                    <chatId>%d</chatId>
                    <oldName>%s</oldName>
                    <newName>%s</newName>
                    %s
                    <systemMessageText>%s</systemMessageText>
                </message>
                """.formatted(
                senderId,
                chatId,
                escape(oldName),
                escape(newName),
                intList("memberIds", memberIds),
                escape(systemMessageText)
        );
    }

    public static class IncomingMessage {

        private String type;
        private int chatId;
        private int senderId;
        private int userId;
        private List<Integer> userIds;
        private List<Integer> memberIds;
        private String senderUsername;
        private String senderDisplayName;
        private String displayName;
        private String systemMessageText;
        private String text;
        private List<Message> messages;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getChatId() { return chatId; }
        public void setChatId(int chatId) { this.chatId = chatId; }

        public int getSenderId() { return senderId; }
        public void setSenderId(int senderId) { this.senderId = senderId; }

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public List<Integer> getUserIds() { return userIds; }
        public void setUserIds(List<Integer> userIds) { this.userIds = userIds; }

        public List<Integer> getMemberIds() { return memberIds; }
        public void setMemberIds(List<Integer> memberIds) { this.memberIds = memberIds; }

        public String getSenderUsername() { return senderUsername; }
        public void setSenderUsername(String v) { this.senderUsername = v; }

        public String getSenderDisplayName() { return senderDisplayName; }
        public void setSenderDisplayName(String v) { this.senderDisplayName = v; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getSystemMessageText() { return systemMessageText; }
        public void setSystemMessageText(String v) { this.systemMessageText = v; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }

        public boolean isPrivateMessage() { return "PRIVATE_MESSAGE".equalsIgnoreCase(type); }
        public boolean isGroupMessage() { return "GROUP_MESSAGE".equalsIgnoreCase(type); }
        public boolean isTyping() { return "TYPING".equalsIgnoreCase(type); }
        public boolean isUserOnline() { return "USER_ONLINE".equalsIgnoreCase(type); }
        public boolean isUserOffline() { return "USER_OFFLINE".equalsIgnoreCase(type); }
        public boolean isOnlineUsers() { return "ONLINE_USERS".equalsIgnoreCase(type); }
        public boolean isUserProfileUpdated() { return "USER_PROFILE_UPDATED".equalsIgnoreCase(type); }
        public boolean isGroupCreated() { return "GROUP_CREATED".equalsIgnoreCase(type); }
        public boolean isGroupMembersUpdated() { return "GROUP_MEMBERS_UPDATED".equalsIgnoreCase(type); }
        public boolean isGroupRenamed() { return "GROUP_RENAMED".equalsIgnoreCase(type); }
        public boolean isHistory() { return "HISTORY".equalsIgnoreCase(type); }
        public boolean isClearChat() { return "CLEAR_CHAT".equalsIgnoreCase(type); }
    }

    public static IncomingMessage parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element root = document.getDocumentElement();
            IncomingMessage message = new IncomingMessage();

            message.setType(root.getAttribute("type"));
            message.setChatId(getInt(root, "chatId"));
            message.setSenderId(getInt(root, "senderId"));
            message.setUserId(getInt(root, "userId"));
            message.setUserIds(getIntList(root, "userIds"));
            message.setMemberIds(getIntList(root, "memberIds"));
            message.setSenderUsername(getText(root, "senderUsername"));
            message.setSenderDisplayName(getText(root, "senderDisplayName"));
            message.setDisplayName(getText(root, "displayName"));
            message.setSystemMessageText(getText(root, "systemMessageText"));
            message.setText(getText(root, "text"));

            if ("HISTORY".equalsIgnoreCase(message.getType())) {
                message.setMessages(parseMessages(root));
            }

            return message;
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse XML WebSocket message", e);
        }
    }

    private static List<Message> parseMessages(Element root) {
        List<Message> messages = new ArrayList<>();
        NodeList msgNodes = root.getElementsByTagName("msg");

        for (int i = 0; i < msgNodes.getLength(); i++) {
            Element msg = (Element) msgNodes.item(i);
            messages.add(new Message(
                    getIntFromElement(msg, "senderId"),
                    getTextFromElement(msg, "senderUsername"),
                    getTextFromElement(msg, "senderDisplayName"),
                    getTextFromElement(msg, "text"),
                    getTextFromElement(msg, "time")
            ));
        }

        return messages;
    }

    private static String getTextFromElement(Element el, String tagName) {
        NodeList nodes = el.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private static int getIntFromElement(Element el, String tagName) {
        String text = getTextFromElement(el, tagName);
        if (text == null || text.isBlank()) return 0;
        return Integer.parseInt(text.trim());
    }

    private static String intList(String tagName, List<Integer> values) {
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
        java.util.ArrayList<Integer> values = new java.util.ArrayList<>();

        for (int index = 0; index < ids.getLength(); index++) {
            values.add(Integer.parseInt(ids.item(index).getTextContent().trim()));
        }

        return values;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
