package com.mindtalk.forum.modules.post.repository;

import com.mindtalk.forum.modules.post.document.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch 帖子仓库
 */
@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Long> {
}
