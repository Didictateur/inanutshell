package fr.didictateur.inanutshell.service;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import fr.didictateur.inanutshell.data.model.RecipeComment;

public class CommentManager {
    private static CommentManager instance;
    private Context context;
    private ExecutorService executorService;
    
    // Cache des commentaires en mémoire
    private Map<String, List<RecipeComment>> recipeCommentsCache = new HashMap<>();
    private MutableLiveData<List<RecipeComment>> commentsLiveData = new MutableLiveData<>();
    private MutableLiveData<CommentStats> statsLiveData = new MutableLiveData<>();
    
    public enum SortOption {
        NEWEST_FIRST,
        OLDEST_FIRST,
        HIGHEST_RATED,
        MOST_HELPFUL
    }
    
    public enum FilterOption {
        ALL,
        FIVE_STAR_ONLY,
        FOUR_STAR_PLUS,
        WITH_CONTENT,
        REPORTED_ONLY,
        PENDING_APPROVAL
    }
    
    public static class CommentStats {
        public int totalComments;
        public float averageRating;
        public int fiveStars;
        public int fourStars;
        public int threeStars;
        public int twoStars;
        public int oneStar;
        public int pendingModeration;
        public int reportedComments;
    }
    
    private CommentManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    public static synchronized CommentManager getInstance(Context context) {
        if (instance == null) {
            instance = new CommentManager(context);
        }
        return instance;
    }
    
    public void addComment(String recipeId, String content, float rating, 
                          String parentCommentId, CommentCallback callback) {
        executorService.execute(() -> {
            try {
                // TODO: Récupérer les infos utilisateur actuelles
                String currentUserId = getCurrentUserId();
                String currentUserName = getCurrentUserName();
                
                RecipeComment comment = new RecipeComment(recipeId, currentUserId, currentUserName, content, rating);
                comment.setParentCommentId(parentCommentId);
                
                // Vérifier si la modération est nécessaire
                if (requiresModeration(comment)) {
                    comment.setApproved(false);
                }
                
                // Sauvegarder dans la base de données
                // TODO: Implémenter la sauvegarde en base
                saveCommentToDatabase(comment);
                
                // Mettre à jour le cache
                updateCache(recipeId, comment);
                
                // Notifier le callback
                if (callback != null) {
                    callback.onSuccess(comment);
                }
                
                // Mettre à jour les statistiques
                updateStats(recipeId);
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de l'ajout du commentaire: " + e.getMessage());
                }
            }
        });
    }
    
    public void editComment(String commentId, String newContent, float newRating, CommentCallback callback) {
        executorService.execute(() -> {
            try {
                // TODO: Récupérer le commentaire de la base
                RecipeComment comment = getCommentById(commentId);
                if (comment == null) {
                    if (callback != null) {
                        callback.onError("Commentaire introuvable");
                    }
                    return;
                }
                
                // Vérifier les permissions
                if (!canEditComment(comment)) {
                    if (callback != null) {
                        callback.onError("Pas d'autorisation pour modifier ce commentaire");
                    }
                    return;
                }
                
                comment.setContent(newContent);
                comment.setRating(newRating);
                
                // Vérifier si la modération est nécessaire après modification
                if (requiresModeration(comment)) {
                    comment.setApproved(false);
                }
                
                // Sauvegarder les modifications
                updateCommentInDatabase(comment);
                
                // Mettre à jour le cache
                updateCacheComment(comment);
                
                if (callback != null) {
                    callback.onSuccess(comment);
                }
                
                updateStats(comment.getRecipeId());
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de la modification: " + e.getMessage());
                }
            }
        });
    }
    
    public void deleteComment(String commentId, CommentCallback callback) {
        executorService.execute(() -> {
            try {
                RecipeComment comment = getCommentById(commentId);
                if (comment == null) {
                    if (callback != null) {
                        callback.onError("Commentaire introuvable");
                    }
                    return;
                }
                
                if (!canDeleteComment(comment)) {
                    if (callback != null) {
                        callback.onError("Pas d'autorisation pour supprimer ce commentaire");
                    }
                    return;
                }
                
                // Supprimer de la base de données
                deleteCommentFromDatabase(commentId);
                
                // Supprimer du cache
                removeFromCache(comment);
                
                if (callback != null) {
                    callback.onSuccess(comment);
                }
                
                updateStats(comment.getRecipeId());
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }
    
    public void reportComment(String commentId, String reason, CommentCallback callback) {
        executorService.execute(() -> {
            try {
                RecipeComment comment = getCommentById(commentId);
                if (comment == null) {
                    if (callback != null) {
                        callback.onError("Commentaire introuvable");
                    }
                    return;
                }
                
                comment.setReported(true);
                comment.setModeratorNote(reason);
                
                updateCommentInDatabase(comment);
                updateCacheComment(comment);
                
                if (callback != null) {
                    callback.onSuccess(comment);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors du signalement: " + e.getMessage());
                }
            }
        });
    }
    
    public void voteHelpful(String commentId, boolean isHelpful, CommentCallback callback) {
        executorService.execute(() -> {
            try {
                RecipeComment comment = getCommentById(commentId);
                if (comment == null) {
                    if (callback != null) {
                        callback.onError("Commentaire introuvable");
                    }
                    return;
                }
                
                if (isHelpful) {
                    comment.incrementHelpfulVotes();
                } else {
                    comment.decrementHelpfulVotes();
                }
                
                updateCommentInDatabase(comment);
                updateCacheComment(comment);
                
                if (callback != null) {
                    callback.onSuccess(comment);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onError("Erreur lors du vote: " + e.getMessage());
                }
            }
        });
    }
    
    public LiveData<List<RecipeComment>> getCommentsForRecipe(String recipeId, SortOption sortOption, FilterOption filterOption) {
        MutableLiveData<List<RecipeComment>> result = new MutableLiveData<>();
        
        executorService.execute(() -> {
            try {
                List<RecipeComment> comments = loadCommentsForRecipe(recipeId);
                
                // Appliquer le filtre
                comments = applyFilter(comments, filterOption);
                
                // Appliquer le tri
                comments = applySorting(comments, sortOption);
                
                result.postValue(comments);
                
            } catch (Exception e) {
                e.printStackTrace();
                result.postValue(new ArrayList<>());
            }
        });
        
        return result;
    }
    
    private List<RecipeComment> applyFilter(List<RecipeComment> comments, FilterOption filter) {
        switch (filter) {
            case ALL:
                return comments.stream()
                    .filter(RecipeComment::isApproved)
                    .collect(Collectors.toList());
                    
            case FIVE_STAR_ONLY:
                return comments.stream()
                    .filter(c -> c.isApproved() && c.getRating() == 5.0f)
                    .collect(Collectors.toList());
                    
            case FOUR_STAR_PLUS:
                return comments.stream()
                    .filter(c -> c.isApproved() && c.getRating() >= 4.0f)
                    .collect(Collectors.toList());
                    
            case WITH_CONTENT:
                return comments.stream()
                    .filter(c -> c.isApproved() && c.getContent() != null && !c.getContent().trim().isEmpty())
                    .collect(Collectors.toList());
                    
            case REPORTED_ONLY:
                return comments.stream()
                    .filter(RecipeComment::isReported)
                    .collect(Collectors.toList());
                    
            case PENDING_APPROVAL:
                return comments.stream()
                    .filter(c -> !c.isApproved())
                    .collect(Collectors.toList());
                    
            default:
                return comments;
        }
    }
    
    private List<RecipeComment> applySorting(List<RecipeComment> comments, SortOption sortOption) {
        switch (sortOption) {
            case NEWEST_FIRST:
                comments.sort((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()));
                break;
                
            case OLDEST_FIRST:
                comments.sort((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt()));
                break;
                
            case HIGHEST_RATED:
                comments.sort((c1, c2) -> Float.compare(c2.getRating(), c1.getRating()));
                break;
                
            case MOST_HELPFUL:
                comments.sort((c1, c2) -> Integer.compare(c2.getHelpfulVotes(), c1.getHelpfulVotes()));
                break;
        }
        
        return comments;
    }
    
    public LiveData<CommentStats> getStatsForRecipe(String recipeId) {
        MutableLiveData<CommentStats> result = new MutableLiveData<>();
        
        executorService.execute(() -> {
            CommentStats stats = calculateStats(recipeId);
            result.postValue(stats);
        });
        
        return result;
    }
    
    private CommentStats calculateStats(String recipeId) {
        CommentStats stats = new CommentStats();
        List<RecipeComment> comments = loadCommentsForRecipe(recipeId);
        
        stats.totalComments = comments.size();
        
        if (!comments.isEmpty()) {
            float totalRating = 0;
            for (RecipeComment comment : comments) {
                if (comment.isApproved()) {
                    totalRating += comment.getRating();
                    
                    // Compter par étoiles
                    int rating = Math.round(comment.getRating());
                    switch (rating) {
                        case 5: stats.fiveStars++; break;
                        case 4: stats.fourStars++; break;
                        case 3: stats.threeStars++; break;
                        case 2: stats.twoStars++; break;
                        case 1: stats.oneStar++; break;
                    }
                }
                
                if (!comment.isApproved()) {
                    stats.pendingModeration++;
                }
                
                if (comment.isReported()) {
                    stats.reportedComments++;
                }
            }
            
            int approvedCount = stats.fiveStars + stats.fourStars + stats.threeStars + stats.twoStars + stats.oneStar;
            if (approvedCount > 0) {
                stats.averageRating = totalRating / approvedCount;
            }
        }
        
        return stats;
    }
    
    // Méthodes privées d'aide
    private boolean requiresModeration(RecipeComment comment) {
        // TODO: Implémenter la détection de contenu inapproprié
        // Pour l'instant, modérer les commentaires très courts ou suspects
        String content = comment.getContent();
        if (content != null) {
            content = content.toLowerCase();
            String[] bannedWords = {"spam", "fake", "arnaque", "nul"};
            for (String word : bannedWords) {
                if (content.contains(word)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private String getCurrentUserId() {
        // TODO: Récupérer l'ID utilisateur actuel
        return "user_1"; // Simulation
    }
    
    private String getCurrentUserName() {
        // TODO: Récupérer le nom utilisateur actuel
        return "Utilisateur"; // Simulation
    }
    
    private boolean canEditComment(RecipeComment comment) {
        // TODO: Vérifier si l'utilisateur actuel peut modifier ce commentaire
        return comment.getUserId().equals(getCurrentUserId());
    }
    
    private boolean canDeleteComment(RecipeComment comment) {
        // TODO: Vérifier les permissions de suppression
        return comment.getUserId().equals(getCurrentUserId()) || isCurrentUserModerator();
    }
    
    private boolean isCurrentUserModerator() {
        // TODO: Vérifier si l'utilisateur actuel est modérateur
        return false;
    }
    
    // Méthodes de persistance (à implémenter avec Room)
    private void saveCommentToDatabase(RecipeComment comment) {
        // TODO: Implémenter avec Room DAO
    }
    
    private void updateCommentInDatabase(RecipeComment comment) {
        // TODO: Implémenter avec Room DAO
    }
    
    private void deleteCommentFromDatabase(String commentId) {
        // TODO: Implémenter avec Room DAO
    }
    
    private RecipeComment getCommentById(String commentId) {
        // TODO: Implémenter avec Room DAO
        return null; // Simulation
    }
    
    private List<RecipeComment> loadCommentsForRecipe(String recipeId) {
        // Vérifier le cache d'abord
        List<RecipeComment> cached = recipeCommentsCache.get(recipeId);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        
        // TODO: Charger depuis la base de données
        List<RecipeComment> comments = new ArrayList<>();
        recipeCommentsCache.put(recipeId, comments);
        return comments;
    }
    
    private void updateCache(String recipeId, RecipeComment comment) {
        List<RecipeComment> comments = recipeCommentsCache.get(recipeId);
        if (comments == null) {
            comments = new ArrayList<>();
            recipeCommentsCache.put(recipeId, comments);
        }
        comments.add(comment);
    }
    
    private void updateCacheComment(RecipeComment comment) {
        for (List<RecipeComment> comments : recipeCommentsCache.values()) {
            for (int i = 0; i < comments.size(); i++) {
                if (comments.get(i).getId().equals(comment.getId())) {
                    comments.set(i, comment);
                    break;
                }
            }
        }
    }
    
    private void removeFromCache(RecipeComment comment) {
        List<RecipeComment> comments = recipeCommentsCache.get(comment.getRecipeId());
        if (comments != null) {
            comments.removeIf(c -> c.getId().equals(comment.getId()));
        }
    }
    
    private void updateStats(String recipeId) {
        CommentStats stats = calculateStats(recipeId);
        statsLiveData.postValue(stats);
    }
    
    // Interface pour les callbacks
    public interface CommentCallback {
        void onSuccess(RecipeComment comment);
        void onError(String message);
    }
}
