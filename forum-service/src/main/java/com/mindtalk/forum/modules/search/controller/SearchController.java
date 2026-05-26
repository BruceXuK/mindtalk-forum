package com.mindtalk.forum.modules.search.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.search.dto.SearchDTO;
import com.mindtalk.forum.modules.search.service.SearchService;
import com.mindtalk.forum.modules.search.vo.SearchSuggestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索接口
 */
@Tag(name = "搜索")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "全文搜索")
    @GetMapping
    public Result<PageResult<PostVO>> search(@Valid SearchDTO dto) {
        return Result.ok(searchService.search(dto));
    }

    @Operation(summary = "搜索建议")
    @GetMapping("/suggest")
    public Result<List<SearchSuggestionVO>> suggest(@RequestParam @jakarta.validation.constraints.NotBlank(message = "关键词不能为空") String keyword,
                                                     @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(searchService.suggest(keyword, limit));
    }

    @Operation(summary = "热门搜索")
    @GetMapping("/hot")
    public Result<List<String>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(searchService.getHotSearches(limit));
    }
}
