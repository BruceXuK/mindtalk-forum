package com.mindtalk.forum.modules.dict.service;

import com.mindtalk.forum.modules.dict.vo.DictItemVO;

import java.util.List;

public interface DictService {

    List<DictItemVO> getItems(String typeCode);

    boolean isValidKey(String typeCode, String itemKey);
}
