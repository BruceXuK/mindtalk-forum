package com.mindtalk.forum.modules.dict.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO {

    private String itemKey;

    private String itemValue;

    private String extra;

    private Integer sortOrder;
}
