package com.example.schoolforum.service;

import com.example.schoolforum.pojo.dto.CombinedSearchResult;
import com.example.schoolforum.pojo.dto.KeywordSuggestion;
import com.example.schoolforum.pojo.dto.UserSearchDocument;

import java.util.List;

public interface SearchService {

    CombinedSearchResult search(String query, int page, int pageSize);

    List<KeywordSuggestion> getKeywordSuggestions(String prefix, int limit);

    void indexPostById(Long postId);

    void deletePost(Long postId);

    void indexUser(UserSearchDocument document);

    void deleteUser(Long userId);

    void syncAllPosts();

    void syncAllUsers();

    void deleteAllIndexes();

    long getPostsCollectionCount();

    long getUsersCollectionCount();
}
