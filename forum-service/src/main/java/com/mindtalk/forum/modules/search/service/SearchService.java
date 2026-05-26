package com.mindtalk.forum.modules.search.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.search.dto.SearchDTO;
import com.mindtalk.forum.modules.search.vo.SearchSuggestionVO;

import java.util.List;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /** 全文搜索 */
    PageResult<PostVO> search(SearchDTO dto);

    /** 搜索建议 */
    List<SearchSuggestionVO> suggest(String keyword, int limit);

    /** 热门搜索 */
    List<String> getHotSearches(int limit);
}
