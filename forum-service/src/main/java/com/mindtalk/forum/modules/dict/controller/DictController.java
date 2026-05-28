package com.mindtalk.forum.modules.dict.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.dict.service.DictService;
import com.mindtalk.forum.modules.dict.vo.DictItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "数据字典")
@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @Operation(summary = "获取字典项")
    @GetMapping("/{typeCode}")
    public Result<List<DictItemVO>> getItems(@PathVariable String typeCode) {
        return Result.ok(dictService.getItems(typeCode));
    }
}
