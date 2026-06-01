package org.example.messengerserver.websocket;

import org.example.messengerserver.dto.MessageResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlProtocolTest {

    @Test
    void parseReadsCoreFields() {
        String xml = """
                <message type="PRIVATE_MESSAGE">
                    <chatId>7</chatId>
                    <senderId>1</senderId>
                    <receiverId>2</receiverId>
                    <text>hello</text>
                    <msgId>cid-1</msgId>
                </message>
                """;

        XmlMessage m = XmlProtocol.parse(xml);

        assertEquals("PRIVATE_MESSAGE", m.getType());
        assertEquals(7, m.getChatId());
        assertEquals(1, m.getSenderId());
        assertEquals(2, m.getReceiverId());
        assertEquals("hello", m.getText());
        assertEquals("cid-1", m.getMsgId());
    }

    @Test
    void historyIncludesMsgIdReadAndEdited() {
        MessageResponse r =
                new MessageResponse(1, "a", "A", "hi", "10:00", "2026-06-01", true, "cid-1", true);

        String xml = XmlProtocol.history(4, List.of(r));

        assertTrue(xml.contains("type=\"HISTORY\""));
        assertTrue(xml.contains("<msgId>cid-1</msgId>"));
        assertTrue(xml.contains("<read>true</read>"));
        assertTrue(xml.contains("<edited>true</edited>"));
        assertTrue(xml.contains("<text>hi</text>"));
    }

    @Test
    void deletedAndEditedBuilders() {
        String deleted = XmlProtocol.messageDeleted(3, "cid");
        assertTrue(deleted.contains("type=\"MESSAGE_DELETED\""));
        assertTrue(deleted.contains("<msgId>cid</msgId>"));

        String edited = XmlProtocol.messageEdited(3, "cid", "new <b>");
        assertTrue(edited.contains("type=\"MESSAGE_EDITED\""));
        assertTrue(edited.contains("new &lt;b&gt;"));
    }

    @Test
    void historyEscapesText() {
        MessageResponse r =
                new MessageResponse(1, "a", "A", "<x> & y", "10:00", "2026-06-01", false, "c", false);

        String xml = XmlProtocol.history(4, List.of(r));

        assertTrue(xml.contains("&lt;x&gt; &amp; y"));
    }
}
