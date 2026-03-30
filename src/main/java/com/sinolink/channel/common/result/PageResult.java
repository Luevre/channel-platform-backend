package com.sinolink.channel.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页码")
    private long current;

    @Schema(description = "每页条数")
    private long size;

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "总页数")
    private long pages;

    @Schema(description = "数据列表")
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords());
        return result;
    }
}
