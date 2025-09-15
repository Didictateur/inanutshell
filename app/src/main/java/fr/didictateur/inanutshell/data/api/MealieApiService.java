package fr.didictateur.inanutshell.data.api;

import fr.didictateur.inanutshell.data.model.Recipe;
import fr.didictateur.inanutshell.data.response.RecipeListResponse;
import fr.didictateur.inanutshell.data.response.LoginResponse;
import fr.didictateur.inanutshell.data.request.LoginRequest;

import retrofit2.Call;
import retrofit2.http.*;
import okhttp3.ResponseBody;

import java.util.List;

public interface MealieApiService {
    
    // Authentication
    @POST("api/auth/token")
    @FormUrlEncoded
    Call<LoginResponse> login(
        @Field("username") String username,
        @Field("password") String password
    );
    
    // Recipes
    @GET("api/recipes")
    Call<RecipeListResponse> getRecipes(
        @Header("Authorization") String token,
        @Query("page") int page,
        @Query("per_page") int perPage,
        @Query("order_by") String orderBy,
        @Query("order_direction") String orderDirection,
        @Query("search") String search
    );
    
    @GET("api/recipes/{slug}")
    Call<Recipe> getRecipe(
        @Header("Authorization") String token,
        @Path("slug") String slug
    );
    
    @POST("api/recipes")
    Call<ResponseBody> createRecipe(
        @Header("Authorization") String token,
        @Body Recipe recipe
    );
    
    @PUT("api/recipes/{slug}")
    Call<Recipe> updateRecipe(
        @Header("Authorization") String token,
        @Path("slug") String slug,
        @Body Recipe recipe
    );
    
    @DELETE("api/recipes/{slug}")
    Call<Void> deleteRecipe(
        @Header("Authorization") String token,
        @Path("slug") String slug
    );
    
    // Recipe Image
    @GET("api/recipes/{slug}/image")
    Call<okhttp3.ResponseBody> getRecipeImage(
        @Header("Authorization") String token,
        @Path("slug") String slug
    );
    
    // Categories
    @GET("api/categories")
    Call<List<fr.didictateur.inanutshell.data.model.Category>> getCategories(
        @Header("Authorization") String token
    );
    
    // Tags
    @GET("api/tags")
    Call<List<fr.didictateur.inanutshell.data.model.Tag>> getTags(
        @Header("Authorization") String token
    );
    
    @POST("api/tags")
    Call<fr.didictateur.inanutshell.data.model.Tag> createTag(
        @Header("Authorization") String token,
        @Body fr.didictateur.inanutshell.data.model.Tag tag
    );
    
    @POST("api/categories")
    Call<fr.didictateur.inanutshell.data.model.Category> createCategory(
        @Header("Authorization") String token,
        @Body fr.didictateur.inanutshell.data.model.Category category
    );
    
    // Tools
    @GET("api/tools")
    Call<List<fr.didictateur.inanutshell.data.model.Tool>> getTools(
        @Header("Authorization") String token
    );
}
