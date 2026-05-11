package com.example.schoolforum.repository;

import com.example.schoolforum.pojo.document.PopularQueryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopularQueryRepository extends ElasticsearchRepository<PopularQueryDocument, String> {
}