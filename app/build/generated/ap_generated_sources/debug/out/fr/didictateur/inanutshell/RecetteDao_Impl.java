package fr.didictateur.inanutshell;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class RecetteDao_Impl implements RecetteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Recette> __insertionAdapterOfRecette;

  private final EntityDeletionOrUpdateAdapter<Recette> __deletionAdapterOfRecette;

  public RecetteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRecette = new EntityInsertionAdapter<Recette>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Recette` (`id`,`titre`,`taille`,`tempsPrep`,`ingredients`,`preparation`,`notes`,`imageResId`,`parentId`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Recette entity) {
        statement.bindLong(1, entity.id);
        if (entity.titre == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.titre);
        }
        if (entity.taille == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.taille);
        }
        if (entity.tempsPrep == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.tempsPrep);
        }
        if (entity.ingredients == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.ingredients);
        }
        if (entity.preparation == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.preparation);
        }
        if (entity.notes == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.notes);
        }
        statement.bindLong(8, entity.imageResId);
        if (entity.parentId == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.parentId);
        }
      }
    };
    this.__deletionAdapterOfRecette = new EntityDeletionOrUpdateAdapter<Recette>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Recette` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Recette entity) {
        statement.bindLong(1, entity.id);
      }
    };
  }

  @Override
  public long insert(final Recette recette) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfRecette.insertAndReturnId(recette);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final Recette recette) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfRecette.handle(recette);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Recette> getRecettesByParent(final Integer parentId) {
    final String _sql = "SELECT * FROM Recette Where parentId IS ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (parentId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, parentId);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitre = CursorUtil.getColumnIndexOrThrow(_cursor, "titre");
      final int _cursorIndexOfTaille = CursorUtil.getColumnIndexOrThrow(_cursor, "taille");
      final int _cursorIndexOfTempsPrep = CursorUtil.getColumnIndexOrThrow(_cursor, "tempsPrep");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfPreparation = CursorUtil.getColumnIndexOrThrow(_cursor, "preparation");
      final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
      final int _cursorIndexOfImageResId = CursorUtil.getColumnIndexOrThrow(_cursor, "imageResId");
      final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
      final List<Recette> _result = new ArrayList<Recette>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Recette _item;
        _item = new Recette();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTitre)) {
          _item.titre = null;
        } else {
          _item.titre = _cursor.getString(_cursorIndexOfTitre);
        }
        if (_cursor.isNull(_cursorIndexOfTaille)) {
          _item.taille = null;
        } else {
          _item.taille = _cursor.getString(_cursorIndexOfTaille);
        }
        if (_cursor.isNull(_cursorIndexOfTempsPrep)) {
          _item.tempsPrep = null;
        } else {
          _item.tempsPrep = _cursor.getString(_cursorIndexOfTempsPrep);
        }
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _item.ingredients = null;
        } else {
          _item.ingredients = _cursor.getString(_cursorIndexOfIngredients);
        }
        if (_cursor.isNull(_cursorIndexOfPreparation)) {
          _item.preparation = null;
        } else {
          _item.preparation = _cursor.getString(_cursorIndexOfPreparation);
        }
        if (_cursor.isNull(_cursorIndexOfNotes)) {
          _item.notes = null;
        } else {
          _item.notes = _cursor.getString(_cursorIndexOfNotes);
        }
        _item.imageResId = _cursor.getInt(_cursorIndexOfImageResId);
        if (_cursor.isNull(_cursorIndexOfParentId)) {
          _item.parentId = null;
        } else {
          _item.parentId = _cursor.getInt(_cursorIndexOfParentId);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Recette getRecetteById(final int id) {
    final String _sql = "SELECT * FROM Recette WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitre = CursorUtil.getColumnIndexOrThrow(_cursor, "titre");
      final int _cursorIndexOfTaille = CursorUtil.getColumnIndexOrThrow(_cursor, "taille");
      final int _cursorIndexOfTempsPrep = CursorUtil.getColumnIndexOrThrow(_cursor, "tempsPrep");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfPreparation = CursorUtil.getColumnIndexOrThrow(_cursor, "preparation");
      final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
      final int _cursorIndexOfImageResId = CursorUtil.getColumnIndexOrThrow(_cursor, "imageResId");
      final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
      final Recette _result;
      if (_cursor.moveToFirst()) {
        _result = new Recette();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTitre)) {
          _result.titre = null;
        } else {
          _result.titre = _cursor.getString(_cursorIndexOfTitre);
        }
        if (_cursor.isNull(_cursorIndexOfTaille)) {
          _result.taille = null;
        } else {
          _result.taille = _cursor.getString(_cursorIndexOfTaille);
        }
        if (_cursor.isNull(_cursorIndexOfTempsPrep)) {
          _result.tempsPrep = null;
        } else {
          _result.tempsPrep = _cursor.getString(_cursorIndexOfTempsPrep);
        }
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _result.ingredients = null;
        } else {
          _result.ingredients = _cursor.getString(_cursorIndexOfIngredients);
        }
        if (_cursor.isNull(_cursorIndexOfPreparation)) {
          _result.preparation = null;
        } else {
          _result.preparation = _cursor.getString(_cursorIndexOfPreparation);
        }
        if (_cursor.isNull(_cursorIndexOfNotes)) {
          _result.notes = null;
        } else {
          _result.notes = _cursor.getString(_cursorIndexOfNotes);
        }
        _result.imageResId = _cursor.getInt(_cursorIndexOfImageResId);
        if (_cursor.isNull(_cursorIndexOfParentId)) {
          _result.parentId = null;
        } else {
          _result.parentId = _cursor.getInt(_cursorIndexOfParentId);
        }
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<Recette> getAllRecettes() {
    final String _sql = "SELECT * FROM Recette";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTitre = CursorUtil.getColumnIndexOrThrow(_cursor, "titre");
      final int _cursorIndexOfTaille = CursorUtil.getColumnIndexOrThrow(_cursor, "taille");
      final int _cursorIndexOfTempsPrep = CursorUtil.getColumnIndexOrThrow(_cursor, "tempsPrep");
      final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
      final int _cursorIndexOfPreparation = CursorUtil.getColumnIndexOrThrow(_cursor, "preparation");
      final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
      final int _cursorIndexOfImageResId = CursorUtil.getColumnIndexOrThrow(_cursor, "imageResId");
      final int _cursorIndexOfParentId = CursorUtil.getColumnIndexOrThrow(_cursor, "parentId");
      final List<Recette> _result = new ArrayList<Recette>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Recette _item;
        _item = new Recette();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTitre)) {
          _item.titre = null;
        } else {
          _item.titre = _cursor.getString(_cursorIndexOfTitre);
        }
        if (_cursor.isNull(_cursorIndexOfTaille)) {
          _item.taille = null;
        } else {
          _item.taille = _cursor.getString(_cursorIndexOfTaille);
        }
        if (_cursor.isNull(_cursorIndexOfTempsPrep)) {
          _item.tempsPrep = null;
        } else {
          _item.tempsPrep = _cursor.getString(_cursorIndexOfTempsPrep);
        }
        if (_cursor.isNull(_cursorIndexOfIngredients)) {
          _item.ingredients = null;
        } else {
          _item.ingredients = _cursor.getString(_cursorIndexOfIngredients);
        }
        if (_cursor.isNull(_cursorIndexOfPreparation)) {
          _item.preparation = null;
        } else {
          _item.preparation = _cursor.getString(_cursorIndexOfPreparation);
        }
        if (_cursor.isNull(_cursorIndexOfNotes)) {
          _item.notes = null;
        } else {
          _item.notes = _cursor.getString(_cursorIndexOfNotes);
        }
        _item.imageResId = _cursor.getInt(_cursorIndexOfImageResId);
        if (_cursor.isNull(_cursorIndexOfParentId)) {
          _item.parentId = null;
        } else {
          _item.parentId = _cursor.getInt(_cursorIndexOfParentId);
        }
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
