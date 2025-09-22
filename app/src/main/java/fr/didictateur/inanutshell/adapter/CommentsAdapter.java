package fr.didictateur.inanutshell.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fr.didictateur.inanutshell.R;
import fr.didictateur.inanutshell.data.model.RecipeComment;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
    
    private List<RecipeComment> comments = new ArrayList<>();
    private OnCommentActionListener actionListener;
    private Context context;
    
    public interface OnCommentActionListener {
        void onEditComment(RecipeComment comment);
        void onDeleteComment(RecipeComment comment);
        void onReplyComment(RecipeComment comment);
        void onReportComment(RecipeComment comment);
        void onVoteHelpful(RecipeComment comment, boolean isHelpful);
    }
    
    public CommentsAdapter(Context context, OnCommentActionListener actionListener) {
        this.context = context;
        this.actionListener = actionListener;
    }
    
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        RecipeComment comment = comments.get(position);
        holder.bind(comment, actionListener);
    }
    
    @Override
    public int getItemCount() {
        return comments.size();
    }
    
    public void updateComments(List<RecipeComment> newComments) {
        this.comments.clear();
        this.comments.addAll(newComments);
        notifyDataSetChanged();
    }
    
    public void addComment(RecipeComment comment) {
        comments.add(0, comment); // Ajouter en tête
        notifyItemInserted(0);
    }
    
    public void updateComment(RecipeComment updatedComment) {
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getId().equals(updatedComment.getId())) {
                comments.set(i, updatedComment);
                notifyItemChanged(i);
                break;
            }
        }
    }
    
    public void removeComment(String commentId) {
        for (int i = 0; i < comments.size(); i++) {
            if (comments.get(i).getId().equals(commentId)) {
                comments.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
    
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private ImageView userAvatar;
        private TextView userName;
        private TextView timeAgo;
        private RatingBar rating;
        private TextView content;
        private TextView helpfulCount;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private ImageButton replyButton;
        private ImageButton reportButton;
        private ImageButton helpfulButton;
        private View editedIndicator;
        private View reportedIndicator;
        private View replyIndent;
        
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            timeAgo = itemView.findViewById(R.id.timeAgo);
            rating = itemView.findViewById(R.id.rating);
            content = itemView.findViewById(R.id.content);
            helpfulCount = itemView.findViewById(R.id.helpfulCount);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            replyButton = itemView.findViewById(R.id.replyButton);
            reportButton = itemView.findViewById(R.id.reportButton);
            helpfulButton = itemView.findViewById(R.id.helpfulButton);
            editedIndicator = itemView.findViewById(R.id.editedIndicator);
            reportedIndicator = itemView.findViewById(R.id.reportedIndicator);
            replyIndent = itemView.findViewById(R.id.replyIndent);
        }
        
        public void bind(RecipeComment comment, OnCommentActionListener actionListener) {
            // Informations utilisateur
            userName.setText(comment.getUserName());
            timeAgo.setText(comment.getTimeAgo());
            
            // TODO: Charger l'avatar utilisateur
            userAvatar.setImageResource(R.drawable.appicon);
            
            // Note
            if (comment.getRating() > 0) {
                rating.setRating(comment.getRating());
                rating.setVisibility(View.VISIBLE);
            } else {
                rating.setVisibility(View.GONE);
            }
            
            // Contenu
            if (comment.getContent() != null && !comment.getContent().isEmpty()) {
                content.setText(comment.getContent());
                content.setVisibility(View.VISIBLE);
            } else {
                content.setVisibility(View.GONE);
            }
            
            // Votes utiles
            helpfulCount.setText(String.valueOf(comment.getHelpfulVotes()));
            
            // Indicateurs
            editedIndicator.setVisibility(comment.isEdited() ? View.VISIBLE : View.GONE);
            reportedIndicator.setVisibility(comment.isReported() ? View.VISIBLE : View.GONE);
            
            // Indentation pour les réponses
            if (comment.isReply()) {
                replyIndent.setVisibility(View.VISIBLE);
            } else {
                replyIndent.setVisibility(View.GONE);
            }
            
            // Boutons d'action
            setupActionButtons(comment, actionListener);
        }
        
        private void setupActionButtons(RecipeComment comment, OnCommentActionListener actionListener) {
            // Bouton modifier (visible seulement pour ses propres commentaires)
            if (canEditComment(comment)) {
                editButton.setVisibility(View.VISIBLE);
                editButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onEditComment(comment);
                    }
                });
            } else {
                editButton.setVisibility(View.GONE);
            }
            
            // Bouton supprimer
            if (canDeleteComment(comment)) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onDeleteComment(comment);
                    }
                });
            } else {
                deleteButton.setVisibility(View.GONE);
            }
            
            // Bouton répondre (pas disponible pour les réponses)
            if (!comment.isReply()) {
                replyButton.setVisibility(View.VISIBLE);
                replyButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onReplyComment(comment);
                    }
                });
            } else {
                replyButton.setVisibility(View.GONE);
            }
            
            // Bouton signaler (pas visible pour ses propres commentaires)
            if (!isOwnComment(comment)) {
                reportButton.setVisibility(View.VISIBLE);
                reportButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onReportComment(comment);
                    }
                });
            } else {
                reportButton.setVisibility(View.GONE);
            }
            
            // Bouton utile
            helpfulButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onVoteHelpful(comment, true);
                }
            });
        }
        
        private boolean canEditComment(RecipeComment comment) {
            // TODO: Vérifier si l'utilisateur peut modifier ce commentaire
            return isOwnComment(comment);
        }
        
        private boolean canDeleteComment(RecipeComment comment) {
            // TODO: Vérifier les permissions de suppression
            return isOwnComment(comment) || isCurrentUserModerator();
        }
        
        private boolean isOwnComment(RecipeComment comment) {
            // TODO: Vérifier si c'est le commentaire de l'utilisateur actuel
            return true; // Simulation
        }
        
        private boolean isCurrentUserModerator() {
            // TODO: Vérifier si l'utilisateur actuel est modérateur
            return false;
        }
    }
}
