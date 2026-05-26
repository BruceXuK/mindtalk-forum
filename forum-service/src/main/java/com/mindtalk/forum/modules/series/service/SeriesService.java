package com.mindtalk.forum.modules.series.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.series.dto.CreateSeriesDTO;
import com.mindtalk.forum.modules.series.dto.UpdateSeriesDTO;
import com.mindtalk.forum.modules.series.vo.PostSeriesContextVO;
import com.mindtalk.forum.modules.series.vo.SeriesDetailVO;
import com.mindtalk.forum.modules.series.vo.SeriesVO;

import java.util.List;

public interface SeriesService {

    SeriesDetailVO createSeries(Long userId, CreateSeriesDTO dto);

    SeriesDetailVO updateSeries(Long userId, Long seriesId, UpdateSeriesDTO dto);

    void deleteSeries(Long userId, Long seriesId);

    SeriesDetailVO getSeriesDetail(Long seriesId);

    PageResult<SeriesVO> getUserSeries(Long userId, int page, int size);

    List<SeriesVO> getMySeries(Long userId);

    void addPost(Long userId, Long seriesId, Long postId);

    void removePost(Long userId, Long seriesId, Long postId);

    PostSeriesContextVO getPostSeriesContext(Long postId);
}
