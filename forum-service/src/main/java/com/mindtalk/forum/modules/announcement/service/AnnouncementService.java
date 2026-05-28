package com.mindtalk.forum.modules.announcement.service;

import com.mindtalk.forum.modules.announcement.dto.CreateAnnouncementDTO;
import com.mindtalk.forum.modules.announcement.vo.AnnouncementVO;

import java.util.List;

public interface AnnouncementService {

    List<AnnouncementVO> listAll();

    List<AnnouncementVO> listActive();

    AnnouncementVO create(CreateAnnouncementDTO dto, Long userId);

    AnnouncementVO update(Long id, CreateAnnouncementDTO dto);

    void delete(Long id);

    void togglePublish(Long id);

    void togglePin(Long id);
}
