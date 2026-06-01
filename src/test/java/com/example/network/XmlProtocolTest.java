package com.example.network;

import com.example.model.Message;
import com.example.network.XmlProtocol.IncomingMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlProtocolTest {

    @Test
    void privateMessageRoundTrip() {
        String xml = XmlProtocol.privateMessage(7, 1, 2, "alice", "Alice", "hello", "cid-1");
        IncomingMessage in = XmlProtocol.parse(xml);

        assertTrue(in.isPrivateMessage());
        assertEquals(7, in.getChatId());
        assertEquals(1, in.getSenderId());
        assertEquals("alice", in.getSenderUsername());
        assertEquals("Alice", in.getSenderDisplayName());
        assertEquals("hello", in.getText());
        assertEquals("cid-1", in.getMsgId());
    }

    @Test
    void specialCharactersSurviveEscapingRoundTrip() {
        String tricky = "<b> & \"q\" 'x'";
        String xml = XmlProtocol.groupMessage(5, 1, "u", "U", tricky, "cid");

        IncomingMessage in = XmlProtocol.parse(xml);

        assertTrue(in.isGroupMessage());
        assertEquals(tricky, in.getText());
    }

    @Test
    void parsesIncomingDeletedAndEditedFrames() {
        // The client SENDS DELETE_MESSAGE/EDIT_MESSAGE; the server broadcasts back
        // MESSAGE_DELETED/MESSAGE_EDITED, which is what is*() detect.
        IncomingMessage del = XmlProtocol.parse("""
                <message type="MESSAGE_DELETED">
                    <chatId>3</chatId>
                    <msgId>cid-9</msgId>
                </message>
                """);
        assertTrue(del.isMessageDeleted());
        assertEquals("cid-9", del.getMsgId());

        IncomingMessage ed = XmlProtocol.parse("""
                <message type="MESSAGE_EDITED">
                    <chatId>3</chatId>
                    <msgId>cid-9</msgId>
                    <text>new text</text>
                </message>
                """);
        assertTrue(ed.isMessageEdited());
        assertEquals("cid-9", ed.getMsgId());
        assertEquals("new text", ed.getText());
    }

    @Test
    void deleteAndEditBuildersProduceOutgoingTypes() {
        assertTrue(XmlProtocol.deleteMessage(3, 1, "cid-9").contains("type=\"DELETE_MESSAGE\""));
        assertTrue(XmlProtocol.editMessage(3, 1, "cid-9", "new").contains("type=\"EDIT_MESSAGE\""));
    }

    @Test
    void historyParsesMessageFields() {
        String xml = """
                <message type="HISTORY">
                    <chatId>4</chatId>
                    <messages>
                        <msg>
                            <msgId>m1</msgId>
                            <senderId>1</senderId>
                            <senderUsername>a</senderUsername>
                            <senderDisplayName>A</senderDisplayName>
                            <text>hi</text>
                            <time>10:00</time>
                            <date>2026-06-01</date>
                            <read>true</read>
                            <edited>true</edited>
                        </msg>
                    </messages>
                </message>
                """;

        IncomingMessage in = XmlProtocol.parse(xml);

        assertTrue(in.isHistory());
        assertEquals(1, in.getMessages().size());

        Message m = in.getMessages().get(0);
        assertEquals("hi", m.getText());
        assertEquals("m1", m.getClientId());
        assertTrue(m.isRead());
        assertTrue(m.isEdited());
    }
}
