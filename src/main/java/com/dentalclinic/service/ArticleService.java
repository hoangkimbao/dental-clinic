package com.dentalclinic.service;

import com.dentalclinic.model.Article;
import com.dentalclinic.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    public List<Article> getAllPublishedArticles() {
        return articleRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Article> getArticlesByCategory(String category) {
        return articleRepository.findByCategoryAndIsPublishedTrue(category);
    }

    public Optional<Article> getArticleBySlug(String slug) {
        Optional<Article> opt = articleRepository.findBySlug(slug);
        opt.ifPresent(art -> {
            art.setViewCount((art.getViewCount() == null ? 0 : art.getViewCount()) + 1);
            articleRepository.save(art);
        });
        return opt;
    }

    public Article createArticle(Article article) {
        return articleRepository.save(article);
    }

    public Optional<Article> updateArticle(Long id, Article updated) {
        return articleRepository.findById(id).map(art -> {
            art.setTitle(updated.getTitle());
            art.setSlug(updated.getSlug());
            art.setCategory(updated.getCategory());
            art.setAuthorName(updated.getAuthorName());
            art.setReadTime(updated.getReadTime());
            art.setIcon(updated.getIcon());
            art.setSummary(updated.getSummary());
            art.setContent(updated.getContent());
            art.setIsPublished(updated.getIsPublished());
            return articleRepository.save(art);
        });
    }

    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
    }
}
