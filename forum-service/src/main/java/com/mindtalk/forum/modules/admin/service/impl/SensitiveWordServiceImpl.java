package com.mindtalk.forum.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.admin.entity.SensitiveWord;
import com.mindtalk.forum.modules.admin.mapper.SensitiveWordMapper;
import com.mindtalk.forum.modules.admin.service.SensitiveWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    @Override
    public PageResult<SensitiveWord> getList(int page, int size) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SensitiveWord::getCreateTime);
        IPage<SensitiveWord> result = sensitiveWordMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public SensitiveWord add(String word, String replacement) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getWord, word);
        if (sensitiveWordMapper.selectCount(wrapper) > 0) {
            return sensitiveWordMapper.selectOne(wrapper);
        }
        SensitiveWord sw = SensitiveWord.builder()
                .word(word).replacement(replacement != null ? replacement : "***").enabled(true).build();
        sensitiveWordMapper.insert(sw);
        return sw;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        sensitiveWordMapper.deleteById(id);
    }

    @Override
    public String filter(String text) {
        if (text == null || text.isEmpty()) return text;
        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getEnabled, true));
        String result = text;
        for (SensitiveWord sw : words) {
            result = result.replace(sw.getWord(), sw.getReplacement());
        }
        return result;
    }

    @Override
    public boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) return false;
        return sensitiveWordMapper.selectCount(
                new LambdaQueryWrapper<SensitiveWord>().eq(SensitiveWord::getEnabled, true)) > 0
                && !filter(text).equals(text);
    }
}
