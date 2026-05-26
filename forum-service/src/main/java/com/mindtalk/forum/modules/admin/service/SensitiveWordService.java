package com.mindtalk.forum.modules.admin.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.admin.entity.SensitiveWord;

public interface SensitiveWordService {

    PageResult<SensitiveWord> getList(int page, int size);

    SensitiveWord add(String word, String replacement);

    void delete(Long id);

    String filter(String text);

    boolean containsSensitive(String text);
}
