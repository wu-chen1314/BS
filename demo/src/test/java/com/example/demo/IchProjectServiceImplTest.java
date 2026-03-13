package com.example.demo;

import com.example.demo.mapper.AppCommentMapper;
import com.example.demo.mapper.AppFavoriteMapper;
import com.example.demo.mapper.IchCategoryMapper;
import com.example.demo.mapper.IchProjectViewMapper;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.service.IchInheritorService;
import com.example.demo.service.impl.IchProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IchProjectServiceImplTest {

    @Test
    void deleteProjectsWithRelationsUnlinksInheritorsAndDeletesProjectViews() {
        IchProjectServiceImpl service = spy(new IchProjectServiceImpl());
        IchInheritorService inheritorService = mock(IchInheritorService.class);
        IchProjectViewMapper projectViewMapper = mock(IchProjectViewMapper.class);
        AppCommentMapper commentMapper = mock(AppCommentMapper.class);
        AppFavoriteMapper favoriteMapper = mock(AppFavoriteMapper.class);

        ReflectionTestUtils.setField(service, "inheritorService", inheritorService);
        ReflectionTestUtils.setField(service, "projectViewMapper", projectViewMapper);
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "favoriteMapper", favoriteMapper);
        ReflectionTestUtils.setField(service, "categoryMapper", mock(IchCategoryMapper.class));
        ReflectionTestUtils.setField(service, "regionMapper", mock(IchRegionMapper.class));

        List<Long> projectIds = Arrays.asList(1L, 2L);
        doReturn(true).when(service).removeByIds(projectIds);

        boolean deleted = service.deleteProjectsWithRelations(projectIds);

        assertThat(deleted).isTrue();
        verify(inheritorService).update(any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
        verify(projectViewMapper).delete(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class));
        verify(commentMapper).delete(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class));
        verify(favoriteMapper).delete(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class));
    }

    @Test
    void deleteProjectsWithRelationsReturnsFalseForEmptyIds() {
        IchProjectServiceImpl service = spy(new IchProjectServiceImpl());
        IchInheritorService inheritorService = mock(IchInheritorService.class);

        ReflectionTestUtils.setField(service, "inheritorService", inheritorService);

        assertThat(service.deleteProjectsWithRelations(Collections.emptyList())).isFalse();
        verifyNoInteractions(inheritorService);
    }
}
