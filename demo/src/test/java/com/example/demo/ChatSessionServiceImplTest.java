package com.example.demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.AiChatHistoryService;
import com.example.demo.service.impl.ChatSessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatSessionServiceImplTest {

    @Test
    void deleteSessionRemovesChatHistoryBeforeDeletingSession() {
        ChatSessionServiceImpl service = spy(new ChatSessionServiceImpl());
        AiChatHistoryService aiChatHistoryService = mock(AiChatHistoryService.class);
        ReflectionTestUtils.setField(service, "aiChatHistoryService", aiChatHistoryService);

        ChatSession session = new ChatSession();
        session.setId(11L);
        session.setUserId(7L);

        doReturn(session).when(service).getById(11L);
        doReturn(true).when(service).removeById(11L);
        when(aiChatHistoryService.remove(any(LambdaQueryWrapper.class))).thenReturn(true);

        boolean deleted = service.deleteSession(11L, 7L);

        assertThat(deleted).isTrue();
        verify(aiChatHistoryService).remove(any(LambdaQueryWrapper.class));
        verify(service).removeById(11L);
    }

    @Test
    void deleteSessionRejectsDeletingOtherUsersConversation() {
        ChatSessionServiceImpl service = spy(new ChatSessionServiceImpl());
        AiChatHistoryService aiChatHistoryService = mock(AiChatHistoryService.class);
        ReflectionTestUtils.setField(service, "aiChatHistoryService", aiChatHistoryService);

        ChatSession session = new ChatSession();
        session.setId(11L);
        session.setUserId(7L);

        doReturn(session).when(service).getById(11L);

        assertThatThrownBy(() -> service.deleteSession(11L, 8L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("无权删除其他用户的会话");

        verify(aiChatHistoryService, never()).remove(any(LambdaQueryWrapper.class));
        verify(service, never()).removeById(anyLong());
    }
}
