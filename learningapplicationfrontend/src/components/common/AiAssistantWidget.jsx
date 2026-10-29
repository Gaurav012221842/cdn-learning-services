import React, { useState, useRef, useEffect, useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import { getCookie } from '../../services/api';
import './AiAssistantWidget.css';

export const AiAssistantWidget = () => {
  const { user } = useContext(AuthContext);
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'ROLE_ADMIN';
  const isStudent = user?.role === 'STUDENT' || user?.role === 'ROLE_STUDENT';

  // Initial welcome message based on role
  const getInitialMessage = React.useCallback(() => {
    if (isAdmin) {
      return `👋 **Hello Admin (${user?.email || 'Admin'})!**\n\nI am your **Operations & Curriculum AI Companion** powered by MCP.\n\nYou have full administrative clearance: check platform metrics, audit enrollments, or verify curriculum.`;
    }
    if (isStudent) {
      return `👋 **Hi ${user?.name || user?.email || 'there'}!**\n\nI am your **AI Study Companion**.\n\nAsk me about your course progress, video lessons, or technical doubts from the curriculum!`;
    }
    return `👋 **Welcome to Gaurav's CDN Learning Platform!**\n\nI am your **AI Course Advisor**.\n\nAsk me anything about our courses, syllabus, curriculum topics, pricing, or how to enroll!`;
  }, [isAdmin, isStudent, user?.email, user?.name]);

  const [messages, setMessages] = useState(() => [
    {
      role: 'assistant',
      text: getInitialMessage()
    }
  ]);

  // Update greeting if user logs in or changes
  useEffect(() => {
    setMessages([
      {
        role: 'assistant',
        text: getInitialMessage()
      }
    ]);
  }, [getInitialMessage]);

  // Suggested prompt chips
  const chips = isAdmin
    ? [
        '📊 Platform Summary',
        '🔍 Search Spring Lessons',
        '🎓 Kajal ka Progress'
      ]
    : isStudent
    ? [
        '📈 Mera Progress Report',
        '🔍 Search Spring Boot Lessons',
        '📚 Course Syllabus Breakdown'
      ]
    : [
        '🎓 What courses are available?',
        '📚 Spring Boot Syllabus',
        '💳 How to enroll?',
        '🔍 Search REST API Lessons'
      ];

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen]);

  const handleSend = async (messageText = null) => {
    const textToSend = messageText || input;
    if (!textToSend || !textToSend.trim()) return;

    const userMessage = { role: 'user', text: textToSend };
    setMessages((prev) => [...prev, userMessage]);
    if (!messageText) setInput('');
    setIsLoading(true);

    const token = getCookie('token');

    // Build headers with JWT and identity fallbacks
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(user?.email ? { 'X-User-Email': user.email } : {}),
      ...(user?.role ? { 'X-User-Role': user.role } : {})
    };

    try {
      let res;
      // 1. Try Gateway endpoint (Port 8080)
      try {
        res = await fetch('http://localhost:8080/api/v1/ai/chat', {
          method: 'POST',
          headers,
          body: JSON.stringify({ message: textToSend })
        });
      } catch (gatewayErr) {
        // 2. Direct fallback to AI Service (Port 8089)
        res = await fetch('http://localhost:8089/api/v1/ai/chat', {
          method: 'POST',
          headers,
          body: JSON.stringify({ message: textToSend })
        });
      }

      const data = await res.json();
      const replyText = data.reply || data.error || 'No response received from assistant.';

      setMessages((prev) => [...prev, { role: 'assistant', text: replyText }]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: `⚠️ **Connection Error**: Unable to reach AI Assistant service. Please ensure the backend is running.\n\n*Details: ${err.message}*`
        }
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Simple Markdown-to-HTML parser for formatted text
  const formatText = (text) => {
    return text.split('\n').map((line, idx) => {
      // Headers
      if (line.startsWith('### ')) {
        return <h3 key={idx}>{line.replace('### ', '')}</h3>;
      }
      // Bullets
      if (line.startsWith('- ') || line.startsWith('* ')) {
        const content = line.substring(2);
        return (
          <li key={idx} dangerouslySetInnerHTML={{ __html: renderInline(content) }} />
        );
      }
      if (line.trim() === '') {
        return <div key={idx} style={{ height: '6px' }} />;
      }
      return (
        <div key={idx} dangerouslySetInnerHTML={{ __html: renderInline(line) }} />
      );
    });
  };

  const renderInline = (str) => {
    return str
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code>$1</code>');
  };

  return (
    <div className="ai-widget-container">
      {/* Closed State Trigger Button */}
      {!isOpen && (
        <button
          className="ai-trigger-btn"
          onClick={() => setIsOpen(true)}
          title="Open AI Tutor & Assistant"
          aria-label="Open AI Assistant"
        >
          <span style={{ fontSize: '24px' }}>✨</span>
          <span className="ai-trigger-badge" />
        </button>
      )}

      {/* Open Chat Window */}
      {isOpen && (
        <div className="ai-chat-window">
          {/* Header */}
          <div className="ai-chat-header">
            <div className="ai-header-left">
              <div className="ai-avatar">🤖</div>
              <div>
                <h4 className="ai-title">AI Study & Ops Tutor</h4>
                <span className="ai-role-badge">
                  {isAdmin
                    ? '👑 Admin Mode'
                    : isStudent
                    ? `🎓 Student: ${user?.email?.split('@')[0] || 'Active'}`
                    : '👤 Guest Explorer'}
                </span>
              </div>
            </div>
            <div className="ai-header-actions">
              <button
                onClick={() => setIsOpen(false)}
                title="Minimize Chat"
                aria-label="Close"
              >
                ✕
              </button>
            </div>
          </div>

          {/* Messages Body */}
          <div className="ai-messages-body">
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`ai-message ${
                  msg.role === 'user' ? 'ai-msg-user' : 'ai-msg-assistant'
                }`}
              >
                {msg.role === 'assistant' ? formatText(msg.text) : msg.text}
              </div>
            ))}

            {/* Quick Suggestion Chips (shown after first assistant message) */}
            {messages.length === 1 && (
              <div className="ai-chips-container">
                {chips.map((chip, idx) => (
                  <button
                    key={idx}
                    className="ai-chip"
                    onClick={() => handleSend(chip)}
                  >
                    {chip}
                  </button>
                ))}
              </div>
            )}

            {/* Typing Loader */}
            {isLoading && (
              <div className="ai-typing">
                <div className="ai-dot" />
                <div className="ai-dot" />
                <div className="ai-dot" />
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Bar */}
          <div className="ai-input-bar">
            <input
              type="text"
              className="ai-input-field"
              placeholder={
                isAdmin
                  ? 'Ask stats, search lessons, or enroll...'
                  : 'Ask about course progress or topics...'
              }
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isLoading}
            />
            <button
              className="ai-send-btn"
              onClick={() => handleSend()}
              disabled={isLoading || !input.trim()}
            >
              Send
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default AiAssistantWidget;
