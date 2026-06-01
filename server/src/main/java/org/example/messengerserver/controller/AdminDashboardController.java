package org.example.messengerserver.controller;

import org.example.messengerserver.dto.ConversationResponse;
import org.example.messengerserver.dto.MessageHistoryResponse;
import org.example.messengerserver.repository.AdminDashboardRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminDashboardController {

    private final AdminDashboardRepository adminDashboardRepository;

    public AdminDashboardController(AdminDashboardRepository adminDashboardRepository) {
        this.adminDashboardRepository = adminDashboardRepository;
    }

    @GetMapping("/api/conversations")
    // =================== Endpoints ===================

    public List<ConversationResponse> getConversations() {
        return adminDashboardRepository.getAllConversations();
    }

    @GetMapping("/api/conversations/{chatId}/messages")
    public List<MessageHistoryResponse> getMessagesForChat(@PathVariable int chatId) {
        return adminDashboardRepository.getMessagesForChat(chatId);
    }

    @GetMapping(value = "/admin", produces = MediaType.TEXT_HTML_VALUE)
    public String adminPage() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Messenger Server Dashboard</title>
                    <style>
                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            font-family: Arial, sans-serif;
                            background: #1E1F22;
                            color: white;
                        }

                        .page {
                            min-height: 100vh;
                            padding: 30px;
                        }

                        .header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 16px;
                            margin-bottom: 24px;
                        }

                        h1 {
                            margin: 0;
                            font-size: 30px;
                        }

                        .status-pill {
                            background: #2B2D31;
                            padding: 10px 16px;
                            border-radius: 999px;
                            color: #B8BDC7;
                            white-space: nowrap;
                        }

                        .grid {
                            display: grid;
                            grid-template-columns: 320px minmax(420px, 1fr) 420px;
                            gap: 20px;
                        }

                        .panel {
                            background: #2B2D31;
                            border-radius: 18px;
                            padding: 20px;
                            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
                        }

                        .panel-title {
                            font-size: 20px;
                            font-weight: bold;
                            margin-bottom: 16px;
                        }

                        .session-card,
                        .conversation-card {
                            background: #343740;
                            border-radius: 14px;
                            padding: 14px;
                            margin-bottom: 12px;
                        }

                        .conversation-card {
                            cursor: pointer;
                            border: 1px solid transparent;
                            transition: background 0.15s ease, border-color 0.15s ease;
                        }

                        .conversation-card:hover,
                        .conversation-card.active {
                            background: #3D414B;
                            border-color: #5B9DFF;
                        }

                        .session-row {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            gap: 12px;
                        }

                        .dot {
                            width: 10px;
                            height: 10px;
                            background: #48D17A;
                            border-radius: 50%;
                            display: inline-block;
                            margin-right: 8px;
                        }

                        .online {
                            color: #48D17A;
                            font-weight: bold;
                        }

                        .chat-name {
                            font-size: 17px;
                            font-weight: bold;
                            margin-bottom: 6px;
                        }

                        .chat-meta {
                            color: #B8BDC7;
                            font-size: 13px;
                            margin-bottom: 8px;
                            line-height: 1.35;
                        }

                        .last-message {
                            color: #E3E3E3;
                            font-size: 14px;
                            margin-top: 8px;
                            line-height: 1.35;
                        }

                        .tag {
                            display: inline-block;
                            padding: 4px 8px;
                            border-radius: 999px;
                            background: #444957;
                            color: #B8BDC7;
                            font-size: 12px;
                            margin-left: 8px;
                        }

                        .empty {
                            color: #777D89;
                            padding: 20px 0;
                            text-align: center;
                        }

                        .history-list {
                            max-height: calc(100vh - 170px);
                            overflow-y: auto;
                            padding-right: 4px;
                        }

                        .history-message {
                            background: #343740;
                            border-radius: 14px;
                            padding: 12px 14px;
                            margin-bottom: 10px;
                        }

                        .history-message.system {
                            background: #3B3F48;
                            text-align: center;
                        }

                        .message-header {
                            display: flex;
                            justify-content: space-between;
                            gap: 12px;
                            color: #B8BDC7;
                            font-size: 12px;
                            margin-bottom: 6px;
                        }

                        .message-sender {
                            color: #A9C4FF;
                            font-weight: bold;
                        }

                        .message-text {
                            color: white;
                            font-size: 14px;
                            line-height: 1.35;
                            overflow-wrap: anywhere;
                        }

                        .history-image {
                            max-width: 220px;
                            max-height: 220px;
                            border-radius: 8px;
                            display: block;
                            margin-top: 4px;
                        }

                        @media (max-width: 850px) {
                            .grid {
                                grid-template-columns: 1fr;
                            }

                            .header {
                                align-items: flex-start;
                                flex-direction: column;
                            }
                        }
                    </style>
                </head>

                <body>
                    <div class="page">
                        <div class="header">
                            <h1>Messenger Server Dashboard</h1>
                            <div class="status-pill" id="serverStatus">Loading...</div>
                        </div>

                        <div class="grid">
                            <div class="panel">
                                <div class="panel-title">Active Sessions</div>
                                <div id="sessionsList"></div>
                            </div>

                            <div class="panel">
                                <div class="panel-title">Conversation Sessions</div>
                                <div id="conversationsList"></div>
                            </div>

                            <div class="panel">
                                <div class="panel-title" id="historyTitle">Chat History</div>
                                <div id="historyList" class="history-list">
                                    <div class="empty">Select a conversation to view messages</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <script>
                        let selectedChatId = null;
                        let selectedChatName = '';

                        function escapeHtml(value) {
                            return String(value ?? '')
                                .replaceAll('&', '&amp;')
                                .replaceAll('<', '&lt;')
                                .replaceAll('>', '&gt;')
                                .replaceAll('"', '&quot;')
                                .replaceAll("'", '&#039;');
                        }

                        function imageId(text) {
                            const t = String(text ?? '');
                            if (!t.startsWith('[IMG]')) return null;
                            const id = t.slice(5);
                            return /^[0-9]+$/.test(id) ? id : null;
                        }

                        function renderMessageText(text) {
                            const id = imageId(text);
                            if (id) {
                                return '<img class="history-image" src="/api/files/' + id + '" alt="image">';
                            }
                            return escapeHtml(text);
                        }

                        function previewText(text) {
                            return imageId(text) ? '&#128247; Photo' : escapeHtml(text);
                        }

                        async function loadSessions() {
                            const response = await fetch('/api/sessions');
                            const sessions = await response.json();

                            const container = document.getElementById('sessionsList');
                            const status = document.getElementById('serverStatus');

                            status.textContent = sessions.length + ' online users';

                            if (sessions.length === 0) {
                                container.innerHTML = '<div class="empty">No active sessions</div>';
                                return;
                            }

                            container.innerHTML = sessions.map(session => `
                                <div class="session-card">
                                    <div class="session-row">
                                        <div>User ID: ${escapeHtml(session.userId)}</div>
                                        <div class="online"><span class="dot"></span>Online</div>
                                    </div>
                                </div>
                            `).join('');
                        }

                        async function loadConversations() {
                            const response = await fetch('/api/conversations');
                            const conversations = await response.json();

                            const container = document.getElementById('conversationsList');

                            if (conversations.length === 0) {
                                container.innerHTML = '<div class="empty">No conversations found</div>';
                                return;
                            }

                            container.innerHTML = conversations.map(chat => `
                                <div class="conversation-card ${selectedChatId === chat.chatId ? 'active' : ''}"
                                     onclick="selectConversation(${chat.chatId}, '${escapeJs(chat.chatName)}')">
                                    <div class="chat-name">
                                        ${escapeHtml(chat.chatName)}
                                        <span class="tag">${escapeHtml(chat.chatType)}</span>
                                    </div>

                                    <div class="chat-meta">
                                        Chat ID: ${escapeHtml(chat.chatId)} - Messages: ${escapeHtml(chat.messagesCount)}
                                    </div>

                                    <div class="chat-meta">
                                        Members: ${escapeHtml(chat.members)}
                                    </div>

                                    <div class="last-message">
                                        Last message:
                                        ${chat.lastMessageSender ? escapeHtml(chat.lastMessageSender) + ': ' : ''}
                                        ${previewText(chat.lastMessage)}
                                        ${chat.lastMessageTime ? ' - ' + escapeHtml(chat.lastMessageTime) : ''}
                                    </div>
                                </div>
                            `).join('');
                        }

                        function escapeJs(value) {
                            return String(value ?? '')
                                .replaceAll('\\\\', '\\\\\\\\')
                                .replaceAll("'", "\\\\'");
                        }

                        async function selectConversation(chatId, chatName) {
                            selectedChatId = chatId;
                            selectedChatName = chatName;
                            await loadMessages();
                            await loadConversations();
                        }

                        async function loadMessages() {
                            if (selectedChatId === null) {
                                return;
                            }

                            const response = await fetch(`/api/conversations/${selectedChatId}/messages`);
                            const messages = await response.json();

                            const title = document.getElementById('historyTitle');
                            const container = document.getElementById('historyList');

                            title.textContent = 'Chat History - ' + selectedChatName;

                            if (messages.length === 0) {
                                container.innerHTML = '<div class="empty">No messages in this chat</div>';
                                return;
                            }

                            container.innerHTML = messages.map(message => `
                                <div class="history-message ${message.systemMessage ? 'system' : ''}">
                                    <div class="message-header">
                                        <span class="message-sender">
                                            ${escapeHtml(message.senderName)}
                                            ${message.senderUsername ? '(@' + escapeHtml(message.senderUsername) + ')' : ''}
                                        </span>
                                        <span>${escapeHtml(message.createdAt)}</span>
                                    </div>
                                    <div class="message-text">${renderMessageText(message.text)}</div>
                                </div>
                            `).join('');

                            container.scrollTop = container.scrollHeight;
                        }

                        async function refreshDashboard() {
                            await Promise.all([
                                loadSessions(),
                                loadConversations(),
                                loadMessages()
                            ]);
                        }

                        refreshDashboard();
                        setInterval(refreshDashboard, 2000);
                    </script>
                </body>
                </html>
                """;
    }
}
