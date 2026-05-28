package com.mindtalk.forum.modules.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.dict.entity.DictItem;
import com.mindtalk.forum.modules.dict.mapper.DictItemMapper;
import com.mindtalk.forum.modules.dict.service.DictService;
import com.mindtalk.forum.modules.dict.vo.DictItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final DictItemMapper dictItemMapper;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = Constants.REDIS_PREFIX + "dict:";
    private static final long CACHE_TTL = 60;

    @Override
    public List<DictItemVO> getItems(String typeCode) {
        String cacheKey = CACHE_PREFIX + typeCode;
        String cached = redisUtils.get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<DictItemVO>>() {});
            } catch (JsonProcessingException e) {
                log.debug("[字典] 缓存反序列化失败 typeCode={}", typeCode);
            }
        }

        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItem::getTypeCode, typeCode)
                .eq(DictItem::getStatus, 1)
                .orderByAsc(DictItem::getSortOrder);

        List<DictItemVO> items = dictItemMapper.selectList(wrapper).stream()
                .map(d -> DictItemVO.builder()
                        .itemKey(d.getItemKey())
                        .itemValue(d.getItemValue())
                        .extra(d.getExtra())
                        .sortOrder(d.getSortOrder())
                        .build())
                .toList();

        try {
            redisUtils.set(cacheKey, objectMapper.writeValueAsString(items), CACHE_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[字典] 缓存序列化失败 typeCode={}", typeCode);
        }
        return items;
    }

    @Override
    public boolean isValidKey(String typeCode, String itemKey) {
        return getItems(typeCode).stream()
                .anyMatch(d -> d.getItemKey().equals(itemKey));
    }
}
